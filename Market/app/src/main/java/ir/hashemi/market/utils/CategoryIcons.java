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

    /**
     * The source images have different amounts of transparent space around the artwork.
     * Compensate for that so their visible sizes match without distorting their aspect ratio.
     */
    public static float scaleFor(String name, String icon) {
        String key = searchableKey(name, icon);

        if (key.contains("painting")) return 1.12f;
        if (key.contains("sculpture")) return 1.22f;
        if (key.contains("photography")) return 1.18f;
        if (key.contains("handicraft")) return 1.30f;
        return 1.0f;
    }

    private static String searchableKey(String name, String icon) {
        return ((icon == null ? "" : icon) + " " + (name == null ? "" : name))
                .toLowerCase(Locale.US);
    }
}
