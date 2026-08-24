package ir.hashemi.market;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;


import java.util.ArrayList;
import java.util.List;

import ir.hashemi.market.adapter.AdapterProductImage;
import ir.hashemi.market.connection.API;
import ir.hashemi.market.connection.RestAdapter;
import ir.hashemi.market.connection.callbacks.CallbackProductDetails;
import ir.hashemi.market.data.Constant;
import ir.hashemi.market.data.DatabaseHandler;
import ir.hashemi.market.data.SharedPref;
import ir.hashemi.market.model.Cart;
import ir.hashemi.market.model.Product;
import ir.hashemi.market.model.ProductImage;
import ir.hashemi.market.model.Wishlist;
import ir.hashemi.market.utils.NetworkCheck;
import ir.hashemi.market.utils.Tools;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityProductDetails extends AppCompatActivity {
    private ProgressDialog pDialog;
    private static final String EXTRA_OBJECT_ID = "key.EXTRA_OBJECT_ID";

    // activity transition
    public static void navigate(Activity activity, Long id) {
        Intent i = new Intent(activity, ActivityProductDetails.class);
        i.putExtra(EXTRA_OBJECT_ID, id);
        activity.startActivity(i);
    }

    private Long product_id;

    // extra obj
    private Product product;

    private MenuItem wishlist_menu;
    private boolean flag_wishlist = false;
    private boolean flag_cart = false;
    private DatabaseHandler db;

    private Call<CallbackProductDetails> callbackCall = null;
    private Toolbar toolbar;
    private ActionBar actionBar;
    private View parent_view;
    private SwipeRefreshLayout swipe_refresh;
    private View lyt_add_cart;
    private TextView tv_add_cart;
    private WebView webview = null;
    private SharedPref sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_details);
        initpDialog();
        showpDialog();
        product_id = getIntent().getLongExtra(EXTRA_OBJECT_ID, -1L);

        db = new DatabaseHandler(this);
        sharedPref = new SharedPref(this);

        initToolbar();
        initComponent();
        requestAction();
    }

    private void initToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle("");
    }

    private void initComponent() {
        parent_view = findViewById(android.R.id.content);
        swipe_refresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        lyt_add_cart = findViewById(R.id.lyt_add_cart);
        tv_add_cart = (TextView) findViewById(R.id.tv_add_cart);
        // on swipe
        swipe_refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestAction();
            }
        });

        lyt_add_cart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (product.name == null || product.name.equals("")) {
                    Toast.makeText(getApplicationContext(), R.string.please_wait_text, Toast.LENGTH_SHORT).show();
                    return;
                }
                toggleCartButton();
            }
        });

    }

    private void requestAction() {
        showFailedView(false, "");
        swipeProgress(true);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                requestNewsInfoDetailsApi();
            }
        }, 1000);
    }

    private void onFailRequest() {
        swipeProgress(false);
        if (NetworkCheck.isConnect(this)) {
            showFailedView(true, getString(R.string.failed_text));
        } else {
            showFailedView(true, getString(R.string.no_internet_text));
        }
    }

    private void requestNewsInfoDetailsApi() {
        API api = RestAdapter.createAPI();
        callbackCall = api.getProductDetails(product_id);
        callbackCall.enqueue(new Callback<CallbackProductDetails>() {
            @Override
            public void onResponse(Call<CallbackProductDetails> call, Response<CallbackProductDetails> response) {
                CallbackProductDetails resp = response.body();
                if (resp != null && resp.status.equals("success")) {
                    product = resp.product;
                    displayPostData();
                    swipeProgress(false);
                } else {
                    onFailRequest();
                }
            }

            @Override
            public void onFailure(Call<CallbackProductDetails> call, Throwable t) {
                Log.e("onFailure", t.getMessage());
                if (!call.isCanceled()) onFailRequest();
            }
        });
    }

    private void displayPostData() {
        ((TextView) findViewById(R.id.title)).setText(Html.fromHtml(product.name));

        webview = (WebView) findViewById(R.id.content);
        String html_data = "<style>img{max-width:100%;height:auto;} iframe{width:100%;}</style> ";
        html_data += product.description;
        //webview.loadDataWithBaseURL("", html_data, "text/html", "UTF-8", "");
        //webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setBuiltInZoomControls(true);
        webview.setBackgroundColor(Color.TRANSPARENT);
        webview.setWebChromeClient(new WebChromeClient());
        // webview.loadData(html_data, "text/html; charset=UTF-8", null);
        webview.loadDataWithBaseURL("", "<html dir=\"ltr\" lang=\"en\"><body <style>" + html_data + "</body></html>", "text/html", "UTF-8", null);
        // disable scroll on touch
        webview.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return (event.getAction() == MotionEvent.ACTION_MOVE);
            }
        });

        // ((TextView) findViewById(R.id.date)).setText(Tools.getFormattedDate(product.last_update));
        String date = Tools.getFormattedDate(product.last_update);
        Log.i("LOG", date);
        ((TextView) findViewById(R.id.date)).setText(ir.hashemi.market.ShamsiCalleder.getCurrentShamsidate(date));
        ((TextView) findViewById(R.id.price)).setText(product.price.longValue() + " " + sharedPref.getInfoData().currency);
        if (product.status.equalsIgnoreCase("READY STOCK")) {
            ((TextView) findViewById(R.id.status)).setText(getString(R.string.ready_stock));
        } else if (product.status.equalsIgnoreCase("OUT OF STOCK")) {
            ((TextView) findViewById(R.id.status)).setText(getString(R.string.out_of_stock));
        } else if (product.status.equalsIgnoreCase("SUSPEND")) {
            ((TextView) findViewById(R.id.status)).setText(getString(R.string.suspend));
        } else {
            ((TextView) findViewById(R.id.status)).setText(product.status);
        }

        // display Image slider
        displayImageSlider();

        // display category list at bottom
        displayCategoryProduct();

        //Toast.makeText(this, R.string.msg_data_loaded, Toast.LENGTH_SHORT).show();
        hidepDialog();
    }

    private void displayImageSlider() {
        final LinearLayout layout_dots = (LinearLayout) findViewById(R.id.layout_dots);
        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        final AdapterProductImage adapterSlider = new AdapterProductImage(this, new ArrayList<ProductImage>());

        final List<ProductImage> productImages = new ArrayList<>();
        ProductImage p = new ProductImage();
        p.product_id = product.id;
        p.name = product.image;
        productImages.add(p);
        if (product.product_images != null) productImages.addAll(product.product_images);
        adapterSlider.setItems(productImages);
        viewPager.setAdapter(adapterSlider);

        // displaying selected image first
        viewPager.setCurrentItem(0);
        addBottomDots(layout_dots, adapterSlider.getCount(), 0);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int pos, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int pos) {
                addBottomDots(layout_dots, adapterSlider.getCount(), pos);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });


        final ArrayList<String> images_list = new ArrayList<>();
        for (ProductImage img : productImages) {
            images_list.add(Constant.getURLimgProduct(img.name));
        }

        adapterSlider.setOnItemClickListener(new AdapterProductImage.OnItemClickListener() {
            @Override
            public void onItemClick(View view, ProductImage obj, int pos) {
                Intent i = new Intent(ir.hashemi.market.ActivityProductDetails.this, ir.hashemi.market.ActivityFullScreenImage.class);
                i.putExtra(ir.hashemi.market.ActivityFullScreenImage.EXTRA_POS, pos);
                i.putStringArrayListExtra(ir.hashemi.market.ActivityFullScreenImage.EXTRA_IMGS, images_list);
                startActivity(i);
            }
        });
    }

    private void displayCategoryProduct() {
        TextView category = (TextView) findViewById(R.id.category);
        String html_data = "";
        for (int i = 0; i < product.categories.size(); i++) {
            html_data += (i + 1) + ". " + product.categories.get(i).name + "\n";
        }
        category.setText(html_data);
    }

    private void addBottomDots(LinearLayout layout_dots, int size, int current) {
        ImageView[] dots = new ImageView[size];

        layout_dots.removeAllViews();
        for (int i = 0; i < dots.length; i++) {
            dots[i] = new ImageView(this);
            int width_height = 15;
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(new ViewGroup.LayoutParams(width_height, width_height));
            params.setMargins(10, 10, 10, 10);
            dots[i].setLayoutParams(params);
            dots[i].setImageResource(R.drawable.shape_circle);
            dots[i].setColorFilter(ContextCompat.getColor(this, R.color.darkOverlaySoft));
            layout_dots.addView(dots[i]);
        }

        if (dots.length > 0)
            dots[current].setColorFilter(ContextCompat.getColor(this, R.color.colorPrimaryLight));
    }

    private void showFailedView(boolean show, String message) {
        View lyt_failed = (View) findViewById(R.id.lyt_failed);
        View lyt_main_content = (View) findViewById(R.id.lyt_main_content);

        ((TextView) findViewById(R.id.failed_message)).setText(message);
        if (show) {
            lyt_main_content.setVisibility(View.GONE);
            lyt_failed.setVisibility(View.VISIBLE);
        } else {
            lyt_main_content.setVisibility(View.VISIBLE);
            lyt_failed.setVisibility(View.GONE);
        }
        ((Button) findViewById(R.id.failed_retry)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestAction();
            }
        });
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_activity_product_details, menu);
        wishlist_menu = menu.findItem(R.id.action_wish);
        refreshWishlistMenu();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int item_id = item.getItemId();
        if (item_id == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
        } else if (item_id == R.id.action_wish) {
            if (product.name == null || product.name.equals("")) {
                Toast.makeText(this, R.string.cannot_add_wishlist, Toast.LENGTH_SHORT).show();
                return true;
            }
            if (flag_wishlist) {
                db.deleteWishlist(product_id);
                Toast.makeText(this, R.string.remove_wishlist, Toast.LENGTH_SHORT).show();
            } else {
                Wishlist w = new Wishlist(product.id, product.name, product.image, System.currentTimeMillis());
                db.saveWishlist(w);
                Toast.makeText(this, R.string.add_wishlist, Toast.LENGTH_SHORT).show();
            }
            refreshWishlistMenu();
        } else if (item_id == R.id.action_cart) {
            Intent i = new Intent(this, ir.hashemi.market.ActivityShoppingCart.class);
            startActivity(i);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webview != null) webview.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webview != null) webview.onPause();
        refreshCartButton();
    }

    private void refreshWishlistMenu() {
        Wishlist w = db.getWishlist(product_id);
        flag_wishlist = (w != null);
        if (flag_wishlist) {
            wishlist_menu.setIcon(R.drawable.ic_wish);
        } else {
            wishlist_menu.setIcon(R.drawable.ic_wish_outline);
        }
    }

    private void toggleCartButton() {
        if (flag_cart) {
            db.deleteActiveCart(product_id);
            Toast.makeText(this, R.string.remove_cart, Toast.LENGTH_SHORT).show();
        } else {
            // check stock product
            if (product.stock == 0 || product.status.equalsIgnoreCase("OUT OF STOCK")) {
                Toast.makeText(this, R.string.msg_out_of_stock, Toast.LENGTH_SHORT).show();
                return;
            }
            if (product.status.equalsIgnoreCase("SUSPEND")) {
                Toast.makeText(this, R.string.msg_suspend, Toast.LENGTH_SHORT).show();
                return;
            }
            Cart cart = new Cart(product.id, product.name, product.image, 1, product.stock, product.price, System.currentTimeMillis());
            db.saveCart(cart);
            Toast.makeText(this, R.string.add_cart, Toast.LENGTH_SHORT).show();
        }
        refreshCartButton();
    }

    private void refreshCartButton() {
        Cart c = db.getCart(product_id);
        flag_cart = (c != null);
        if (flag_cart) {
            lyt_add_cart.setBackgroundColor(getResources().getColor(R.color.colorRemoveCart));
            tv_add_cart.setText(R.string.bt_remove_cart);
        } else {
            lyt_add_cart.setBackgroundColor(getResources().getColor(R.color.colorAddCart));
            tv_add_cart.setText(R.string.bt_add_cart);
        }
    }

    protected void initpDialog() {

        pDialog = new ProgressDialog(this);
        pDialog.setMessage(getString(R.string.msg_loading));
        pDialog.setCancelable(false);
    }

    protected void showpDialog() {

        if (!pDialog.isShowing()) {

            try {

                pDialog.show();

            } catch (Exception e) {

                e.printStackTrace();
            }
        }
    }

    protected void hidepDialog() {

        if (pDialog.isShowing()) {

            try {

                pDialog.dismiss();

            } catch (Exception e) {

                e.printStackTrace();
            }
        }
    }
}
