package ir.hashemi.market.connection.callbacks;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CallbackCartValidation implements Serializable {

    public String status = "";
    public String msg = "";
    public boolean valid;
    public List<Item> data = new ArrayList<>();

    public static class Item implements Serializable {
        public Long product_id;
        public String product_name;
        public String image;
        public Long stock;
        public Integer amount;
        public Double price_item;
        public boolean available;
        public String msg;
    }
}
