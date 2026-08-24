angular.module('App').controller('AuctionController', function ($rootScope, $scope, $mdDialog, request) {
    var self = $scope;
    var root = $rootScope;

    if (!root.isCookieExist()) {
        window.location.href = '#/login';
        return;
    }

    root.pagetitle = 'Auctions';
    root.search_enable = true;
    root.toolbar_menu = {title: 'Add Auction'};
    self.loading = true;
    self.auctions = [];

    self.$on('barAction', function () {
        root.setCurProductAuctionId('');
        window.location.href = '#/create_auction';
    });

    self.$on('submitSearch', function (event, query) {
        self.q = query || '';
        self.paging.current = 1;
        self.loadPages();
    });

    self.loadPages = function () {
        var query = self.q || '';
        self.loading = true;

        request.getAllProductAuctionCount(query).then(function (response) {
            self.paging.total = Math.max(1, Math.ceil(Number(response.data || 0) / self.paging.limit));
        }, self.handleLoadError);

        request.getAllProductAuctionByPage(self.paging.current, self.paging.limit, query).then(function (response) {
            self.auctions = angular.isArray(response.data) ? response.data : [];
            self.loading = false;
        }, self.handleLoadError);
    };

    self.handleLoadError = function () {
        self.loading = false;
        root.showInfoDialogSimple('Auctions', 'Could not load auctions. Please sign in again or check the database connection.');
    };

    self.paging = {
        total: 1,
        current: 1,
        step: 3,
        limit: 20,
        onPageChanged: self.loadPages
    };

    self.statusOf = function (auction) {
        var now = new Date();
        var starts = self.asDate(auction.start_date);
        var ends = self.asDate(auction.end_date);
        if (now < starts) return 'Scheduled';
        if (now <= ends) return 'Active';
        return 'Ended';
    };

    self.asDate = function (value) {
        if (angular.isDate(value)) return value;
        var normalized = String(value || '')
            .replace(' ', 'T')
            .replace(/\.(\d{3})\d+/, '.$1')
            .replace(/([+-]\d{2})$/, '$1:00');
        return new Date(normalized);
    };

    self.editAuction = function (auction) {
        root.setCurProductAuctionId(auction.id);
        window.location.href = '#/create_auction';
    };

    self.deleteAuction = function (event, auction) {
        var confirm = $mdDialog.confirm()
            .title('Delete Auction')
            .textContent('Are you sure you want to delete “' + auction.name + '” and all of its bids?')
            .targetEvent(event)
            .ok('Delete')
            .cancel('Cancel');

        $mdDialog.show(confirm).then(function () {
            request.deleteOneProductAuction(auction.id).then(function (response) {
                if (response.status !== 'success') {
                    root.showInfoDialogSimple('Auctions', response.msg || 'Could not delete this auction.');
                    return;
                }

                request.deleteFiles('../../../uploads/product/', [auction.image]);
                root.showConfirmDialogSimple('', 'Auction <b>' + auction.name + '</b> was deleted successfully.', function () {
                    self.loadPages();
                });
            });
        });
    };

    self.viewImage = function (event, fileUrl) {
        $mdDialog.show({
            controller: AuctionImageDialogController,
            parent: angular.element(document.body),
            targetEvent: event,
            clickOutsideToClose: true,
            file_url: fileUrl,
            template: '<md-dialog ng-cloak aria-label="Auction image">' +
                '<md-dialog-content style="max-width:800px;max-height:810px">' +
                '<img class="auction-dialog-image" ng-src="{{file_url}}" alt="Auction image">' +
                '</md-dialog-content></md-dialog>'
        });
    };
});

function AuctionImageDialogController($scope, file_url) {
    $scope.file_url = file_url;
}
