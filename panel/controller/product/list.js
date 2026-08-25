angular.module('App').controller('ProductController', function ($rootScope, $scope, $http, $mdToast, $mdDialog, $route, request) {
	var self = $scope;
	var root = $rootScope;

	// not login checker
	if (!root.isCookieExist()) { window.location.href = '#/login'; }
	
	root.pagetitle = 'Products';
	self.loading = true;
	self.category_id = -1;
	self.max_item = 20;
	self.max_item_array = [];
	for(var i = 1; i<5; i++){
	    var _value = 20*i;
	    var _text = _value + " items";
	    self.max_item_array.push({value:_value, text:_text});
	}

	root.search_enable = true;
	root.toolbar_menu = { title: 'Add Product' };

	// receiver barAction from rootScope
	self.$on('barAction', function (event, data) {		
		root.setCurProductId("");
		window.location.href = '#/create_product';
	});
	
	// receiver submitSearch from rootScope
	self.$on('submitSearch', function (event, data) {
		self.q = data;
		self.paging.current = 1;
		self.loadPages();
	});

	request.getAllCategory().then(function(resp){
		var temp_category = {id:-1, name:'All Categories'};
		self.categories_data = resp.data;
		self.categories_data.unshift(temp_category);
	});
	
	// load pages from database and display
	self.loadPages = function () {
		var query = self.q || '';
		self.loading = true;
		self.paging.limit = self.max_item;
		request.getAllProductCount(query, self.category_id).then(function (resp) {
			self.paging.total = Math.max(1, Math.ceil(Number(resp.data || 0) / self.paging.limit));
		}, self.handleLoadError);
		request.getAllProductByPage(self.paging.current, self.paging.limit, query, self.category_id).then(function (resp) {
			self.product = angular.isArray(resp.data) ? resp.data : [];
			self.loading = false;
		}, self.handleLoadError);
		
	};

	self.handleLoadError = function () {
		self.loading = false;
		root.showInfoDialogSimple('Products', 'Could not load products. Please check the database connection.');
	};

	// pagination property
	self.paging = {
		total: 0, // total whole item
		current: 1, // start page
		step: 3, // count number display
		limit: self.max_item, // max item per page
		onPageChanged: self.loadPages,
	};
	self.loadPages();
	
	self.editProduct = function(ev, p) {
		root.setCurProductId(p.id);
		window.location.href = '#/create_product';
	};
	
	self.detailsProduct = function(ev, p) {
		$mdDialog.show({
			controller          : DetailsProductControllerDialog,
			templateUrl         : 'view/product/details.html',
			parent              : angular.element(document.body),
			targetEvent         : ev,
			clickOutsideToClose : true,
			locals              : { product: angular.copy(p) }
		})
	};

	self.deleteProduct = function(ev, p) {
		var confirm = $mdDialog.confirm().title('Confirm Deletion');
			confirm.textContent('Are you sure you want to delete this product: '+p.name+'?');
			confirm.targetEvent(ev).ok('Yes').cancel('Cancel');
			
		var dir = "../../../uploads/product/";
		var images_obj = new Array();	
		images_obj.push(p.image);
		request.getAllProductImageByProductId(p.id).then(function(resp){
			for (var i = 0; i < resp.data.length; i++) {
				images_obj.push(resp.data[i].name);
			}
		});
		
		$mdDialog.show(confirm).then(function() {
			request.deleteOneProduct(p.id).then(function(res){
				if(res.status == 'success'){
					request.deleteFiles(dir, images_obj).then(function(res){ });
                    root.showConfirmDialogSimple('', 'Product '+p.name+' was <b>deleted successfully</b>.', function(){
                        self.loadPages();
                    });
				}else{
                    root.showInfoDialogSimple('', 'Could not delete product: '+p.name);
				}
			});
		});

	};

    /* dialog Publish confirmation*/
    self.publishDialog = function (ev, o) {
        $mdDialog.show({
            controller : PublishProductDialogCtl,
	            parent: angular.element(document.body), targetEvent: ev, clickOutsideToClose: true,
	            locals: { obj: angular.copy(o) },
            template:
            '<md-dialog ng-cloak aria-label="publishData">' +
            '  <md-dialog-content>' +
            '   <h2 class="md-title">Confirm Publication</h2> ' +
            '   <p>Are you sure you want to publish this product: <b>{{obj.name}}</b>?</p><br>' +
            '   <div layout="row"> <span flex></span>' +
            '       <md-button ng-if="!submit_loading" class="md-warn" ng-click="cancel()" >Cancel</md-button>' +
            '       <md-button ng-click="publish()" class="md-raised md-primary">Yes</md-button>' +
            '   </div>' +
            '  </md-dialog-content>' +
            '</md-dialog>'
        });
        function PublishProductDialogCtl($scope, $mdDialog, $mdToast, obj) {
        	$scope.obj = angular.copy(obj);
        	$scope.cancel = function() { $mdDialog.cancel(); };
        	$scope.publish = function() {
        	    $scope.obj.draft = 0;
                request.updateOneProduct($scope.obj.id, $scope.obj).then(function(resp){
                    if(resp.status == 'success'){
                        root.showConfirmDialogSimple('', 'Product '+obj.name+' was <b>published successfully</b>.', function(){
							self.loadPages();
                        });
                    }else{
                        var failed_txt = 'Could not publish product: '+obj.name;
                        if(resp.msg != null) failed_txt = resp.msg;
                        root.showInfoDialogSimple('', failed_txt);
                    }
                });
        	};
        }
    };
	
});

function DetailsProductControllerDialog($scope, $mdDialog, request, $mdToast, $route, product) {
	var self        = $scope;
	self.product    = product;
	self.categories = [];
	self.images     = [];
	self.hide   = function() { $mdDialog.hide(); };
	self.cancel = function() { $mdDialog.cancel(); };

	request.getAllCategoryByProductId(self.product.id).then(function(resp){
		self.categories = resp.data;
	});
	request.getAllProductImageByProductId(self.product.id).then(function(resp){
		self.images = resp.data;
	});
}

