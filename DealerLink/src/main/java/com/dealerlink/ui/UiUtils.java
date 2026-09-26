package com.dealerlink.ui;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;

import java.util.function.Function;

/**
 * Small shared helpers so every screen (Login, Register, Shop/Dealer dashboards)
 * looks and behaves consistently: same currency format, same colored status
 * badges, same button styles. Keeps theme.css as the single source of truth
 * for colors/fonts - this class just wires Java controls to those CSS classes.
 */
public final class UiUtils {

    private UiUtils() {}

    public static final String CURRENCY_SYMBOL = "\u09F3"; // Bengali Taka sign (৳)

    /** Formats a price as "৳1,450.00". */
    public static String currency(double value) {
        return CURRENCY_SYMBOL + String.format("%,.2f", value);
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
        return l;
    }

    /** Wraps content in the rounded, drop-shadowed "card" panel used on Login/Register. */
    public static VBox card(javafx.scene.Node... children) {
        VBox box = new VBox(16, children);
        box.getStyleClass().add("card");
        return box;
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
                setStyle(empty ? "" : "-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold;");
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
