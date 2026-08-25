package ir.hashemi.market.connection.callbacks;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import ir.hashemi.market.model.Order;

public class CallbackOrderHistory implements Serializable {

    public String status = "";
    public String msg = "";
    public List<Order> data = new ArrayList<>();
}
