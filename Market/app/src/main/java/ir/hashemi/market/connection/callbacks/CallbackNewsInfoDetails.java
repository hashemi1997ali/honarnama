package ir.hashemi.market.connection.callbacks;

import java.io.Serializable;

import ir.hashemi.market.model.NewsInfo;

public class CallbackNewsInfoDetails implements Serializable {

    public String status = "";
    public NewsInfo news_info = null;

}
