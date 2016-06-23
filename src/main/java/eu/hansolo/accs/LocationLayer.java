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

import com.gluonhq.maps.MapLayer;
import com.gluonhq.maps.MapPoint;
import com.gluonhq.maps.MapPoint.MapPointEventListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.shape.Circle;
import javafx.util.Pair;


/**
 * Created by hansolo on 16.06.16.
 */
public class LocationLayer extends MapLayer {
    private final ObservableList<Pair<JMapPoint, Circle>> POINTS;
    private       MapPointEventListener                   pointEventListener;

    public LocationLayer() {
        POINTS             = FXCollections.observableArrayList();
        pointEventListener = mapPointEvent -> layoutLayer();
    }

    public void addPoint(final JMapPoint POINT, final Circle CIRCLE) {
        POINTS.add(new Pair(POINT, CIRCLE));
        getChildren().add(CIRCLE);
        markDirty();
        POINT.addMapPointEventListener(pointEventListener);
    }
    public void removePoint(final JMapPoint POINT) {
        for (int i = 0 ; i < POINTS.size() ; i++) {
            if (POINTS.get(i).getKey().equals(POINT)) {
                POINT.removeMapPointEventListener(pointEventListener);
                POINTS.remove(i);
                break;
            }
        }
    }
    public ObservableList<Pair<JMapPoint, Circle>> getPoints() { return POINTS; }
    public void clearPoints() {
        for (Pair<JMapPoint, Circle> point : POINTS) { point.getKey().removeMapPointEventListener(pointEventListener); }
        POINTS.clear();
        getChildren().clear();
        markDirty();
    }

    @Override protected void layoutLayer() {
        for (Pair<JMapPoint, Circle> candidate : POINTS) {
            JMapPoint point    = candidate.getKey();
            Circle   circle   = candidate.getValue();
            Point2D  mapPoint = baseMap.getMapPoint(point.getLatitude(), point.getLongitude());
            circle.setVisible(true);
            circle.setTranslateX(mapPoint.getX() - (circle.getLayoutBounds().getWidth() * 0.5));
            circle.setTranslateY(mapPoint.getY() - (circle.getLayoutBounds().getHeight() * 0.5));
        }
    }
}
