package ir.hashemi.market.connection.callbacks;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import ir.hashemi.market.model.Category;

public class CallbackCategory implements Serializable {

    public String status = "";
    public List<Category> categories = new ArrayList<>();

}
