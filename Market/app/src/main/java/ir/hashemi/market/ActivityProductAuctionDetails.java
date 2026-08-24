package ir.hashemi.market;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
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
import android.widget.EditText;
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


import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ir.hashemi.market.adapter.AdapterProductImage;
import ir.hashemi.market.connection.API;
import ir.hashemi.market.connection.RestAdapter;
import ir.hashemi.market.connection.callbacks.CallbackBid;
import ir.hashemi.market.connection.callbacks.CallbackProductAuction;
import ir.hashemi.market.connection.callbacks.CallbackProductAuctionDetails;
import ir.hashemi.market.connection.callbacks.CallbackProductDetails;
import ir.hashemi.market.connection.callbacks.CallbackUser;
import ir.hashemi.market.data.Constant;
import ir.hashemi.market.data.DatabaseHandler;
import ir.hashemi.market.data.SharedPref;
import ir.hashemi.market.model.Bid;
import ir.hashemi.market.model.Cart;
import ir.hashemi.market.model.Product;
import ir.hashemi.market.model.ProductAuction;
import ir.hashemi.market.model.ProductImage;
import ir.hashemi.market.model.User;
import ir.hashemi.market.model.Wishlist;
import ir.hashemi.market.utils.NetworkCheck;
import ir.hashemi.market.utils.Tools;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityProductAuctionDetails extends AppCompatActivity {
    private ProgressDialog pDialog;
    private static final String EXTRA_OBJECT_ID = "key.EXTRA_OBJECT_ID";

    // activity transition
    public static void navigate(Activity activity, Long id) {
        Intent i = new Intent(activity, ActivityProductAuctionDetails.class);
        i.putExtra(EXTRA_OBJECT_ID, id);
        activity.startActivity(i);
    }

    private Long product_id;

    // extra obj
    private ProductAuction product_auction;

    private MenuItem wishlist_menu;

    private Call<CallbackProductAuctionDetails> callbackCall = null;
    private Call<CallbackBid> callbackBidCall = null;
    private Toolbar toolbar;
    private ActionBar actionBar;
    private View parent_view;
    private SwipeRefreshLayout swipe_refresh;
    private View lyt_add_bid;
    private TextView tv_add_bid;
    private EditText et_bid_price;
    private WebView webview = null;
    private SharedPref sharedPref;
    private LinearLayout lyt_text_time;
    private LinearLayout lyt_time;
    private TextView remaining_time;
    private CountDownTimer countDownTimer;
    private long timeLeftInMillis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_auction_details);

        initpDialog();
        showpDialog();
        product_id = getIntent().getLongExtra(EXTRA_OBJECT_ID, -1L);

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
        lyt_add_bid = findViewById(R.id.lyt_add_bid);
        tv_add_bid = (TextView) findViewById(R.id.tv_add_bid);
        et_bid_price = (EditText) findViewById(R.id.et_bid_price);
        // on swipe
        swipe_refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestAction();
            }
        });

        lyt_add_bid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (product_auction.name == null || product_auction.name.equals("")) {
                    Toast.makeText(getApplicationContext(), R.string.please_wait_text, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (et_bid_price.getText().toString().trim().equals("")) {
                    Toast.makeText(ActivityProductAuctionDetails.this, R.string.bid_price_not_acceptable, Toast.LENGTH_SHORT).show();
                    return;
                }
                Bid bid = new Bid(
                        product_auction.id,
                        sharedPref.getUserData().id,
                        Double.parseDouble(et_bid_price.getText().toString())
                );
                API api = RestAdapter.createAPI();
                callbackBidCall = api.addBid(bid);
                callbackBidCall.enqueue(new Callback<CallbackBid>() {
                    @Override
                    public void onResponse(Call<CallbackBid> call, Response<CallbackBid> response) {
                        CallbackBid resp = response.body();
                        if (resp != null && resp.status.equals("success")) {
                            Bid bid = resp.data;
                            Toast.makeText(ActivityProductAuctionDetails.this, getString(R.string.bid_added_seccessful), Toast.LENGTH_SHORT).show();
                            requestAction();
                        } else {
                            Toast.makeText(ActivityProductAuctionDetails.this, resp.msg, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<CallbackBid> call, Throwable t) {
                        Log.e("onFailure", t.getMessage());
                        if (!call.isCanceled()) onFailRequest();
                    }
                });
            }
        });

    }

    private void requestAction() {
        showFailedView(false, "");
        swipeProgress(true);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                requestProductAuctionDetailsApi();
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

    private void requestProductAuctionDetailsApi() {
        API api = RestAdapter.createAPI();
        callbackCall = api.getProductAuctionDetails(product_id);
        callbackCall.enqueue(new Callback<CallbackProductAuctionDetails>() {
            @Override
            public void onResponse(Call<CallbackProductAuctionDetails> call, Response<CallbackProductAuctionDetails> response) {
                CallbackProductAuctionDetails resp = response.body();
                if (resp != null && resp.status.equals("success")) {
                    product_auction = resp.product_auction;
                    displayPostData();
                    swipeProgress(false);
                } else {
                    onFailRequest();
                }
            }

            @Override
            public void onFailure(Call<CallbackProductAuctionDetails> call, Throwable t) {
                Log.e("onFailure", t.getMessage());
                if (!call.isCanceled()) onFailRequest();
            }
        });
    }

    private void displayPostData() {
        ((TextView) findViewById(R.id.title)).setText(Html.fromHtml(product_auction.name));

        webview = (WebView) findViewById(R.id.content);
        String html_data = "<style>img{max-width:100%;height:auto;} iframe{width:100%;}</style> ";
        html_data += product_auction.description;
        // webview.loadDataWithBaseURL("", html_data, "text/html", "UTF-8", "");
        // webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setBuiltInZoomControls(true);
        webview.setBackgroundColor(Color.TRANSPARENT);
        webview.setWebChromeClient(new WebChromeClient());
        // webview.loadData(html_data, "text/html; charset=UTF-8", null);
        webview.loadDataWithBaseURL("", "<html dir=\"rtl\" lang=\"\"><body <style>" + html_data + "</body></html>", "text/html", "UTF-8", null);
        // disable scroll on touch
        webview.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return (event.getAction() == MotionEvent.ACTION_MOVE);
            }
        });

        // ((TextView) findViewById(R.id.date)).setText(Tools.getFormattedDate(product.last_update));
        Log.i("LOG", product_auction.start_date);
        if (product_auction.winner_price == null)
            ((TextView) findViewById(R.id.winner)).setText("-");
        else
            ((TextView) findViewById(R.id.winner)).setText(product_auction.winner_username);
        if (product_auction.winner_price == null)
            ((TextView) findViewById(R.id.price)).setText("- " + sharedPref.getInfoData().currency);
        else
            ((TextView) findViewById(R.id.price)).setText(product_auction.winner_price.longValue() + " " + sharedPref.getInfoData().currency);
        ((TextView) findViewById(R.id.basicPrice)).setText(product_auction.start_price.longValue() + " " + sharedPref.getInfoData().currency);
        try {
            SimpleDateFormat sdformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            DateFormat dateformat = new SimpleDateFormat("yyyy/MM/dd");
            DateFormat Timeformat = new SimpleDateFormat("HH:mm:ss");
            Date start_date = sdformat.parse(product_auction.start_date);
            Date end_date = sdformat.parse(product_auction.end_date);
            String startDate = dateformat.format(start_date);
            String endDate = dateformat.format(end_date);
            String startTime = Timeformat.format(start_date);
            String endTime = Timeformat.format(end_date);
            ((TextView) findViewById(R.id.startDate)).setText(ShamsiCalleder.getCurrentShamsidate(startDate) + " " + startTime);
            ((TextView) findViewById(R.id.endDate)).setText(ShamsiCalleder.getCurrentShamsidate(endDate) + " " + endTime);
            Date current_date = new Date();
            if (current_date.compareTo(start_date) < 0)
                ((TextView) findViewById(R.id.status)).setText(getString(R.string.not_started));
            else if (current_date.compareTo(end_date) > 0)
                ((TextView) findViewById(R.id.status)).setText(getString(R.string.finished));
            else {
                ((TextView) findViewById(R.id.status)).setText(getString(R.string.on_performing));

                lyt_text_time = (LinearLayout) findViewById(R.id.lyt_text_time);
                lyt_time = (LinearLayout) findViewById(R.id.lyt_time);
                remaining_time = (TextView) findViewById(R.id.remaining_time);

                lyt_text_time.setVisibility(View.VISIBLE);
                lyt_text_time.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                lyt_time.setVisibility(View.VISIBLE);
                lyt_time.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                long diffInMs = end_date.getTime() - current_date.getTime();
                timeLeftInMillis = diffInMs;
                countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
                    @Override
                    public void onTick(long l) {
                        timeLeftInMillis = l;
                        updateRemainingTimeText();
                    }

                    @Override
                    public void onFinish() {
                        lyt_text_time.setVisibility(View.INVISIBLE);
                        lyt_text_time.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0));
                        lyt_time.setVisibility(View.INVISIBLE);
                        lyt_time.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0));
                    }
                }.start();
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // display Image slider
        displayImageSlider();

        //Toast.makeText(this, R.string.msg_data_loaded, Toast.LENGTH_SHORT).show();
        hidepDialog();
    }

    private void updateRemainingTimeText() {
        int day = (int) (timeLeftInMillis / 3600000) / 24;
        int hour = (int) (timeLeftInMillis / 3600000) % 24;
        int minute = (int) (timeLeftInMillis / 60000) % 60;
        int second = (int) (timeLeftInMillis / 1000) % 60;

        String timeLeft = String.format(Locale.getDefault(), "%d " + getString(R.string.day) + " %02d:%02d:%02d", day, hour, minute, second);
        remaining_time.setText(timeLeft);
    }

    private void displayImageSlider() {
        final LinearLayout layout_dots = (LinearLayout) findViewById(R.id.layout_dots);
        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        final AdapterProductImage adapterSlider = new AdapterProductImage(this, new ArrayList<ProductImage>());

        final List<ProductImage> productImages = new ArrayList<>();
        ProductImage p = new ProductImage();
        p.product_id = product_auction.id;
        p.name = product_auction.image;
        productImages.add(p);
        if (product_auction.product_images != null)
            productImages.addAll(product_auction.product_images);
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
                Intent i = new Intent(ActivityProductAuctionDetails.this, ActivityFullScreenImage.class);
                i.putExtra(ActivityFullScreenImage.EXTRA_POS, pos);
                i.putStringArrayListExtra(ActivityFullScreenImage.EXTRA_IMGS, images_list);
                startActivity(i);
            }
        });
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int item_id = item.getItemId();
        if (item_id == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
        } else if (item_id == R.id.action_wish) {
            Toast.makeText(this, R.string.cannot_add_wishlist, Toast.LENGTH_SHORT).show();
            return true;
        } else if (item_id == R.id.action_cart) {
            Intent i = new Intent(this, ActivityShoppingCart.class);
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
