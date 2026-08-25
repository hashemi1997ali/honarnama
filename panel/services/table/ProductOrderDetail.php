<?php
require_once(realpath(dirname(__FILE__) . "/../tools/rest.php"));
require_once(realpath(dirname(__FILE__) . "/../tools/mail_handler.php"));

class ProductOrderDetail extends REST {

    private $db = NULL;
    private $mail_handler = NULL;

    public function __construct($db) {
        parent::__construct();
        $this->db = $db;
        $this->mail_handler = new MailHandler($this->db);
    }

    public function findAll() {
        if ($this->get_request_method() != "GET") $this->response('', 406);
        $this->show_response($this->db->get_list('SELECT * FROM product_order_detail pod'));
    }

    public function insertAllPlain($order_id, $data) {
        foreach ($data as $index => $detail) {
            $data[$index]['order_id'] = (int)$order_id;
        }
        $columns = array('order_id', 'product_id', 'product_name', 'amount', 'price_item', 'created_at', 'last_update');
        return $this->db->post_array($data, $columns, 'product_order_detail');
    }

    public function replaceAllPlain($orderId, $data) {
        $columns = array('order_id', 'product_id', 'product_name', 'amount', 'price_item', 'created_at', 'last_update');
        $this->db->execute('DELETE FROM product_order_detail WHERE order_id = :order_id', array('order_id' => (int)$orderId));
        foreach ($data as $index => $detail) {
            $data[$index]['order_id'] = (int)$orderId;
        }
        return $this->db->post_array($data, $columns, 'product_order_detail');
    }

    public function deleteInsertAll() {
        if ($this->get_request_method() != "POST") $this->response('', 406);
        $data = json_decode(file_get_contents("php://input"), true);
        if (!isset($data) || count($data) === 0 || !isset($data[0]['order_id'])) $this->responseInvalidParam();
        $isNew = isset($this->_request['is_new']) ? (int)$this->_request['is_new'] : 1;
        $orderId = (int)$data[0]['order_id'];

        $order = $this->db->get_one(
            'SELECT status FROM product_order WHERE id = :order_id LIMIT 1',
            array('order_id' => $orderId)
        );
        if (empty($order) || $order['status'] === 'PROCESSED') {
            $this->show_response(array('status' => 'failed', 'msg' => 'Processed or missing orders cannot be edited.', 'data' => null));
        }

        $validation = $this->validateAndNormalizeForSubmission($data, true);
        if ($validation['status'] !== 'success') {
            $this->show_response(array('status' => 'failed', 'msg' => $validation['msg'], 'data' => $validation['data']));
        }
        $normalizedDetails = $validation['details'];

        try {
            $response = $this->db->transaction(function ($db) use ($normalizedDetails, $orderId) {
                $result = $this->replaceAllPlain($orderId, $normalizedDetails);
                if ($result['status'] !== 'success') {
                    throw new RuntimeException($result['msg']);
                }
                $db->execute(
                    'UPDATE product_order po SET total_fees = totals.subtotal * (1 + po.tax / 100), ' .
                    'last_update = :last_update FROM (' .
                    'SELECT order_id, COALESCE(SUM(amount * price_item), 0) AS subtotal ' .
                    'FROM product_order_detail WHERE order_id = :detail_order_id GROUP BY order_id' .
                    ') totals WHERE po.id = totals.order_id AND po.id = :order_id',
                    array(
                        'detail_order_id' => $orderId,
                        'last_update' => (int)round(microtime(true) * 1000),
                        'order_id' => $orderId,
                    )
                );
                return $result;
            });
        } catch (Throwable $exception) {
            error_log($exception->getMessage());
            $response = array('status' => 'failed', 'msg' => 'Order details could not be updated', 'data' => $data);
        }

        if ($response['status'] === 'success' && $isNew === 1) {
            $this->mail_handler->sendNewOrder($orderId);
        }
        $this->show_response($response);
    }

    public function findAllByOrderIdPlain($order_id) {
        return $this->db->get_list(
            'SELECT DISTINCT * FROM product_order_detail pod WHERE pod.order_id = :order_id',
            array('order_id' => (int)$order_id)
        );
    }

    public function findAllByOrderId() {
        if ($this->get_request_method() != "GET") $this->response('', 406);
        if (!isset($this->_request['order_id'])) $this->responseInvalidParam();
        $this->show_response($this->findAllByOrderIdPlain((int)$this->_request['order_id']));
    }

    public function checkAvailableProductOrderDetail($orderDetails) {
        $validation = $this->validateAndNormalizeForSubmission($orderDetails, false);
        unset($validation['details']);
        return $validation;
    }

    public function validateAndNormalizeForSubmission($orderDetails, $requirePublished = true) {
        $response = array(
            'status' => 'success',
            'msg' => 'All products are available.',
            'data' => array(),
            'details' => array(),
        );
        if (!is_array($orderDetails) || count($orderDetails) === 0) {
            $response['status'] = 'failed';
            $response['msg'] = 'The order must contain at least one product.';
            return $response;
        }

        $requested = array();
        foreach ($orderDetails as $detail) {
            $productId = isset($detail['product_id']) ? (int)$detail['product_id'] : 0;
            $amount = isset($detail['amount']) ? (int)$detail['amount'] : 0;
            if ($productId <= 0 || $amount <= 0) {
                $response['status'] = 'failed';
                $response['msg'] = 'Every order item must have a valid product and positive quantity.';
                continue;
            }
            if (!isset($requested[$productId])) {
                $requested[$productId] = array('amount' => 0, 'source' => $detail);
            }
            $requested[$productId]['amount'] += $amount;
        }

        $now = (int)round(microtime(true) * 1000);
        foreach ($requested as $productId => $request) {
            $amount = (int)$request['amount'];
            $product = $this->db->get_one(
                'SELECT id, name, image, price, price_discount, stock, draft, status ' .
                'FROM product WHERE id = :product_id LIMIT 1',
                array('product_id' => (int)$productId)
            );

            $itemStatus = array(
                'product_id' => (int)$productId,
                'product_name' => isset($request['source']['product_name']) ? $request['source']['product_name'] : '',
                'image' => '',
                'stock' => 0,
                'amount' => $amount,
                'price_item' => 0,
                'available' => false,
                'msg' => 'Product not found',
            );

            if (!empty($product)) {
                $effectivePrice = (float)$product['price_discount'] > 0
                    && (float)$product['price_discount'] <= (float)$product['price']
                    ? (float)$product['price_discount']
                    : (float)$product['price'];
                $itemStatus['product_name'] = $product['name'];
                $itemStatus['image'] = $product['image'];
                $itemStatus['stock'] = (int)$product['stock'];
                $itemStatus['price_item'] = $effectivePrice;

                if ($requirePublished && (int)$product['draft'] !== 0) {
                    $itemStatus['msg'] = 'Product is not published';
                } elseif (strtoupper((string)$product['status']) !== 'READY STOCK' || (int)$product['stock'] <= 0) {
                    $itemStatus['msg'] = 'Product is out of stock';
                } elseif ((int)$product['stock'] < $amount) {
                    $itemStatus['msg'] = 'Only ' . (int)$product['stock'] . ' item(s) are available';
                } else {
                    $itemStatus['available'] = true;
                    $itemStatus['msg'] = 'OK';
                    $source = $request['source'];
                    $response['details'][] = array(
                        'product_id' => (int)$productId,
                        'product_name' => $product['name'],
                        'amount' => $amount,
                        'price_item' => $effectivePrice,
                        'created_at' => isset($source['created_at']) && is_numeric($source['created_at'])
                            ? (int)$source['created_at']
                            : $now,
                        'last_update' => $now,
                    );
                }
            }

            if (!$itemStatus['available']) {
                $response['status'] = 'failed';
                $response['msg'] = 'One or more products are unavailable.';
            }
            $response['data'][] = $itemStatus;
        }

        if (count($requested) === 0) {
            $response['status'] = 'failed';
        }
        return $response;
    }
}
?>
