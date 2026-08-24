<?php

// Router for PHP's local development server. Production should use the
// existing Apache rewrite rules in panel/services/.htaccess.
$path = rawurldecode(parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH));
if (str_contains($path, '..')) {
    http_response_code(400);
    echo 'Invalid path';
    return true;
}

if ($path === '/.env' || str_starts_with($path, '/.git/') || $path === '/Market/local.properties') {
    http_response_code(404);
    return true;
}

$file = __DIR__ . $path;
if ($path !== '/' && is_file($file)) {
    return false;
}

if ($path === '/panel') {
    header('Location: /panel/');
    return true;
}

if (preg_match('#^/panel/services(?:/(.*))?/?$#', $path, $matches)) {
    $route = isset($matches[1]) ? trim($matches[1], '/') : '';
    $_GET['x'] = $route;
    $_REQUEST['x'] = $route;
    require __DIR__ . '/panel/services/API.php';
    return true;
}

return false;

?>
