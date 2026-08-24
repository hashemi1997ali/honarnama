<?php
require_once(realpath(dirname(__FILE__) . "/../tools/rest.php"));
require_once(realpath(dirname(__FILE__) . "/ProductAuction.php"));

class Bid extends REST {

    private $db = NULL;
    private $product_auction = NULL;

    public function __construct($db) {
        parent::__construct();
        $this->db = $db;
        $this->product_auction = new ProductAuction($this->db);
    }

    public function findBid() {
        $data = json_decode(file_get_contents("php://input"), true);
        $auctionId = isset($data['product_auction_id']) ? (int)$data['product_auction_id'] : 0;
        $userId = isset($data['user_id']) ? (int)$data['user_id'] : 0;
        if ($auctionId < 1 || $userId < 1 || empty($this->product_auction->findOnePlain($auctionId))) {
            $this->show_response(array('status' => 'failed', 'msg' => 'Invalid bid.'));
        }
        $bid = $this->db->get_one(
            'SELECT id FROM bid WHERE product_auction_id = :auction_id AND user_id = :user_id LIMIT 1',
            array('auction_id' => $auctionId, 'user_id' => $userId)
        );
        return !empty($bid);
    }

    public function insertOne() {
        if ($this->get_request_method() != "POST") $this->response('', 406);
        $data = json_decode(file_get_contents("php://input"), true);
        if (!isset($data['product_auction_id'], $data['user_id'], $data['bid_price'])) $this->responseInvalidParam();
        $this->show_response($this->db->post_one(
            $data,
            'id',
            array('product_auction_id', 'user_id', 'bid_price'),
            'bid'
        ));
    }

    public function updateOne() {
        if ($this->get_request_method() != "POST") $this->response('', 406);
        $data = json_decode(file_get_contents("php://input"), true);
        if (!isset($data['user_id'], $data['product_auction_id'], $data['bid_price'])) $this->responseInvalidParam();
        $params = array(
            'auction_id' => (int)$data['product_auction_id'],
            'user_id' => (int)$data['user_id'],
        );
        $current = $this->db->get_one(
            'SELECT * FROM bid WHERE product_auction_id = :auction_id AND user_id = :user_id LIMIT 1',
            $params
        );
        if (empty($current)) $this->responseInvalidParam();

        $wrapper = array('bid' => array(
            'bid_price' => $data['bid_price'],
            'last_update' => date(DATE_ATOM),
        ));
        $response = $this->db->post_update((int)$current['id'], $wrapper, 'id', array('bid_price', 'last_update'), 'bid');
        if ($response['status'] === 'success') {
            $response['data'] = $this->db->get_one(
                'SELECT * FROM bid WHERE product_auction_id = :auction_id AND user_id = :user_id LIMIT 1',
                $params
            );
        }
        $this->show_response($response);
    }
}
?>
