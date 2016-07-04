package eu.hansolo.accs;

import com.gluonhq.maps.MapLayer;
import com.gluonhq.maps.MapPointEventListener;
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
                markDirty();
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
            JMapPoint point   = candidate.getKey();
            Circle   circle   = candidate.getValue();
            Point2D  mapPoint = baseMap.getMapPoint(point.getLatitude(), point.getLongitude());
            circle.setVisible(true);
            circle.setTranslateX(mapPoint.getX() - (circle.getLayoutBounds().getWidth() * 0.5));
            circle.setTranslateY(mapPoint.getY() - (circle.getLayoutBounds().getHeight() * 0.5));
        }
    }
}
