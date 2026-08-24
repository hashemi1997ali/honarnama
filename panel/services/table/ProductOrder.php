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
        $this->show_response($this->db->get_list('SELECT * FROM product_order po ORDER BY po.id DESC'));
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

        $query = 'SELECT DISTINCT * FROM product_order po' . $where .
            " ORDER BY po.id DESC LIMIT {$limit} OFFSET {$offset}";
        $this->show_response($this->db->get_list($query, $params));
    }

    public function allCount() {
        if ($this->get_request_method() != "GET") $this->response('', 406);
        $this->show_response_plain($this->db->get_count('SELECT COUNT(DISTINCT po.id) FROM product_order po'));
    }

    public function insertOne() {
        if ($this->get_request_method() != "POST") $this->response('', 406);
        $data = json_decode(file_get_contents("php://input"), true);
        if (!isset($data)) $this->responseInvalidParam();
        $this->show_response($this->insertOnePlain($data));
    }

    public function insertOnePlain($data) {
        $columns = array('code', 'buyer', 'address', 'email', 'shipping', 'date_ship', 'phone', 'comment', 'status', 'total_fees', 'tax', 'created_at', 'last_update');
        $data['code'] = $this->getRandomCode();
        $data['status'] = $this->normalizeStatus(isset($data['status']) ? $data['status'] : 'WAITING');
        return $this->db->post_one($data, 'id', $columns, 'product_order');
    }

    public function updateOne() {
        if ($this->get_request_method() != "POST") $this->response('', 406);
        $data = json_decode(file_get_contents("php://input"), true);
        if (!isset($data['id'])) $this->responseInvalidParam();
        if (isset($data['product_order']['status'])) {
            $data['product_order']['status'] = $this->normalizeStatus($data['product_order']['status']);
        }
        $columns = array('buyer', 'address', 'email', 'shipping', 'date_ship', 'phone', 'comment', 'status', 'total_fees', 'tax', 'created_at', 'last_update');
        $this->show_response($this->db->post_update((int)$data['id'], $data, 'id', $columns, 'product_order'));
    }

    public function deleteOne() {
        if ($this->get_request_method() != "GET") $this->response('', 406);
        if (!isset($this->_request['id'])) $this->responseInvalidParam();
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
        if (!isset($data['id'], $data['product_order'], $data['product_order_detail'])) {
            $this->responseInvalidParam();
        }

        $order = $data['product_order'];
        $orderDetails = $data['product_order_detail'];
        $response = $this->product_order_detail->checkAvailableProductOrderDetail($orderDetails);

        if ($response['status'] === 'success') {
            try {
                $this->db->transaction(function ($db) use ($order, $orderDetails) {
                    foreach ($orderDetails as $detail) {
                        $updated = $db->execute(
                            'UPDATE product SET stock = stock - :amount WHERE id = :product_id AND stock >= :minimum_stock',
                            array(
                                'amount' => (int)$detail['amount'],
                                'product_id' => (int)$detail['product_id'],
                                'minimum_stock' => (int)$detail['amount'],
                            )
                        );
                        if ($updated !== 1) {
                            throw new RuntimeException('Product stock changed while processing the order.');
                        }
                    }
                    $db->execute(
                        "UPDATE product_order SET status = 'PROCESSED' WHERE id = :order_id",
                        array('order_id' => (int)$order['id'])
                    );
                });
                $this->mail_handler->sendOrderProcess((int)$order['id']);
            } catch (Throwable $exception) {
                error_log($exception->getMessage());
                $response['status'] = 'failed';
                $response['msg'] = 'Order could not be processed because stock changed.';
            }
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
        $legacyStatuses = array(
            'در انتظار تایید' => 'WAITING',
            'پردازش شده' => 'PROCESSED',
            'لغو شده' => 'CANCEL',
        );
        $status = trim((string)$status);
        if (isset($legacyStatuses[$status])) {
            return $legacyStatuses[$status];
        }

        $status = strtoupper($status);
        return in_array($status, array('WAITING', 'PROCESSED', 'CANCEL'), true)
            ? $status
            : 'WAITING';
    }
}
?>
