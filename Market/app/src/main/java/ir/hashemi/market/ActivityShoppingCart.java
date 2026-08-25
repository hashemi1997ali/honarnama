package ir.hashemi.market;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import ir.hashemi.market.adapter.AdapterShoppingCart;
import ir.hashemi.market.connection.API;
import ir.hashemi.market.connection.RestAdapter;
import ir.hashemi.market.connection.callbacks.CallbackCartValidation;
import ir.hashemi.market.data.DatabaseHandler;
import ir.hashemi.market.data.SharedPref;
import ir.hashemi.market.model.Cart;
import ir.hashemi.market.model.Checkout;
import ir.hashemi.market.model.Info;
import ir.hashemi.market.model.ProductOrderDetail;
import ir.hashemi.market.utils.Tools;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityShoppingCart extends AppCompatActivity {

    private View parent_view;
    private RecyclerView recyclerView;
    private DatabaseHandler db;
    private AdapterShoppingCart adapter;
    private TextView price_total;
    private SharedPref sharedPref;
    private Info info;
    private Call<CallbackCartValidation> validationCall;
    private boolean cartValidating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping_cart);
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
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        price_total = (TextView) findViewById(R.id.price_total);
    }

    private void initToolbar() {
        ActionBar actionBar;
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle(R.string.title_activity_cart);
        Tools.systemBarLolipop(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_shopping_cart, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int item_id = item.getItemId();
        if (item_id == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
        } else if (item_id == R.id.action_checkout) {
            if (cartValidating) {
                Snackbar.make(parent_view, R.string.please_wait_text, Snackbar.LENGTH_SHORT).show();
                return true;
            }
            if (adapter.getItemCount() > 0) {
                Intent intent = new Intent(ir.hashemi.market.ActivityShoppingCart.this, ActivityCheckout.class);
                startActivity(intent);
            } else {
                Snackbar.make(parent_view, R.string.msg_cart_empty, Snackbar.LENGTH_SHORT).show();
            }
        } else if (item_id == R.id.action_delete) {
            if (adapter.getItemCount() == 0) {
                Snackbar.make(parent_view, R.string.msg_cart_empty, Snackbar.LENGTH_SHORT).show();
                return true;
            }
            dialogDeleteConfirmation();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        displayData();
    }

    private void displayData() {
        renderData();
        validateCart(adapter.getItem());
    }

    private void renderData() {
        List<Cart> items = db.getActiveCartList();
        adapter = new AdapterShoppingCart(this, true, items);
        recyclerView.setAdapter(adapter);
        recyclerView.setNestedScrollingEnabled(false);

        adapter.setOnItemClickListener(new AdapterShoppingCart.OnItemClickListener() {
            @Override
            public void onItemClick(View view, Cart obj) {
                dialogCartAction(obj);
            }
        });
        View lyt_no_item = (View) findViewById(R.id.lyt_no_item);
        if (adapter.getItemCount() == 0) {
           // lyt_no_item.setVisibility(View.VISIBLE);
        } else {
           // lyt_no_item.setVisibility(View.GONE);
        }
        setTotalPrice();
    }

    private void validateCart(List<Cart> items) {
        if (items == null || items.isEmpty() || cartValidating) return;
        cartValidating = true;

        Checkout checkout = new Checkout();
        checkout.product_order_detail.clear();
        for (Cart cart : items) {
            checkout.product_order_detail.add(new ProductOrderDetail(
                    cart.product_id, cart.product_name, cart.amount, cart.price_item
            ));
        }

        validationCall = RestAdapter.createAPI().validateCart(checkout);
        validationCall.enqueue(new Callback<CallbackCartValidation>() {
            @Override
            public void onResponse(Call<CallbackCartValidation> call, Response<CallbackCartValidation> response) {
                cartValidating = false;
                CallbackCartValidation result = response.body();
                if (!response.isSuccessful() || result == null || !"success".equals(result.status)) {
                    Snackbar.make(parent_view, R.string.cart_validation_failed, Snackbar.LENGTH_SHORT).show();
                    return;
                }

                boolean changed = false;
                Set<Long> checkedProductIds = new HashSet<>();
                for (CallbackCartValidation.Item current : result.data) {
                    if (current.product_id == null) continue;
                    checkedProductIds.add(current.product_id);
                    Cart local = db.getCart(current.product_id);
                    if (local == null) continue;
                    if (!current.available || current.stock == null || current.stock <= 0) {
                        db.deleteActiveCart(current.product_id);
                        changed = true;
                        continue;
                    }

                    int allowedAmount = Math.min(local.amount, current.stock.intValue());
                    String refreshedName = current.product_name == null ? local.product_name : current.product_name;
                    String refreshedImage = current.image == null ? local.image : current.image;
                    double localPrice = local.price_item == null ? 0 : local.price_item;
                    double refreshedPrice = current.price_item == null ? localPrice : current.price_item;
                    long localStock = local.stock == null ? 0 : local.stock;
                    if (allowedAmount != local.amount || localStock != current.stock
                            || Double.compare(localPrice, refreshedPrice) != 0
                            || !Objects.equals(local.product_name, refreshedName)
                            || !Objects.equals(local.image, refreshedImage)) {
                        changed = true;
                    }
                    local.amount = allowedAmount;
                    local.stock = current.stock;
                    local.product_name = refreshedName;
                    local.image = refreshedImage;
                    local.price_item = refreshedPrice;
                    db.saveCart(local);
                }

                for (Cart local : items) {
                    if (!checkedProductIds.contains(local.product_id)) {
                        db.deleteActiveCart(local.product_id);
                        changed = true;
                    }
                }

                if (changed) {
                    renderData();
                    Snackbar.make(parent_view, R.string.cart_updated_for_stock, Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<CallbackCartValidation> call, Throwable throwable) {
                cartValidating = false;
                if (!call.isCanceled()) {
                    Log.e("CartValidation", throwable.getMessage() == null ? "Request failed" : throwable.getMessage());
                    Snackbar.make(parent_view, R.string.cart_validation_failed, Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (validationCall != null) validationCall.cancel();
        super.onDestroy();
    }

    private void setTotalPrice() {
        List<Cart> items = adapter.getItem();
        Double _price_total = 0D;
        String _price_total_tax_str;
        for (Cart c : items) {
            _price_total = _price_total + (c.amount * c.price_item);
        }
        _price_total_tax_str = String.format(Locale.US, "%1$,.2f", _price_total);
        price_total.setText(" " + _price_total_tax_str + " " + info.currency);
    }

    private void dialogCartAction(final Cart model) {

        final Dialog dialog = new Dialog(ir.hashemi.market.ActivityShoppingCart.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // before
        dialog.setContentView(R.layout.dialog_cart_option);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        ((TextView) dialog.findViewById(R.id.title)).setText(model.product_name);
        ((TextView) dialog.findViewById(R.id.stock)).setText(getString(R.string.stock) + model.stock);
        final TextView qty = (TextView) dialog.findViewById(R.id.quantity);
        qty.setText(model.amount + "");

        ((ImageView) dialog.findViewById(R.id.img_decrease)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (model.amount > 1) {
                    model.amount = model.amount - 1;
                    qty.setText(model.amount + "");
                }
            }
        });
        ((ImageView) dialog.findViewById(R.id.img_increase)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (model.amount < model.stock) {
                    model.amount = model.amount + 1;
                    qty.setText(model.amount + "");
                }
            }
        });
        ((Button) dialog.findViewById(R.id.bt_save)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                db.saveCart(model);
                displayData();
                dialog.dismiss();
            }
        });
        ((Button) dialog.findViewById(R.id.bt_remove)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                db.deleteActiveCart(model.product_id);
                displayData();
                dialog.dismiss();
            }
        });
        dialog.show();
        dialog.getWindow().setAttributes(lp);
    }

    public void dialogDeleteConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_delete_confirm);
        builder.setMessage(getString(R.string.content_delete_confirm) + getString(R.string.title_activity_cart));
        builder.setPositiveButton(R.string.YES, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface di, int i) {
                di.dismiss();
                db.deleteActiveCart();
                onResume();
                Snackbar.make(parent_view, R.string.delete_success, Snackbar.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.CANCEL, null);
        builder.show();
    }

}
