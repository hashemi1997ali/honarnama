<?php
require_once(realpath(dirname(__FILE__) . "/../tools/rest.php"));

class Currency extends REST{
	
	private $db = NULL; 
	
	public function __construct($db) {
		parent::__construct();
		$this->db = $db;
    }
	
	public function findAll(){
		if($this->get_request_method() != "GET") $this->response('',406); 
		$query="SELECT * FROM currency cu";
		$this->show_response($this->db->get_list($query));
	}
	
}	
?>
