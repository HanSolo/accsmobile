/*
 * Copyright (c) 2016 by Gerrit Grunwald
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.hansolo.accs;

import com.gluonhq.charm.down.common.PlatformFactory;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import eu.hansolo.accs.transitions.SlideInRightTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;

import static eu.hansolo.accs.Common.PROPERTIES_FILE_NAME;


/**
 * Created by hansolo on 15.06.16.
 */
public class ConfigView extends View {
    private File       localStoragePath;
    private Properties properties;
    private TextField  urlField;
    private TextField  nameField;
    private AnchorPane configPane;


    // ******************** Constructors **************************************
    public ConfigView(final String NAME) {
        super(NAME);

        try {
            localStoragePath = PlatformFactory.getPlatform().getPrivateStorage();
        } catch (IOException e) {
            String tmp = System.getProperty("java.io.tmpdir");
            localStoragePath = new File(tmp);
        }
        properties = createProperties();
        retrieveConfig();

        initGraphics();
        registerListeners();

        setBackground(new Background(new BackgroundFill(Main.BACKGROUND_COLOR.brighter(), CornerRadii.EMPTY, Insets.EMPTY)));
        setCenter(configPane);
        setShowTransitionFactory(SlideInRightTransition::new);
    }

    @Override protected void updateAppBar(AppBar appBar) {
        appBar.setTitleText("ShareLoc Config");
        appBar.getActionItems().add(MaterialDesignIcon.ARROW_BACK.button(e -> {
            saveConfig();
            getApplication().switchView(Main.MAIN_VIEW);
        }));
    }


    // ******************** Initialization ************************************
    private void initGraphics() {
        Label urlLabel = new Label("URL");
        urlField = new TextField(properties.getProperty("url"));
        urlField.setPromptText("http://your.server/base/route");

        Label nameLabel = new Label("YOUR NAME");
        nameField = new TextField(properties.getProperty("name"));
        nameField.setPromptText("your.name@mailserver.domain");

        GridPane gridPane = new GridPane();
        gridPane.setHgap(5);
        gridPane.setVgap(5);
        gridPane.setPadding(new Insets(10));

        gridPane.add(urlLabel, 0, 0);
        gridPane.add(urlField, 1, 0);

        GridPane.setHgrow(urlField, Priority.ALWAYS);

        gridPane.add(nameLabel, 0, 1);
        gridPane.add(nameField, 1, 1);

        GridPane.setHgrow(nameField, Priority.ALWAYS);

        gridPane.setAlignment(Pos.CENTER);


        setFieldsFromProperties();

        AnchorPane.setTopAnchor(gridPane, 0d);
        AnchorPane.setRightAnchor(gridPane, 0d);
        AnchorPane.setLeftAnchor(gridPane, 0d);

        configPane = new AnchorPane(gridPane);
        configPane.setPadding(new Insets(10));
        configPane.setBackground(new Background(new BackgroundFill(Main.BACKGROUND_COLOR.brighter(), CornerRadii.EMPTY, Insets.EMPTY)));
    }

    private void registerListeners() {
        urlField.focusedProperty().addListener(o -> { if (!urlField.isFocused()) saveConfig(); });
        nameField.focusedProperty().addListener(o -> { if (!nameField.isFocused()) saveConfig(); });
    }


    // ******************** Properties/Config *********************************
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
    private void saveConfig() {
        if (null == properties) properties = new Properties();
        try {
            properties.setProperty("url", urlField.getText());
            properties.setProperty("name", nameField.getText());
            File file = new File(localStoragePath, PROPERTIES_FILE_NAME);
            properties.store(new FileWriter(file), PROPERTIES_FILE_NAME);
        } catch (IOException ex) {}
    }
    private Properties createProperties() {
        Properties p = new Properties();
        p.setProperty("url", "");
        p.setProperty("name", Common.getUniqueId());
        return p;
    }
    private void setFieldsFromProperties() {
        urlField.setText(properties.getProperty("url"));
        nameField.setText(properties.getProperty("name"));
    }
}
