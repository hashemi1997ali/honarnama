<?php

require_once(realpath(dirname(__FILE__) . "/../table/User.php"));
require_once(realpath(dirname(__FILE__) . "/mail.php"));


class MailHandler {

    private $db = NULL;

    public function __construct($db) {
        $this->db = $db;
    }

    // this function will be access after submit order finished
    public function sendNewOrder($order_id) {
        $this->send($order_id, 'NEW_ORDER');
    }

    // this function will be access after order updated
    public function sendOrderProcess($order_id) {
        $this->send($order_id, 'ORDER_PROCESS');
    }

    // this function will be access after order updated
    public function sendOrderUpdate($order_id) {
        $this->send($order_id, 'ORDER_UPDATE');
    }

    private function send($order_id, $type) {
        $mail = new Mail($this->db);
        $mail->sendOrder((int)$order_id, $type);
    }
}

?>
