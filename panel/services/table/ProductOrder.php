<?php
require_once(realpath(dirname(__FILE__) . "/../tools/rest.php"));
require_once(realpath(dirname(__FILE__) . "/../tools/mail_handler.php"));

class ProductOrder extends REST {

    private $db = NULL;
    private $product_order_detail = NULL;
    private $mail_handler = NULL;

    public function __construct($db) {
        parent::__construct();
        $this->db = $db;
        $this->product_order_detail = new ProductOrderDetail($this->db);
        $this->mail_handler = new MailHandler($this->db);
    }

    public function findAll() {
        if ($this->get_request_method() != "GET") $this->response('', 406);
        $this->show_response($this->db->get_list(
            'SELECT po.*, COALESCE(SUM(pod.amount), 0) AS item_count ' .
            'FROM product_order po LEFT JOIN product_order_detail pod ON pod.order_id = po.id ' .
            'GROUP BY po.id ORDER BY po.id DESC'
        ));
    }

    public function findOne() {
        if ($this->get_request_method() != "GET") $this->response('', 406);
        if (!isset($this->_request['id'])) $this->responseInvalidParam();
        $this->show_response($this->findOnePlain((int)$this->_request['id']));
    }

    public function findOnePlain($id) {
        return $this->db->get_one(
            'SELECT * FROM product_order po WHERE po.id = :id',
            array('id' => (int)$id)
        );
    }

    public function findAllByPage() {
        if ($this->get_request_method() != "GET") $this->response('', 406);
        if (!isset($this->_request['limit']) || !isset($this->_request['page'])) $this->responseInvalidParam();
        $limit = min(100, max(1, (int)$this->_request['limit']));
        $page = max(1, (int)$this->_request['page']);
        $offset = ($page - 1) * $limit;
        $q = isset($this->_request['q']) ? $this->_request['q'] : '';

        $params = array();
        $where = '';
        if ($q !== '') {
            $search = '%' . $q . '%';
            $fields = array('buyer', 'code', 'address', 'email', 'phone', 'comment', 'shipping');
            $conditions = array();
            foreach ($fields as $field) {
                $parameter = 'q_' . $field;
                $conditions[] = 'po.' . $field . ' ILIKE :' . $parameter;
                $params[$parameter] = $search;
            }
            $where = ' WHERE ' . implode(' OR ', $conditions);
        }

        $query = 'SELECT po.*, COALESCE(SUM(pod.amount), 0) AS item_count ' .
            'FROM product_order po LEFT JOIN product_order_detail pod ON pod.order_id = po.id' . $where .
            " GROUP BY po.id ORDER BY po.id DESC LIMIT {$limit} OFFSET {$offset}";
        $this->show_response($this->db->get_list($query, $params));
    }

    public function allCount() {
        if ($this->get_request_method() != "GET") $this->response('', 406);
        $q = isset($this->_request['q']) ? $this->_request['q'] : '';
        $params = array();
        $where = '';
        if ($q !== '') {
            $search = '%' . $q . '%';
            $fields = array('buyer', 'code', 'address', 'email', 'phone', 'comment', 'shipping');
            $conditions = array();
            foreach ($fields as $field) {
                $parameter = 'q_' . $field;
                $conditions[] = 'po.' . $field . ' ILIKE :' . $parameter;
                $params[$parameter] = $search;
            }
            $where = ' WHERE ' . implode(' OR ', $conditions);
        }
        $this->show_response_plain($this->db->get_count(
            'SELECT COUNT(DISTINCT po.id) FROM product_order po' . $where,
            $params
        ));
    }

    public function insertOne() {
        if ($this->get_request_method() != "POST") $this->response('', 406);
        $data = json_decode(file_get_contents("php://input"), true);
        if (!isset($data)) $this->responseInvalidParam();
        $this->show_response($this->insertOnePlain($data));
    }

    public function insertOnePlain($data) {
        $columns = array('app_user_id', 'code', 'buyer', 'address', 'email', 'shipping', 'date_ship', 'phone', 'comment', 'status', 'total_fees', 'tax', 'created_at', 'last_update');
        $now = (int)round(microtime(true) * 1000);
        $data['code'] = $this->getRandomCode();
        $data['status'] = $this->normalizeStatus(isset($data['status']) ? $data['status'] : 'WAITING');
        $data['created_at'] = $now;
        $data['last_update'] = $now;
        return $this->db->post_one($data, 'id', $columns, 'product_order');
    }

    public function updateOne() {
        if ($this->get_request_method() != "POST") $this->response('', 406);
        $data = json_decode(file_get_contents("php://input"), true);
        if (!isset($data['id'])) $this->responseInvalidParam();
        $current = $this->findOnePlain((int)$data['id']);
        if (empty($current) || $current['status'] === 'PROCESSED') {
            $this->show_response(array('status' => 'failed', 'msg' => 'Processed or missing orders cannot be edited.', 'data' => null));
        }
        if (isset($data['product_order']['status'])) {
            $data['product_order']['status'] = $this->normalizeStatus($data['product_order']['status']);
            if ($data['product_order']['status'] === 'PROCESSED') {
                $this->show_response(array('status' => 'failed', 'msg' => 'Use Process Order to confirm stock changes.', 'data' => null));
            }
        }
        $data['product_order']['last_update'] = (int)round(microtime(true) * 1000);
        $columns = array('buyer', 'address', 'email', 'shipping', 'date_ship', 'phone', 'comment', 'status', 'total_fees', 'tax', 'created_at', 'last_update');
        $this->show_response($this->db->post_update((int)$data['id'], $data, 'id', $columns, 'product_order'));
    }

    public function deleteOne() {
        if ($this->get_request_method() != "GET") $this->response('', 406);
        if (!isset($this->_request['id'])) $this->responseInvalidParam();
        $order = $this->findOnePlain((int)$this->_request['id']);
        if (empty($order) || $order['status'] !== 'CANCEL') {
            $this->show_response(array('status' => 'failed', 'msg' => 'Only cancelled orders can be deleted.', 'data' => null));
        }
        $this->show_response($this->deleteOnePlain((int)$this->_request['id']));
    }

    public function deleteOnePlain($id) {
        return $this->db->delete_one($id, 'id', 'product_order');
    }

    public function countByStatusPlain($status) {
        return $this->db->get_count(
            'SELECT COUNT(DISTINCT po.id) FROM product_order po WHERE po.status = :status',
            array('status' => $status)
        );
    }

    public function processOrder() {
        if ($this->get_request_method() != "POST") $this->response('', 406);
        $data = json_decode(file_get_contents("php://input"), true);
        if (!isset($data['id'])) {
            $this->responseInvalidParam();
        }

        $orderId = (int)$data['id'];
        $response = array('status' => 'failed', 'msg' => 'Order not found.', 'data' => array());

        try {
            $processed = $this->db->transaction(function ($db) use ($orderId, &$response) {
                $lockedOrder = $db->get_one(
                    'SELECT status FROM product_order WHERE id = :order_id FOR UPDATE',
                    array('order_id' => $orderId)
                );
                if (empty($lockedOrder)) return false;
                if ($lockedOrder['status'] !== 'WAITING') {
                    $response['msg'] = 'Only waiting orders can be processed.';
                    return false;
                }

                $orderDetails = $this->product_order_detail->findAllByOrderIdPlain($orderId);
                $response = $this->product_order_detail->checkAvailableProductOrderDetail($orderDetails);
                if ($response['status'] !== 'success') return false;

                    $now = (int)round(microtime(true) * 1000);
                    foreach ($response['data'] as $detail) {
                        $updated = $db->execute(
                            "UPDATE product SET stock = stock - :amount, " .
                            "status = CASE WHEN stock - :status_amount = 0 THEN 'OUT OF STOCK' ELSE status END, " .
                            'last_update = :last_update ' .
                            "WHERE id = :product_id AND status = 'READY STOCK' AND stock >= :minimum_stock",
                            array(
                                'amount' => (int)$detail['amount'],
                                'status_amount' => (int)$detail['amount'],
                                'last_update' => $now,
                                'product_id' => (int)$detail['product_id'],
                                'minimum_stock' => (int)$detail['amount'],
                            )
                        );
                        if ($updated !== 1) {
                            throw new RuntimeException('Product stock changed while processing the order.');
                        }
                    }
                    $db->execute(
                        "UPDATE product_order SET status = 'PROCESSED', last_update = :last_update WHERE id = :order_id",
                        array('last_update' => $now, 'order_id' => $orderId)
                    );
                return true;
            });

            if ($processed) {
                $response['msg'] = 'Order processed and stock updated successfully.';
                try {
                    $this->mail_handler->sendOrderProcess($orderId);
                } catch (Throwable $exception) {
                    // Stock and order state are already committed; keep the successful response.
                    error_log('Processed order email failed: ' . $exception->getMessage());
                }
            }
        } catch (Throwable $exception) {
            error_log($exception->getMessage());
            $response['status'] = 'failed';
            $response['msg'] = 'Order could not be processed because its status or stock changed.';
        }

        $this->show_response($response);
    }

    private function getRandomCode() {
        do {
            $letters = range('A', 'Z');
            $numbers = range(0, 9);
            $code = $letters[array_rand($letters)] . $letters[array_rand($letters)];
            for ($i = 0; $i < 5; $i++) {
                $code .= $numbers[array_rand($numbers)];
            }
            $code .= $letters[array_rand($letters)] . $letters[array_rand($letters)];
            $exists = $this->db->get_count(
                'SELECT COUNT(*) FROM product_order WHERE code = :code',
                array('code' => $code)
            );
        } while ($exists > 0);

        return $code;
    }

    private function normalizeStatus($status) {
        $status = trim((string)$status);
        $status = strtoupper($status);
        return in_array($status, array('WAITING', 'PROCESSED', 'CANCEL'), true)
            ? $status
            : 'WAITING';
    }
}
?>
