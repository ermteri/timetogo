package se.torsteneriksson.timetogo.graph;

import android.app.Activity;
import android.content.Context;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.maps.android.geometry.Point;
import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import se.torsteneriksson.timetogo.R;

import static se.torsteneriksson.timetogo.MainActivity.INTERVAL;

/**
 * Created by torsten on 3/23/2017.
 * Implements ILineGraph using the jjoe64 library
 */

public class LineGraphJJOE64 implements ILineGraph {

    private GraphView mGraph;
    private LineGraphSeries<DataPoint> mSeries;
    Context mContext;


    public LineGraphJJOE64(Context context, FrameLayout container) {

        mGraph = new GraphView(context);
        mGraph.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        container.addView(mGraph);

      // mGraph = (GraphView) a.findViewById(view);
        mContext = context;
        mGraph.getViewport().setXAxisBoundsManual(true);
        setLabelRenderer();
    }

    @Override
    public void setMinY(double minY) {
        mGraph.getViewport().setMinY(minY);
    }

    @Override
    public void setMaxY(double maxY) {
        mGraph.getViewport().setMaxY(maxY);
    }

    @Override
    public void setMinX(double minX) {
        mGraph.getViewport().setMinX(minX);
    }

    @Override
    public void setMaxX(double maxX) {
        mGraph.getViewport().setMaxX(maxX);
    }

    @Override
    public void addSerie(Point[] data) {
        mSeries = getDataPointFromPoint(data);
        mGraph.addSeries(mSeries);
    }

    @Override
    public void updateSerie(Point[] data) {
        mGraph.removeAllSeries();
        addSerie(data);
        //mGraph.addSeries(getDataPointFromPoint(data));
    }

    @Override
    public void deleteSerie() {
        mGraph.removeAllSeries();
    }

    @Override
    public void setSerieColor(int color) {
        if(mSeries!= null)
            mSeries.setColor(color);
    }

    private void setLabelRenderer(){
        mGraph.getViewport().setYAxisBoundsManual(true);
        mGraph.getViewport().setXAxisBoundsManual(true);
        mGraph.getGridLabelRenderer().setNumVerticalLabels(5);
        mGraph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {

                if (!isValueX) {
                    //return super.formatLabel(value, isValueX);
                    int val = (int) value;
                    return (String.valueOf(val));
                } else {
                    if (value != 0) {
                        return super.formatLabel(value * (-1.0) * INTERVAL / 60000, isValueX);
                    } else {
                        //return super.formatLabel(value,isValueX);
                        return mContext.getString(R.string.now);
                    }
                }
            }
        });
    }

    private LineGraphSeries<DataPoint> getDataPointFromPoint(Point[] p) {
        DataPoint[] dp = new DataPoint[p.length];
        for (int i = 0;i<p.length;i++){
            dp[i] = new DataPoint(p[i].x,p[i].y);
        }
        return new LineGraphSeries<>(dp);
    }
}
