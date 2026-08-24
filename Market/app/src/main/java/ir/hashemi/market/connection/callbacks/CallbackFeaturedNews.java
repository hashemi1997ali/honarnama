package ir.hashemi.market.connection.callbacks;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import ir.hashemi.market.model.NewsInfo;

public class CallbackFeaturedNews implements Serializable {

    public String status = "";
    public List<NewsInfo> news_infos = new ArrayList<>();

}
