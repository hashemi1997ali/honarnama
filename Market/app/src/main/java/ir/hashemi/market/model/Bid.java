package ir.hashemi.market.model;

import java.io.Serializable;
import java.util.Date;

public class Bid implements Serializable {

    public Long id;
    public Long product_auction_id;
    public Long user_id;
    public Double bid_price;
    public String created_at;
    public String last_update;

    public Bid() {
    }

    public Bid(Long product_auction_id, Long user_id, Double bid_price) {
        this.product_auction_id = product_auction_id;
        this.user_id = user_id;
        this.bid_price = bid_price;
    }
}
