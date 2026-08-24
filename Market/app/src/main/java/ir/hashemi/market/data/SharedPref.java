package ir.hashemi.market.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import ir.hashemi.market.model.BuyerProfile;
import ir.hashemi.market.model.Info;
import ir.hashemi.market.model.User;

public class SharedPref {

    private SharedPreferences sharedPreferences;

    private static final String FIRST_LAUNCH = "_.FIRST_LAUNCH";
    private static final String USER_DATA = "_.USER_DATA";
    private static final String INFO_DATA = "_.INFO_DATA_KEY";
    private static final String BUYER_PROFILE = "_.BUYER_PROFILE_KEY";

    public SharedPref(Context context) {
        sharedPreferences = context.getSharedPreferences("MAIN_PREF", Context.MODE_PRIVATE);
    }

    /**
     * Preference for user data
     */
    public void setUserData(User user) {
        Gson gson = new Gson();
        String json = gson.toJson(user);
        sharedPreferences.edit().putString(USER_DATA, json).apply();
    }

    public User getUserData() {
        Gson gson = new Gson();
        return gson.fromJson(sharedPreferences.getString(USER_DATA, null), User.class);
    }

    public boolean isUserDataEmpty() {
        if(getUserData() == null)
            return true;
        return false;
    }

    /**
     * Preference for first launch
     */
    public void setFirstLaunch(boolean flag) {
        sharedPreferences.edit().putBoolean(FIRST_LAUNCH, flag).apply();
    }

    public boolean isFirstLaunch() {
        return sharedPreferences.getBoolean(FIRST_LAUNCH, true);
    }

    // info API loaded
    public Info setInfoData(Info info) {
        if (info == null) return null;
        String json = new Gson().toJson(info, Info.class);
        sharedPreferences.edit().putString(INFO_DATA, json).apply();
        return getInfoData();
    }

    public void clearInfoData() {
        sharedPreferences.edit().putString(INFO_DATA, null).apply();
    }

    public Info getInfoData() {
        String data = sharedPreferences.getString(INFO_DATA, null);
        if (data == null) return null;
        return new Gson().fromJson(data, Info.class);
    }

    public boolean isInfoLoaded() {
        Info info = getInfoData();
        return (info != null);
    }

    // info buyer profile data
    public BuyerProfile setBuyerProfile(BuyerProfile buyerProfile) {
        if (buyerProfile == null) return null;
        String json = new Gson().toJson(buyerProfile, BuyerProfile.class);
        sharedPreferences.edit().putString(BUYER_PROFILE, json).apply();
        return getBuyerProfile();
    }

    public void clearBuyerProfile() {
        sharedPreferences.edit().putString(BUYER_PROFILE, null).apply();
    }

    public BuyerProfile getBuyerProfile() {
        String data = sharedPreferences.getString(BUYER_PROFILE, null);
        if (data == null) return null;
        return new Gson().fromJson(data, BuyerProfile.class);
    }

    public void clearAllData() {
        sharedPreferences.edit().clear().apply();
    }
}
