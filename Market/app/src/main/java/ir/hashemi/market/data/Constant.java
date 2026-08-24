package ir.hashemi.market.data;

import ir.hashemi.market.BuildConfig;

public class Constant {

    /**
     * -------------------- EDIT THIS WITH YOURS -------------------------------------------------
     */

    // Configure these values in Market/local.properties or environment variables.
    // BACKEND_URL must use HTTPS and end with a slash.
    public static final String WEB_URL = BuildConfig.BACKEND_URL;

    /* [ IMPORTANT ] be careful when edit this security code */
    /* This string must be same with security code at Server, if its different android unable to submit order */
    public static final String SECURITY_CODE = BuildConfig.SECURITY_CODE;

    /**
     * ------------------- DON'T EDIT THIS -------------------------------------------------------
     */

    // this limit value used for give pagination (request and display) to decrease payload
    public static int NEWS_PER_REQUEST = 10;
    public static int PRODUCT_PER_REQUEST = 10;
    public static int WISHLIST_PAGE = 20;

    // Method get path to image
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
