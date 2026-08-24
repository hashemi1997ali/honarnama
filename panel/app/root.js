angular.module('App').controller('RootCtrl', function ($rootScope, $scope, $mdSidenav, $mdToast, $mdDialog, $cookies, request, focus) {

    var self = $scope;
    var root = $rootScope;

	/* panel name and version */
	root.PANEL_NAME = "فروشگاه";
	root.PANEL_VERSION = "2.1";

	/* Constant String data */
    /* expired session for admin login*/
    var SESSION_EXPIRED = 1; // in days

    /* Data for side menu
     * icon reference : https://material.io/icons/
     */
    self.sidenav = {
		actions: [
			{ name: 'داشبورد', icon: 'store', link: '#dashboard', sub: false },
			{ name: 'لیست سفارشات', icon: 'event_note', link: '#order', sub: false },
			{ name: 'محصولات', icon: 'widgets', link: '#product', sub: false },
			{ name: 'موضوعات', icon: 'dns', link: '#category', sub : false },
			{ name: 'اخبار', icon: 'subject', link: '#news', sub: false },
			{ name: 'تنظیمات', icon: 'settings', link: '#setting', sub: false },
		]
	};

    self.bgColor = '#cccccc';
    self.black = '#000000';

    // flag toolbar action button
    root.search_enable = false;
    root.search_show = false;

    root.base_url = window.location.origin;
    self.uid_key = root.base_url + '_session_uid';
    self.uid_name = root.base_url + '_session_name';
    self.uid_email = root.base_url + '_session_email';
    self.uid_password = root.base_url + '_session_password';

    // retrieve session data
    self.user = {
        name: $cookies.get(self.uid_name),
        email: $cookies.get(self.uid_email)
    };

    // when bar action clicked
    root.barAction = function (ev) {
        root.$broadcast('barAction', "");
    };

    // when search icon click
    root.searchAction = function (ev) {
        focus('search_input');
        root.search_show = true;
        root.$broadcast('searchAction', null);
    };

    // when search close
    root.closeSearch = function (ev) {
        root.search_show = false;
        root.$broadcast('submitSearch', "");
    };

    // when search text submit
    root.submitSearch = function (ev, q) {
        root.$broadcast('submitSearch', q);
    };
    // when search text submit by press enter
    root.keypressAction = function (k_ev, q) {
        if (k_ev.which === 13) {
            root.$broadcast('submitSearch', q);
        }
    };

    root.closeAndDisableSearch = function () {
        root.search_enable = false;
        root.search_show = false;
    };

    // toggle drawer menu
    self.toggleSidenav = function () {
        $mdSidenav('left').toggle();
    };

    self.doLogout = function (ev) {
        var confirm = $mdDialog.confirm().title('تایید خروج')
            .content('آیا مطمئنید میخواهید خارج شوید؟ : ' + root.getSessionName() + ' ?')
            .targetEvent(ev)
            .ok('بله').cancel('انصراف');
        $mdDialog.show(confirm).then(function () {
            // clear session
            root.clearCookies();
            window.location.href = '#login';
            $mdToast.show($mdToast.simple().content('با موفقیت خارج شدید').position('bottom right'));
        });
    };

    root.clearCookies = function () {
        // saving session
        $cookies.remove(self.uid_key, null);
        $cookies.remove(self.uid_name, null);
        $cookies.remove(self.uid_email, null);
        $cookies.remove(self.uid_password, null);
    };

    root.saveCookies = function (id, name, email, password) {
        // saving session
        var now = new Date();
        now.setDate(now.getDate() + SESSION_EXPIRED);
        $cookies.put(self.uid_key, id, {expires: now});
        $cookies.put(self.uid_name, name);
        $cookies.put(self.uid_email, email);
        if (password != '*****') $cookies.put(self.uid_password, password);
    };

    root.isCookieExist = function () {
        var uid = $cookies.get(self.uid_key);
        var name = $cookies.get(self.uid_name);
        var email = $cookies.get(self.uid_email);
        var password = $cookies.get(self.uid_password);
        if (uid == null || name == null || email == null || password == null) {
            return false;
        }
        return true;
    };

    root.getSessionUid = function () {
        return $cookies.get(self.uid_key);
    };
    root.getSessionName = function () {
        return $cookies.get(self.uid_name);
    };
    root.getSessionEmail = function () {
        return $cookies.get(self.uid_email);
    };

    self.directHref = function (href) {
        root.sub_obj = '';
        self.toggleSidenav();
        window.location.href = href;
    };

    root.sub_obj = '';
    root.subMenuAction = function (ev, obj) {
        root.sub_obj = obj.cat_id;
        window.location.href = '#place';
        root.pagetitle = 'مکان : ' + obj.name;
    };

    root.sortArrayOfInt = function (array_of_int) {
        array_of_int.sort(function (a, b) {
            return a - b
        });
    };

    root.getExtension = function (f) {
        return (f.type == "image/jpeg" ? '.jpg' : '.png');
    };
    root.constrainFile = function (f) {
        return ((f.type == "image/jpeg" || f.type == "image/png") && f.size <= 500000);
    };
    root.constrainFilePng = function (f) {
        return (f.type == "image/png" && f.size <= 500000);
    };

    // for editing product
    root.setCurProductId = function (product_id) {
        $cookies.put(root.base_url + 'cur_product_id', product_id);
    };
    root.getCurProductId = function () {
        var product_id = $cookies.get(root.base_url + 'cur_product_id');
        return (product_id != "") ? product_id : null;
    };

    // for editing order
    root.setCurOrderId = function (order_id) {
        $cookies.put(root.base_url + 'cur_order_id', order_id);
    };
    root.getCurOrderId = function () {
        var order_id = $cookies.get(root.base_url + 'cur_order_id');
        return (order_id != "") ? order_id : null;
    };

    // for editing category
    root.setCurCategoryId = function (category_id) {
        $cookies.put(root.base_url + 'cur_category_id', category_id);
    };
    root.getCurCategoryId = function () {
        var category_id = $cookies.get(root.base_url + 'cur_category_id');
        return (category_id != "") ? category_id : null;
    };

    // for editing news info
    root.setCurNewsInfoId = function (news_id) {
        $cookies.put(root.base_url + 'cur_news_info_id', news_id);
    };
    root.getCurNewsInfoId = function () {
        var news_id = $cookies.get(root.base_url + 'cur_news_info_id');
        return (news_id != "") ? news_id : null;
    };

    root.findValue = function (config, code) {
        for (var i = 0; i < config.length; ++i) {
            var obj = config[i];
            if (obj.code == code) return obj.value;
        }
    };

    // show dialog confirmation
    root.showConfirmDialogSimple = function (title, msg, callback) {
        var confirm = $mdDialog.confirm().title(title).content(msg).ok('بسیار خب');
        $mdDialog.show(confirm).then(callback);
    };
    root.showConfirmDialog = function (title, msg, callback) {
        var confirm = $mdDialog.confirm().title(title).content(msg);
        confirm.ok('بسیار خب').cancel('انصراف');
        $mdDialog.show(confirm).then(callback);
    };

    // show dialog info
    root.showInfoDialogSimple = function (title, msg) {
        var alert = $mdDialog.alert().title(title).content(msg).ok('بستن');
        $mdDialog.show(alert)
    };

});
