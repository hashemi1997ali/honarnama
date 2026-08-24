<?php
require_once(realpath(dirname(__FILE__) . "/../conf.php"));

class DB {

    public $pdo = NULL;
    public $conf = NULL;

    public function __construct($databaseUrl = null) {
        $this->conf = new CONF();
        $this->connect($databaseUrl ?: $this->conf->DATABASE_URL);
    }

    private function connect($databaseUrl) {
        if (!extension_loaded('pdo_pgsql')) {
            throw new RuntimeException('The pdo_pgsql PHP extension is required.');
        }
        if (empty($databaseUrl)) {
            throw new RuntimeException('DATABASE_URL is not configured.');
        }

        $parts = parse_url($databaseUrl);
        if ($parts === false || !isset($parts['scheme'], $parts['host'], $parts['user'], $parts['path'])) {
            throw new RuntimeException('DATABASE_URL is invalid.');
        }
        if (!in_array($parts['scheme'], array('postgres', 'postgresql'), true)) {
            throw new RuntimeException('DATABASE_URL must use the postgresql scheme.');
        }

        $host = $parts['host'];
        $port = isset($parts['port']) ? (int)$parts['port'] : 5432;
        $database = rawurldecode(ltrim($parts['path'], '/'));
        $user = rawurldecode($parts['user']);
        $password = rawurldecode(isset($parts['pass']) ? $parts['pass'] : '');

        if (!preg_match('/^[a-zA-Z0-9.-]+$/', $host) ||
            !preg_match('/^[a-zA-Z0-9_.-]+$/', $database) ||
            $port < 1 || $port > 65535) {
            throw new RuntimeException('DATABASE_URL contains an invalid host, port, or database name.');
        }

        $options = array();
        if (isset($parts['query'])) {
            parse_str($parts['query'], $options);
        }

        $sslmode = isset($options['sslmode']) ? $options['sslmode'] : 'require';
        $isLocalDatabase = in_array($host, array('127.0.0.1', 'localhost', '::1'), true);
        $allowedSslModes = $isLocalDatabase
            ? array('disable', 'prefer', 'require', 'verify-ca', 'verify-full')
            : array('require', 'verify-ca', 'verify-full');
        if (!in_array($sslmode, $allowedSslModes, true)) {
            throw new RuntimeException('Remote PostgreSQL connections must require TLS.');
        }

        $dsn = "pgsql:host={$host};port={$port};dbname={$database};sslmode={$sslmode}";
        if (isset($options['channel_binding']) && in_array($options['channel_binding'], array('prefer', 'require'), true)) {
            $dsn .= ';channel_binding=' . $options['channel_binding'];
        }
        $dsn .= ';application_name=honarnama';

        $this->pdo = new PDO($dsn, $user, $password, array(
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES => false,
            PDO::ATTR_STRINGIFY_FETCHES => false,
        ));
    }

    public function reConnect() {
        $this->connect($this->conf->DATABASE_URL);
        return $this;
    }

    public function checkResponse_Impl() {
        $this->get_one('SELECT 1 AS connected');
        echo 'Database Connection : Success';
    }

    public function query($query, $params = array()) {
        $statement = $this->pdo->prepare($query);
        foreach ($params as $name => $value) {
            $type = PDO::PARAM_STR;
            if (is_int($value)) {
                $type = PDO::PARAM_INT;
            } elseif (is_bool($value)) {
                $type = PDO::PARAM_BOOL;
            } elseif ($value === null) {
                $type = PDO::PARAM_NULL;
            }
            $parameter = is_int($name) ? $name + 1 : (str_starts_with($name, ':') ? $name : ':' . $name);
            $statement->bindValue($parameter, $value, $type);
        }
        $statement->execute();
        return $statement;
    }

    public function execute($query, $params = array()) {
        return $this->query($query, $params)->rowCount();
    }

    public function get_list($query, $params = array()) {
        return $this->query($query, $params)->fetchAll();
    }

    public function get_one($query, $params = array()) {
        $result = $this->query($query, $params)->fetch();
        return $result === false ? array() : $result;
    }

    public function get_count($query, $params = array()) {
        $result = $this->query($query, $params)->fetchColumn();
        return $result === false ? 0 : (int)$result;
    }

    public function transaction($callback) {
        $ownsTransaction = !$this->pdo->inTransaction();
        if ($ownsTransaction) {
            $this->pdo->beginTransaction();
        }

        try {
            $result = $callback($this);
            if ($ownsTransaction) {
                $this->pdo->commit();
            }
            return $result;
        } catch (Throwable $exception) {
            if ($ownsTransaction && $this->pdo->inTransaction()) {
                $this->pdo->rollBack();
            }
            throw $exception;
        }
    }

    public function post_one($obj, $pk, $column_names, $table_name) {
        if (empty($obj)) {
            return array('status' => 'failed', 'msg' => 'No data supplied', 'data' => null);
        }

        try {
            $columns = array();
            $placeholders = array();
            $params = array();
            foreach ($column_names as $index => $column) {
                $columns[] = $this->identifier($column);
                $placeholder = 'value_' . $index;
                $placeholders[] = ':' . $placeholder;
                $params[$placeholder] = array_key_exists($column, $obj) ? $obj[$column] : null;
            }

            $query = 'INSERT INTO ' . $this->identifier($table_name) .
                ' (' . implode(', ', $columns) . ') VALUES (' . implode(', ', $placeholders) . ') RETURNING *';
            $created = $this->get_one($query, $params);
            return array(
                'status' => 'success',
                'msg' => $table_name . ' created successfully',
                'data' => $created,
            );
        } catch (Throwable $exception) {
            return $this->failure($exception, $table_name . ' could not be created');
        }
    }

    public function post_array($obj_array, $column_names, $table_name) {
        if (empty($obj_array)) {
            return array('status' => 'success', 'msg' => 'Nothing to create', 'data' => array());
        }

        try {
            $this->transaction(function () use ($obj_array, $column_names, $table_name) {
                $columns = array_map(array($this, 'identifier'), $column_names);
                $placeholders = array();
                foreach ($column_names as $index => $column) {
                    $placeholders[] = ':value_' . $index;
                }
                $query = 'INSERT INTO ' . $this->identifier($table_name) .
                    ' (' . implode(', ', $columns) . ') VALUES (' . implode(', ', $placeholders) . ')';

                foreach ($obj_array as $obj) {
                    $params = array();
                    foreach ($column_names as $index => $column) {
                        $params['value_' . $index] = array_key_exists($column, $obj) ? $obj[$column] : null;
                    }
                    $this->execute($query, $params);
                }
            });

            return array(
                'status' => 'success',
                'msg' => $table_name . ' created successfully',
                'data' => $obj_array,
            );
        } catch (Throwable $exception) {
            return $this->failure($exception, $table_name . ' could not be created');
        }
    }

    public function update_array($pk, $obj_array, $column_names, $table_name) {
        return $this->updateArrayInternal($pk, $obj_array, $column_names, $table_name);
    }

    public function update_array_pk_str($pk, $obj_array, $column_names, $table_name) {
        return $this->updateArrayInternal($pk, $obj_array, $column_names, $table_name);
    }

    private function updateArrayInternal($pk, $obj_array, $column_names, $table_name) {
        if (empty($obj_array)) {
            return array('status' => 'success', 'msg' => 'Nothing to update', 'data' => array());
        }

        try {
            $this->transaction(function () use ($pk, $obj_array, $column_names, $table_name) {
                foreach ($obj_array as $obj) {
                    if (!array_key_exists($pk, $obj)) {
                        throw new InvalidArgumentException('Missing primary key: ' . $pk);
                    }
                    $assignments = array();
                    $params = array('pk_value' => $obj[$pk]);
                    foreach ($column_names as $index => $column) {
                        $placeholder = 'value_' . $index;
                        $assignments[] = $this->identifier($column) . ' = :' . $placeholder;
                        $params[$placeholder] = array_key_exists($column, $obj) ? $obj[$column] : null;
                    }
                    $query = 'UPDATE ' . $this->identifier($table_name) . ' SET ' . implode(', ', $assignments) .
                        ' WHERE ' . $this->identifier($pk) . ' = :pk_value';
                    $this->execute($query, $params);
                }
            });

            return array(
                'status' => 'success',
                'msg' => $table_name . ' updated successfully',
                'data' => $obj_array,
            );
        } catch (Throwable $exception) {
            return $this->failure($exception, $table_name . ' could not be updated');
        }
    }

    public function post_update($id, $obj, $pk, $column_names, $table_name) {
        if (!isset($obj[$table_name]) || !is_array($obj[$table_name])) {
            return array('status' => 'failed', 'msg' => 'Invalid update data', 'data' => $obj);
        }

        try {
            $assignments = array();
            $params = array('pk_value' => $id);
            foreach ($column_names as $index => $column) {
                $placeholder = 'value_' . $index;
                $assignments[] = $this->identifier($column) . ' = :' . $placeholder;
                $params[$placeholder] = array_key_exists($column, $obj[$table_name]) ? $obj[$table_name][$column] : null;
            }

            $query = 'UPDATE ' . $this->identifier($table_name) . ' SET ' . implode(', ', $assignments) .
                ' WHERE ' . $this->identifier($pk) . ' = :pk_value';
            $this->execute($query, $params);
            return array(
                'status' => 'success',
                'msg' => $table_name . ' updated successfully',
                'data' => $obj,
            );
        } catch (Throwable $exception) {
            return $this->failure($exception, $table_name . ' could not be updated', $obj);
        }
    }

    public function delete_one($id, $pk, $table_name) {
        return $this->deleteInternal($id, $pk, $table_name);
    }

    public function delete_one_str($pkval, $pk, $table_name) {
        return $this->deleteInternal($pkval, $pk, $table_name);
    }

    private function deleteInternal($value, $pk, $table_name) {
        try {
            $query = 'DELETE FROM ' . $this->identifier($table_name) .
                ' WHERE ' . $this->identifier($pk) . ' = :value';
            $this->execute($query, array('value' => $value));
            return array('status' => 'success', 'msg' => 'One ' . $table_name . ' record deleted successfully');
        } catch (Throwable $exception) {
            return $this->failure($exception, $table_name . ' could not be deleted');
        }
    }

    private function identifier($name) {
        if (!preg_match('/^[a-z_][a-z0-9_]*$/i', $name)) {
            throw new InvalidArgumentException('Invalid database identifier.');
        }
        return '"' . $name . '"';
    }

    private function failure($exception, $message, $data = null) {
        error_log($exception->getMessage());
        return array('status' => 'failed', 'msg' => $message, 'data' => $data);
    }
}
?>
