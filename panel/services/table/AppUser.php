<?php
require_once(realpath(dirname(__FILE__) . "/../tools/rest.php"));

class AppUser extends REST {

    private $db = NULL;

    public function __construct($db) {
        parent::__construct();
        $this->db = $db;
    }

    public function findUser() {
        if ($this->get_request_method() != "POST") $this->response('', 406);
        $data = json_decode(file_get_contents("php://input"), true);
        $userId = isset($data['user_id']) ? (int)$data['user_id'] : 0;
        if ($userId < 1) {
            $this->show_response(array('status' => 'failed', 'msg' => 'Unauthorized'));
        }
        $user = $this->db->get_one(
            'SELECT id FROM app_user WHERE id = :id LIMIT 1',
            array('id' => $userId)
        );
        if (empty($user)) {
            $this->show_response(array('status' => 'failed', 'msg' => 'Unauthorized'));
        }
    }

    public function insertOne() {
        if ($this->get_request_method() != "POST") $this->response('', 406);
        $data = json_decode(file_get_contents("php://input"), true);
        $username = isset($data['username']) ? trim($data['username']) : '';
        $password = isset($data['password']) ? $data['password'] : '';
        if ($username === '' || $password === '') {
            $this->show_response(array('status' => 'failed', 'msg' => 'Username and password are required.'));
        }

        $existing = $this->db->get_one(
            'SELECT id FROM app_user WHERE username = :username LIMIT 1',
            array('username' => $username)
        );
        if (!empty($existing)) {
            $this->show_response(array('status' => 'failed', 'msg' => 'That username already exists.'));
        }

        $data['username'] = $username;
        $data['password'] = password_hash($password, PASSWORD_DEFAULT);
        $response = $this->db->post_one($data, 'id', array('name', 'username', 'password'), 'app_user');
        if (isset($response['data']['password'])) {
            unset($response['data']['password']);
        }
        $this->show_response($response);
    }

    public function loginUser() {
        if ($this->get_request_method() != "POST") $this->response('', 406);
        $data = json_decode(file_get_contents("php://input"), true);
        $username = isset($data['username']) ? trim($data['username']) : '';
        $password = isset($data['password']) ? $data['password'] : '';
        if ($username === '' || $password === '') {
            $this->show_response(array('status' => 'failed', 'msg' => 'Username and password are required.'));
        }

        $user = $this->db->get_one(
            'SELECT id, name, username, password FROM app_user WHERE username = :username LIMIT 1',
            array('username' => $username)
        );
        if (empty($user) || !$this->passwordMatches($password, $user['password'])) {
            $this->show_response(array('status' => 'failed', 'msg' => 'Incorrect username or password.'));
        }

        if (!str_starts_with($user['password'], '$') || password_needs_rehash($user['password'], PASSWORD_DEFAULT)) {
            $newHash = password_hash($password, PASSWORD_DEFAULT);
            $this->db->execute(
                'UPDATE app_user SET password = :password WHERE id = :id',
                array('password' => $newHash, 'id' => (int)$user['id'])
            );
        }
        unset($user['password']);
        $this->show_response(array('status' => 'success', 'data' => $user));
    }

    private function passwordMatches($plainText, $storedHash) {
        if (is_string($storedHash) && str_starts_with($storedHash, '$')) {
            return password_verify($plainText, $storedHash);
        }
        return is_string($storedHash) && hash_equals($storedHash, $plainText);
    }
}
?>
