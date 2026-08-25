package ir.hashemi.market.data;

import ir.hashemi.market.BuildConfig;

public class Constant {
    // Values are supplied through Market/local.properties or environment variables.
    public static final String WEB_URL = BuildConfig.BACKEND_URL;
    public static final String SECURITY_CODE = BuildConfig.SECURITY_CODE;

    public static int NEWS_PER_REQUEST = 10;
    public static int PRODUCT_PER_REQUEST = 10;
    public static int WISHLIST_PAGE = 20;

    public static String getURLimgProduct(String file_name) {
        return getImageUrl("product", file_name);
    }

    public static String getURLimgNews(String file_name) {
        return getImageUrl("news", file_name);
    }

    public static String getURLimgCategory(String file_name) {
        return getImageUrl("category", file_name);
    }

    private static String getImageUrl(String directory, String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) return "";
        return WEB_URL + "uploads/" + directory + "/" + fileName;
    }

}
