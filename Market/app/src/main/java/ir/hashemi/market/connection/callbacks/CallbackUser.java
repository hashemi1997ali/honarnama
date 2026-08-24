package ir.hashemi.market.connection.callbacks;

import java.io.Serializable;
import ir.hashemi.market.model.User;

public class CallbackUser implements Serializable {

    public String status = "";
    public String msg = "";
    public User data = new User();

}
