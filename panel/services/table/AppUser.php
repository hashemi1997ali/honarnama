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
            'SELECT id FROM app_user WHERE id = :id AND active = TRUE LIMIT 1',
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
            'SELECT id, name, username, password, active FROM app_user WHERE username = :username LIMIT 1',
            array('username' => $username)
        );
        if (empty($user) || !$this->passwordMatches($password, $user['password'])) {
            $this->show_response(array('status' => 'failed', 'msg' => 'Incorrect username or password.'));
        }
        if (!$this->isActive($user['active'])) {
            $this->show_response(array('status' => 'failed', 'msg' => 'This account has been disabled.'));
        }

        if (!str_starts_with($user['password'], '$') || password_needs_rehash($user['password'], PASSWORD_DEFAULT)) {
            $newHash = password_hash($password, PASSWORD_DEFAULT);
            $this->db->execute(
                'UPDATE app_user SET password = :password WHERE id = :id',
                array('password' => $newHash, 'id' => (int)$user['id'])
            );
        }
        unset($user['password'], $user['active']);
        $this->show_response(array('status' => 'success', 'data' => $user));
    }

    public function findAllByPage() {
        if ($this->get_request_method() != "GET") $this->response('', 406);
        if (!isset($this->_request['limit'], $this->_request['page'])) $this->responseInvalidParam();

        $limit = min(100, max(1, (int)$this->_request['limit']));
        $page = max(1, (int)$this->_request['page']);
        $offset = ($page - 1) * $limit;
        $query = isset($this->_request['q']) ? trim($this->_request['q']) : '';
        $params = array();
        $where = '';
        if ($query !== '') {
            $where = ' WHERE au.name ILIKE :q_name OR au.username ILIKE :q_username';
            $params['q_name'] = '%' . $query . '%';
            $params['q_username'] = '%' . $query . '%';
        }

        $sql = 'SELECT au.id, au.name, au.username, '
            . 'CASE WHEN au.active THEN 1 ELSE 0 END AS active, '
            . 'au.created_at, au.last_update, COUNT(b.id)::INTEGER AS bid_count '
            . 'FROM app_user au LEFT JOIN bid b ON b.user_id = au.id' . $where . ' '
            . 'GROUP BY au.id, au.name, au.username, au.active, au.created_at, au.last_update '
            . "ORDER BY au.id DESC LIMIT {$limit} OFFSET {$offset}";
        $this->show_response($this->db->get_list($sql, $params));
    }

    public function allCount() {
        if ($this->get_request_method() != "GET") $this->response('', 406);
        $query = isset($this->_request['q']) ? trim($this->_request['q']) : '';
        $params = array();
        $where = '';
        if ($query !== '') {
            $where = ' WHERE name ILIKE :q_name OR username ILIKE :q_username';
            $params['q_name'] = '%' . $query . '%';
            $params['q_username'] = '%' . $query . '%';
        }
        $this->show_response_plain($this->db->get_count('SELECT COUNT(*) FROM app_user' . $where, $params));
    }

    public function findOneAdmin() {
        if ($this->get_request_method() != "GET") $this->response('', 406);
        if (!isset($this->_request['id'])) $this->responseInvalidParam();
        $user = $this->db->get_one(
            'SELECT au.id, au.name, au.username, '
            . 'CASE WHEN au.active THEN 1 ELSE 0 END AS active, '
            . 'au.created_at, au.last_update, COUNT(b.id)::INTEGER AS bid_count '
            . 'FROM app_user au LEFT JOIN bid b ON b.user_id = au.id '
            . 'WHERE au.id = :id '
            . 'GROUP BY au.id, au.name, au.username, au.active, au.created_at, au.last_update',
            array('id' => (int)$this->_request['id'])
        );
        $this->show_response($user);
    }

    public function updateStatus() {
        if ($this->get_request_method() != "POST") $this->response('', 406);
        $data = json_decode(file_get_contents("php://input"), true);
        if (!isset($data['id']) || !array_key_exists('active', $data)) $this->responseInvalidParam();
        $active = filter_var($data['active'], FILTER_VALIDATE_BOOLEAN, FILTER_NULL_ON_FAILURE);
        if ($active === null) $this->responseInvalidParam();

        $updated = $this->db->execute(
            'UPDATE app_user SET active = :active, last_update = now() WHERE id = :id',
            array('active' => $active, 'id' => (int)$data['id'])
        );
        $this->show_response(array(
            'status' => $updated === 1 ? 'success' : 'failed',
            'msg' => $updated === 1 ? 'User status updated.' : 'User was not found.',
            'data' => array('id' => (int)$data['id'], 'active' => $active ? 1 : 0),
        ));
    }

    public function deleteAdmin() {
        if ($this->get_request_method() != "GET") $this->response('', 406);
        if (!isset($this->_request['id'])) $this->responseInvalidParam();
        $this->show_response($this->db->delete_one((int)$this->_request['id'], 'id', 'app_user'));
    }

    private function passwordMatches($plainText, $storedHash) {
        if (is_string($storedHash) && str_starts_with($storedHash, '$')) {
            return password_verify($plainText, $storedHash);
        }
        return is_string($storedHash) && hash_equals($storedHash, $plainText);
    }

    private function isActive($value) {
        return $value === true || $value === 1 || $value === '1' || $value === 't';
    }
}
?>
