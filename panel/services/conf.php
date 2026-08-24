<?php

class CONF {

    private static $environmentLoaded = false;

    public $DEMO_VERSION = false;

    public $DATABASE_URL;
    public $DATABASE_URL_UNPOOLED;
    public $SECURITY_CODE;

    public $SMTP_EMAIL;
    public $SMTP_PASSWORD;
    public $SMTP_HOST;
    public $SMTP_PORT;

    public $ADMIN_NAME;
    public $ADMIN_USERNAME;
    public $ADMIN_EMAIL;
    public $ADMIN_PASSWORD;

    public $SUBJECT_EMAIL_NEW_ORDER = "Market New Order";
    public $TITLE_REPORT_NEW_ORDER  = "Market New Order";

    public $SUBJECT_EMAIL_ORDER_PROCESSED = "Order PROCESSED";
    public $TITLE_REPORT_ORDER_PROCESSED  = "Order Status Change to PROCESSED";

    public $SUBJECT_EMAIL_ORDER_UPDATED = "Order Data Updated";
    public $TITLE_REPORT_ORDER_UPDATED  = "Order Data Updated By Admin";

    public function __construct() {
        $this->loadEnvironmentFile();

        $this->DATABASE_URL_UNPOOLED = $this->env('DATABASE_URL_UNPOOLED');
        $runtimeDatabaseUrl = $this->env('DATABASE_URL');
        $this->DATABASE_URL = $runtimeDatabaseUrl !== ''
            ? $runtimeDatabaseUrl
            : $this->DATABASE_URL_UNPOOLED;
        $this->SECURITY_CODE = $this->env('SECURITY_CODE');

        $this->SMTP_EMAIL = $this->env('SMTP_EMAIL');
        $this->SMTP_PASSWORD = $this->env('SMTP_PASSWORD');
        $this->SMTP_HOST = $this->env('SMTP_HOST');
        $this->SMTP_PORT = (int)$this->env('SMTP_PORT', '587');

        $this->ADMIN_NAME = $this->env('ADMIN_NAME', 'Administrator');
        $this->ADMIN_USERNAME = $this->env('ADMIN_USERNAME', 'admin');
        $this->ADMIN_EMAIL = $this->env('ADMIN_EMAIL', 'admin@example.com');
        $this->ADMIN_PASSWORD = $this->env('ADMIN_PASSWORD');
    }

    private function env($name, $default = '') {
        $value = getenv($name);
        return $value === false ? $default : $value;
    }

    private function loadEnvironmentFile() {
        if (self::$environmentLoaded) {
            return;
        }
        self::$environmentLoaded = true;

        $path = dirname(__DIR__, 2) . '/.env';
        if (!is_readable($path)) {
            return;
        }

        $lines = file($path, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES);
        if ($lines === false) {
            return;
        }

        foreach ($lines as $line) {
            $line = trim($line);
            if ($line === '' || str_starts_with($line, '#')) {
                continue;
            }

            $separator = strpos($line, '=');
            if ($separator === false) {
                continue;
            }

            $name = trim(substr($line, 0, $separator));
            if (!preg_match('/^[A-Z][A-Z0-9_]*$/', $name) || getenv($name) !== false) {
                continue;
            }

            $value = trim(substr($line, $separator + 1));
            if (strlen($value) >= 2) {
                $first = $value[0];
                $last = $value[strlen($value) - 1];
                if (($first === '"' && $last === '"') || ($first === "'" && $last === "'")) {
                    $value = substr($value, 1, -1);
                }
            }

            putenv($name . '=' . $value);
            $_ENV[$name] = $value;
        }
    }
}

?>
