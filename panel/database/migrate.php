<?php

if (PHP_SAPI !== 'cli') {
    http_response_code(404);
    exit;
}

require_once(dirname(__DIR__) . '/services/conf.php');
require_once(dirname(__DIR__) . '/services/tools/db.php');

try {
    $conf = new CONF();
    $databaseUrl = $conf->DATABASE_URL_UNPOOLED ?: $conf->DATABASE_URL;
    if ($databaseUrl === '' || str_contains($databaseUrl, 'REPLACE_WITH_ROTATED_PASSWORD')) {
        throw new RuntimeException('Set a rotated Neon URL in DATABASE_URL_UNPOOLED or DATABASE_URL first.');
    }
    if (strlen($conf->ADMIN_PASSWORD) < 12 ||
        str_contains(strtolower($conf->ADMIN_PASSWORD), 'change-me') ||
        str_contains(strtolower($conf->ADMIN_PASSWORD), 'replace')) {
        throw new RuntimeException('ADMIN_PASSWORD must be a new password with at least 12 characters.');
    }

    $db = new DB($databaseUrl);
    $schema = file_get_contents(__DIR__ . '/schema.sql');
    if ($schema === false) {
        throw new RuntimeException('Could not read schema.sql.');
    }
    $db->pdo->exec($schema);

    $created = $db->execute(
        'INSERT INTO "user" (name, username, email, password) '
        . 'VALUES (:name, :username, :email, :password) '
        . 'ON CONFLICT (username) DO NOTHING',
        array(
            'name' => $conf->ADMIN_NAME,
            'username' => $conf->ADMIN_USERNAME,
            'email' => $conf->ADMIN_EMAIL,
            'password' => password_hash($conf->ADMIN_PASSWORD, PASSWORD_DEFAULT),
        )
    );

    echo "Database schema is ready.\n";
    echo $created === 1
        ? "The initial panel administrator was created.\n"
        : "The panel administrator already exists; its password was not changed.\n";
} catch (Throwable $exception) {
    fwrite(STDERR, 'Migration failed: ' . $exception->getMessage() . PHP_EOL);
    exit(1);
}

?>
