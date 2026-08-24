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
    if ($databaseUrl === '') {
        throw new RuntimeException('Set DATABASE_URL_UNPOOLED or DATABASE_URL before seeding.');
    }

    $seedSql = file_get_contents(__DIR__ . '/seed.sql');
    if ($seedSql === false) {
        throw new RuntimeException('Could not read seed.sql.');
    }

    $db = new DB($databaseUrl);
    $db->pdo->exec($seedSql);

    $tables = array('category', 'product', 'news_info', 'product_auction', 'product_order');
    echo "Sample data is ready.\n";
    foreach ($tables as $table) {
        $count = $db->get_count('SELECT COUNT(*) FROM "' . $table . '"');
        echo $table . ': ' . $count . PHP_EOL;
    }
} catch (Throwable $exception) {
    fwrite(STDERR, 'Seeding failed: ' . $exception->getMessage() . PHP_EOL);
    exit(1);
}

?>
