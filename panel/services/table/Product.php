<?php
require_once(realpath(dirname(__FILE__) . "/../tools/rest.php"));

class Product extends REST {

    private $db = NULL;

    public function __construct($db) {
        parent::__construct();
        $this->db = $db;
    }

    public function findAll() {
        if ($this->get_request_method() != "GET") $this->response('', 406);
        $this->show_response($this->db->get_list('SELECT * FROM product p ORDER BY p.id DESC'));
    }

    public function findOnePlain($id) {
        return $this->db->get_one(
            'SELECT * FROM product p WHERE p.id = :id LIMIT 1',
            array('id' => (int)$id)
        );
    }

    public function findOne() {
        if ($this->get_request_method() != "GET") $this->response('', 406);
        if (!isset($this->_request['id'])) $this->responseInvalidParam();
        $this->show_response($this->findOnePlain((int)$this->_request['id']));
    }

    public function allCountPlain($q, $category_id) {
        list($where, $params, $join) = $this->buildFilters($q, $category_id, false);
        return $this->db->get_count(
            'SELECT COUNT(DISTINCT p.id) FROM product p ' . $join . $where,
            $params
        );
    }

    public function allCountPlainForClient($q, $category_id) {
        list($where, $params, $join) = $this->buildFilters($q, $category_id, true);
        return $this->db->get_count(
            'SELECT COUNT(DISTINCT p.id) FROM product p ' . $join . $where,
            $params
        );
    }

    public function allCount() {
        if ($this->get_request_method() != "GET") $this->response('', 406);
        $q = isset($this->_request['q']) ? $this->_request['q'] : '';
        $category_id = isset($this->_request['category_id']) ? (int)$this->_request['category_id'] : -1;
        $this->show_response_plain($this->allCountPlain($q, $category_id));
    }

    public function findAllByPagePlain($limit, $offset, $q, $category_id) {
        list($where, $params, $join) = $this->buildFilters($q, $category_id, false);
        $limit = $this->safeLimit($limit);
        $offset = max(0, (int)$offset);
        return $this->db->get_list(
            'SELECT DISTINCT p.* FROM product p ' . $join . $where .
            " ORDER BY p.id DESC LIMIT {$limit} OFFSET {$offset}",
            $params
        );
    }

    public function findAllByPagePlainForClient($limit, $offset, $q, $category_id) {
        list($where, $params, $join) = $this->buildFilters($q, $category_id, true);
        $limit = $this->safeLimit($limit);
        $offset = max(0, (int)$offset);
        return $this->db->get_list(
            'SELECT DISTINCT p.* FROM product p ' . $join . $where .
            " ORDER BY p.id DESC LIMIT {$limit} OFFSET {$offset}",
            $params
        );
    }

    public function findAllByPage() {
        if ($this->get_request_method() != "GET") $this->response('', 406);
        if (!isset($this->_request['limit']) || !isset($this->_request['page'])) $this->responseInvalidParam();
        $limit = $this->safeLimit($this->_request['limit']);
        $page = max(1, (int)$this->_request['page']);
        $offset = ($page - 1) * $limit;
        $q = isset($this->_request['q']) ? $this->_request['q'] : '';
        $category_id = isset($this->_request['category_id']) ? (int)$this->_request['category_id'] : -1;
        $this->show_response($this->findAllByPagePlain($limit, $offset, $q, $category_id));
    }

    public function insertOne() {
        if ($this->get_request_method() != "POST") $this->response('', 406);
        $data = json_decode(file_get_contents("php://input"), true);
        if (!is_array($data)) $this->responseInvalidParam();
        try {
            $data = $this->prepareData($data);
            $columns = array('name', 'image', 'price', 'price_discount', 'stock', 'draft', 'description', 'status', 'created_at', 'last_update');
            $this->show_response($this->db->post_one($data, 'id', $columns, 'product'));
        } catch (InvalidArgumentException $exception) {
            $this->show_response(array('status' => 'failed', 'msg' => $exception->getMessage(), 'data' => null));
        }
    }

    public function updateOne() {
        if ($this->get_request_method() != "POST") $this->response('', 406);
        $data = json_decode(file_get_contents("php://input"), true);
        if (!isset($data['id'], $data['product']) || !is_array($data['product'])) $this->responseInvalidParam();
        try {
            $data['product'] = $this->prepareData($data['product']);
            $columns = array('name', 'image', 'price', 'price_discount', 'stock', 'draft', 'description', 'status', 'created_at', 'last_update');
            $this->show_response($this->db->post_update((int)$data['id'], $data, 'id', $columns, 'product'));
        } catch (InvalidArgumentException $exception) {
            $this->show_response(array('status' => 'failed', 'msg' => $exception->getMessage(), 'data' => null));
        }
    }

    public function deleteOne() {
        if ($this->get_request_method() != "GET") $this->response('', 406);
        if (!isset($this->_request['id'])) $this->responseInvalidParam();
        $this->show_response($this->db->delete_one((int)$this->_request['id'], 'id', 'product'));
    }

    public function countByDraftPlain($draft) {
        return $this->db->get_count(
            'SELECT COUNT(DISTINCT p.id) FROM product p WHERE p.draft = :draft',
            array('draft' => (int)$draft)
        );
    }

    public function countByStatusPlain($status) {
        return $this->db->get_count(
            'SELECT COUNT(DISTINCT p.id) FROM product p WHERE p.status = :status',
            array('status' => $status)
        );
    }

    private function buildFilters($q, $category_id, $clientOnly) {
        $conditions = array();
        $params = array();
        $join = '';

        if ($clientOnly) {
            $conditions[] = 'p.draft = 0';
        }
        if ((int)$category_id !== -1) {
            $join = 'INNER JOIN product_category pc ON pc.product_id = p.id ';
            $conditions[] = 'pc.category_id = :category_id';
            $params['category_id'] = (int)$category_id;
        }
        if ($q !== '') {
            $search = '%' . $q . '%';
            $conditions[] = '(p.name ILIKE :q_name OR p.status ILIKE :q_status OR p.description ILIKE :q_description)';
            $params['q_name'] = $search;
            $params['q_status'] = $search;
            $params['q_description'] = $search;
        }

        $where = empty($conditions) ? '' : 'WHERE ' . implode(' AND ', $conditions);
        return array($where, $params, $join);
    }

    private function safeLimit($limit) {
        return min(100, max(1, (int)$limit));
    }

    private function prepareData($data) {
        $name = isset($data['name']) ? trim((string)$data['name']) : '';
        $price = isset($data['price']) && is_numeric($data['price']) ? (float)$data['price'] : -1;
        $discountPrice = isset($data['price_discount']) && is_numeric($data['price_discount'])
            ? (float)$data['price_discount']
            : 0;
        $stockValue = isset($data['stock']) && is_numeric($data['stock']) ? (float)$data['stock'] : -1;
        $stock = (int)$stockValue;

        if ($name === '') throw new InvalidArgumentException('Product name is required.');
        if ($price < 0) throw new InvalidArgumentException('Price must be zero or greater.');
        if ($discountPrice < 0 || ($discountPrice > 0 && $discountPrice > $price)) {
            throw new InvalidArgumentException('Discounted price must be between zero and the regular price.');
        }
        if ((float)$stock !== $stockValue) {
            throw new InvalidArgumentException('Stock must be a whole number.');
        }
        if ($stock < 0) throw new InvalidArgumentException('Stock must be a whole number of zero or greater.');

        $status = strtoupper(trim(isset($data['status']) ? (string)$data['status'] : 'READY STOCK'));
        if (!in_array($status, array('READY STOCK', 'OUT OF STOCK', 'SUSPEND'), true)) {
            throw new InvalidArgumentException('Invalid product status.');
        }
        if ($stock === 0 && $status === 'READY STOCK') {
            $status = 'OUT OF STOCK';
        }

        $data['name'] = $name;
        $data['price'] = $price;
        $data['price_discount'] = $discountPrice;
        $data['stock'] = $stock;
        $data['draft'] = !empty($data['draft']) ? 1 : 0;
        $data['status'] = $status;
        $data['description'] = isset($data['description']) ? (string)$data['description'] : '';
        return $data;
    }
}
?>
