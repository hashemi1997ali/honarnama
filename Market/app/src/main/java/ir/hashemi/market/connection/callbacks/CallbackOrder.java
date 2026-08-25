package ir.hashemi.market.connection.callbacks;

import java.io.Serializable;

public class CallbackOrder implements Serializable {

    public String status = "";
    public String msg = "";
    public DataResp data = new DataResp();

    public static class DataResp implements Serializable {
        public Long id = -1L;
        public String code = "";
        public Double total_fees = 0D;
    }

}
