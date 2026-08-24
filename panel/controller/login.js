angular.module('App').controller('LoginController', function ($rootScope, $scope, $mdToast, request) {
	var self = $scope;
	var root = $rootScope;

	function showToast(message) {
		$mdToast.show($mdToast.simple().textContent(message).position('bottom right'));
	}

	if (root.isCookieExist()) {
		root.isLogin = false;
		window.location.href = '#dashboard';
		return;
	}

	root.isLogin = true;
	root.toolbar_menu = null;

	$rootScope.pagetitle = 'ورود';
	self.submit_loading = false;

	self.doLogin = function () {
		self.submit_loading = true;
		request.login(self.userdata).then(function (result) {
			var resp = result.data;
			if (resp && resp.status === 'success' && resp.user) {
				root.saveCookies(resp.user.id, resp.user.name, resp.user.email, resp.user.password);
				root.isLogin = false;
				showToast('با موفقیت وارد شدید');
				window.location.href = '#dashboard';
				return;
			}
			showToast(resp && resp.msg ? resp.msg : 'ورود ناموفق');
		}, function (error) {
			var message = error && error.data && error.data.msg
				? error.data.msg
				: 'ارتباط با سرور برقرار نشد.';
			showToast(message);
		}).finally(function () {
			self.submit_loading = false;
		});
	};

});
