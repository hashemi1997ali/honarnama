package ir.hashemi.market;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ir.hashemi.market.connection.RestAdapter;
import ir.hashemi.market.connection.callbacks.CallbackOrderHistory;
import ir.hashemi.market.data.DatabaseHandler;
import ir.hashemi.market.data.SharedPref;
import ir.hashemi.market.fragment.FragmentCategory;
import ir.hashemi.market.fragment.FragmentFeaturedNews;
import ir.hashemi.market.model.Order;
import ir.hashemi.market.model.OrderHistoryRequest;
import ir.hashemi.market.model.User;
import ir.hashemi.market.utils.CallbackDialog;
import ir.hashemi.market.utils.DialogUtils;
import ir.hashemi.market.utils.Tools;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityMain extends AppCompatActivity {

    private ActionBar actionBar;
    private Toolbar toolbar;
    private FloatingActionButton fab;
    private View cartFabContainer;
    private TextView cartBadge;
    private CardView search_bar;
    private TextView username;
    private SwipeRefreshLayout swipe_refresh;
    private View parent_view;
    private NavigationView nav_view;
    private DatabaseHandler db;
    private SharedPref sharedPref;
    private Call<CallbackOrderHistory> orderHistoryCall;
    private Dialog dialog_failed = null;
    public boolean category_load = false, news_load = false;

    static ir.hashemi.market.ActivityMain activityMain;

    public static ir.hashemi.market.ActivityMain getInstance() {
        return activityMain;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        applySystemBarInsets();
        addStatusBarBackground();

        activityMain = this;
        db = new DatabaseHandler(this);
        sharedPref = new SharedPref(this);

        initToolbar();
        initDrawerMenu();
        initComponent();
        initFragment();
        swipeProgress(false);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackNavigation();
            }
        });

        // launch instruction when first launch
        if (sharedPref.isFirstLaunch()) {
            startActivity(new Intent(this, ir.hashemi.market.ActivityInstruction.class));
            sharedPref.setFirstLaunch(false);
        }
    }

    private void initToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle(R.string.app_name);
    }

    private void initDrawerMenu() {
        nav_view = (NavigationView) findViewById(R.id.nav_view);
        View headerView = nav_view.getHeaderView(0);
        username = (TextView) headerView.findViewById(R.id.tv_nav_username);
        username.setText(sharedPref.getUserData().username);
        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }
        };
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        nav_view.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(final MenuItem item) {
                onItemSelected(item.getItemId());
                return true;
            }
        });
        nav_view.setItemIconTintList(getResources().getColorStateList(R.color.nav_state_list));
    }

    private void initFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // init fragment slider new product
        FragmentFeaturedNews fragmentFeaturedNews = new FragmentFeaturedNews();
        fragmentTransaction.replace(R.id.frame_content_new_product, fragmentFeaturedNews);
        // init fragment category
        FragmentCategory fragmentCategory = new FragmentCategory();
        fragmentTransaction.replace(R.id.frame_content_category, fragmentCategory);

        fragmentTransaction.commit();
    }

    private void initComponent() {
        parent_view = findViewById(R.id.parent_view);
        search_bar = (CardView) findViewById(R.id.search_bar);
        swipe_refresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        cartFabContainer = findViewById(R.id.cart_fab_container);
        cartBadge = (TextView) findViewById(R.id.cart_badge);
        NestedScrollView nested_content = (NestedScrollView) findViewById(R.id.nested_content);
        nested_content.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if (scrollY < oldScrollY) { // up
                    animateFab(false);
                    animateSearchBar(false);
                }
                if (scrollY > oldScrollY) { // down
                    animateFab(true);
                    animateSearchBar(true);
                }
            }
        });
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), ir.hashemi.market.ActivityShoppingCart.class);
                startActivity(i);
            }
        });

        // on swipe list
        swipe_refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshFragment();
            }
        });

        ((ImageButton) findViewById(R.id.action_search)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivitySearch.navigate(ir.hashemi.market.ActivityMain.this);
            }
        });

    }

    private void applySystemBarInsets() {
        final View root = findViewById(R.id.drawer_layout);
        final View headerSpacer = findViewById(R.id.main_header_spacer);
        final View searchBar = findViewById(R.id.search_bar);
        final NavigationView navigationView = findViewById(R.id.nav_view);

        final int headerBaseHeight = headerSpacer.getLayoutParams().height;
        final ViewGroup.MarginLayoutParams searchBaseParams =
                (ViewGroup.MarginLayoutParams) searchBar.getLayoutParams();
        final int searchBaseTopMargin = searchBaseParams.topMargin;
        final int navigationPaddingLeft = navigationView.getPaddingLeft();
        final int navigationPaddingTop = navigationView.getPaddingTop();
        final int navigationPaddingRight = navigationView.getPaddingRight();
        final int navigationPaddingBottom = navigationView.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets statusBars = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
            Insets navigationBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());

            ViewGroup.LayoutParams headerParams = headerSpacer.getLayoutParams();
            int headerHeight = headerBaseHeight + statusBars.top;
            if (headerParams.height != headerHeight) {
                headerParams.height = headerHeight;
                headerSpacer.setLayoutParams(headerParams);
            }

            ViewGroup.MarginLayoutParams searchParams =
                    (ViewGroup.MarginLayoutParams) searchBar.getLayoutParams();
            int searchTopMargin = searchBaseTopMargin + statusBars.top;
            if (searchParams.topMargin != searchTopMargin) {
                searchParams.topMargin = searchTopMargin;
                searchBar.setLayoutParams(searchParams);
            }

            navigationView.setPadding(
                    navigationPaddingLeft,
                    navigationPaddingTop + statusBars.top,
                    navigationPaddingRight,
                    navigationPaddingBottom + navigationBars.bottom
            );
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private void addStatusBarBackground() {
        ViewGroup decor = (ViewGroup) getWindow().getDecorView();
        View statusBarBackground = new View(this);
        statusBarBackground.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        statusBarBackground.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                Gravity.TOP
        );
        decor.addView(statusBarBackground, params);

        ViewCompat.setOnApplyWindowInsetsListener(statusBarBackground, (view, windowInsets) -> {
            Insets safeTop = windowInsets.getInsets(
                    WindowInsetsCompat.Type.statusBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            if (layoutParams.height != safeTop.top) {
                layoutParams.height = safeTop.top;
                view.setLayoutParams(layoutParams);
            }
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(statusBarBackground);
    }

    private void refreshFragment() {
        category_load = false;
        news_load = false;
        swipeProgress(true);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                initFragment();
            }
        }, 500);
    }

    private void swipeProgress(final boolean show) {
        if (!show) {
            swipe_refresh.setRefreshing(show);
            return;
        }
        swipe_refresh.post(new Runnable() {
            @Override
            public void run() {
                swipe_refresh.setRefreshing(show);
            }
        });
    }

    public boolean onItemSelected(int id) {
        Intent i;
        if (id == R.id.nav_cart) {
            i = new Intent(this, ActivityShoppingCart.class);
            startActivity(i);
        } else if (id == R.id.nav_wish) {
            i = new Intent(this, ActivityWishlist.class);
            startActivity(i);
        } else if (id == R.id.nav_history) {
            i = new Intent(this, ActivityOrderHistory.class);
            startActivity(i);
        } else if (id == R.id.nav_news) {
            i = new Intent(this, ActivityNewsInfo.class);
            startActivity(i);
        } else if (id == R.id.nav_setting) {
            i = new Intent(this, ActivitySettings.class);
            startActivity(i);
        } else if (id == R.id.nav_instruction) {
            i = new Intent(this, ActivityInstruction.class);
            startActivity(i);
        } else if (id == R.id.nav_about) {
            Tools.showDialogAbout(this);
        } else if (id == R.id.nav_exit) {
            dialogLogOut();
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawers();
        return true;
    }

    boolean isFabHide = false;

    public void dialogLogOut() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_logout);
        builder.setMessage(getString(R.string.msg_logout));
        builder.setPositiveButton(R.string.YES, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                logout();
            }
        });
        builder.setNegativeButton(R.string.NO, null);
        builder.show();
    }

    private void logout(){
        sharedPref.clearAllData();
        clearApplicationData();
        Intent i = new Intent(this, ir.hashemi.market.ActivitySplash.class);
        startActivity(i);
        finish();
    }

    public void clearApplicationData() {
        File cacheDirectory = getCacheDir();
        File applicationDirectory = new File(cacheDirectory.getParent());
        if (applicationDirectory.exists()) {
            String[] fileNames = applicationDirectory.list();
            for (String fileName : fileNames) {
                if (!fileName.equals("lib")) {
                    deleteFile(new File(applicationDirectory, fileName));
                }
            }
        }
    }

    public static boolean deleteFile(File file) {
        boolean deletedAll = true;
        if (file != null) {
            if (file.isDirectory()) {
                String[] children = file.list();
                for (int i = 0; i < children.length; i++) {
                    deletedAll = deleteFile(new File(file, children[i])) && deletedAll;
                }
            } else {
                deletedAll = file.delete();
            }
        }

        return deletedAll;
    }

    private void animateFab(final boolean hide) {
        if (isFabHide && hide || !isFabHide && !hide) return;
        isFabHide = hide;
        int moveY = hide ? (2 * cartFabContainer.getHeight()) : 0;
        cartFabContainer.animate().translationY(moveY).setStartDelay(100).setDuration(300).start();
    }

    boolean isSearchBarHide = false;

    private void animateSearchBar(final boolean hide) {
        if (isSearchBarHide && hide || !isSearchBarHide && !hide) return;
        isSearchBarHide = hide;
        int moveY = hide ? -(2 * search_bar.getHeight()) : 0;
        search_bar.animate().translationY(moveY).setStartDelay(100).setDuration(300).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNavCounter(nav_view);
        syncOrderHistory();
    }

    private void handleBackNavigation() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (!drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.openDrawer(GravityCompat.START);
        } else {
            doExitApp();
        }
    }

    private long exitTime = 0;
    public void doExitApp() {
        if ((System.currentTimeMillis() - exitTime) > 2000) {
            Toast.makeText(this, R.string.press_again_exit_app, Toast.LENGTH_SHORT).show();
            exitTime = System.currentTimeMillis();
        } else {
            finish();
        }
    }

    public void showDataLoaded() {
        if (category_load && news_load) {
            swipeProgress(false);
        }
    }

    public void showDialogFailed(@StringRes int msg) {
        if (dialog_failed != null && dialog_failed.isShowing()) return;
        swipeProgress(false);
        dialog_failed = new DialogUtils(this).buildDialogWarning(-1, msg, R.string.TRY_AGAIN, R.drawable.img_no_connect, new CallbackDialog() {
            @Override
            public void onPositiveClick(Dialog dialog) {
                dialog.dismiss();
                refreshFragment();
            }

            @Override
            public void onNegativeClick(Dialog dialog) {
            }
        });
        dialog_failed.show();
    }

    private void updateNavCounter(NavigationView nav) {
        Menu menu = nav.getMenu();
        // update cart counter
        int cart_count = db.getActiveCartSize();
        View cartAction = menu.findItem(R.id.nav_cart).getActionView();
        if (cartAction != null) {
            ((TextView) cartAction.findViewById(R.id.counter)).setText(String.valueOf(cart_count));
        }
        if (cartBadge != null) {
            cartBadge.setText(cart_count > 99 ? "99+" : String.valueOf(cart_count));
            cartBadge.setVisibility(cart_count > 0 ? View.VISIBLE : View.GONE);
        }

        // update wishlist counter
        int wishlist_count = db.getWishlistSize();
        View wishlistAction = menu.findItem(R.id.nav_wish).getActionView();
        if (wishlistAction != null) {
            ((TextView) wishlistAction.findViewById(R.id.counter)).setText(String.valueOf(wishlist_count));
        }

        // update order history counter
        int order_count = db.getOrderSize();
        View historyAction = menu.findItem(R.id.nav_history).getActionView();
        if (historyAction != null) {
            ((TextView) historyAction.findViewById(R.id.counter)).setText(String.valueOf(order_count));
        }

    }

    private void syncOrderHistory() {
        User user = sharedPref.getUserData();
        if (user == null || user.auth_token == null || user.auth_token.trim().isEmpty()) return;

        if (orderHistoryCall != null) orderHistoryCall.cancel();
        OrderHistoryRequest request = new OrderHistoryRequest();
        request.auth_token = user.auth_token;
        for (Order localOrder : db.getOrderList()) {
            if (localOrder.id != null && localOrder.code != null) {
                request.legacy_orders.add(
                        new OrderHistoryRequest.LegacyOrder(localOrder.id, localOrder.code)
                );
            }
        }

        orderHistoryCall = RestAdapter.createAPI().listOrderHistory(request);
        orderHistoryCall.enqueue(new Callback<CallbackOrderHistory>() {
            @Override
            public void onResponse(
                    Call<CallbackOrderHistory> call,
                    Response<CallbackOrderHistory> response
            ) {
                CallbackOrderHistory result = response.body();
                if (!response.isSuccessful() || result == null || !"success".equals(result.status)) {
                    Log.w("OrderHistory", "Main-screen order sync was not successful");
                    return;
                }

                List<Order> orders = result.data == null ? new ArrayList<>() : result.data;
                db.replaceOrderHistory(orders);
                updateNavCounter(nav_view);
            }

            @Override
            public void onFailure(Call<CallbackOrderHistory> call, Throwable throwable) {
                if (!call.isCanceled()) {
                    Log.w("OrderHistory", "Main-screen order sync failed", throwable);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (orderHistoryCall != null) orderHistoryCall.cancel();
        if (activityMain == this) activityMain = null;
        super.onDestroy();
    }


}
