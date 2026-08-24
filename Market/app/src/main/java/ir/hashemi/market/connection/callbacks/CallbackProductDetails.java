package ir.hashemi.market.connection.callbacks;

import java.io.Serializable;

import ir.hashemi.market.model.Product;

public class CallbackProductDetails implements Serializable {

    public String status = "";
    public Product product = null;

}
