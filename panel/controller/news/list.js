angular.module('App').controller('NewsController', function ($rootScope, $scope, $http, $mdToast, $mdDialog, $route, request) {
	// not login checker
	if (!$rootScope.isCookieExist()) { window.location.href = '#/login'; }

	var self = $scope;
	var root = $rootScope;
	
	root.pagetitle = 'News';
	self.loading = true;
	self.news = [];

	root.search_enable = true;
	root.toolbar_menu = { title: 'Add News' }

	// receiver barAction from rootScope
	self.$on('barAction', function (event, data) {		
		root.setCurNewsInfoId("");
		window.location.href = '#/create_news';
	});
	
	// receiver submitSearch from rootScope
	self.$on('submitSearch', function (event, data) {
		self.q = data;
		self.paging.current = 1;
		self.loadPages();
	});
	
	// load pages from database and display
	self.loadPages = function () {
		var query = self.q || '';
		self.loading = true;
		request.getAllNewsInfoCount(query).then(function (resp) {
			self.paging.total = Math.max(1, Math.ceil(Number(resp.data || 0) / self.paging.limit));
		}, self.handleLoadError);
		request.getAllNewsInfoByPage(self.paging.current, self.paging.limit, query).then(function (resp) {
			self.news = angular.isArray(resp.data) ? resp.data : [];
			self.loading = false;
		}, self.handleLoadError);
		
	};

	self.handleLoadError = function () {
		self.loading = false;
		root.showInfoDialogSimple('News', 'Could not load news. Please check the database connection.');
	};

	// pagination property
	self.paging = {
		total: 0, // total whole item
		current: 1, // start page
		step: 3, // count number display
		limit: 30, // max item per page
		onPageChanged: self.loadPages,
	};
	self.loadPages();
	
	self.editNewsInfo = function(ev, n) {
		root.setCurNewsInfoId(n.id);
		window.location.href = '#/create_news';
	};
	
	self.detailsNewsInfo = function(ev, n) {
		$mdDialog.show({
			controller          : DetailsNewsInfoControllerDialog,
			templateUrl         : 'view/news/details.html',
			parent              : angular.element(document.body),
			targetEvent         : ev,
			clickOutsideToClose : true,
			news             	: n
		})
	};

	self.deleteNewsInfo = function(ev, n) {
		var confirm = $mdDialog.confirm().title('Delete Confirmation');
			confirm.textContent('Are you sure you want to delete this news item: '+n.title+'?');
			confirm.targetEvent(ev).ok('Yes').cancel('Cancel');
			
		var dir = "../../../uploads/news/";
		var images_obj = new Array();	
		images_obj.push(n.image);
		$mdDialog.show(confirm).then(function() {
			request.deleteOneNewsInfo(n.id).then(function(resp){
				if(resp.status == 'success'){
					request.deleteFiles(dir, images_obj).then(function(res){ });
				    root.showConfirmDialogSimple('', 'News item '+n.title+' was <b>deleted successfully</b>.', function(){
		        self.loadPages();
				    });
				}else{
				    var failed_txt = '';
                    if(resp.msg != null) failed_txt += '<br>' + resp.msg;
                    root.showInfoDialogSimple('Deletion Failed', failed_txt);
				}
			});
		});

	};

    /* dialog Publish confirmation*/
    self.publishDialog = function (ev, o) {
        $mdDialog.show({
            controller : PublishNewsInfoDialogCtl,
            parent: angular.element(document.body), targetEvent: ev, clickOutsideToClose: true, obj: o,
            template:
            '<md-dialog ng-cloak aria-label="publishData">' +
            '  <md-dialog-content>' +
            '   <h2 class="md-title">Confirm Publication</h2> ' +
            '   <p>Are you sure you want to publish this news item: <b>{{obj.title}}</b>?</p><br>' +
            '   <div layout="row"> <span flex></span>' +
            '       <md-button ng-if="!submit_loading" class="md-warn" ng-click="cancel()" >Cancel</md-button>' +
            '       <md-button ng-click="publish()" class="md-raised md-primary">Yes</md-button>' +
            '   </div>' +
            '  </md-dialog-content>' +
            '</md-dialog>'
        });
        function PublishNewsInfoDialogCtl($scope, $mdDialog, $mdToast, obj) {
            $scope.obj = angular.copy(obj);
            $scope.cancel = function() { $mdDialog.cancel(); };
            $scope.publish = function() {
                $scope.obj.draft = 0;
                request.updateOneNewsInfo($scope.obj.id, $scope.obj).then(function(resp){
                    if(resp.status == 'success'){
                        root.showConfirmDialogSimple('', 'News item '+obj.title+' was <b>published successfully</b>.', function(){
							self.loadPages();
                        });
                    }else{
                        var failed_txt = 'Could not publish news item: '+obj.title;
                        if(resp.msg != null) failed_txt = resp.msg;
				        root.showInfoDialogSimple('', failed_txt);
                    }
                });
            };
        }
    };
	
});

function DetailsNewsInfoControllerDialog($scope, $mdDialog, request, $mdToast, $route, news) {
	var self    = $scope;
	self.news   = news;
	self.hide   = function() { $mdDialog.hide(); };
	self.cancel = function() { $mdDialog.cancel(); };
}

