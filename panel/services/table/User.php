<?php
require_once(realpath(dirname(__FILE__) . "/../tools/rest.php"));
require_once(realpath(dirname(__FILE__) . "/../conf.php"));

class User extends REST {

    private $db = NULL;
    private $conf = NULL;

    public function __construct($db) {
        parent::__construct();
        $this->db = $db;
        $this->conf = new CONF();
    }

    public function checkAuthorization() {
        $unauthorized = array('status' => 'failed', 'msg' => 'Unauthorized');
        if (!isset($this->_header['Token']) || empty($this->_header['Token'])) {
            $this->show_response($unauthorized);
        }

        $user = $this->db->get_one(
            'SELECT id FROM "user" WHERE password = :token LIMIT 1',
            array('token' => $this->_header['Token'])
        );
        if (empty($user)) {
            $this->show_response($unauthorized);
        }
    }

    public function processLogin() {
        if ($this->get_request_method() != "POST") $this->response('', 406);
        $credentials = json_decode(file_get_contents("php://input"), true);
        $username = isset($credentials['username']) ? trim($credentials['username']) : '';
        $password = isset($credentials['password']) ? $credentials['password'] : '';
        if ($username === '' || $password === '') {
            $this->show_response(array('status' => 'failed', 'msg' => 'نام کاربری و پسورد نامعتبر است.'));
        }

        $user = $this->db->get_one(
            'SELECT id, name, username, email, password FROM "user" WHERE username = :username LIMIT 1',
            array('username' => $username)
        );
        if (empty($user) || !$this->passwordMatches($password, $user['password'])) {
            $this->show_response(array('status' => 'failed', 'msg' => 'نام کاربری یا پسورد وجود ندارد.'));
        }

        if ($this->isLegacyMd5($user['password']) || password_needs_rehash($user['password'], PASSWORD_DEFAULT)) {
            $user['password'] = password_hash($password, PASSWORD_DEFAULT);
            $this->db->execute(
                'UPDATE "user" SET password = :password WHERE id = :id',
                array('password' => $user['password'], 'id' => (int)$user['id'])
            );
        }
        $this->show_response(array('status' => 'success', 'user' => $user));
    }

    public function findOne() {
        if ($this->get_request_method() != "GET") $this->response('', 406);
        if (!isset($this->_request['id'])) $this->responseInvalidParam();
        $this->show_response($this->db->get_one(
            'SELECT id, name, username, email FROM "user" WHERE id = :id',
            array('id' => (int)$this->_request['id'])
        ));
    }

    public function findOneToken() {
        $user = $this->db->get_one('SELECT password FROM "user" ORDER BY id ASC LIMIT 1');
        return isset($user['password']) ? $user['password'] : '';
    }

    public function updateOne() {
        if ($this->get_request_method() != "POST") $this->response('', 406);
        if ($this->conf->DEMO_VERSION) {
            $this->show_response(array('status' => 'failed', 'msg' => 'متاسفانه این یک نسخه دمو می باشد.', 'data' => null));
        }

        $data = json_decode(file_get_contents("php://input"), true);
        if (!isset($data['id'], $data['user'])) $this->responseInvalidParam();
        $columns = array('name', 'username', 'email');
        if (isset($data['user']['password']) && $data['user']['password'] !== '*****') {
            $data['user']['password'] = password_hash($data['user']['password'], PASSWORD_DEFAULT);
            $columns[] = 'password';
        }
        $this->show_response($this->db->post_update((int)$data['id'], $data, 'id', $columns, 'user'));
    }

    public function insertOne() {
        if ($this->get_request_method() != "POST") $this->response('', 406);
        if ($this->conf->DEMO_VERSION) {
            $this->show_response(array('status' => 'failed', 'msg' => 'متاسفانه این یک نسخه دمو می باشد.', 'data' => null));
        }

        $user = json_decode(file_get_contents("php://input"), true);
        if (!isset($user['username'], $user['password']) || trim($user['username']) === '' || $user['password'] === '') {
            $this->responseInvalidParam();
        }
        $user['password'] = password_hash($user['password'], PASSWORD_DEFAULT);
        $columns = array('name', 'username', 'email', 'password');
        $this->show_response($this->db->post_one($user, 'id', $columns, 'user'));
    }

    private function passwordMatches($plainText, $storedHash) {
        if ($this->isLegacyMd5($storedHash)) {
            return hash_equals(strtolower($storedHash), md5($plainText));
        }
        return password_verify($plainText, $storedHash);
    }

    private function isLegacyMd5($value) {
        return is_string($value) && preg_match('/^[a-f0-9]{32}$/i', $value) === 1;
    }
}
?>
