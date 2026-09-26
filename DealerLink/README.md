# DealerLink – B2B Shop ↔ Dealer Platform (JavaFX + Maven)

## Run

```bash
mvn clean javafx:run                     # run the app
mvn clean package                        # build a runnable jar
java -jar target/DealerLink-1.0.0.jar    # run the jar
```

Demo logins: `shop1 / 1234`, `dealer1 / 1234`, `dealer2 / 1234`

## What's in this version

| Added / changed | Purpose |
|---|---|
| `src/main/resources/fxml/login.fxml` | Login screen layout (mirrors `ui/LoginScreen.java`) |
| `src/main/resources/fxml/register.fxml` | Register screen layout (mirrors `ui/RegisterScreen.java`) |
| `src/main/resources/fxml/shop-dashboard.fxml` | Shop owner dashboard (mirrors `ui/ShopDashboard.java`) |
| `src/main/resources/fxml/dealer-dashboard.fxml` | Dealer dashboard (mirrors `ui/DealerDashboard.java`) |
| `pom.xml` | Configured for FXML resources, UTF-8, Java 17, runnable fat jar |
| `src/main/java/com/dealerlink/Launcher.java` | Main-Class for the fat jar (see below) |

All other Java files and `css/theme.css` are unchanged.

### FXML files
- Open any of them in **Scene Builder** (File → Open). Each one links
  `@../css/theme.css`, so the preview uses the real theme.
- Every layout is built with `GridPane` **percentWidth / percentHeight** constraints, so all
  regions are sized **relative to the window width and height** and resize with it
  (select a GridPane → *Layout* in Scene Builder to see them).
- `fx:id`s match the variable names in the Java screen classes, ready for a controller.

### pom.xml changes
- `javafx-fxml` dependency (needed by `FXMLLoader`) is included.
- `<resources>` explicitly copies `*.fxml`, `*.css` (and images/json/properties) to the classpath.
- `maven-compiler-plugin` (Java 17 `release`) and `maven-resources-plugin` with UTF-8, because the
  FXML/CSS contain emoji.
- `maven-shade-plugin` now uses `com.dealerlink.Launcher` as Main-Class, strips signature files,
  and merges service files. Without the launcher, `java -jar` on a JavaFX app fails with
  *"JavaFX runtime components are missing"*.
- `javafx-maven-plugin` still starts `com.dealerlink.Main` for `mvn javafx:run`.

### Loading an FXML (optional, for later)
```java
Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
stage.setScene(new Scene(root, 1000, 680));
```
