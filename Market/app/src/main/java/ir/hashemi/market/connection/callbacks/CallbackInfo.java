package ir.hashemi.market.connection.callbacks;

import java.io.Serializable;

import ir.hashemi.market.model.Info;

public class CallbackInfo implements Serializable {
    public String status = "";
    public Info info = null;
}
