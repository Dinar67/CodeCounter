module com.example.codecounter {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;
    requires java.net.http;

    // POI модули
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;
    requires org.apache.xmlbeans;
    requires org.apache.commons.compress;
    requires org.apache.commons.collections4;
    requires java.desktop;
    requires com.google.gson;

    opens com.example.codecounter to javafx.fxml;
    opens images to javafx.fxml, javafx.graphics;
    opens Classes to com.google.gson;
    opens LanguageLexer.LanguageToken to com.google.gson;
    exports com.example.codecounter;
}