package ir.hashemi.market.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Info implements Serializable {

    public Double tax;
    public String currency;
    public List<String> shipping = new ArrayList<>();

}
