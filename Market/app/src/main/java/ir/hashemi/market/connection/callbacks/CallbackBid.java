package ir.hashemi.market.connection.callbacks;

import java.io.Serializable;
import ir.hashemi.market.model.Bid;

public class CallbackBid implements Serializable {

    public String status = "";
    public String msg = "";
    public Bid data = new Bid();

}
