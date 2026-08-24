angular.module('App').controller('LoginController', function ($rootScope, $scope, $http, $mdToast, $route, $timeout, request) {
	var self = $scope;
	var root = $rootScope;

	if (root.isCookieExist()) {
		root.isLogin = false;
		window.location.href = '#dashboard';
		$mdToast.show($mdToast.simple().content('وارد شدید، در حال انتقال به پنل').position('bottom right'));
		window.location.reload();
	}

	root.isLogin = true;
	root.toolbar_menu = null;

	$rootScope.pagetitle = 'ورود';
	self.submit_loading = false;

	self.doLogin = function () {
		self.submit_loading = true;
		request.login(self.userdata).then(function (result) {
		    var resp = result.data;
			$timeout(function () { // give delay for good UI
				self.submit_loading = false;
				if (resp == "") {
				    $mdToast.show($mdToast.simple().content('Login Failed').position('bottom right'));
				    return;
				}
                if(resp.status == "success"){
                    // saving session
                    root.saveCookies(resp.user.id, resp.user.name, resp.user.email, resp.user.password);
                    $mdToast.show($mdToast.simple().content('با موفقیت وارد شدید').position('bottom right'));
                    $route.reload();
                } else {
				    $mdToast.show($mdToast.simple().content('ورود ناموفق').position('bottom right'));
                }
			}, 1000);
			//console.log(JSON.stringify(result.data));
		});
	};

});
