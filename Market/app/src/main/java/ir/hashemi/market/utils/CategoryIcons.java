package ir.hashemi.market.utils;

import androidx.annotation.DrawableRes;

import java.util.Locale;

import ir.hashemi.market.R;

public final class CategoryIcons {

    private CategoryIcons() {
    }

    @DrawableRes
    public static int drawableFor(String name, String icon) {
        String key = searchableKey(name, icon);

        if (key.contains("auction")) return R.drawable.category_auction_icon;
        if (key.contains("painting")) return R.drawable.category_painting_icon;
        if (key.contains("sculpture")) return R.drawable.category_sculpture_icon;
        if (key.contains("photography")) return R.drawable.category_photography_icon;
        if (key.contains("handicraft")) return R.drawable.category_handicrafts_icon;
        return R.drawable.ic_logo;
    }

    public static boolean isAuction(String name, String icon) {
        return searchableKey(name, icon).contains("auction");
    }

    private static String searchableKey(String name, String icon) {
        return ((icon == null ? "" : icon) + " " + (name == null ? "" : name))
                .toLowerCase(Locale.US);
    }
}
