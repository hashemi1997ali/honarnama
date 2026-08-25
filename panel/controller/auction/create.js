angular.module('App').controller('AddAuctionController', function ($rootScope, $scope, $mdToast, $mdDialog, request) {
    var self = $scope;
    var root = $rootScope;

    if (!root.isCookieExist()) {
        window.location.href = '#/login';
        return;
    }

    var auctionId = root.getCurProductAuctionId();
    var isNew = auctionId == null;
    var uploadDir = '/uploads/product/';

    root.closeAndDisableSearch();
    root.toolbar_menu = null;
    root.pagetitle = isNew ? 'Add Auction' : 'Edit Auction';
    self.button_text = isNew ? 'Save' : 'Update';
    self.submit_loading = false;
    self.image = {valid: !isNew, file: null};

    if (isNew) {
        var starts = roundedDate(1);
        var ends = roundedDate(24 * 7);
        self.auction = {
            name: '',
            image: '',
            description: '',
            start_date: starts,
            end_date: ends,
            start_price: null
        };
    } else {
        request.getOneProductAuction(auctionId).then(function (response) {
            if (!response.data || !response.data.id) {
                root.showInfoDialogSimple('Auctions', 'Auction not found.');
                return;
            }
            self.auction = response.data;
            self.auction.start_date = new Date(self.auction.start_date);
            self.auction.end_date = new Date(self.auction.end_date);
            self.auction.start_date.setMilliseconds(0);
            self.auction.end_date.setMilliseconds(0);
            self.auction.start_price = Number(self.auction.start_price);
        }, function () {
            root.showInfoDialogSimple('Auctions', 'Could not load this auction.');
        });
    }

    function roundedDate(hoursAhead) {
        var date = new Date(Date.now() + hoursAhead * 60 * 60 * 1000);
        date.setSeconds(0, 0);
        return date;
    }

    self.onFileSelect = function (files) {
        var file = files && files.length ? files[0] : null;
        self.image.file = file;
        self.image.valid = file != null && root.constrainFile(file);
        $mdToast.show($mdToast.simple()
            .textContent(self.image.valid ? 'Image selected' : 'Use a JPG or PNG image no larger than 0.5 MB')
            .position('bottom right'));
        self.$applyAsync();
    };

    self.datesValid = function () {
        if (!self.auction) return false;
        var start = new Date(self.auction.start_date);
        var end = new Date(self.auction.end_date);
        return !isNaN(start.getTime()) && !isNaN(end.getTime()) && end > start;
    };

    self.isReadySubmit = function () {
        return self.auction && self.image.valid && self.datesValid() && Number(self.auction.start_price) >= 0;
    };

    self.submit = function (auction) {
        if (!self.isReadySubmit()) return;
        self.submit_loading = true;

        var payload = angular.copy(auction);
        payload.start_date = new Date(payload.start_date).toISOString();
        payload.end_date = new Date(payload.end_date).toISOString();
        payload.start_price = Number(payload.start_price);

        var oldImage = payload.image || '';
        if (self.image.file) {
            payload.image = 'auction-' + Date.now() + root.getExtension(self.image.file);
        }

        var saveRequest = isNew
            ? request.insertOneProductAuction(payload)
            : request.updateOneProductAuction(auctionId, payload);

        saveRequest.then(function (response) {
            if (response.status !== 'success') {
                finishWithError(response.msg || 'Could not save this auction.');
                return;
            }

            if (!self.image.file) {
                finishWithSuccess(response.msg);
                return;
            }

            request.uploadFileToUrl(self.image.file, uploadDir, payload.image, oldImage).then(function (uploadResponse) {
                if (uploadResponse.status === 'success') {
                    finishWithSuccess(response.msg);
                } else {
                    finishWithError(uploadResponse.msg || 'The auction was saved, but its image could not be uploaded.');
                }
            }, function () {
                finishWithError('The auction was saved, but its image could not be uploaded.');
            });
        }, function () {
            finishWithError('Could not save this auction. Check the panel connection and try again.');
        });
    };

    function finishWithSuccess(message) {
        self.submit_loading = false;
        root.showConfirmDialogSimple('', message || 'Auction saved successfully.', function () {
            window.location.href = '#/auction';
        });
    }

    function finishWithError(message) {
        self.submit_loading = false;
        root.showInfoDialogSimple('Auctions', message);
    }

    self.cancel = function () {
        window.location.href = '#/auction';
    };

    self.isNewEntry = function () {
        return isNew;
    };

    self.viewImage = function (event, fileUrl) {
        $mdDialog.show({
            controller: AuctionEditImageDialogController,
            parent: angular.element(document.body),
            targetEvent: event,
            clickOutsideToClose: true,
            locals: {file_url: fileUrl},
            template: '<md-dialog ng-cloak aria-label="Auction image">' +
                '<md-dialog-content style="max-width:800px;max-height:810px">' +
                '<img class="auction-dialog-image" ng-src="{{file_url}}" alt="Auction image">' +
                '</md-dialog-content></md-dialog>'
        });
    };
});

function AuctionEditImageDialogController($scope, file_url) {
    $scope.file_url = file_url;
}
