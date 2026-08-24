<?php
require_once(realpath(dirname(__FILE__) . "/../tools/rest.php"));

class ProductAuction extends REST {

    private $db = NULL;

    public function __construct($db) {
        parent::__construct();
        $this->db = $db;
    }

    public function findAll() {
        if ($this->get_request_method() != "GET") $this->response('', 406);
        $this->show_response($this->db->get_list('SELECT * FROM product_auction pa ORDER BY pa.id DESC'));
    }

    public function findOnePlain($id) {
        return $this->db->get_one(
            'SELECT * FROM product_auction pa WHERE pa.id = :id LIMIT 1',
            array('id' => (int)$id)
        );
    }

    public function findOne() {
        if ($this->get_request_method() != "GET") $this->response('', 406);
        if (!isset($this->_request['id'])) $this->responseInvalidParam();
        $this->show_response($this->findOnePlain((int)$this->_request['id']));
    }

    public function allCountPlainForClient($q) {
        list($where, $params) = $this->searchFilter($q);
        return $this->db->get_count('SELECT COUNT(DISTINCT pa.id) FROM product_auction pa ' . $where, $params);
    }

    public function findAllByPagePlainForClient($limit, $offset, $q) {
        list($where, $params) = $this->searchFilter($q);
        $limit = min(100, max(1, (int)$limit));
        $offset = max(0, (int)$offset);
        return $this->db->get_list(
            'SELECT DISTINCT pa.* FROM product_auction pa ' . $where .
            " ORDER BY pa.id DESC LIMIT {$limit} OFFSET {$offset}",
            $params
        );
    }

    public function updateOne() {
        if ($this->get_request_method() != "POST") $this->response('', 406);
        $data = json_decode(file_get_contents("php://input"), true);
        $auctionId = isset($data['product_auction_id']) ? (int)$data['product_auction_id'] : 0;
        $userId = isset($data['user_id']) ? (int)$data['user_id'] : 0;
        $bidPrice = isset($data['bid_price']) ? (float)$data['bid_price'] : 0;
        if ($auctionId < 1 || $userId < 1 || $bidPrice <= 0) {
            $this->show_response(array('status' => 'failed', 'msg' => 'Auction not found.'));
        }

        $auction = $this->findOnePlain($auctionId);
        if (empty($auction)) {
            $this->show_response(array('status' => 'failed', 'msg' => 'Auction not found.'));
        }

        $now = time();
        $start = strtotime($auction['start_date']);
        $end = strtotime($auction['end_date']);
        if ($start !== false && $now < $start) {
            $this->show_response(array('status' => 'failed', 'msg' => 'This auction has not started yet.'));
        }
        if ($end !== false && $now > $end) {
            $this->show_response(array('status' => 'failed', 'msg' => 'This auction has ended.'));
        }

        if ($auction['winner_price'] === null) {
            if ($bidPrice < (float)$auction['start_price']) {
                $this->show_response(array('status' => 'failed', 'msg' => 'Your bid is below the starting price.'));
            }
        } else {
            if ($userId === (int)$auction['winner_id']) {
                $this->show_response(array('status' => 'failed', 'msg' => 'You already placed the latest bid.'));
            }
            if ($bidPrice <= (float)$auction['winner_price']) {
                $this->show_response(array('status' => 'failed', 'msg' => 'Your bid must be higher than the latest bid.'));
            }
        }

        $user = $this->db->get_one(
            'SELECT username FROM app_user WHERE id = :id LIMIT 1',
            array('id' => $userId)
        );
        if (empty($user)) {
            $this->show_response(array('status' => 'failed', 'msg' => 'User not found.'));
        }

        $wrapper = array('product_auction' => array(
            'winner_id' => $userId,
            'winner_username' => $user['username'],
            'winner_price' => $bidPrice,
            'last_update' => date(DATE_ATOM),
        ));
        $response = $this->db->post_update(
            $auctionId,
            $wrapper,
            'id',
            array('winner_id', 'winner_username', 'winner_price', 'last_update'),
            'product_auction'
        );
        if ($response['status'] !== 'success') {
            $this->show_response($response);
        }
    }

    private function searchFilter($q) {
        if ($q === '') return array('', array());
        $search = '%' . $q . '%';
        return array(
            'WHERE pa.name ILIKE :q_name OR pa.description ILIKE :q_description',
            array('q_name' => $search, 'q_description' => $search),
        );
    }
}
?>
