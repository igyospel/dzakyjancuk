package com.calendar;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

public class CalendarApp extends Application {

    private YearMonth currentYearMonth;
    private LocalDate selectedDate;
    private final Map<LocalDate, List<Event>> events = new HashMap<>();

    private GridPane calendarGrid;
    private Label monthYearLabel;
    private VBox eventsContainer;
    private Label selectedDateDayLabel;
    private Label selectedDateFullLabel;
    private Button addEventBtn;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        currentYearMonth = YearMonth.now();
        selectedDate = LocalDate.now();

        // Seed some data for demo purposes
        seedData();

        // Main Layout
        HBox mainContent = new HBox(30);
        mainContent.setPadding(new Insets(30));
        mainContent.getStyleClass().add("main-background");

        // Left Side: Calendar
        VBox calendarPane = buildCalendarPane();
        HBox.setHgrow(calendarPane, Priority.ALWAYS);

        // Right Side: Details
        VBox detailsPane = buildDetailsPane();
        detailsPane.setPrefWidth(350);
        detailsPane.setMinWidth(350);

        mainContent.getChildren().addAll(calendarPane, detailsPane);

        // Root StackPane for layering background + content
        StackPane root = new StackPane();

        // 1. Animated Background Canvas
        javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(1000, 700);
        root.getChildren().add(canvas);

        // 2. Main Content
        root.getChildren().add(mainContent);

        // Start Animation
        startBackgroundAnimation(canvas);

        Scene scene = new Scene(root, 1000, 700);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        primaryStage.setTitle("Calendar");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Bind canvas size to scene size
        canvas.widthProperty().bind(scene.widthProperty());
        canvas.heightProperty().bind(scene.heightProperty());

        updateCalendar();
        updateDetails();

        startNotificationService();
    }

    private final Set<Event> notifiedEvents = new HashSet<>();

    private void startNotificationService() {
        // Check every 30 seconds
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(30), e -> {
                    checkEventsForNotification();
                }));
        timeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        timeline.play();
    }

    private void checkEventsForNotification() {
        java.time.LocalTime now = java.time.LocalTime.now();
        LocalDate today = LocalDate.now();

        if (events.containsKey(today)) {
            for (Event event : events.get(today)) {
                if (notifiedEvents.contains(event))
                    continue;

                java.time.LocalTime eventTime = parseStartTime(event);
                if (eventTime == java.time.LocalTime.MAX)
                    continue; // Invalid time

                // Check if match (ignoring seconds)
                if (eventTime.getHour() == now.getHour() && eventTime.getMinute() == now.getMinute()) {
                    showNotification("Event Reminder", "It's time for: " + event.title);
                    notifiedEvents.add(event);
                }
            }
        }
    }

    private void showNotification(String title, String message) {
        // Use JavaFX Alert with a specific "Stop" button to simulate dismissing an
        // alarm
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText("Reminder");
        alert.setContentText(message);

        // Custom "Stop" button
        javafx.scene.control.ButtonType stopButton = new javafx.scene.control.ButtonType("Stop",
                javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(stopButton);

        alert.initOwner(calendarGrid.getScene().getWindow());
        alert.show();

        // Play sound
        java.awt.Toolkit.getDefaultToolkit().beep();
    }

    private void startBackgroundAnimation(javafx.scene.canvas.Canvas canvas) {
        javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();
        Random rand = new Random();
        int particleCount = 150; // More particles for stars
        double[] x = new double[particleCount];
        double[] y = new double[particleCount];
        double[] vx = new double[particleCount];
        double[] vy = new double[particleCount];
        double[] size = new double[particleCount];
        double[] opacity = new double[particleCount];
        double[] opacitySpeed = new double[particleCount];

        // Initialize particles
        for (int i = 0; i < particleCount; i++) {
            x[i] = rand.nextDouble() * 1000;
            y[i] = rand.nextDouble() * 700;
            // Very slow drift
            vx[i] = (rand.nextDouble() - 0.5) * 0.2;
            vy[i] = (rand.nextDouble() - 0.5) * 0.2;
            size[i] = 1 + rand.nextDouble() * 3; // Small stars
            opacity[i] = rand.nextDouble();
            opacitySpeed[i] = (rand.nextDouble() - 0.5) * 0.02; // Twinkle speed
        }

        new javafx.animation.AnimationTimer() {
            @Override
            public void handle(long now) {
                // Clear background
                gc.setFill(Color.web("#0b0b0b"));
                gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

                for (int i = 0; i < particleCount; i++) {
                    // Update movement
                    x[i] += vx[i];
                    y[i] += vy[i];

                    // Wrap around screen
                    if (x[i] < -10)
                        x[i] = canvas.getWidth() + 10;
                    if (x[i] > canvas.getWidth() + 10)
                        x[i] = -10;
                    if (y[i] < -10)
                        y[i] = canvas.getHeight() + 10;
                    if (y[i] > canvas.getHeight() + 10)
                        y[i] = -10;

                    // Update twinkle
                    opacity[i] += opacitySpeed[i];
                    if (opacity[i] > 1.0 || opacity[i] < 0.2) {
                        opacitySpeed[i] = -opacitySpeed[i]; // Reverse twinkle direction
                    }
                    // Clamp opacity
                    double currentOpacity = Math.max(0.2, Math.min(1.0, opacity[i]));

                    gc.setFill(Color.color(1, 1, 1, currentOpacity)); // White stars
                    gc.fillOval(x[i], y[i], size[i], size[i]);
                }
            }
        }.start();
    }

    // Helper to add hover animation to buttons
    private void addSmoothColorAnimation(Button btn, Color normalBg, Color hoverBg, Color normalText, Color hoverText) {
        btn.setOnMouseEntered(e -> {
            javafx.animation.Transition t = new javafx.animation.Transition() {
                {
                    setCycleDuration(javafx.util.Duration.millis(300));
                } // 300ms smooth transition

                @Override
                protected void interpolate(double frac) {
                    Color currentBg = normalBg.interpolate(hoverBg, frac);
                    Color currentText = normalText.interpolate(hoverText, frac);
                    String bgHex = toHex(currentBg);
                    String textHex = toHex(currentText);
                    btn.setStyle(String.format(
                            "-fx-background-color: %s; -fx-text-fill: %s; -fx-background-radius: 6; -fx-border-color: rgba(255, 255, 255, 0.15); -fx-border-radius: 6; -fx-padding: 12 30; -fx-font-size: 14px; -fx-font-weight: 700;",
                            bgHex, textHex));
                }
            };
            t.play();
        });

        btn.setOnMouseExited(e -> {
            javafx.animation.Transition t = new javafx.animation.Transition() {
                {
                    setCycleDuration(javafx.util.Duration.millis(300));
                }

                @Override
                protected void interpolate(double frac) {
                    Color currentBg = hoverBg.interpolate(normalBg, frac);
                    Color currentText = hoverText.interpolate(normalText, frac);
                    String bgHex = toHex(currentBg);
                    String textHex = toHex(currentText);
                    btn.setStyle(String.format(
                            "-fx-background-color: %s; -fx-text-fill: %s; -fx-background-radius: 6; -fx-border-color: rgba(255, 255, 255, 0.15); -fx-border-radius: 6; -fx-padding: 12 30; -fx-font-size: 14px; -fx-font-weight: 700;",
                            bgHex, textHex));
                }

            };
            t.play();
        });
    }

    private String toHex(Color c) {
        return String.format("#%02X%02X%02X",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }

    private void addInteractiveAnimation(javafx.scene.Node node) {
        javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(200),
                node);
        node.setOnMouseEntered(e -> {
            st.setToX(1.1);
            st.setToY(1.1);
            st.playFromStart();
            node.setEffect(new javafx.scene.effect.DropShadow(10, Color.BLACK));
        });
        node.setOnMouseExited(e -> {
            st.setToX(1.0);
            st.setToY(1.0);
            st.playFromStart();
            node.setEffect(null);
        });
    }

    private void seedData() {
        LocalDate today = LocalDate.now();
        // Create some events for "today" or specific dates
        // Image example: 7 Nov 25
        LocalDate exampleDate = LocalDate.of(2025, 11, 7);
        // If we are around that time, great, otherwise let's just add to today + offset

        addEvent(exampleDate, "Lecture", "08:00-09:00");
        addEvent(exampleDate, "Math Deadline", "13:00-14:00");
        addEvent(exampleDate, "Futsal", "20:00-22:00");

        // Also add to current date so user sees something immediately
        addEvent(LocalDate.now(), "Team Meeting", "10:00-11:00");
        addEvent(LocalDate.now(), "Lunch", "12:00-13:00");
    }

    private void addEvent(LocalDate date, String title, String time) {
        events.computeIfAbsent(date, k -> new ArrayList<>()).add(new Event(title, time));
    }

    private VBox buildCalendarPane() {
        VBox pane = new VBox(20);

        // Header: MONTH YEAR >
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        monthYearLabel = new Label();
        monthYearLabel.getStyleClass().add("month-year-label");
        // Allow clicking the label to jump to a date
        monthYearLabel.setCursor(javafx.scene.Cursor.HAND);
        monthYearLabel.setOnMouseClicked(e -> {
            javafx.scene.control.DatePicker datePicker = new javafx.scene.control.DatePicker(
                    currentYearMonth.atDay(1));
            // Hide the week numbers and just use it as a standard picker
            datePicker.setShowWeekNumbers(false);

            javafx.scene.control.Dialog<LocalDate> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle("Jump to Date");
            dialog.setHeaderText("Select a date to jump to that month");
            dialog.getDialogPane().setContent(datePicker);
            dialog.getDialogPane().getButtonTypes().addAll(
                    javafx.scene.control.ButtonType.OK,
                    javafx.scene.control.ButtonType.CANCEL);

            dialog.setResultConverter(btnType -> {
                if (btnType == javafx.scene.control.ButtonType.OK) {
                    return datePicker.getValue();
                }
                return null;
            });

            dialog.showAndWait().ifPresent(date -> {
                currentYearMonth = java.time.YearMonth.from(date);
                updateCalendar();
            });
        });

        Button nextBtn = new Button(">");
        nextBtn.getStyleClass().add("nav-button");
        addInteractiveAnimation(nextBtn);
        nextBtn.setOnAction(e -> {
            currentYearMonth = currentYearMonth.plusMonths(1);
            updateCalendar();
        });

        Button prevBtn = new Button("<");
        prevBtn.getStyleClass().add("nav-button");
        addInteractiveAnimation(prevBtn);
        prevBtn.setOnAction(e -> {
            currentYearMonth = currentYearMonth.minusMonths(1);
            updateCalendar();
        });

        header.getChildren().addAll(monthYearLabel, prevBtn, nextBtn);

        // Grid
        calendarGrid = new GridPane();
        calendarGrid.setHgap(10);
        calendarGrid.setVgap(10);

        pane.getChildren().addAll(header, calendarGrid);
        return pane;
    }

    private VBox buildDetailsPane() {
        VBox pane = new VBox(20);
        pane.getStyleClass().add("details-pane");

        // Date Header
        VBox dateHeader = new VBox(0);
        selectedDateDayLabel = new Label(); // e.g. FRIDAY
        selectedDateDayLabel.getStyleClass().add("detail-day-label");

        selectedDateFullLabel = new Label(); // e.g. 7 NOV 25
        selectedDateFullLabel.getStyleClass().add("detail-date-label");

        dateHeader.getChildren().addAll(selectedDateDayLabel, selectedDateFullLabel);

        // Events List
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("event-scroll-pane");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        eventsContainer = new VBox(10);
        scrollPane.setContent(eventsContainer);

        // Add Event Button
        addEventBtn = new Button("ADD EVENT");
        addEventBtn.getStyleClass().add("add-event-button");
        addEventBtn.setMaxWidth(Double.MAX_VALUE);
        addInteractiveAnimation(addEventBtn);

        // Smooth transition for AUDIT & BUILD style (Dark to White)
        addSmoothColorAnimation(addEventBtn,
                Color.web("#111111"), Color.WHITE, // Bg: Dark -> White
                Color.WHITE, Color.BLACK // Text: White -> Black
        );

        addEventBtn.setOnAction(e -> {
            // Create a custom dialog for entering event details
            javafx.scene.control.Dialog<Event> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle("Add New Event");
            dialog.setHeaderText("Enter event details");

            // Set the button types
            javafx.scene.control.ButtonType loginButtonType = new javafx.scene.control.ButtonType("Add",
                    javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, javafx.scene.control.ButtonType.CANCEL);

            // Create the username and password labels and fields.
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            javafx.scene.control.TextField titleField = new javafx.scene.control.TextField();
            titleField.setPromptText("Event Title");
            javafx.scene.control.TextField timeField = new javafx.scene.control.TextField();
            timeField.setPromptText("Time (e.g. 10:00-11:00)");

            grid.add(new Label("Title:"), 0, 0);
            grid.add(titleField, 1, 0);
            grid.add(new Label("Time:"), 0, 1);
            grid.add(timeField, 1, 1);

            dialog.getDialogPane().setContent(grid);

            // Request focus on the title field by default.
            javafx.application.Platform.runLater(titleField::requestFocus);

            // Convert the result to a username-password-pair when the login button is
            // clicked.
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == loginButtonType) {
                    return new Event(titleField.getText(), timeField.getText());
                }
                return null;
            });

            Optional<Event> result = dialog.showAndWait();

            result.ifPresent(event -> {
                if (!event.title.isEmpty()) {
                    addEvent(selectedDate, event.title, event.time.isEmpty() ? "All Day" : event.time);
                    updateDetails();
                    updateCalendar();
                }
            });
        });

        pane.getChildren().addAll(dateHeader, scrollPane);
        return pane;

    }

    private void updateCalendar() {
        monthYearLabel.setText(currentYearMonth.getMonth().name() + " " + currentYearMonth.getYear());
        calendarGrid.getChildren().clear();

        // Days of week can be headers if desired, but image doesn't show them clearly
        // inside the grid cells.
        // Image assumes a grid. Let's make it 7 columns.

        LocalDate firstDayOfMonth = currentYearMonth.atDay(1);
        int dayOfWeekValue = firstDayOfMonth.getDayOfWeek().getValue(); // 1=Mon, 7=Sun

        // Adjust standard calendar start (Sunday vs Monday). Let's start Monday (1) for
        // now or match image?
        // Image has 7 cols. If we assume row 1 col 1 is the 1st of month:
        // That means we fill purely by date, not aligning to weekdays?
        // "31" is on row 5, col 3.
        // It's safer to align to weekdays. Let's align to Sunday-Saturday or
        // Monday-Sunday.
        // I will use Monday start.

        // Let's assume the user wants a standard calendar view.
        // 1-indexed.
        int col = 0;
        int row = 0;

        // Pad previous month? Image doesn't show previous month days.
        // We just need to offset the start.
        // If we want Mon-Sun:
        int offset = dayOfWeekValue - 1; // Mon(1) -> 0 offset.

        // Fill empty slots
        for (int i = 0; i < offset; i++) {
            Region spacer = new Region();
            calendarGrid.add(spacer, i, 0);
        }

        col = offset;

        int daysInMonth = currentYearMonth.lengthOfMonth();
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentYearMonth.atDay(day);
            Button dayCell = createDayCell(date);
            calendarGrid.add(dayCell, col, row);

            col++;
            if (col > 6) {
                col = 0;
                row++;
            }
        }
    }

    private Button createDayCell(LocalDate date) {
        Button cell = new Button();
        cell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        cell.setPrefSize(80, 80);
        cell.setAlignment(Pos.TOP_LEFT);
        cell.getStyleClass().add("day-cell");

        VBox content = new VBox(0);
        content.setAlignment(Pos.TOP_LEFT);

        Label dateLbl = new Label(String.valueOf(date.getDayOfMonth()));
        dateLbl.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: 700;");
        content.getChildren().add(dateLbl);

        List<Event> evs = events.get(date);
        if (evs != null && !evs.isEmpty()) {
            HBox countBox = new HBox(0);
            countBox.setAlignment(Pos.CENTER_LEFT);

            Label countLbl = new Label(String.valueOf(evs.size()));
            countLbl.setStyle("-fx-text-fill: #FF5252; -fx-font-size: 11px; -fx-font-weight: 900;");

            SVGPath icon = new SVGPath();
            // Bell/Alarm icon path
            icon.setContent(
                    "M12 22c1.1 0 2-.9 2-2h-4c0 1.1.9 2 2 2zm6-6v-5c0-3.07-1.63-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.64 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z");
            icon.setFill(Color.web("#FF5252")); // Neon Red
            icon.setScaleX(0.6);
            icon.setScaleY(0.6);

            // Container for icon to ensure correct spacing/sizing
            StackPane iconPane = new StackPane(icon);
            iconPane.prefWidthProperty().set(16);
            iconPane.prefHeightProperty().set(16);

            countBox.getChildren().addAll(countLbl, iconPane);
            content.getChildren().add(countBox);
        }

        cell.setGraphic(content);

        addInteractiveAnimation(cell);

        if (date.equals(LocalDate.now())) {
            cell.getStyleClass().add("current-day-cell");
        }

        if (date.equals(selectedDate)) {
            cell.getStyleClass().add("selected-day-cell");
        }

        cell.setOnAction(e -> {
            selectedDate = date;
            updateCalendar(); // to refresh selection
            updateDetails();
        });

        return cell;
    }

    private void updateDetails() {
        if (selectedDate == null)
            return;

        selectedDateDayLabel
                .setText(selectedDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toUpperCase());
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("d MMM yy");
        selectedDateFullLabel.setText(selectedDate.format(dtf).toUpperCase());

        eventsContainer.getChildren().clear();
        List<Event> daysEvents = events.getOrDefault(selectedDate, Collections.emptyList());

        List<Event> sortedEvents = new ArrayList<>(daysEvents);
        sortedEvents.sort(Comparator.comparing(this::parseStartTime));

        for (Event event : sortedEvents) {
            eventsContainer.getChildren().add(createEventCard(event));
        }
        eventsContainer.getChildren().add(addEventBtn);
    }

    private java.time.LocalTime parseStartTime(Event event) {
        try {
            // Attempt to parse the first 5 characters "HH:mm"
            // We strip any whitespace first just in case
            String t = event.time.trim();
            if (t.matches("^\\d{1,2}:\\d{2}.*")) {
                // If it looks like H:mm or HH:mm...
                // Normalize "8:00" to "08:00" if necessary for standard parsing,
                // but LocalTime.parse usually wants 08:00.
                if (t.indexOf(':') == 1) {
                    t = "0" + t;
                }
                return java.time.LocalTime.parse(t.substring(0, 5));
            }
        } catch (Exception e) {
            // Fallthrough
        }
        // If parsing fails, put it at the end
        return java.time.LocalTime.MAX;
    }

    private HBox createEventCard(Event event) {
        HBox card = new HBox(10);
        card.getStyleClass().add("event-card");
        card.setAlignment(Pos.CENTER_LEFT);

        VBox txt = new VBox(2);
        Label title = new Label(event.title);
        title.getStyleClass().add("event-title");
        Label time = new Label(event.time);
        time.getStyleClass().add("event-time");
        txt.getChildren().addAll(title, time);
        HBox.setHgrow(txt, Priority.ALWAYS);

        Button delBtn = new Button();
        delBtn.getStyleClass().add("delete-button");
        addInteractiveAnimation(delBtn);
        SVGPath trashIcon = new SVGPath();
        trashIcon.setContent("M15.5 4l-1-1h-5l-1 1H5v2h14V4zM6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12z"); // Simple
                                                                                                            // trash can
        trashIcon.setFill(Color.WHITE);
        trashIcon.setScaleX(0.7);
        trashIcon.setScaleY(0.7);
        delBtn.setGraphic(trashIcon);

        delBtn.setOnAction(e -> {
            List<Event> daysEvents = events.get(selectedDate);
            if (daysEvents != null) {
                daysEvents.remove(event);
                updateDetails();
                updateCalendar();
            }
        });

        card.getChildren().addAll(txt, delBtn);
        return card;
    }

    static class Event {
        String title;
        String time;

        public Event(String title, String time) {
            this.title = title;
            this.time = time;
        }
    }
}
