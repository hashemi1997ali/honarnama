package ir.hashemi.market.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ProductAuction implements Serializable {

    public Long id;
    public String name;
    public String image;
    public String description;
    public String start_date;
    public String end_date;
    public Double start_price;
    public Long winner_id;
    public String winner_username;
    public Double winner_price;
    public String created_at;
    public String last_update;

    public List<ProductImage> product_images = new ArrayList<>();

}
