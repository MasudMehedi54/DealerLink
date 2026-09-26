package com.dealerlink.ui;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.value.ObservableDoubleValue;
import javafx.scene.Node;
import javafx.scene.layout.Region;

import java.util.Locale;

/**
 * Layout responsiveness helpers.
 *
 * Every method links a node's size, font or visibility to the WINDOW's width or
 * height through JavaFX property bindings. So when the window is resized the UI
 * recalculates automatically - nothing is hard-coded to one resolution.
 *
 * Pattern used everywhere:
 *     value = clamp(windowSize * factor, min, max)
 * so things scale proportionally but never become unusably small or absurdly large.
 *
 * Example (LoginController):
 *     Responsive.bindPrefWidth(card, root.widthProperty(), 0.34, 340, 460);
 *     -> the login card is always 34% of the window width, between 340 and 460 px.
 */
public final class Responsive {

    private Responsive() {}

    /** A binding equal to clamp(size * factor, min, max) that updates whenever size changes. */
    public static DoubleBinding scaled(ObservableDoubleValue size, double factor, double min, double max) {
        return Bindings.createDoubleBinding(
                () -> Math.max(min, Math.min(max, size.get() * factor)), size);
    }

    /** prefWidth = clamp(windowWidth * factor, min, max). */
    public static void bindPrefWidth(Region region, ObservableDoubleValue windowWidth,
                                     double factor, double min, double max) {
        region.prefWidthProperty().bind(scaled(windowWidth, factor, min, max));
    }

    /** prefWidth AND maxWidth follow the window, so parents can't stretch it further. */
    public static void bindWidth(Region region, ObservableDoubleValue windowWidth,
                                 double factor, double min, double max) {
        DoubleBinding w = scaled(windowWidth, factor, min, max);
        region.prefWidthProperty().bind(w);
        region.maxWidthProperty().bind(w);
    }

    /** prefHeight = clamp(windowHeight * factor, min, max). */
    public static void bindPrefHeight(Region region, ObservableDoubleValue windowHeight,
                                      double factor, double min, double max) {
        region.prefHeightProperty().bind(scaled(windowHeight, factor, min, max));
    }

    /** minHeight = clamp(windowHeight * factor, min, max) - e.g. tables never collapse on short windows. */
    public static void bindMinHeight(Region region, ObservableDoubleValue windowHeight,
                                     double factor, double min, double max) {
        region.minHeightProperty().bind(scaled(windowHeight, factor, min, max));
    }

    /** Font size in px = clamp(windowSize * factor, min, max) - text grows with the window. */
    public static void bindFontSize(Node node, ObservableDoubleValue windowSize,
                                    double factor, double min, double max) {
        DoubleBinding px = scaled(windowSize, factor, min, max);
        node.styleProperty().bind(Bindings.createStringBinding(
                () -> String.format(Locale.US, "-fx-font-size: %.1fpx;", px.get()), px));
    }

    /**
     * Shows the node only while the window is at least {@code threshold} px.
     * managed is bound too, so a hidden node also gives its space back to the layout.
     * (A size of 0 means "not laid out yet" and counts as wide, to avoid a flicker.)
     */
    public static void showWhenAtLeast(Node node, ObservableDoubleValue windowSize, double threshold) {
        BooleanBinding wide = Bindings.createBooleanBinding(
                () -> windowSize.get() == 0 || windowSize.get() >= threshold, windowSize);
        node.visibleProperty().bind(wide);
        node.managedProperty().bind(wide);
    }

    /** Spacing between children follows the window height (tighter on short windows). */
    public static void bindSpacing(javafx.scene.layout.VBox box, ObservableDoubleValue windowHeight,
                                   double factor, double min, double max) {
        box.spacingProperty().bind(scaled(windowHeight, factor, min, max));
    }
}
