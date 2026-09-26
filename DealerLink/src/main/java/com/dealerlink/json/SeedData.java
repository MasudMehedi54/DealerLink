package com.dealerlink.json;

import com.dealerlink.model.Product;
import com.dealerlink.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Java mirror of the whole data/dealerlink-data.json document.
 * Jackson maps each top-level JSON key onto the matching field:
 *   "settings"  -> AppSettings
 *   "users"     -> List&lt;User&gt;        (existing model class)
 *   "products"  -> List&lt;Product&gt;     (existing model class)
 *   "inventory" -> List&lt;InventorySeed&gt;
 *   "cities"    -> List&lt;CityCoordinate&gt;  (offline lookup: city name -> lat/lon)
 */
public class SeedData {
    private AppSettings settings = new AppSettings();
    private List<User> users = new ArrayList<>();
    private List<Product> products = new ArrayList<>();
    private List<InventorySeed> inventory = new ArrayList<>();
    private List<CityCoordinate> cities = new ArrayList<>();

    public AppSettings getSettings() { return settings; }
    public void setSettings(AppSettings settings) { this.settings = settings == null ? new AppSettings() : settings; }
    public List<User> getUsers() { return users; }
    public void setUsers(List<User> users) { this.users = users == null ? new ArrayList<>() : users; }
    public List<Product> getProducts() { return products; }
    public void setProducts(List<Product> products) { this.products = products == null ? new ArrayList<>() : products; }
    public List<InventorySeed> getInventory() { return inventory; }
    public void setInventory(List<InventorySeed> inventory) { this.inventory = inventory == null ? new ArrayList<>() : inventory; }
    public List<CityCoordinate> getCities() { return cities; }
    public void setCities(List<CityCoordinate> cities) { this.cities = cities == null ? new ArrayList<>() : cities; }
}
