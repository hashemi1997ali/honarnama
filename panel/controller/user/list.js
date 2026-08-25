angular.module('App').controller('AppUserController', function ($rootScope, $scope, $mdDialog, $mdToast, request) {
    var self = $scope;
    var root = $rootScope;

    if (!root.isCookieExist()) {
        window.location.href = '#/login';
        return;
    }

    root.pagetitle = 'Users';
    root.search_enable = true;
    root.toolbar_menu = null;
    self.loading = true;
    self.users = [];

    self.$on('submitSearch', function (event, query) {
        self.q = query || '';
        self.paging.current = 1;
        self.loadPages();
    });

    self.loadPages = function () {
        var query = self.q || '';
        self.loading = true;

        request.getAllAppUserCount(query).then(function (response) {
            self.paging.total = Math.max(1, Math.ceil(Number(response.data || 0) / self.paging.limit));
        }, self.handleLoadError);

        request.getAllAppUserByPage(self.paging.current, self.paging.limit, query).then(function (response) {
            self.users = angular.isArray(response.data) ? response.data : [];
            self.loading = false;
        }, self.handleLoadError);
    };

    self.handleLoadError = function () {
        self.loading = false;
        root.showInfoDialogSimple('Users', 'Could not load application users. Please check the database connection.');
    };

    self.paging = {
        total: 1,
        current: 1,
        step: 3,
        limit: 20,
        onPageChanged: self.loadPages
    };
    self.loadPages();

    self.isActive = function (user) {
        return user.active === true || Number(user.active) === 1;
    };

    self.asDate = function (value) {
        if (angular.isDate(value)) return value;
        var normalized = String(value || '')
            .replace(' ', 'T')
            .replace(/\.(\d{3})\d+/, '.$1')
            .replace(/([+-]\d{2})$/, '$1:00');
        return new Date(normalized);
    };

    self.detailsUser = function (event, user) {
        request.getOneAppUser(user.id).then(function (response) {
            $mdDialog.show({
                controller: AppUserDetailsDialogController,
                templateUrl: 'view/user/details.html',
                parent: angular.element(document.body),
                targetEvent: event,
                clickOutsideToClose: true,
                locals: {user: response.data}
            });
        }, self.handleLoadError);
    };

    self.toggleUserStatus = function (event, user) {
        var nextActive = !self.isActive(user);
        var action = nextActive ? 'Enable' : 'Disable';
        var confirm = $mdDialog.confirm()
            .title(action + ' User')
            .textContent('Are you sure you want to ' + action.toLowerCase() + ' “' + user.username + '”?')
            .targetEvent(event)
            .ok(action)
            .cancel('Cancel');

        $mdDialog.show(confirm).then(function () {
            request.updateAppUserStatus(user.id, nextActive).then(function (response) {
                if (response.status !== 'success') {
                    root.showInfoDialogSimple('Users', response.msg || 'Could not update this user.');
                    return;
                }
                user.active = nextActive ? 1 : 0;
                user.last_update = new Date().toISOString();
                $mdToast.show($mdToast.simple().textContent(response.msg).position('bottom right'));
            });
        });
    };

    self.deleteUser = function (event, user) {
        var confirm = $mdDialog.confirm()
            .title('Delete User')
            .textContent('Delete “' + user.username + '”? Their bids will also be removed. This cannot be undone.')
            .targetEvent(event)
            .ok('Delete')
            .cancel('Cancel');

        $mdDialog.show(confirm).then(function () {
            request.deleteAppUser(user.id).then(function (response) {
                if (response.status !== 'success') {
                    root.showInfoDialogSimple('Users', response.msg || 'Could not delete this user.');
                    return;
                }
                self.loadPages();
                $mdToast.show($mdToast.simple().textContent('User deleted.').position('bottom right'));
            });
        });
    };
});

function AppUserDetailsDialogController($scope, $mdDialog, user) {
    $scope.user = user;
    $scope.isActive = user && (user.active === true || Number(user.active) === 1);
    $scope.asDate = function (value) {
        var normalized = String(value || '')
            .replace(' ', 'T')
            .replace(/\.(\d{3})\d+/, '.$1')
            .replace(/([+-]\d{2})$/, '$1:00');
        return new Date(normalized);
    };
    $scope.cancel = function () { $mdDialog.cancel(); };
}
