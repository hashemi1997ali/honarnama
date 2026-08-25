package ir.hashemi.market.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class OrderHistoryRequest implements Serializable {

    public String auth_token;
    public List<LegacyOrder> legacy_orders = new ArrayList<>();

    public static class LegacyOrder implements Serializable {
        public Long id;
        public String code;

        public LegacyOrder(Long id, String code) {
            this.id = id;
            this.code = code;
        }
    }
}
