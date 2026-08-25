angular.module('App').controller('OrderController', function ($rootScope, $scope, $http, $mdToast, $mdDialog, $cookies, request) {
	if (!$scope.isCookieExist()) { window.location.href = '#/login'; }

    var self = $scope;
	var root = $rootScope;

	root.search_enable = true;
    root.toolbar_menu = { title: 'Add Order' };
	root.pagetitle = 'Orders';
	self.loading = true;
	self.product_order = [];

	// receiver barAction from rootScope
    self.$on('barAction', function (event, data) {
        root.setCurOrderId("");
        window.location.href = '#/create_order';
    });

    // receiver submitSearch from rootScope
    self.$on('submitSearch', function (event, data) {
        self.q = data;
        self.paging.current = 1;
        self.loadPages();
    });

	self.loadPages = function () {
		var query = self.q || '';
		self.loading = true;
		request.getAllProductOrderCount(query).then(function (resp) {
			self.paging.total = Math.max(1, Math.ceil(Number(resp.data || 0) / self.paging.limit));
		}, self.handleLoadError);
		request.getAllProductOrderByPage(self.paging.current, self.paging.limit, query).then(function (resp) {
			self.product_order = angular.isArray(resp.data) ? resp.data : [];
			self.loading = false;
		}, self.handleLoadError);
	}

	self.handleLoadError = function () {
		self.loading = false;
		root.showInfoDialogSimple('Orders', 'Could not load orders. Please check the database connection.');
	};

	//pagination property
	self.paging = {
		total: 0, // total whole item
		current: 1, // start page
		step: 3, // count number display
		limit: 20, // max item per page
		onPageChanged: self.loadPages,
	};
	self.loadPages();

    self.editOrder = function(ev, po) {
        root.setCurOrderId(po.id);
        window.location.href = '#/create_order';
    };

	self.detailsOrder = function(ev, po) {
		$mdDialog.show({
			controller          : DetailsOrderControllerDialog,
			templateUrl         : 'view/order/details.html',
			parent              : angular.element(document.body),
			targetEvent         : ev,
			clickOutsideToClose : true,
			locals              : { order: angular.copy(po), process: false }
		})
	};

    self.processedOrderConfirm = function(ev, po) {
        var confirm = $mdDialog.confirm().title('Process Order');
            confirm.textContent('Processing is final and reduces product stock. Review the order before continuing.');
            confirm.targetEvent(ev).ok('Process').cancel('Cancel');

        $mdDialog.show(confirm).then(function() {
            $mdDialog.show({
                controller          : DetailsOrderControllerDialog,
                templateUrl         : 'view/order/details.html',
                parent              : angular.element(document.body),
                targetEvent         : ev,
                clickOutsideToClose : true,
	            locals              : { order: angular.copy(po), process: true }
            })
        });
    };

    self.cancelOrder = function(ev, po) {
        var confirm = $mdDialog.confirm().title('Cancel Order');
            confirm.textContent('Cancel the order for '+po.buyer+'? The record will be kept.');
            confirm.targetEvent(ev).ok('Cancel Order').cancel('Keep Order');

        $mdDialog.show(confirm).then(function() {
            var new_ob = angular.copy(po);
            new_ob.status = 'CANCEL';
            request.updateOneProductOrder(new_ob.id, new_ob).then(function(resp){
                if(resp.status == 'success'){
				    root.showConfirmDialogSimple('', 'The order for '+po.buyer+' was <b>cancelled successfully</b>.', function(){
				        self.loadPages();
				    });
                }else{
                    root.showInfoDialogSimple('', 'Could not delete the order for '+po.buyer+'.');
                }
            });
        });
    };

    self.deleteOrder = function(ev, po) {
        var confirm = $mdDialog.confirm().title('Delete Order');
            confirm.textContent('Permanently delete the cancelled order for '+po.buyer+'? This cannot be undone.');
            confirm.targetEvent(ev).ok('Delete').cancel('Keep Order');

        $mdDialog.show(confirm).then(function() {
            request.deleteOneProductOrder(po.id).then(function(resp){
                if(resp.status == 'success'){
                    root.showConfirmDialogSimple('', 'The order for '+po.buyer+' was <b>deleted successfully</b>.', function(){
                        self.loadPages();
                    });
                }else{
                    root.showInfoDialogSimple('', 'Could not delete the order for '+po.buyer+'.');
                }
            });
        });

    };
});

function DetailsOrderControllerDialog($scope, $rootScope, $mdDialog, request, $mdToast, $route, order, process) {
	var self        	= $scope;
	var root            = $rootScope;
	self.order      	= angular.copy(order);
	self.process      	= process;
	self.order_details 	= [];
	self.loading_details = true;
	self.processing = false;
	self.hide   = function() { $mdDialog.hide(); };
	self.cancel = function() { $mdDialog.cancel(); };
	self.order.total_fees = parseFloat(self.order.total_fees).toFixed(2)

	request.getAllProductOrderDetailByOrderId(order.id).then(function (resp) {
		self.order_details = angular.isArray(resp.data) ? resp.data : [];
		self.loading_details = false;
        // calculate data
        self.calculateTotal();
	}, function() {
		self.loading_details = false;
		root.showInfoDialogSimple('Orders', 'Could not load the order items.');
	});

    request.getAllConfig().then(function (resp) {
        self.config = resp.data;
        self.conf_currency = root.findValue(self.config, 'CURRENCY');
        self.conf_tax = root.findValue(self.config, 'TAX');
        self.conf_featured_news = root.findValue(self.config, 'FEATURED_NEWS');
    });

	self.getPriceTotal = function (pod) {
	    return parseFloat(pod.price_item*pod.amount).toFixed(2);
    };

	self.calculateTotal = function () {
	    var price_total = 0;
	    var price_tax = 0;
	    self.amount_total = 0;
	    self.price_tax_formatted = 0;
	    self.price_total_formatted = 0;
	    self.price_after_tax = 0;
        for(var i=0; i<self.order_details.length; i++){
            self.amount_total += self.order_details[i].amount;
            price_total += self.order_details[i].price_item * self.order_details[i].amount;
        }
	    price_tax = (self.order.tax / 100) * price_total;
        self.price_tax_formatted = parseFloat(price_tax).toFixed(2);
        self.price_total_formatted = parseFloat(price_total).toFixed(2);
        self.price_after_tax = parseFloat(price_total + price_tax).toFixed(2);
    };

    self.processOrder = function (od) {
		if(self.processing || self.loading_details || self.order_details.length === 0) return;
		self.processing = true;
        request.processProductOrder(od.id, od, self.order_details).then(function(resp){
            $mdDialog.show({
                templateUrl         : 'view/order/process_result.html',
                parent              : angular.element(document.body),
                clickOutsideToClose : false,
				locals              : { resp: resp, order: od },
                controller          : function DialogController($scope, $rootScope, $mdDialog, $route, resp, order) {
                    $scope.resp     = resp;
                    $scope.order    = order;
                    $scope.success  = ( resp.status == 'success' );
                    $scope.cancel   = function() {
                        $mdDialog.cancel();
                        if($scope.success){
                            window.location.reload();
                        }
                    };
                    $scope.edit   = function() {
                        $mdDialog.cancel();
                        root.setCurOrderId(od.id);
                        window.location.href = '#/create_order';
                    };
                }
            });
		}, function() {
			self.processing = false;
			root.showInfoDialogSimple('Orders', 'The order could not be processed. Please try again.');
        });
    };
}
