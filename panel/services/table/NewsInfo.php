<?php
require_once(realpath(dirname(__FILE__) . "/../tools/rest.php"));

class NewsInfo extends REST {

    private $db = NULL;

    public function __construct($db) {
        parent::__construct();
        $this->db = $db;
    }

    public function findAll() {
        if ($this->get_request_method() != "GET") $this->response('', 406);
        $this->show_response($this->findAllPlain());
    }

    public function findAllPlain() {
        return $this->db->get_list('SELECT * FROM news_info ni ORDER BY ni.id DESC');
    }

    public function findOnePlain($id) {
        return $this->db->get_one(
            'SELECT DISTINCT * FROM news_info ni WHERE ni.id = :id',
            array('id' => (int)$id)
        );
    }

    public function findOne() {
        if ($this->get_request_method() != "GET") $this->response('', 406);
        if (!isset($this->_request['id'])) $this->responseInvalidParam();
        $this->show_response($this->findOnePlain((int)$this->_request['id']));
    }

    public function allCountPlain($q, $client) {
        list($where, $params) = $this->buildFilters($q, $client);
        return $this->db->get_count('SELECT COUNT(DISTINCT ni.id) FROM news_info ni ' . $where, $params);
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
            'SELECT ni.* FROM news_info ni ' . $where . " ORDER BY ni.id DESC LIMIT {$limit} OFFSET {$offset}",
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
        if ($data['status'] == 'FEATURED' && (int)$data['draft'] === 0 && $this->isFeaturedExceed() === 1) {
            $this->show_response(array('status' => 'failed', 'msg' => 'Featured News exceed the maximum amount', 'data' => null));
        }
        $columns = array('title', 'brief_content', 'full_content', 'image', 'draft', 'status', 'created_at', 'last_update');
        $this->show_response($this->db->post_one($data, 'id', $columns, 'news_info'));
    }

    public function updateOne() {
        if ($this->get_request_method() != "POST") $this->response('', 406);
        $data = json_decode(file_get_contents("php://input"), true);
        if (!isset($data['id'], $data['news_info'])) $this->responseInvalidParam();
        $news = $data['news_info'];
        if ($news['status'] == 'FEATURED' && (int)$news['draft'] === 0 && $this->isFeaturedExceed() === 1) {
            $current = $this->findOnePlain((int)$data['id']);
            if (!isset($current['status']) || $current['status'] !== 'FEATURED' || (int)$current['draft'] !== 0) {
                $this->show_response(array('status' => 'failed', 'msg' => 'Featured News exceed the maximum amount', 'data' => null));
            }
        }
        if ($news['status'] == 'NORMAL' && $this->countFeaturedPlain() <= 1) {
            $current = $this->findOnePlain((int)$data['id']);
            if (isset($current['status']) && $current['status'] === 'FEATURED' && (int)$current['draft'] === 0) {
                $this->show_response(array('status' => 'failed', 'msg' => 'At least one FEATURED news item is required', 'data' => null));
            }
        }
        $columns = array('title', 'brief_content', 'full_content', 'image', 'draft', 'status', 'created_at', 'last_update');
        $this->show_response($this->db->post_update((int)$data['id'], $data, 'id', $columns, 'news_info'));
    }

    public function deleteOne() {
        if ($this->get_request_method() != "GET") $this->response('', 406);
        if (!isset($this->_request['id'])) $this->responseInvalidParam();
        $id = (int)$this->_request['id'];
        $data = $this->findOnePlain($id);
        if (isset($data['status']) && $data['status'] === 'FEATURED' && (int)$data['draft'] === 0 && $this->countFeaturedPlain() <= 1) {
            $this->show_response(array('status' => 'failed', 'msg' => 'At least one FEATURED news item is required', 'data' => null));
        }
        $this->show_response($this->db->delete_one($id, 'id', 'news_info'));
    }

    public function findAllFeatured() {
        if ($this->get_request_method() != "GET") $this->response('', 406);
        return $this->db->get_list("SELECT * FROM news_info ni WHERE ni.status = 'FEATURED' AND ni.draft = 0 ORDER BY ni.id DESC");
    }

    public function countByDraftPlain($draft) {
        return $this->db->get_count(
            'SELECT COUNT(DISTINCT ni.id) FROM news_info ni WHERE ni.draft = :draft',
            array('draft' => (int)$draft)
        );
    }

    public function countFeaturedPlain() {
        return $this->db->get_count("SELECT COUNT(DISTINCT ni.id) FROM news_info ni WHERE ni.status = 'FEATURED' AND ni.draft = 0");
    }

    public function isFeaturedExceed() {
        $result = $this->db->get_one(
            "SELECT CASE WHEN COUNT(*) >= COALESCE((SELECT value::integer FROM config WHERE code = 'FEATURED_NEWS'), 5) " .
            "THEN 1 ELSE 0 END AS resp FROM news_info WHERE status = 'FEATURED' AND draft = 0"
        );
        return isset($result['resp']) ? (int)$result['resp'] : 0;
    }

    public function isFeaturedNewsExceed() {
        $this->show_response_plain($this->isFeaturedExceed());
    }

    private function buildFilters($q, $client) {
        $conditions = array();
        $params = array();
        if ((int)$client !== 0) {
            $conditions[] = 'ni.draft = 0';
        }
        if ($q !== '') {
            $search = '%' . $q . '%';
            $conditions[] = '(ni.title ILIKE :q_title OR ni.brief_content ILIKE :q_brief OR ni.full_content ILIKE :q_full)';
            $params['q_title'] = $search;
            $params['q_brief'] = $search;
            $params['q_full'] = $search;
        }
        return array(empty($conditions) ? '' : 'WHERE ' . implode(' AND ', $conditions), $params);
    }

    private function safeLimit($limit) {
        return min(100, max(1, (int)$limit));
    }
}
?>
