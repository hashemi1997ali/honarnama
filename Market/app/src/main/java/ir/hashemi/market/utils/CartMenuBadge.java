package ir.hashemi.market.utils;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import ir.hashemi.market.ActivityShoppingCart;
import ir.hashemi.market.R;
import ir.hashemi.market.data.DatabaseHandler;

public final class CartMenuBadge {

    private CartMenuBadge() {
    }

    public static void bind(Activity activity, Menu menu) {
        MenuItem cartItem = menu.findItem(R.id.action_cart);
        if (cartItem == null || cartItem.getActionView() == null) return;

        View actionView = cartItem.getActionView();
        TextView counter = (TextView) actionView.findViewById(R.id.cart_counter);
        int count = new DatabaseHandler(activity).getActiveCartSize();
        counter.setText(count > 99 ? "99+" : String.valueOf(count));
        counter.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        actionView.setOnClickListener(view -> activity.startActivity(
                new Intent(activity, ActivityShoppingCart.class)
        ));
    }
}
