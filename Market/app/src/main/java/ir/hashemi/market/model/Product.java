package ir.hashemi.market.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Product implements Serializable {

    public Long id;
    public String name;
    public String image;
    public Double price;
    public Double price_discount;
    public Long stock;
    public Integer draft;
    public String description;
    public String status;
    public Long created_at;
    public Long last_update;

    public List<Category> categories = new ArrayList<>();
    public List<ProductImage> product_images = new ArrayList<>();

    public double getEffectivePrice() {
        if (price_discount != null && price_discount > 0 && (price == null || price_discount <= price)) {
            return price_discount;
        }
        return price == null ? 0 : price;
    }

    public boolean hasDiscount() {
        return price != null && price_discount != null && price_discount > 0 && price_discount < price;
    }

    public boolean isAvailable() {
        return stock != null && stock > 0 && draft != null && draft == 0
                && status != null && "READY STOCK".equalsIgnoreCase(status);
    }

}
