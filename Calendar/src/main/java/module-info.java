module com.calendar {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop; // For Toolkit.beep()

    opens com.calendar to javafx.fxml;

    exports com.calendar;
}
