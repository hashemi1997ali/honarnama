package ir.hashemi.market;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Locale;

import ir.hashemi.market.connection.API;
import ir.hashemi.market.connection.RestAdapter;
import ir.hashemi.market.connection.callbacks.CallbackNewsInfoDetails;
import ir.hashemi.market.data.Constant;
import ir.hashemi.market.model.NewsInfo;
import ir.hashemi.market.utils.NetworkCheck;
import ir.hashemi.market.utils.Tools;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityNewsInfoDetails extends AppCompatActivity {

    private static final String EXTRA_OBJECT_ID = "key.EXTRA_OBJECT_ID";

    // activity transition
    public static void navigate(Activity activity, Long id) {
        Intent i = new Intent(activity, ActivityNewsInfoDetails.class);
        i.putExtra(EXTRA_OBJECT_ID, id);
        activity.startActivity(i);
    }

    private Long news_id;

    // extra obj
    private NewsInfo newsInfo;

    private Call<CallbackNewsInfoDetails> callbackCall = null;
    private Toolbar toolbar;
    private ActionBar actionBar;
    private View parent_view;
    private SwipeRefreshLayout swipe_refresh;
    private WebView webview;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_info_details);

        Configuration configuration = getResources().getConfiguration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLayoutDirection(new Locale("fa"));
        }

        news_id = getIntent().getLongExtra(EXTRA_OBJECT_ID, -1L);

        initComponent();
        initToolbar();
        requestAction();
    }

    private void initComponent() {
        parent_view = findViewById(android.R.id.content);
        swipe_refresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        // on swipe
        swipe_refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestAction();
            }
        });
    }

    private void initToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle("");
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
        callbackCall = api.getNewsDetails(news_id);
        callbackCall.enqueue(new Callback<CallbackNewsInfoDetails>() {
            @Override
            public void onResponse(Call<CallbackNewsInfoDetails> call, Response<CallbackNewsInfoDetails> response) {
                CallbackNewsInfoDetails resp = response.body();
                if (resp != null && resp.status.equals("success")) {
                    newsInfo = resp.news_info;
                    displayPostData();
                    swipeProgress(false);
                } else {
                    onFailRequest();
                }
            }

            @Override
            public void onFailure(Call<CallbackNewsInfoDetails> call, Throwable t) {
                Log.e("onFailure", t.getMessage());
                if (!call.isCanceled()) onFailRequest();
            }
        });
    }

    private void displayPostData() {
        ((TextView) findViewById(R.id.title)).setText(Html.fromHtml(newsInfo.title));

        webview = (WebView) findViewById(R.id.content);
        String html_data = "<style>img{max-width:100%;height:auto;} iframe{width:100%;}</style> ";
        html_data += newsInfo.full_content;
        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings();
        webview.getSettings().setBuiltInZoomControls(true);
        webview.setBackgroundColor(Color.TRANSPARENT);
        webview.setWebChromeClient(new WebChromeClient());
        webview.loadData(html_data, "text/html; charset=UTF-8", null);
        // disable scroll on touch
        webview.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return (event.getAction() == MotionEvent.ACTION_MOVE);
            }
        });

        //((TextView) findViewById(R.id.date)).setText(Tools.getFormattedDate(newsInfo.last_update));
        String date = Tools.getFormattedDate(newsInfo.last_update);
        Log.i("LOG", date);
        ((TextView) findViewById(R.id.date)).setText(ShamsiCalleder.getCurrentShamsidate(date));
        if (newsInfo.status.equalsIgnoreCase("FEATURED")) {
            ((TextView) findViewById(R.id.featured)).setVisibility(View.VISIBLE);
        }

        Tools.displayImageOriginal(this, ((ImageView) findViewById(R.id.image)), Constant.getURLimgNews(newsInfo.image));

        findViewById(R.id.lyt_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ArrayList<String> images_list = new ArrayList<>();
                images_list.add(Constant.getURLimgNews(newsInfo.image));
                Intent i = new Intent(ir.hashemi.market.ActivityNewsInfoDetails.this, ir.hashemi.market.ActivityFullScreenImage.class);
                i.putStringArrayListExtra(ir.hashemi.market.ActivityFullScreenImage.EXTRA_IMGS, images_list);
                startActivity(i);
            }
        });

        Snackbar.make(parent_view, R.string.msg_data_loaded, Snackbar.LENGTH_SHORT).show();
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
    public boolean onOptionsItemSelected(MenuItem item) {
        int item_id = item.getItemId();
        if (item_id == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

}
