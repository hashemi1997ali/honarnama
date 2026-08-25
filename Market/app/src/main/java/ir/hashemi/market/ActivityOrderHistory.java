package ir.hashemi.market;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ir.hashemi.market.adapter.AdapterOrderHistory;
import ir.hashemi.market.adapter.AdapterShoppingCart;
import ir.hashemi.market.connection.API;
import ir.hashemi.market.connection.RestAdapter;
import ir.hashemi.market.connection.callbacks.CallbackOrderHistory;
import ir.hashemi.market.data.DatabaseHandler;
import ir.hashemi.market.data.SharedPref;
import ir.hashemi.market.model.Info;
import ir.hashemi.market.model.Order;
import ir.hashemi.market.model.OrderHistoryRequest;
import ir.hashemi.market.model.User;
import ir.hashemi.market.utils.Tools;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityOrderHistory extends AppCompatActivity {

    private View parent_view;
    private RecyclerView recyclerView;
    private DatabaseHandler db;
    private AdapterOrderHistory adapter;
    private SharedPref sharedPref;
    private Info info;
    private ProgressBar loading;
    private View emptyView;
    private Call<CallbackOrderHistory> historyCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history);
        Tools.applyTopWindowInsets(this, findViewById(R.id.app_bar_layout));

        db = new DatabaseHandler(this);
        sharedPref = new SharedPref(this);
        info = sharedPref.getInfoData();

        initToolbar();
        iniComponent();
    }

    private void iniComponent() {
        parent_view = findViewById(android.R.id.content);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        loading = (ProgressBar) findViewById(R.id.history_loading);
        emptyView = findViewById(R.id.lyt_no_item);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void initToolbar() {
        ActionBar actionBar;
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle(R.string.title_activity_history);
        Tools.systemBarLolipop(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int item_id = item.getItemId();
        if (item_id == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrderHistory();
    }

    private void loadOrderHistory() {
        if (historyCall != null) historyCall.cancel();
        User user = sharedPref.getUserData();
        if (user == null || user.auth_token == null || user.auth_token.trim().isEmpty()) {
            redirectToLogin();
            return;
        }

        OrderHistoryRequest request = new OrderHistoryRequest();
        request.auth_token = user.auth_token;
        for (Order localOrder : db.getOrderList()) {
            if (localOrder.id != null && localOrder.code != null) {
                request.legacy_orders.add(new OrderHistoryRequest.LegacyOrder(localOrder.id, localOrder.code));
            }
        }

        setLoading(true);
        historyCall = RestAdapter.createAPI().listOrderHistory(request);
        historyCall.enqueue(new Callback<CallbackOrderHistory>() {
            @Override
            public void onResponse(Call<CallbackOrderHistory> call, Response<CallbackOrderHistory> response) {
                CallbackOrderHistory result = response.body();
                if (response.isSuccessful() && result != null && "success".equals(result.status)) {
                    List<Order> orders = result.data == null ? new ArrayList<>() : result.data;
                    db.replaceOrderHistory(orders);
                    displayData(orders);
                    return;
                }

                setLoading(false);
                String message = result != null && result.msg != null && !result.msg.trim().isEmpty()
                        ? result.msg
                        : getString(R.string.msg_history_sync_failed);
                if (message.toLowerCase(Locale.US).contains("session")) {
                    redirectToLogin();
                } else {
                    Snackbar.make(parent_view, message, Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<CallbackOrderHistory> call, Throwable throwable) {
                setLoading(false);
                if (!call.isCanceled()) {
                    Log.e("OrderHistory", "History request failed", throwable);
                    Snackbar.make(parent_view, R.string.msg_history_sync_failed, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void displayData(List<Order> items) {
        adapter = new AdapterOrderHistory(this, items);
        recyclerView.setAdapter(adapter);
        recyclerView.setNestedScrollingEnabled(false);
        adapter.setOnItemClickListener(new AdapterOrderHistory.OnItemClickListener() {
            @Override
            public void onItemClick(View view, Order obj) {
                dialogOrderHistoryDetails(obj);
            }
        });
        setLoading(false);
        emptyView.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean show) {
        loading.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
        if (show) emptyView.setVisibility(View.GONE);
    }

    private void redirectToLogin() {
        sharedPref.clearUserData();
        Toast.makeText(this, R.string.msg_session_expired, Toast.LENGTH_LONG).show();
        startActivity(new Intent(this, ActivityLogin.class));
        finish();
    }

    private void dialogOrderHistoryDetails(final Order order) {
        final Dialog dialog = new Dialog(ir.hashemi.market.ActivityOrderHistory.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // before
        dialog.setContentView(R.layout.dialog_order_history_details);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        RecyclerView recyclerView = (RecyclerView) dialog.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        AdapterShoppingCart _adapter = new AdapterShoppingCart(this, false, order.cart_list);
        recyclerView.setAdapter(_adapter);
        recyclerView.setNestedScrollingEnabled(false);
        ((ImageView) dialog.findViewById(R.id.img_close)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        ((TextView) dialog.findViewById(R.id.code)).setText(order.code);
        int itemCount = 0;
        for (ir.hashemi.market.model.Cart item : order.cart_list) {
            itemCount += item.amount;
        }
        ((TextView) dialog.findViewById(R.id.order_item_count)).setText(
                itemCount + " " + getString(R.string.items)
        );
        ((TextView) dialog.findViewById(R.id.order_status)).setText(order.status == null ? "" : order.status);
        String currency = info == null || info.currency == null ? "EUR" : info.currency;
        double total = order.total_fees == null ? 0 : order.total_fees;
        ((TextView) dialog.findViewById(R.id.order_total)).setText(
                String.format(Locale.US, "%1$,.2f %2$s", total, currency)
        );
        ((ImageButton) dialog.findViewById(R.id.copy)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Tools.copyToClipboard(getApplicationContext(), order.code);
            }
        });
        dialog.show();
        dialog.getWindow().setAttributes(lp);
    }

    @Override
    protected void onDestroy() {
        if (historyCall != null) historyCall.cancel();
        super.onDestroy();
    }

}
