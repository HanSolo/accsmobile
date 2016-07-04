package eu.hansolo.accs;

import com.gluonhq.charm.down.common.PlatformFactory;
import com.gluonhq.charm.down.common.Position;
import com.gluonhq.charm.down.common.PositionService;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.Icon;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import com.gluonhq.charm.glisten.visual.Swatch;
import com.gluonhq.charm.glisten.visual.SwatchElement;
import com.gluonhq.maps.MapPoint;
import com.gluonhq.maps.MapView;
import eu.hansolo.accs.transitions.SlideInLeftTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker.State;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Callback;
import javafx.util.Duration;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static eu.hansolo.accs.Common.PROPERTIES_FILE_NAME;


public class MainView extends View {
    private static final DateTimeFormatter         DTF         = DateTimeFormatter.ofPattern("dd.MM.YYYY HH:mm");
    private static final DateTimeFormatter         TF          = DateTimeFormatter.ofPattern("HH:mm");
    private static final JMapPoint                 MY_LOCATION = new JMapPoint("", 0, 0);
    private static final Circle                    MY_MARKER   = new Circle(10);
    private              PositionService           positionService;
    private              ChangeListener<Position>  positionChangeListener;
    private              LocationLayer             locationLayer;
    private              MapView                   mapView;
    private              Button                    updateButton;
    private              HBox                      buttonBox;
    private              Button                    backToListButton;
    private              Button                    settingsButton;
    private              ObservableList<JMapPoint> locationList;
    private              ListView<JMapPoint>       listView;
    private              AnchorPane                mainPane;
    private              boolean                   firstStart;
    private              File                      localStoragePath;
    private              Properties                properties;
    private              Timeline                  timeline;
    private              DoubleProperty            anchorY;
    private volatile     ScheduledFuture<?>        updateTask;
    private static       ScheduledExecutorService  periodicUpdateExecutorService;


    public MainView(final String NAME) {
        super(NAME);

        try {
            localStoragePath = PlatformFactory.getPlatform().getPrivateStorage();
        } catch (IOException e) {
            String tmp = System.getProperty("java.io.tmpdir");
            localStoragePath = new File(tmp);
        }
        properties = createProperties();
        retrieveConfig();
        timeline = new Timeline();
        anchorY = new SimpleDoubleProperty(160);
        positionChangeListener = (o, ov, nv) -> {
            ReadOnlyObjectProperty<Position> positionProperty = positionService.positionProperty();
            Position pos = positionProperty.get();
            MY_LOCATION.update(pos.getLatitude(), pos.getLongitude());
            if (firstStart) {
                mapView.setCenter(pos.getLatitude(), pos.getLongitude());
                firstStart = false;
                addMyLocation();
            }
        };

        initGraphics();
        registerListeners();

        setBackground(new Background(new BackgroundFill(Main.BACKGROUND_COLOR, CornerRadii.EMPTY, Insets.EMPTY)));
        setCenter(mainPane);
        setShowTransitionFactory(SlideInLeftTransition::new);

        scheduleUpdateTask();

        updateLocations();
    }

    private void initGraphics() {
        firstStart = true;

        MY_LOCATION.setId(properties.getProperty("name"));
        MY_MARKER.getStyleClass().add("current-location");

        positionService = PlatformFactory.getPlatform().getPositionService();

        backToListButton = MaterialDesignIcon.ARROW_BACK.button(e -> showList());
        backToListButton.setVisible(false);

        settingsButton = MaterialDesignIcon.SETTINGS.button(e -> getApplication().switchView(Main.CONFIG_VIEW));

        locationLayer = new LocationLayer();
        locationLayer.addPoint(MY_LOCATION, MY_MARKER);

        mapView = new MapView();
        mapView.setZoom(13);
        mapView.addLayer(locationLayer);

        updateButton = new Button("Share my location");
        updateButton.setPrefHeight(40);
        updateButton.setMaxWidth(Double.MAX_VALUE);
        updateButton.setAlignment(Pos.CENTER);
        updateButton.setGraphic(new Icon(MaterialDesignIcon.SHARE));

        buttonBox = new HBox(updateButton);
        buttonBox.setPrefHeight(40);
        buttonBox.setMinHeight(40);
        buttonBox.setMaxHeight(40);
        HBox.setHgrow(updateButton, Priority.ALWAYS);

        locationList = FXCollections.observableArrayList();

        listView = new ListView<>(locationList);
        listView.prefWidthProperty().bind(prefWidthProperty());
        listView.setPrefHeight(120);
        listView.setBorder(new Border(new BorderStroke(Swatch.RED.getColor(SwatchElement.PRIMARY_500), BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(1), Insets.EMPTY)));
        listView.setPlaceholder(new Label("No Users loaded"));
        listView.setCellFactory(new Callback<ListView<JMapPoint>, ListCell<JMapPoint>>(){
            @Override public ListCell<JMapPoint> call(ListView<JMapPoint> p) {
                ListCell<JMapPoint> cell = new ListCell<JMapPoint>() {
                    @Override protected void updateItem(final JMapPoint LOCATION, final boolean IS_EMPTY) {
                        super.updateItem(LOCATION, IS_EMPTY);
                        if (LOCATION != null) {
                            String dateTime;
                            Circle circle = new Circle(5);
                            LocalDateTime localDateTime = LocalDateTime.ofInstant(LOCATION.getTimestamp(), ZoneId.systemDefault());
                            if (localDateTime.getDayOfYear() < LocalDateTime.now().getDayOfYear()) {
                                dateTime = DTF.format(localDateTime);
                                circle.getStyleClass().add(LOCATION.getId().equals(MY_LOCATION.getId()) ? "my-marker" : "old-marker");
                            } else {
                                dateTime = TF.format(localDateTime);
                                circle.getStyleClass().add(LOCATION.getId().equals(MY_LOCATION.getId()) ? "my-marker" : "active-marker");
                            }
                            String locationDateTime = LOCATION.getInfo().isEmpty() ? dateTime : LOCATION.getInfo() + " - " + dateTime;

                            Label text = new Label(LOCATION.getId().equals(MY_LOCATION.getId()) ? (LOCATION.getId() + " (shared)") : LOCATION.getId());
                            text.setAlignment(Pos.CENTER_LEFT);
                            text.setMaxWidth(Double.MAX_VALUE);
                            HBox.setHgrow(text, Priority.ALWAYS);
                            Label distance = new Label(String.format(Locale.US, "%.1f km", (LOCATION.getDistanceTo(MY_LOCATION) / 1000d)));
                            distance.setAlignment(Pos.CENTER_RIGHT);
                            distance.getStyleClass().add("distance");
                            HBox hbox = new HBox(5, circle, text, distance);
                            hbox.setAlignment(Pos.CENTER);
                            Label lastUpdate = new Label(locationDateTime);
                            lastUpdate.setAlignment(Pos.CENTER_LEFT);
                            lastUpdate.getStyleClass().add("last-update");

                            VBox vBox = new VBox(hbox, lastUpdate);
                            setGraphic(vBox);
                            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                        }
                    }
                };
                return cell;
            }
        });

        mainPane = new AnchorPane(mapView, buttonBox, listView);

        AnchorPane.setTopAnchor(mapView, 0d);
        AnchorPane.setRightAnchor(mapView, 0d);
        AnchorPane.setBottomAnchor(mapView, 160d);
        AnchorPane.setLeftAnchor(mapView, 0d);

        AnchorPane.setRightAnchor(buttonBox, 0d);
        AnchorPane.setBottomAnchor(buttonBox, 120d);
        AnchorPane.setLeftAnchor(buttonBox, 0d);

        AnchorPane.setRightAnchor(listView, 0d);
        AnchorPane.setBottomAnchor(listView, 0d);
        AnchorPane.setLeftAnchor(listView, 0d);
    }

    private void registerListeners() {
        //mapView.setOnZoomStarted(e -> mapView.removeLayer(locationLayer));
        //mapView.setOnZoomFinished(e -> mapView.addLayer(locationLayer));

        anchorY.addListener(o -> AnchorPane.setBottomAnchor(mapView, anchorY.getValue()));

        listView.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
            if (null == nv) return;
            hideList(nv);
        });

        updateButton.setOnAction(e -> shareMyLocation());

        if (positionService != null) { positionService.positionProperty().addListener(positionChangeListener); }
    }

    @Override protected void updateAppBar(AppBar appBar) {
        appBar.setTitleText("ShareLoc");
        appBar.getActionItems().addAll(backToListButton, settingsButton);
    }

    private void addMyLocation() {
        Task<Void> task = new Task<Void>() {
            @Override protected Void call() throws Exception {
                if (RestClient.INSTANCE.getLocation(MY_LOCATION.getId()).isEmpty()) {
                    RestClient.INSTANCE.addLocation(MY_LOCATION);
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    private void shareMyLocation() {
        Task<Void> task = new Task<Void>() {
            @Override protected Void call() throws Exception {
                retrieveConfig();
                MY_LOCATION.setId(properties.getProperty("name"));
                MY_LOCATION.setTimestamp(Instant.now());
                RestClient.INSTANCE.updateLocation(MY_LOCATION);
                updateLocations();
                return null;
            }
        };
        new Thread(task).start();
    }

    private void updateLocations() {
        // Task to get all Locations via REST
        Task<JSONArray> task = new Task<JSONArray>() {
            @Override protected JSONArray call() throws Exception {
                JSONArray jsonArray = RestClient.INSTANCE.getLocations();
                return jsonArray;
            }
        };

        task.stateProperty().addListener((o, ov, nv) -> {
            if (State.SUCCEEDED == nv) {
                /* test
                positionService.positionProperty().removeListener(positionChangeListener);
                positionService = PlatformFactory.getPlatform().getPositionService();
                positionService.positionProperty().addListener(positionChangeListener);
                // test */

                locationList.clear();
                JSONArray jsonArray = task.getValue();
                for (int i = 0 ; i < jsonArray.size() ; i++) { locationList.add(new JMapPoint(((JSONObject) jsonArray.get(i)))); }
                locationLayer.clearPoints();
                locationLayer.addPoint(MY_LOCATION, MY_MARKER);
                for (JMapPoint location : locationList) {
                    Circle        locationMarker = new Circle(10);
                    LocalDateTime localDateTime  = LocalDateTime.ofInstant(location.getTimestamp(), ZoneId.systemDefault());
                    if (localDateTime.getDayOfYear() < LocalDateTime.now().getDayOfYear()) {
                        locationMarker.getStyleClass().add(location.getId().equals(MY_LOCATION.getId()) ? "my-marker" : "old-marker");
                    } else {
                        locationMarker.getStyleClass().add(location.getId().equals(MY_LOCATION.getId()) ? "my-marker" : "active-marker");
                    }
                    //locationMarker.setOnTouchPressed(new WeakEventHandler<>(e -> {
                    //    Alert alert = new Alert(AlertType.INFORMATION, location.name);
                    //    alert.showAndWait();
                    //}));
                    locationLayer.addPoint(location, locationMarker);
                };
            }});

        new Thread(task).start();
    }

    private void hideList(final JMapPoint MAP_POINT) {
        KeyValue kvListView0  = new KeyValue(listView.translateYProperty(), 0);
        KeyValue kvListView1  = new KeyValue(listView.translateYProperty(), 120);
        KeyValue kvButtonBox0 = new KeyValue(buttonBox.translateYProperty(), 0);
        KeyValue kvButtonBox1 = new KeyValue(buttonBox.translateYProperty(), 120);
        KeyValue kvAnchorY0   = new KeyValue(anchorY, 160);
        KeyValue kvAnchorY1   = new KeyValue(anchorY, 40);

        KeyFrame kf0 = new KeyFrame(Duration.ZERO, kvListView0, kvButtonBox0, kvAnchorY0);
        KeyFrame kf1 = new KeyFrame(Duration.millis(50), kvListView1, kvButtonBox1, kvAnchorY1);

        timeline.getKeyFrames().setAll(kf0, kf1);
        timeline.setOnFinished(e -> {
            mapView.flyTo(1., new MapPoint(MAP_POINT.getLatitude(), MAP_POINT.getLongitude()), 2.5);
            settingsButton.setVisible(false);
            settingsButton.setManaged(false);
            backToListButton.setVisible(true);
        });
        timeline.play();
    }
    private void showList() {
        KeyValue kvListView0  = new KeyValue(listView.translateYProperty(), 120);
        KeyValue kvListView1  = new KeyValue(listView.translateYProperty(), 0);
        KeyValue kvButtonBox0 = new KeyValue(buttonBox.translateYProperty(), 120);
        KeyValue kvButtonBox1 = new KeyValue(buttonBox.translateYProperty(), 0);
        KeyValue kvAnchorY0   = new KeyValue(anchorY, 40);
        KeyValue kvAnchorY1   = new KeyValue(anchorY, 160);

        KeyFrame kf0 = new KeyFrame(Duration.ZERO, kvListView0, kvButtonBox0, kvAnchorY0);
        KeyFrame kf1 = new KeyFrame(Duration.millis(50), kvListView1, kvButtonBox1, kvAnchorY1);

        timeline.getKeyFrames().setAll(kf0, kf1);
        timeline.setOnFinished(e -> {
            backToListButton.setVisible(false);
            settingsButton.setManaged(true);
            settingsButton.setVisible(true);
        });
        timeline.play();
    }

    private Properties createProperties() {
        Properties p = new Properties();
        p.setProperty("url", "");
        p.setProperty("name", Common.getUniqueId());
        return p;
    }
    private void retrieveConfig() {
        Reader reader = null;
        try {
            File file = new File(localStoragePath, PROPERTIES_FILE_NAME);
            reader = new FileReader(file);
            properties.load(reader);
        } catch (IOException ex) {
        } finally {
            try { if (reader != null) { reader.close(); } } catch (IOException ex) {}
        }
    }


    // ******************** Scheduled Tasks ***********************************
    private synchronized static void enableUpdateExecutorService() {
        if (null == periodicUpdateExecutorService) {
            periodicUpdateExecutorService = new ScheduledThreadPoolExecutor(1, getThreadFactory("UpdateTask", false));
        }
    }
    private synchronized void scheduleUpdateTask() {
        enableUpdateExecutorService();
        stopTask(updateTask);
        updateTask = periodicUpdateExecutorService.scheduleAtFixedRate(() -> Platform.runLater(() -> updateLocations()), 60, 30, TimeUnit.SECONDS);
    }

    private static ThreadFactory getThreadFactory(final String THREAD_NAME, final boolean IS_DAEMON) {
        return runnable -> {
            Thread thread = new Thread(runnable, THREAD_NAME);
            thread.setDaemon(IS_DAEMON);
            return thread;
        };
    }

    private void stopTask(ScheduledFuture<?> task) {
        if (null == task) return;

        task.cancel(true);
        task = null;
    }
}
