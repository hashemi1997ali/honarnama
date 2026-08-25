package ir.hashemi.market.connection;

import ir.hashemi.market.connection.callbacks.CallbackBid;
import ir.hashemi.market.connection.callbacks.CallbackCategory;
import ir.hashemi.market.connection.callbacks.CallbackCartValidation;
import ir.hashemi.market.connection.callbacks.CallbackFeaturedNews;
import ir.hashemi.market.connection.callbacks.CallbackInfo;
import ir.hashemi.market.connection.callbacks.CallbackNewsInfo;
import ir.hashemi.market.connection.callbacks.CallbackNewsInfoDetails;
import ir.hashemi.market.connection.callbacks.CallbackOrder;
import ir.hashemi.market.connection.callbacks.CallbackOrderHistory;
import ir.hashemi.market.connection.callbacks.CallbackProduct;
import ir.hashemi.market.connection.callbacks.CallbackProductAuction;
import ir.hashemi.market.connection.callbacks.CallbackProductAuctionDetails;
import ir.hashemi.market.connection.callbacks.CallbackProductDetails;
import ir.hashemi.market.connection.callbacks.CallbackUser;
import ir.hashemi.market.data.Constant;
import ir.hashemi.market.model.Bid;
import ir.hashemi.market.model.Checkout;
import ir.hashemi.market.model.User;
import ir.hashemi.market.model.OrderHistoryRequest;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface API {

    String CACHE = "Cache-Control: max-age=0";
    String AGENT = "User-Agent: Market";
    String SECURITY = "Security: " + Constant.SECURITY_CODE;

    /* Recipe API transaction ------------------------------- */

    @Headers({CACHE, AGENT})
    @GET("services/info")
    Call<CallbackInfo> getInfo();

    /* News Info API ---------------------------------------------------- */

    @Headers({CACHE, AGENT})
    @GET("services/listFeaturedNews")
    Call<CallbackFeaturedNews> getFeaturedNews();

    @Headers({CACHE, AGENT})
    @GET("services/listNews")
    Call<CallbackNewsInfo> getListNewsInfo(
            @Query("page") int page,
            @Query("count") int count,
            @Query("q") String query
    );

    @Headers({CACHE, AGENT})
    @GET("services/getNewsDetails")
    Call<CallbackNewsInfoDetails> getNewsDetails(
            @Query("id") long id
    );

    /* Category API ---------------------------------------------------  */
    @Headers({CACHE, AGENT})
    @GET("services/listCategory")
    Call<CallbackCategory> getListCategory();


    /* Product API ---------------------------------------------------- */

    @Headers({CACHE, AGENT})
    @GET("services/listProduct")
    Call<CallbackProduct> getListProduct(
            @Query("page") int page,
            @Query("count") int count,
            @Query("q") String query,
            @Query("category_id") long category_id
    );

    @Headers({CACHE, AGENT})
    @GET("services/getProductDetails")
    Call<CallbackProductDetails> getProductDetails(
            @Query("id") long id
    );

    /* ProductAuction API ---------------------------------------------------- */

    @Headers({CACHE, AGENT})
    @GET("services/listProductAuction")
    Call<CallbackProductAuction> getListProductAuction(
            @Query("page") int page,
            @Query("count") int count,
            @Query("q") String query
    );

    @Headers({CACHE, AGENT})
    @GET("services/getProductAuctionDetails")
    Call<CallbackProductAuctionDetails> getProductAuctionDetails(
            @Query("id") long id
    );

    /* Bid API ---------------------------------------------------- */

    @Headers({CACHE, AGENT, SECURITY})
    @POST("services/addBid")
    Call<CallbackBid> addBid(
            @Body Bid bid
    );

    /* User API ---------------------------------------------------- */

    @Headers({CACHE, AGENT, SECURITY})
    @POST("services/registerUser")
    Call<CallbackUser> registerUser(
            @Body User user
    );

    @Headers({CACHE, AGENT, SECURITY})
    @POST("services/loginUser")
    Call<CallbackUser> loginUser(
            @Body User user
    );

    /* Checkout API ---------------------------------------------------- */
    @Headers({CACHE, AGENT, SECURITY})
    @POST("services/submitProductOrder")
    Call<CallbackOrder> submitProductOrder(
            @Body Checkout checkout
    );

    @Headers({CACHE, AGENT, SECURITY})
    @POST("services/validateCart")
    Call<CallbackCartValidation> validateCart(
            @Body Checkout checkout
    );

    @Headers({CACHE, AGENT, SECURITY})
    @POST("services/listOrderHistory")
    Call<CallbackOrderHistory> listOrderHistory(
            @Body OrderHistoryRequest request
    );

}
