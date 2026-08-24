<?php
require_once(realpath(dirname(__FILE__) . "/../tools/rest.php"));

class Category extends REST {

    private $db = NULL;

    public function __construct($db) {
        parent::__construct();
        $this->db = $db;
    }

    public function findAll() {
        if ($this->get_request_method() != "GET") $this->response('', 406);
        $this->show_response($this->db->get_list('SELECT * FROM category c ORDER BY c.priority ASC'));
    }

    public function findAllForClient() {
        return $this->db->get_list('SELECT * FROM category c WHERE c.draft = 0 ORDER BY c.priority ASC');
    }

    public function findOne() {
        if ($this->get_request_method() != "GET") $this->response('', 406);
        if (!isset($this->_request['id'])) $this->responseInvalidParam();
        $this->show_response($this->db->get_one(
            'SELECT DISTINCT * FROM category c WHERE c.id = :id',
            array('id' => (int)$this->_request['id'])
        ));
    }

    public function allCountPlain($q, $client) {
        list($where, $params) = $this->buildFilters($q, $client);
        return $this->db->get_count('SELECT COUNT(DISTINCT c.id) FROM category c ' . $where, $params);
    }

    public function allCount() {
        if ($this->get_request_method() != "GET") $this->response('', 406);
        $q = isset($this->_request['q']) ? $this->_request['q'] : '';
        $client = isset($this->_request['client']) ? (int)$this->_request['client'] : 0;
        $this->show_response_plain($this->allCountPlain($q, $client));
    }

    public function findAllByPagePlain($limit, $offset, $q, $client) {
        list($where, $params) = $this->buildFilters($q, $client);
        $limit = $this->safeLimit($limit);
        $offset = max(0, (int)$offset);
        return $this->db->get_list(
            'SELECT c.* FROM category c ' . $where . " ORDER BY c.id DESC LIMIT {$limit} OFFSET {$offset}",
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
        $client = isset($this->_request['client']) ? (int)$this->_request['client'] : 0;
        $this->show_response($this->findAllByPagePlain($limit, $offset, $q, $client));
    }

    public function insertOne() {
        if ($this->get_request_method() != "POST") $this->response('', 406);
        $data = json_decode(file_get_contents("php://input"), true);
        if (!isset($data)) $this->responseInvalidParam();
        $columns = array('name', 'icon', 'draft', 'brief', 'color', 'priority', 'created_at', 'last_update');
        $this->show_response($this->db->post_one($data, 'id', $columns, 'category'));
    }

    public function updateOne() {
        if ($this->get_request_method() != "POST") $this->response('', 406);
        $data = json_decode(file_get_contents("php://input"), true);
        if (!isset($data['id'])) $this->responseInvalidParam();
        $columns = array('name', 'icon', 'draft', 'brief', 'color', 'priority', 'created_at', 'last_update');
        $this->show_response($this->db->post_update((int)$data['id'], $data, 'id', $columns, 'category'));
    }

    public function deleteOne() {
        if ($this->get_request_method() != "GET") $this->response('', 406);
        if (!isset($this->_request['id'])) $this->responseInvalidParam();
        $this->show_response($this->db->delete_one((int)$this->_request['id'], 'id', 'category'));
    }

    public function getAllByProductIdPlain($product_id) {
        return $this->db->get_list(
            'SELECT DISTINCT c.* FROM category c WHERE c.id IN '
            . '(SELECT pc.category_id FROM product_category pc WHERE pc.product_id = :product_id)',
            array('product_id' => (int)$product_id)
        );
    }

    public function getAllByProductId() {
        if ($this->get_request_method() != "GET") $this->response('', 406);
        if (!isset($this->_request['product_id'])) $this->responseInvalidParam();
        $this->show_response($this->getAllByProductIdPlain((int)$this->_request['product_id']));
    }

    public function countByDraftPlain($draft) {
        return $this->db->get_count(
            'SELECT COUNT(DISTINCT c.id) FROM category c WHERE c.draft = :draft',
            array('draft' => (int)$draft)
        );
    }

    private function buildFilters($q, $client) {
        $conditions = array();
        $params = array();
        if ((int)$client !== 0) {
            $conditions[] = 'c.draft = 0';
        }
        if ($q !== '') {
            $search = '%' . $q . '%';
            $conditions[] = '(c.name ILIKE :q_name OR c.brief ILIKE :q_brief)';
            $params['q_name'] = $search;
            $params['q_brief'] = $search;
        }
        return array(empty($conditions) ? '' : 'WHERE ' . implode(' AND ', $conditions), $params);
    }

    private function safeLimit($limit) {
        return min(100, max(1, (int)$limit));
    }
}
?>
