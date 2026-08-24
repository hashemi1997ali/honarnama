package ir.hashemi.market.connection.callbacks;

import java.io.Serializable;

import ir.hashemi.market.model.ProductAuction;

public class CallbackProductAuctionDetails implements Serializable {

    public String status = "";
    public ProductAuction product_auction = null;

}
