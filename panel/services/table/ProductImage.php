<?php
require_once(realpath(dirname(__FILE__) . "/../tools/rest.php"));

class ProductImage extends REST{

	private $db = NULL;
	private $upload_path = NULL;
	
	public function __construct($db) {
		parent::__construct();
		$this->db = $db;
		$this->upload_path = dirname(__DIR__, 2) . "/uploads/product/";
    }

	public function findAll(){
		if($this->get_request_method() != "GET") $this->response('',406);
		$query="SELECT DISTINCT * FROM product_image;";
		$this->show_response($this->db->get_list($query));
	}
	
	public function findAllByProductIdPlain($product_id){
		$query="SELECT DISTINCT * FROM product_image i WHERE i.product_id = :product_id";
		return $this->db->get_list($query, array('product_id' => (int)$product_id));
	}

	public function findAllByProductId(){
		if($this->get_request_method() != "GET") $this->response('',406);
		if(!isset($this->_request['product_id']))$this->responseInvalidParam();
		$product_id = (int)$this->_request['product_id'];
		$this->show_response($this->findAllByProductIdPlain($product_id));
	}

	public function insertAll(){
		if($this->get_request_method() != "POST") $this->response('',406);
		$product_image = json_decode(file_get_contents("php://input"),true);
		if(!isset($product_image))$this->responseInvalidParam();
		$column_names = array('product_id', 'name');
		$table_name = 'product_image';
		if(count($product_image) === 0 || !isset($product_image[0]['product_id'])) $this->responseInvalidParam();
		$product_id = (int)$product_image[0]['product_id'];
		try {
			$resp = $this->db->transaction(function($db) use ($product_id, $product_image, $column_names, $table_name) {
				$db->execute('DELETE FROM product_image WHERE product_id = :product_id', array('product_id' => $product_id));
				$result = $db->post_array($product_image, $column_names, $table_name);
				if($result['status'] !== 'success') throw new RuntimeException($result['msg']);
				return $result;
			});
		} catch(Throwable $exception) {
			error_log($exception->getMessage());
			$resp = array('status' => 'failed', 'msg' => 'Product images could not be updated', 'data' => $product_image);
		}
		$this->show_response($resp);
	}

	public function delete(){
		if($this->get_request_method() != "DELETE") $this->response('',406);
		if(!isset($this->_request['name']))$this->responseInvalidParam();
		$_name = basename($this->_request['name']);
		if($_name !== $this->_request['name']) $this->responseInvalidParam();
		$table_name = 'product_image';
		$pk = 'name';
		$target_file = $this->upload_path . $_name;
		if(file_exists($target_file)){
			unlink($target_file);
		}
		$resp = $this->db->delete_one_str($_name, $pk, $table_name);
		$this->show_response($resp);
	}
	
	public function findAllByProductId_arr($product_id){
		$query = "SELECT * FROM product_image i WHERE i.product_id = :product_id";
		return $this->db->get_list($query, array('product_id' => (int)$product_id));
	}
	
}	
?>
