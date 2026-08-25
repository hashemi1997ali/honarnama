package ir.hashemi.market.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Order implements Serializable {

    public Long id;
    public String code;
    public String status;
    public Double total_fees;
    public Long created_at = System.currentTimeMillis();
    public List<Cart> cart_list = new ArrayList<>();

    public Order() {
    }

    public Order(Long id, String code, Double total_fees) {
        this.id = id;
        this.code = code;
        this.total_fees = total_fees;
    }
}



