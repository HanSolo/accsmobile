package eu.hansolo.accs;

import com.gluonhq.charm.down.common.JavaFXPlatform;
import com.gluonhq.charm.down.common.PlatformFactory;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.visual.Swatch;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Random;


public class Main extends MobileApplication {
    public  static final  String MAIN_VIEW        = HOME_VIEW;
    public  static final  String CONFIG_VIEW      = "CONFIG";
    public  static final  Color  BACKGROUND_COLOR = Color.rgb(66, 71, 79);


    @Override public void init() {
        addViewFactory(MAIN_VIEW, () -> new MainView(MAIN_VIEW));
        addViewFactory(CONFIG_VIEW, () -> new ConfigView(CONFIG_VIEW));
    }

    @Override public void postInit(final Scene SCENE) {
        SCENE.getStylesheets().add(Main.class.getResource("styles.css").toExternalForm());
        Swatch.RED.assignTo(SCENE);
        ((Stage) SCENE.getWindow()).getIcons().add(new Image(Main.class.getResourceAsStream("/icon.png")));

        if (JavaFXPlatform.isDesktop()) {
            if (System.getProperty("os.arch").toUpperCase().contains("ARM")) {
                ((Stage) SCENE.getWindow()).setFullScreen(true);
                ((Stage) SCENE.getWindow()).setFullScreenExitHint("");
            } else {
                Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
                (SCENE.getWindow()).setWidth(bounds.getWidth());
                (SCENE.getWindow()).setHeight(bounds.getHeight());
            }
        }
    }

    @Override public void stop() {
        System.exit(0);
    }
}
