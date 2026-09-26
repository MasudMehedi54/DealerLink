package com.dealerlink;

/**
 * Entry point for the runnable fat jar (java -jar target/DealerLink-1.0.0.jar).
 *
 * A class that extends javafx.application.Application cannot be the jar's
 * Main-Class when JavaFX is on the classpath instead of the module path - Java
 * refuses to start with "JavaFX runtime components are missing". This plain class
 * simply forwards to Main, which avoids that check. mvn javafx:run still uses Main.
 */
public class Launcher {
    public static void main(String[] args) {
        Main.main(args);
    }
}
