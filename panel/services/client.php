<?php
require_once(realpath(dirname(__FILE__) . "/tools/rest.php"));
require_once(realpath(dirname(__FILE__) . "/tools/mail_handler.php"));

/*
 * This class handle all communication with Android Client
 */
class CLIENT extends REST{

    private $db = NULL;
    private $product 				= NULL;
    private $product_auction 		= NULL;
    private $product_category		= NULL;
    private $product_order			= NULL;
    private $product_order_detail	= NULL;
    private $product_image 			= NULL;
    private $category 				= NULL;
    private $user 					= NULL;
    private $app_user               = NULL;
    private $news_info 				= NULL;
    private $currency 				= NULL;
    private $config 				= NULL;
    private $mail_handler           = NULL;
	public $conf                    = NULL;

    public function __construct($db) {
        parent::__construct();
        $this->db = $db;
        $this->user = new User($this->db);
        $this->app_user = new AppUser($this->db);
        $this->product = new Product($this->db);
        $this->product_auction = new ProductAuction($this->db);
        $this->product_category = new ProductCategory($this->db);
        $this->product_order = new ProductOrder($this->db);
        $this->product_order_detail = new ProductOrderDetail($this->db);
        $this->product_image = new ProductImage($this->db);
        $this->category = new Category($this->db);
        $this->news_info = new NewsInfo($this->db);
        $this->currency = new Currency($this->db);
        $this->config = new Config($this->db);
        $this->mail_handler = new MailHandler($this->db);
		$this->conf = new CONF();
    }

    /* Cek status version and get some config data */
    public function info(){
        if($this->get_request_method() != "GET") $this->response('',406);
        $config_arr = $this->config->findAllArr();
        $info = array(
            "tax" => $this->getValue($config_arr, 'TAX'),
            "currency" => $this->getValue($config_arr, 'CURRENCY'),
            "shipping" => json_decode($this->getValue($config_arr, 'SHIPPING'), true)
        );
        $response = array( "status" => "success", "info" => $info );
        $this->show_response($response);
    }

    /* Response featured News Info */
    public function findAllFeaturedNewsInfo(){
        if($this->get_request_method() != "GET") $this->response('',406);
        $featured_news = $this->news_info->findAllFeatured();
        $object_res = array();
        foreach ($featured_news as $r){
            unset($r['full_content']);
            array_push($object_res, $r);
        }
		$response = array(
            'status' => 'success', 'news_infos' => $object_res
        );
        $this->show_response($response);
    }

    /* Response All News Info */
    public function findAllNewsInfo(){
        if($this->get_request_method() != "GET") $this->response('',406);
        $limit = isset($this->_request['count']) ? ((int)$this->_request['count']) : 10;
        $page = isset($this->_request['page']) ? ((int)$this->_request['page']) : 1;
        $q = isset($this->_request['q']) && $this->_request['q'] != null ? ($this->_request['q']) : "";

        $offset = ($page * $limit) - $limit;
        $count_total = $this->news_info->allCountPlain($q, 1);
        $news_infos = $this->news_info->findAllByPagePlain($limit, $offset, $q, 1);

        $object_res = array();
        foreach ($news_infos as $r){
            unset($r['full_content']);
            array_push($object_res, $r);
        }
        $count = count($news_infos);
        $response = array(
            'status' => 'success', 'count' => $count, 'count_total' => $count_total, 'pages' => $page, 'news_infos' => $object_res
        );
        $this->show_response($response);
    }

    /* Response All Product */
    public function findAllProduct(){
        if($this->get_request_method() != "GET") $this->response('',406);
        $limit = isset($this->_request['count']) ? ((int)$this->_request['count']) : 10;
        $page = isset($this->_request['page']) ? ((int)$this->_request['page']) : 1;
        $q = isset($this->_request['q']) && $this->_request['q'] != null ? ($this->_request['q']) : "";
        $category_id = isset($this->_request['category_id']) && $this->_request['category_id'] != null ? ((int)$this->_request['category_id']) : -1;

        $offset = ($page * $limit) - $limit;
        $count_total = $this->product->allCountPlainForClient($q, $category_id);
        $products = $this->product->findAllByPagePlainForClient($limit, $offset, $q, $category_id);

        $object_res = array();
        foreach ($products as $r){
            unset($r['description']);
            array_push($object_res, $r);
        }
        $count = count($products);
        $response = array(
            'status' => 'success', 'count' => $count, 'count_total' => $count_total, 'pages' => $page, 'products' => $object_res
        );
        $this->show_response($response);
    }

    /* Response Details Product */
    public function findProductDetails(){
        if($this->get_request_method() != "GET") $this->response('',406);
        if(!isset($this->_request['id'])) $this->responseInvalidParam();
        $id = (int)$this->_request['id'];
        $product = $this->product->findOnePlain($id);
		if(count($product) > 0){
			$categories = $this->category->getAllByProductIdPlain($id);
			$product_images = $this->product_image->findAllByProductIdPlain($id);
			$product['categories'] = $categories;
			$product['product_images'] = $product_images;	
			$response = array( 'status' => 'success', 'product' => $product );
		} else {
			$response = array( 'status' => 'failed', 'product' => null );
		}
        $this->show_response($response);
    }

    /* Response All Product Auction */
    public function findAllProductAuction(){
        if($this->get_request_method() != "GET") $this->response('',406);
        $limit = isset($this->_request['count']) ? ((int)$this->_request['count']) : 10;
        $page = isset($this->_request['page']) ? ((int)$this->_request['page']) : 1;
        $q = isset($this->_request['q']) && $this->_request['q'] != null ? ($this->_request['q']) : "";

        $offset = ($page * $limit) - $limit;
        $count_total = $this->product_auction->allCountPlainForClient($q);
        $products_auction = $this->product_auction->findAllByPagePlainForClient($limit, $offset, $q);

        $object_res = array();
        foreach ($products_auction as $r){
            unset($r['description']);
            array_push($object_res, $r);
        }
        $count = count($products_auction);
        $response = array(
            'status' => 'success', 'count' => $count, 'count_total' => $count_total, 'pages' => $page, 'products_auction' => $object_res
        );
        $this->show_response($response);
    }

    /* Response Details Product Auction */
    public function findProductAuctionDetails(){
        if($this->get_request_method() != "GET") $this->response('',406);
        if(!isset($this->_request['id'])) $this->responseInvalidParam();
        $id = (int)$this->_request['id'];
        $product_auction = $this->product_auction->findOnePlain($id);
        if(count($product_auction) > 0){
            $product_images = $this->product_image->findAllByProductIdPlain($id);
            $product_auction['product_images'] = $product_images;
            $response = array( 'status' => 'success', 'product_auction' => $product_auction );
        } else {
            $response = array( 'status' => 'failed', 'product_auction' => null );
        }
        $this->show_response($response);
    }
	
    /* Response Details News Info */
    public function findNewsDetails(){
        if($this->get_request_method() != "GET") $this->response('',406);
        if(!isset($this->_request['id'])) $this->responseInvalidParam();
        $id = (int)$this->_request['id'];
        $news_info = $this->news_info->findOnePlain($id);
		$response['status'] = 'success';
		$response['news_info'] = $news_info;
        $this->show_response($response);
    }	

    /* Response All Category */
    public function findAllCategory(){
        if($this->get_request_method() != "GET") $this->response('',406);
        $categories = $this->category->findAllForClient();
        $response = array(
            'status' => 'success', 'categories' => $categories
        );
        $this->show_response($response);
    }

    /* Validate and refresh locally cached Android cart data. */
    public function validateCart(){
        if($this->get_request_method() != "POST") $this->response('', 406);
        $data = json_decode(file_get_contents("php://input"), true);
        if(!isset($data['product_order_detail']) || !is_array($data['product_order_detail'])) {
            $this->responseInvalidParam();
        }
        if(!isset($this->_header['Security']) || $this->_header['Security'] != $this->conf->SECURITY_CODE){
            $this->show_response(array('status' => 'failed', 'valid' => false, 'msg' => 'Invalid security code', 'data' => array()));
        }

        $validation = $this->product_order_detail->validateAndNormalizeForSubmission($data['product_order_detail'], true);
        $this->show_response(array(
            'status' => 'success',
            'valid' => $validation['status'] === 'success',
            'msg' => $validation['msg'],
            'data' => $validation['data'],
        ));
    }

    /* Return account-owned orders and their current server-side totals/details. */
    public function listOrderHistory(){
        if($this->get_request_method() != "POST") $this->response('', 406);
        $data = json_decode(file_get_contents("php://input"), true);
        if(!isset($this->_header['Security']) || $this->_header['Security'] != $this->conf->SECURITY_CODE){
            $this->show_response(array('status' => 'failed', 'msg' => 'Invalid security code', 'data' => array()));
        }

        $appUser = $this->app_user->authenticateToken(isset($data['auth_token']) ? $data['auth_token'] : '');
        if(empty($appUser)) {
            $this->show_response(array('status' => 'failed', 'msg' => 'Your session has expired. Please log in again.', 'data' => array()));
        }
        $userId = (int)$appUser['id'];

        $legacyOrders = isset($data['legacy_orders']) && is_array($data['legacy_orders'])
            ? array_slice($data['legacy_orders'], 0, 100)
            : array();
        $this->db->transaction(function ($db) use ($legacyOrders, $userId) {
            foreach($legacyOrders as $legacyOrder) {
                $orderId = isset($legacyOrder['id']) ? (int)$legacyOrder['id'] : 0;
                $code = isset($legacyOrder['code']) ? trim((string)$legacyOrder['code']) : '';
                if($orderId < 1 || $code === '') continue;
                $db->execute(
                    'UPDATE product_order SET app_user_id = :user_id ' .
                    'WHERE id = :order_id AND code = :code AND app_user_id IS NULL',
                    array('user_id' => $userId, 'order_id' => $orderId, 'code' => $code)
                );
            }
        });

        $orders = $this->db->get_list(
            'SELECT id, code, status, total_fees, created_at FROM product_order ' .
            'WHERE app_user_id = :user_id ORDER BY id DESC',
            array('user_id' => $userId)
        );
        $details = $this->db->get_list(
            'SELECT pod.order_id, pod.product_id, pod.product_name, pod.amount, pod.price_item, ' .
            "COALESCE(p.image, '') AS image, COALESCE(p.stock, 0) AS stock, pod.created_at " .
            'FROM product_order_detail pod ' .
            'INNER JOIN product_order po ON po.id = pod.order_id ' .
            'LEFT JOIN product p ON p.id = pod.product_id ' .
            'WHERE po.app_user_id = :user_id ORDER BY pod.id ASC',
            array('user_id' => $userId)
        );

        $detailsByOrder = array();
        foreach($details as $detail) {
            $orderId = (int)$detail['order_id'];
            if(!isset($detailsByOrder[$orderId])) $detailsByOrder[$orderId] = array();
            $detail['id'] = null;
            $detailsByOrder[$orderId][] = $detail;
        }
        foreach($orders as &$order) {
            $orderId = (int)$order['id'];
            $order['cart_list'] = isset($detailsByOrder[$orderId]) ? $detailsByOrder[$orderId] : array();
        }
        unset($order);

        $this->show_response(array(
            'status' => 'success',
            'msg' => 'Order history loaded.',
            'data' => $orders,
        ));
    }

    /* Submit Product Order */
    public function submitProductOrder(){
        if($this->get_request_method() != "POST") $this->response('', 406);
        $data = json_decode(file_get_contents("php://input"), true);
        if(!isset($data) || !isset($data['product_order']) || !isset($data['product_order_detail'])) $this->responseInvalidParam();

        // checking security code
        if(!isset($this->_header['Security']) || $this->_header['Security'] != $this->conf->SECURITY_CODE){
            $m = array('status' => 'failed', 'msg' => 'Invalid security code', 'data' => null);
            $this->show_response($m);
            return;
        }

        $validation = $this->product_order_detail->validateAndNormalizeForSubmission($data['product_order_detail'], true);
        if($validation['status'] !== 'success') {
            $this->show_response(array(
                'status' => 'failed',
                'msg' => $validation['msg'],
                'data' => null,
                'errors' => $validation['data'],
            ));
        }

        $appUser = $this->app_user->authenticateToken(isset($data['auth_token']) ? $data['auth_token'] : '');
        if(empty($appUser)) {
            $this->show_response(array('status' => 'failed', 'msg' => 'Your session has expired. Please log in again.', 'data' => null));
        }

        $config = $this->config->findAllArr();
        $tax = (float)$this->getValue($config, 'TAX');
        $subtotal = 0;
        foreach($validation['details'] as $detail) {
            $subtotal += (float)$detail['price_item'] * (int)$detail['amount'];
        }
        $orderData = $data['product_order'];
        $orderData['app_user_id'] = (int)$appUser['id'];
        $orderData['status'] = 'WAITING';
        $orderData['tax'] = max(0, $tax);
        $orderData['total_fees'] = round($subtotal * (1 + ($orderData['tax'] / 100)), 2);

        try {
            $createdOrder = $this->db->transaction(function () use ($orderData, $validation) {
                $orderResponse = $this->product_order->insertOnePlain($orderData);
                if($orderResponse['status'] !== 'success' || empty($orderResponse['data']['id'])) {
                    throw new RuntimeException(isset($orderResponse['msg']) ? $orderResponse['msg'] : 'Order could not be created.');
                }
                $orderId = (int)$orderResponse['data']['id'];
                $detailResponse = $this->product_order_detail->insertAllPlain($orderId, $validation['details']);
                if($detailResponse['status'] !== 'success') {
                    throw new RuntimeException(isset($detailResponse['msg']) ? $detailResponse['msg'] : 'Order items could not be created.');
                }
                return $orderResponse['data'];
            });
        } catch (Throwable $exception) {
            error_log($exception->getMessage());
            $this->show_response(array('status' => 'failed', 'msg' => 'The order could not be placed.', 'data' => null));
        }

        try {
            $this->mail_handler->sendNewOrder((int)$createdOrder['id']);
        } catch (Throwable $exception) {
            // The order is already committed; notification failure must not cause a duplicate retry.
            error_log('New order email failed: ' . $exception->getMessage());
        }
        $this->show_response(array(
            'status' => 'success',
            'msg' => 'Order placed successfully.',
            'data' => $createdOrder,
        ));
    }

    private function getValue($data, $code){
        foreach($data as $d){
            if($d['code'] == $code){
                return $d['value'];
            }
        }
    }
}
?>
