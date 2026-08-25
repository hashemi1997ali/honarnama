angular.module('App').factory("request", function ($http, $cookies) {

	/**
	 * This file is connector beetween angularJs with REST API(api.php)
	 */

	var api_base = 'services/';

	var obj = {};
	var token = $cookies.get(window.location.origin + '_session_password');
	var config = { headers: { 'Token': token } };
	obj.setToken = function (value) {
		if (value) {
			config.headers.Token = value;
		} else {
			delete config.headers.Token;
		}
	};

	// DASHBOARD ------------------------------------------------------------------------------
	obj.getDashboardProduct = function () {
		return $http.get(api_base + 'getDashboardProduct');
	};
	obj.getDashboardOthers = function () {
		return $http.get(api_base + 'getDashboardOthers');
	};


	// PRODUCT --------------------------------------------------------------------------------
	obj.getOneProduct = function (id) {
		return $http.get(api_base + 'getOneProduct?id='+id);
	};
	obj.getAllProductByPage = function (page, limit, q, category_id) {
		return $http.get(api_base + 'getAllProductByPage?page='+page+'&limit='+limit+'&q='+encodeURIComponent(q || '')+'&category_id='+category_id);
	};
	obj.getAllProductCount = function(q, category_id) {
		return $http.get(api_base + 'getAllProductCount?q='+encodeURIComponent(q || '')+'&category_id='+category_id);
	};
	obj.insertOneProduct = function (object) {
		return $http.post(api_base + 'insertOneProduct', object, config).then(function (results) { return results.data; });
	};	
	obj.updateOneProduct = function (id, object) {
		var data = {id:id, product:object};
		return $http.post(api_base + 'updateOneProduct', data, config).then(function (results) { return results.data; });
	};
	obj.deleteOneProduct = function (id) {
		return $http.get(api_base + 'deleteOneProduct?id='+id, config).then(function (results) { return results.data; });
	};

	// AUCTION --------------------------------------------------------------------------------
	obj.getOneProductAuction = function (id) {
		return $http.get(api_base + 'getOneProductAuction?id=' + id, config);
	};
	obj.getAllProductAuctionByPage = function (page, limit, q) {
		return $http.get(api_base + 'getAllProductAuctionByPage?page=' + page + '&limit=' + limit + '&q=' + encodeURIComponent(q || ''), config);
	};
	obj.getAllProductAuctionCount = function (q) {
		return $http.get(api_base + 'getAllProductAuctionCount?q=' + encodeURIComponent(q || ''), config);
	};
	obj.insertOneProductAuction = function (object) {
		return $http.post(api_base + 'insertOneProductAuction', object, config).then(function (results) { return results.data; });
	};
	obj.updateOneProductAuction = function (id, object) {
		return $http.post(api_base + 'updateOneProductAuction', {id: id, product_auction: object}, config).then(function (results) { return results.data; });
	};
	obj.deleteOneProductAuction = function (id) {
		return $http.get(api_base + 'deleteOneProductAuction?id=' + id, config).then(function (results) { return results.data; });
	};
	
	// PRODUCT CATEGORY -----------------------------------------------------------------------
	obj.insertAllProductCategory = function (object) {
		return $http.post(api_base + 'insertAllProductCategory', object, config).then(function (results) { 
			return results.data; 
		});
	};
	
	// PRODUCT IMAGE -------------------------------------------------------------------------
	obj.insertAllProductImage = function (object) {
		return $http.post(api_base + 'insertAllProductImage', object, config).then(function (results) { 
			return results.data; 
		});
	};
	obj.getAllProductImageByProductId = function(product_id) {
		return $http.get(api_base + 'getAllProductImageByProductId?product_id='+product_id);
	};	
	obj.deleteProductImageByName = function(name){
		return $http.delete(api_base + 'deleteProductImageByName?name=' + name, config).then(function (results) {
			return results.data;
		});
	};
	
	// CATEGORY -----------------------------------------------------------------------
	obj.getAllCategory  = function () {
		return $http.get(api_base + 'getAllCategory');
	};
	obj.getOneCategory = function (id) {
		return $http.get(api_base + 'getOneCategory?id='+id);
	};
	obj.getAllCategoryByPage = function (page, limit, q) {
		return $http.get(api_base + 'getAllCategoryByPage?page='+page+'&limit='+limit+'&q='+encodeURIComponent(q || ''));
	};
	obj.getAllCategoryCount = function(q) {
		return $http.get(api_base + 'getAllCategoryCount?q='+encodeURIComponent(q || ''));
	};	
	obj.getAllCategoryByProductId = function(product_id) {
		return $http.get(api_base + 'getAllCategoryByProductId?product_id='+product_id);
	};
	obj.insertOneCategory = function (object) {
		return $http.post(api_base + 'insertOneCategory', object, config).then(function (results) { return results.data; });
	};
	obj.updateOneCategory = function (id, object) {
		var data = {id:id, category:object};
		return $http.post(api_base + 'updateOneCategory', data, config).then(function (results) { return results.data; });
	};
	obj.deleteOneCategory = function (id) {
		return $http.get(api_base + 'deleteOneCategory?id='+id, config).then(function (results) { return results.data; });
	};
	
	// PRODUCT ORDER --------------------------------------------------------------------------
	obj.getOneProductOrder = function (id) {
		return $http.get(api_base + 'getOneProductOrder?id='+id);
	};
	obj.getAllProductOrderByPage = function (page, limit, q) {
		return $http.get(api_base + 'getAllProductOrderByPage?page=' +page+'&limit='+limit+'&q='+encodeURIComponent(q || ''));
	};
	obj.getAllProductOrderCount = function(q) {
		return $http.get(api_base + 'getAllProductOrderCount?q='+encodeURIComponent(q || ''));
	};
    obj.updateOneProductOrder = function (id, object) {
        var data = {id:id, product_order:object};
        return $http.post(api_base + 'updateOneProductOrder', data, config).then(function (results) { return results.data; });
    };
	obj.insertOneProductOrder = function (object) {
		return $http.post(api_base + 'insertOneProductOrder', object, config).then(function (results) { return results.data; });
	};
    obj.deleteOneProductOrder = function (id) {
        return $http.get(api_base + 'deleteOneProductOrder?id='+id, config).then(function (results) { return results.data; });
    };
	obj.processProductOrder = function (id, order, order_detail) {
		var data = {id:id, product_order:order, product_order_detail:order_detail};
		return $http.post(api_base + 'processProductOrder', data, config).then(function (results) { return results.data; });
	};

	// PRODUCT ORDER DETAILS --------------------------------------------------------------------------
	obj.getAllProductOrderDetailByOrderId = function(order_id) {
		return $http.get(api_base + 'getAllProductOrderDetailByOrderId?order_id='+order_id);
	};
    obj.insertAllProductOrderDetail = function (object, is_new) {
        return $http.post(api_base + 'insertAllProductOrderDetail?is_new='+is_new, object, config).then(function (results) {
            return results.data;
        });
    };
	
	// USER -----------------------------------------------------------------------------------
	obj.login = function (userdata) {
		return $http.post(api_base + 'login', userdata).then(function (results) { return results; });
	};
	obj.getOneUser = function (id) {
		return $http.get(api_base + 'getOneUser?id=' + id);
	};
	obj.updateOneUser = function (id, user) {
		var data = { id: id, user: user };
		return $http.post(api_base + 'updateOneUser', data, config).then(function (results) { return results.data; });
	};
	obj.insertOneUser = function (user) {
		return $http.post(api_base + 'insertOneUser', user, config).then(function (results) { return results.data; });
	};

	// APP USER -------------------------------------------------------------------------------
	obj.getAllAppUserByPage = function (page, limit, q) {
		return $http.get(api_base + 'getAllAppUserByPage?page=' + page + '&limit=' + limit + '&q=' + encodeURIComponent(q || ''), config);
	};
	obj.getAllAppUserCount = function (q) {
		return $http.get(api_base + 'getAllAppUserCount?q=' + encodeURIComponent(q || ''), config);
	};
	obj.getOneAppUser = function (id) {
		return $http.get(api_base + 'getOneAppUser?id=' + id, config);
	};
	obj.updateAppUserStatus = function (id, active) {
		return $http.post(api_base + 'updateAppUserStatus', {id: id, active: active}, config).then(function (results) { return results.data; });
	};
	obj.deleteAppUser = function (id) {
		return $http.get(api_base + 'deleteAppUser?id=' + id, config).then(function (results) { return results.data; });
	};
	
	// NEWS INFO -----------------------------------------------------------------------
	obj.getAllNewsInfo  = function (id) {
		return $http.get(api_base + 'getAllNewsInfo');
	};
	obj.getOneNewsInfo = function (id) {
		return $http.get(api_base + 'getOneNewsInfo?id='+id);
	};
	obj.getAllNewsInfoByPage = function (page, limit, q) {
		return $http.get(api_base + 'getAllNewsInfoByPage?page='+page+'&limit='+limit+'&q='+encodeURIComponent(q || ''));
	};
	obj.getAllNewsInfoCount = function(q) {
		return $http.get(api_base + 'getAllNewsInfoCount?q='+encodeURIComponent(q || ''));
	};	
	obj.insertOneNewsInfo = function (object) {
		return $http.post(api_base + 'insertOneNewsInfo', object, config).then(function (results) { return results.data; });
	};	
	obj.updateOneNewsInfo = function (id, object) {
		var data = {id:id, news_info:object};
		return $http.post(api_base + 'updateOneNewsInfo', data, config).then(function (results) { return results.data; });
	};
	obj.deleteOneNewsInfo = function (id) {
		return $http.get(api_base + 'deleteOneNewsInfo?id='+id, config).then(function (results) { return results.data; });
	};
	obj.isFeaturedNewsExceed = function (id) {
		return $http.get(api_base + 'isFeaturedNewsExceed').then(function (results) { return results.data; });
	};	
	
	// CONFIG -------------------------------------------------------------------------
	obj.getAllConfig = function () {
		return $http.get(api_base + 'getAllConfig', config);
	};
	obj.updateAllConfig = function (object) {
		return $http.post(api_base + 'updateAllConfig', object, config).then(function (results) { return results.data; });
	};
	// CURRENCY -------------------------------------------------------------------------
	obj.getAllCurrency = function () {
		return $http.get(api_base + 'getAllCurrency');
	};

	// EMAIL SENDER----------------------------------------------------------------------
    obj.sendEmail = function (id, type) {
        return $http.get(api_base + 'sendEmail?id='+id+'&type='+type, config);
    };
	
	// FILE UTILITIES ---------------------------------------------------------------------------------
	obj.uploadFileToUrl = function (f, dir, name, oldname) {
		//console.log(JSON.stringify(resp.data));
		var fd = new FormData();
		fd.append("file", f);
		fd.append("target_dir", dir);
		fd.append("file_name", name);
		fd.append("old_name", oldname);
		var request = {
			method: 'POST', data: fd,
			url: 'app/uploader/uploader.php',
			headers: { 'Content-Type': undefined }
		};
		return $http(request).then(function (resp) { return resp.data; });
	};
	
	obj.deleteFiles = function (dir, names) {
		var data = {target_dir:dir, file_names:names};
		var request = {
			method: 'POST', data:data,
			url: 'app/uploader/delete.php',
			headers: { 'Content-Type': 'application/json' }
		};
		return $http(request).then(function (resp) { return resp.data; });
	};

	return obj;
});
