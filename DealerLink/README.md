# DealerLink – B2B Shop ↔ Dealer Platform (JavaFX)

Shop owners post product requests, dealers compete with private bids, the shop accepts the
best offer, and the dealer updates delivery. JavaFX 21 + SQLite + **Jackson**.

## Run

```bash
mvn clean javafx:run          # run from the project root (so data/ is found)
mvn clean package             # builds a runnable fat jar in target/
```

Demo logins: `shop1 / 1234`, `dealer1 / 1234`, `dealer2 / 1234`

> Tip: if you already have an old `dealerlink.db` from the prototype, delete it once so the
> new JSON seed data (7 products, low-stock examples) gets loaded cleanly.

## Project structure

```
DealerLink/
├── pom.xml
├── data/dealerlink-data.json            ← external JSON (settings, users, products, stock, cities)
├── docs/screenshots/                    ← every screen at 1400×860 (wide) and 900×640 (narrow)
└── src/main/
    ├── java/com/dealerlink/
    │   ├── Main.java                    ← starts the app, opens login.fxml
    │   ├── db/  dao/  model/  json/  service/
    │   └── ui/
    │       ├── Navigator.java           ← loads .fxml files and swaps screens
    │       ├── Responsive.java          ← property-binding helpers (size relative to window)
    │       ├── UiUtils.java             ← badges, currency cells, JSON export, table polish
    │       └── controller/
    │           ├── LoginController.java
    │           ├── RegisterController.java
    │           ├── ShopDashboardController.java
    │           └── DealerDashboardController.java
    └── resources/
        ├── fxml/                        ← OPEN THESE IN SCENE BUILDER
        │   ├── login.fxml
        │   ├── register.fxml
        │   ├── shop-dashboard.fxml
        │   └── dealer-dashboard.fxml
        └── css/theme.css                ← "Sage & Linen" theme
```

## FXML + Scene Builder

Every screen is now an `.fxml` file with its own controller (`fx:controller`), instead of
being built in Java code.

1. Scene Builder → **File → Open** → `src/main/resources/fxml/login.fxml` (or any other).
2. Each file links the stylesheet (`stylesheets="@../css/theme.css"`), so the preview in
   Scene Builder shows the real colours.
3. Buttons call controller methods through `onAction="#handleLogin"` etc.; nodes the controller
   needs have an `fx:id` that matches an `@FXML` field.
4. If Scene Builder says it can't find the controller class, that's only a warning — the layout
   still opens and saves. (Compile the project once and add `target/classes` via
   **Library → Import JAR/FXML** if you want the controller's handler names in the dropdowns.)
5. Table columns, cell renderers and responsive bindings are set in each controller's
   `initialize()` method, since Scene Builder can't express them.

## Layout responsiveness

`ui/Responsive.java` binds sizes to the window's **width** and **height** with JavaFX property
bindings, so the UI recalculates whenever the window is resized:
`value = clamp(windowSize × factor, min, max)`.

| Screen | Bound to width | Bound to height |
|---|---|---|
| Login / Register | brand panel = 42% / 38% of width, **hidden below 960 / 1000 px**; card = 34–36% of width; title, logo and tagline font sizes scale with width | spacing between form rows = 1.3–1.8% of height; register form scrolls on short windows |
| Both dashboards | header title font scales; subtitle hidden < 980 px; role chip hidden < 760 px; spinners, price/location/ETA fields and combo boxes are a % of width | every table's minimum height is a % of window height |
| Shop → Requests & Quotations | `SplitPane` orientation is **side-by-side ≥ 1250 px, stacked below** (`Bindings.when(width ≥ 1250)`) | — |
| Toolbars / forms | `FlowPane`s wrap buttons and fields onto a new line when narrow | — |
| Window itself | first size = 80% × 85% of the screen (`Navigator`), min 820 × 600 | |

Example from `LoginController`:

```java
Responsive.bindPrefWidth(brandPanel, root.widthProperty(), 0.42, 360, 640);
Responsive.showWhenAtLeast(brandPanel, root.widthProperty(), 960);
Responsive.bindFontSize(brandTitle, root.widthProperty(), 0.036, 30, 54);
Responsive.bindSpacing(card, root.heightProperty(), 0.018, 8, 16);
```

Compare `docs/screenshots/wide-*.png` with `narrow-*.png` to see the difference.

## 1. JSON with Jackson – what was added

**External file `data/dealerlink-data.json`** has four blocks:

| Block       | Maps to               | Used for |
|-------------|-----------------------|----------|
| `settings`  | `AppSettings`         | app name, currency symbol, low-stock threshold, polling intervals, weather API URL |
| `users`     | `List<User>`          | demo shop/dealer accounts |
| `products`  | `List<Product>`       | product catalog |
| `inventory` | `List<InventorySeed>` | dealer stock (refers to `dealerUsername` + `productName`, not ids) |
| `cities`    | `List<CityCoordinate>`| offline city → latitude/longitude table for weather |

How it flows:

1. `Main` → `DatabaseManager.initializeDatabase()` → `JsonDataService.load()`
   → `ObjectMapper.readValue(file, SeedData.class)`.
2. Each user/product/inventory row is inserted **only if it isn't already in the DB**. So you can
   add a new product to the JSON, restart, and it appears — without wiping orders or stock the
   dealer edited in the app.
3. `settings` are read live: `DealerDashboard` uses `lowStockThreshold`, `ShopDashboard` uses
   `orderMonitorIntervalSeconds`, `UiUtils.currency()` uses `currencySymbol`.
4. `WeatherService` now parses the REST response with Jackson into `WeatherResponse`
   (replacing `org.json`) and shows a readable condition (e.g. "Thunderstorm – deliveries may be delayed").
5. **Export JSON** buttons (Shop → Orders & Delivery, Dealer → Orders & Deliveries) write the
   table rows to a `.json` file with `ObjectMapper.writeValue(...)`.

6. **Weather + geocoding**: accounts created on the Register screen have no coordinates (0,0).
   `GeocodingService` now resolves the typed city automatically — first from the offline
   `cities` table in the JSON file (26 Bangladesh cities, works without internet), then from the
   Open-Meteo geocoding API (parsed into `GeocodingResponse` with Jackson) — and saves the result
   to the `users` table. "Check Area Weather" shows the weather at **both** the shop and the
   selected dealer's location.

Use a different file: `-Ddealerlink.data=C:\path\to\file.json`.
If the file is missing, the app still starts with built-in default settings (and logs a warning).

## 2. Theme – `theme.css` ("Sage & Linen")

- **Calm palette**: deep teal `#2F6F6A` and sage `#7FA89B` on warm linen neutrals, with a small
  muted gold accent. No bright or neon colours, soft low-contrast shadows.
- **Backgrounds**: Login/Register use a misty sage → pale sky → linen gradient with two faint
  glows; the brand panel is a deep teal gradient; dashboards use a very soft sage-to-linen wash.
- **Design tokens**: every colour is defined once on `.root` (`-dl-primary`, `-dl-canvas`, …);
  edit those lines to re-theme the whole app.
- Muted pastel status badges, a gentle focus ring on inputs, pill tabs, striped tables with hover,
  a green bar for the best price and a rose bar for low stock, thin scroll bars.
- No inline `setStyle(...)` colours in Java; everything lives in CSS classes.

## 3. Bugs fixed while reconstructing

| Problem | Fix |
|---|---|
| `getGeneratedKeys()` throws `SQLFeatureNotSupportedException` on newer sqlite-jdbc builds → creating requests/bids/orders could crash | `DatabaseManager.lastInsertId()` using `SELECT last_insert_rowid()` |
| Background threads were non-daemon → closing the window without Logout kept the JVM running | daemon thread factory in `BackgroundTaskService` |
| Dealer's inventory-watch executor was never shut down → every login leaked a thread | now scheduled on `bgService`, which Logout shuts down |
| Shop could accept a quotation twice for the same request → duplicate orders | guard: request must still be `OPEN`; table refreshes after accept |
| `WeatherService` set the interrupt flag on normal I/O errors; accounts with lat/lon 0,0 queried the ocean | separated catch blocks; clear message when coordinates are missing |

## 4. Suggested next modifications

1. **Hash passwords** (`UserDAO.register/login`) – store a BCrypt/SHA-256+salt hash, never plain text
   (the JSON demo passwords would then be hashed on seeding).
2. **Keep order and delivery status in sync** (`DealerDashboard` orders tab) – setting delivery to
   `DELIVERED` should also set the order to `DELIVERED` (one DAO method in a transaction).
3. **Wrap multi-step writes in a transaction** (`ShopDashboard` accept button: accept quote +
   reject others + update request + place order) so a failure can't leave half-updated data.
4. **Move DB calls off the UI thread** – `quotesCol`/`yourBidCol` run one SQL query per row while
   rendering; load counts in one `GROUP BY` query inside a JavaFX `Task`.
5. **Dashboard KPI cards** – a row above the tabs (e.g. "Open requests 3 · Best saving ৳25 ·
   Low-stock items 1"); the CSS tokens make these easy to style.
6. **Import from JSON** – a dealer "Import stock (.json)" button re-using `JsonDataService.mapper()`
   to read a list of `InventorySeed`.
7. **Split big UI classes** – `ShopDashboard`/`DealerDashboard` build every tab inline; one class per
   tab (or FXML + controller) makes them easier to test and change.
8. **Add `module-info.java`** if you package with `jlink`/`jpackage`
   (`requires javafx.controls; requires java.sql; requires java.net.http; requires com.fasterxml.jackson.databind; opens com.dealerlink.model to javafx.base, com.fasterxml.jackson.databind; opens com.dealerlink.json to com.fasterxml.jackson.databind;`).
