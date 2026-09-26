package com.dealerlink.ui;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import com.dealerlink.json.JsonDataService;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Small shared helpers so every screen (Login, Register, Shop/Dealer dashboards)
 * looks and behaves consistently: same currency format, same colored status
 * badges, same button styles. Keeps theme.css as the single source of truth
 * for colors/fonts - this class just wires Java controls to those CSS classes.
 */
public final class UiUtils {

    private UiUtils() {}

    /** Currency symbol from data/dealerlink-data.json ("settings.currencySymbol"), default ৳. */
    public static String currencySymbol() {
        return JsonDataService.settings().getCurrencySymbol();
    }

    /** Formats a price as "৳1,450.00". */
    public static String currency(double value) {
        return currencySymbol() + String.format("%,.2f", value);
    }

    // ---------- Styled buttons ----------
    public static Button primaryButton(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("button-primary");
        return b;
    }

    public static Button secondaryButton(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("button-secondary");
        return b;
    }

    public static Button dangerButton(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("button-danger");
        return b;
    }

    public static Button ghostButton(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("button-ghost");
        return b;
    }

    /** Translucent button that sits on the gradient header bar (e.g. Logout). */
    public static Button headerButton(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("button-header");
        return b;
    }

    /**
     * "Export JSON" button: asks where to save, then writes whatever the supplier
     * returns (e.g. the rows of a table) as pretty-printed JSON using Jackson.
     */
    public static Button exportJsonButton(String defaultFileName, Supplier<?> data, Label status) {
        Button b = secondaryButton("\u2B07  Export JSON");
        b.setTooltip(new Tooltip("Save this list as a .json file"));
        b.setOnAction(e -> exportJson(b.getScene().getWindow(), defaultFileName, data.get(), status));
        return b;
    }

    /**
     * Asks where to save, then writes {@code data} (e.g. the rows of a table) as
     * pretty-printed JSON using Jackson. Used by the "Export JSON" buttons in the FXML screens.
     */
    public static void exportJson(Window owner, String defaultFileName, Object data, Label status) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export to JSON");
        chooser.setInitialFileName(defaultFileName);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files", "*.json"));
        File file = chooser.showSaveDialog(owner);
        if (file == null) return;
        try {
            JsonDataService.exportToFile(data, file);
            status.getStyleClass().setAll("label", "success-text");
            status.setText("\u2714 Exported to " + file.getName());
        } catch (Exception ex) {
            status.getStyleClass().setAll("label", "error-text");
            status.setText("Export failed: " + ex.getMessage());
        }
    }

    /** Shows a success (green) or error (red) message in a status label. */
    public static void setStatus(Label label, boolean ok, String message) {
        label.getStyleClass().setAll("label", ok ? "success-text" : "error-text");
        label.setText(message);
    }

    public static Button withTooltip(Button b, String tooltip) {
        b.setTooltip(new Tooltip(tooltip));
        return b;
    }

    // ---------- Labels ----------
    public static Label sectionTitle(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("section-title");
        return l;
    }

    public static Label hint(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("hint-text");
        return l;
    }

    public static Label errorText(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("error-text");
        return l;
    }

    public static Label successText(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("success-text");
        return l;
    }

    /** A small colored pill, e.g. OPEN / QUOTED / DELIVERED, colored per theme.css. */
    public static Label badge(String status) {
        String safe = status == null ? "pending" : status;
        Label l = new Label(safe.replace("_", " "));
        String cssClass = "badge-" + safe.toLowerCase().replace("_", "-");
        l.getStyleClass().addAll("badge", cssClass);
        l.setMinWidth(Region.USE_PREF_SIZE); // never truncate the pill to "O..."
        return l;
    }

    /** Wraps content in the rounded, drop-shadowed "card" panel used on Login/Register. */
    public static VBox card(javafx.scene.Node... children) {
        VBox box = new VBox(16, children);
        box.getStyleClass().add("card");
        return box;
    }

    // ---------- Tables ----------

    /**
     * Applies the shared table look: theme class, columns stretched to fill the
     * full width (no truncated "Qty..." headers, no empty grey strip on the right),
     * and lets the table grow vertically inside its VBox.
     */
    @SuppressWarnings("deprecation")
    public static <S> TableView<S> polishTable(TableView<S> table) {
        if (!table.getStyleClass().contains("table-view")) table.getStyleClass().add("table-view");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setMinHeight(140);
        return table;
    }

    // ---------- Table column renderers ----------

    /** Renders a String status column as a colored badge instead of plain text. */
    public static <S> void renderAsBadge(TableColumn<S, String> column) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    setGraphic(badge(status));
                }
            }
        });
    }

    /** Renders a Number column (price) formatted as currency, right-aligned. */
    public static <S> void renderAsCurrency(TableColumn<S, Number> column) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : currency(value.doubleValue()));
                getStyleClass().remove("currency-cell");
                if (!empty) getStyleClass().add("currency-cell");
            }
        });
    }

    /**
     * Highlights table rows for which the predicate is true (e.g. lowest price,
     * or low stock) with the given CSS class, applied via a row factory.
     */
    public static <S> javafx.util.Callback<javafx.scene.control.TableView<S>, javafx.scene.control.TableRow<S>>
            highlightRowFactory(Function<S, Boolean> predicate, String styleClass) {
        return tv -> new javafx.scene.control.TableRow<>() {
            @Override
            protected void updateItem(S item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove(styleClass);
                if (!empty && item != null && Boolean.TRUE.equals(predicate.apply(item))) {
                    getStyleClass().add(styleClass);
                }
            }
        };
    }
}
