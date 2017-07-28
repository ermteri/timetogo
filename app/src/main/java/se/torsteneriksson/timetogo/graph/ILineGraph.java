package se.torsteneriksson.timetogo.graph;

import com.google.maps.android.geometry.Point;

/**
 * Created by torsten on 3/23/2017.
 */

public interface ILineGraph {
    public void setMinY(double minY);
    public void setMaxY(double maxY);
    public void setMinX(double minX);
    public void setMaxX(double maxX);
    public void addSerie(Point[] data);
    public void updateSerie(Point[] data);
    public void deleteSerie();
    public void setSerieColor(int color);

}
