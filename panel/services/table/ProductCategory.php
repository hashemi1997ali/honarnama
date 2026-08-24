<?php
require_once(realpath(dirname(__FILE__) . "/../tools/rest.php"));

class ProductCategory extends REST{
	
	private $db = NULL; 
	
	public function __construct($db) {
		parent::__construct();
		$this->db = $db;
    }
	
	public function findAll(){
		if($this->get_request_method() != "GET") $this->response('',406); 
		$query="SELECT * FROM product_category pc";
		$this->show_response($this->db->get_list($query));
	}
	
	public function deleteInsertAll(){
		if($this->get_request_method() != "POST") $this->response('',406);
		$product_category = json_decode(file_get_contents("php://input"),true);
		if(!isset($product_category))$this->responseInvalidParam();
		
		$column_names = array('product_id', 'category_id');
		$table_name = 'product_category';
		if(count($product_category) === 0 || !isset($product_category[0]['product_id'])) $this->responseInvalidParam();
		$product_id = (int)$product_category[0]['product_id'];
		try {
			$resp = $this->db->transaction(function($db) use ($product_id, $product_category, $column_names, $table_name) {
				$db->execute('DELETE FROM product_category WHERE product_id = :product_id', array('product_id' => $product_id));
				$result = $db->post_array($product_category, $column_names, $table_name);
				if($result['status'] !== 'success') throw new RuntimeException($result['msg']);
				return $result;
			});
		} catch(Throwable $exception) {
			error_log($exception->getMessage());
			$resp = array('status' => 'failed', 'msg' => 'Product categories could not be updated', 'data' => $product_category);
		}
		$this->show_response($resp);
	}
	
}	
?>
