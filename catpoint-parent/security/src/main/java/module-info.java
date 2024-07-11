module security {
    requires miglayout.swing;
    requires java.desktop;
    requires com.google.common;
    requires com.google.gson;
    requires java.prefs;
    requires image;
    opens com.uda.security.data to com.google.gson;
    exports com.uda.security.service;
}