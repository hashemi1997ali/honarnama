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

    public function deleteInsertAll() {
        if ($this->get_request_method() != "POST") $this->response('', 406);
        $data = json_decode(file_get_contents("php://input"), true);
        if (!isset($data) || count($data) === 0 || !isset($data[0]['order_id'])) $this->responseInvalidParam();
        $isNew = isset($this->_request['is_new']) ? (int)$this->_request['is_new'] : 1;
        $orderId = (int)$data[0]['order_id'];
        $columns = array('order_id', 'product_id', 'product_name', 'amount', 'price_item', 'created_at', 'last_update');

        try {
            $response = $this->db->transaction(function ($db) use ($data, $columns, $orderId) {
                $db->execute('DELETE FROM product_order_detail WHERE order_id = :order_id', array('order_id' => $orderId));
                $result = $db->post_array($data, $columns, 'product_order_detail');
                if ($result['status'] !== 'success') {
                    throw new RuntimeException($result['msg']);
                }
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
        $response = array('status' => 'success', 'data' => array());

        foreach ($orderDetails as $detail) {
            $productId = (int)$detail['product_id'];
            $amount = (int)$detail['amount'];
            $status = array(
                'product_id' => $productId,
                'stock' => 0,
                'amount' => $amount,
                'product_name' => $detail['product_name'],
                'msg' => 'OK',
            );
            $product = $this->db->get_one(
                'SELECT stock FROM product WHERE id = :product_id LIMIT 1',
                array('product_id' => $productId)
            );

            if (empty($product)) {
                $status['msg'] = 'محصول موجود نیست';
                $response['status'] = 'failed';
            } else {
                $status['stock'] = (int)$product['stock'];
                if ((int)$product['stock'] < $amount) {
                    $status['msg'] = 'موجودی کافی نیست';
                    $response['status'] = 'failed';
                }
            }
            $response['data'][] = $status;
        }

        return $response;
    }
}
?>
