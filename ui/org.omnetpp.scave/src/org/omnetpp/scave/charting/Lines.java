/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.scave.charting;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.omnetpp.common.Debug;
import org.omnetpp.common.canvas.ICoordsMapping;
import org.omnetpp.common.canvas.RectangularArea;
import org.omnetpp.scave.charting.LinePlot.LineProperties;
import org.omnetpp.scave.charting.dataset.IXYDataset;
import org.omnetpp.scave.charting.plotter.IPlotSymbol;
import org.omnetpp.scave.charting.plotter.ILinePlotter;

/**
 * The content area of a line plot.
 */
class Lines implements ILinePlot {

    private static final boolean debug = false;

    private LinePlot parent;
    private Rectangle rect = new Rectangle(0,0,1,1);

    public Lines(LinePlot parent) {
        this.parent = parent;
    }

    public IXYDataset getDataset() {
        return parent.getDataset();
    }

    public double transformX(double x) {
        return parent.transformX(x);
    }

    public double transformY(double y) {
        return parent.transformY(y);
    }

    public double inverseTransformX(double x) {
        return parent.inverseTransformX(x);
    }

    public double inverseTransformY(double y) {
        return parent.inverseTransformY(y);
    }


    public Rectangle getPlotRectangle() {
        return rect;
    }

    protected RectangularArea calculatePlotArea() {

        RectangularArea area = new RectangularArea(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
                                                    Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);

        IXYDataset dataset = getDataset();

        if (dataset!=null && dataset.getSeriesCount() > 0) {

            area.minX = dataset.getMinX();
            area.minY = dataset.getMinY();
            area.maxX = dataset.getMaxX();
            area.maxY = dataset.getMaxY();

            // try to find the area by transforming the dataset range
            area = parent.transformArea(area);

            if (!area.isFinite()) {
                // some bounds are outside of the transformation domain,
                // so calculate the bounding box by transforming the points and
                // omitting infinite values
                long startTime = System.currentTimeMillis();
                long numOfPoints = 0;

                area.minX = Double.POSITIVE_INFINITY;
                area.minY = Double.POSITIVE_INFINITY;
                area.maxX = Double.NEGATIVE_INFINITY;
                area.maxY = Double.NEGATIVE_INFINITY;

                for (int series = 0; series < dataset.getSeriesCount(); series++) {
                    int n = dataset.getItemCount(series);
                    if (n > 0) {
                        // X must be increasing
                        for (int i = 0; i < n; i++) {
                            double x = parent.transformX(dataset.getX(series,i));
                            if (!Double.isNaN(x) && !Double.isInfinite(x))
                            {
                                area.minX = Math.min(area.minX, x);
                                break;
                            }
                        }
                        for (int i = n-1; i >= 0; i--) {
                            double x = parent.transformX(dataset.getX(series,i));
                            if (!Double.isNaN(x) && !Double.isInfinite(x))
                            {
                                area.maxX = Math.max(area.maxX, x);
                                break;
                            }
                        }
                        for (int i = 0; i < n; i++) {
                            double y = parent.transformY(dataset.getY(series, i));
                            if (!Double.isNaN(y) && !Double.isInfinite(y)) {
                                area.minY = Math.min(area.minY, y);
                                area.maxY = Math.max(area.maxY, y);
                            }
                        }

                        numOfPoints += n;
                    }
                }
                if (debug) {
                    long duration = System.currentTimeMillis() - startTime;
                    Debug.format("calculatePlotArea(): %d ms (%d points)%n", duration, numOfPoints);
                }
            }
        }

        if (area.minX > area.maxX) {
            area.minX = 0.0;
            area.maxX = 1.0;
        }
        if (area.minY > area.maxY) {
            area.minY = 0.0;
            area.maxY = 1.0;
        }

        area.minX = (area.minX>=0 ? 0 : area.minX-area.width()/80);
        area.maxX = (area.maxX<=0 ? 0 : area.maxX+area.width()/80);
        area.minY = (area.minY>=0 ? 0 : area.minY-area.height()/3);
        area.maxY = (area.maxY<=0 ? 0 : area.maxY+area.height()/3);

        return area;
    }

    protected Rectangle layout(Graphics graphics, Rectangle area) {
        this.rect = area.getCopy();
        return area;
    }

    protected boolean draw(Graphics graphics, ICoordsMapping coordsMapping, int totalTimeLimitMillis, int perLineTimeLimitMillis) {
        if (getDataset() != null) {
            IXYDataset dataset = getDataset();

            long startTime = System.currentTimeMillis();
            boolean ok = true;

            for (int series = 0; series < dataset.getSeriesCount(); series++) {
                LineProperties props = parent.getLineProperties(series);
                if (props.getEffectiveDisplayLine()) {

                    ILinePlotter plotter = props.getLinePlotter();
                    IPlotSymbol symbol = props.getSymbolPlotter();
                    Color color = props.getEffectiveLineColor();
                    parent.resetDrawingStylesAndColors(graphics);
                    graphics.setAntialias(parent.antialias ? SWT.ON : SWT.OFF);
                    graphics.setForegroundColor(color);
                    graphics.setBackgroundColor(color);
                    graphics.setLineStyle(props.getEffectiveLineStyle().getDraw2DConstant());
                    graphics.setLineWidthFloat(props.getEffectiveLineWidth());

                    int remainingTime = totalTimeLimitMillis - (int)(System.currentTimeMillis() - startTime);
                    int lineTimeout = Math.min(Math.max(100, remainingTime), perLineTimeLimitMillis); // give it at least 100ms, even if time is over

                    boolean lineOK = plotter.plot(this, series, graphics, coordsMapping, symbol, lineTimeout);
                    ok = ok && lineOK; // do NOT merge with previous line! shortcutting would prevent lines after 1st timeout to be drawn

                    // if drawing is taking too long, display busy cursor
                    if (System.currentTimeMillis() - startTime > 1000) {
                        Cursor cursor = Display.getCurrent().getSystemCursor(SWT.CURSOR_WAIT);
                        parent.getShell().setCursor(cursor);
                        parent.setCursor(null); // crosshair cursor would override shell's busy cursor
                    }
                }
            }
            parent.getShell().setCursor(null);
            if (debug) Debug.println("plotting: "+(System.currentTimeMillis()-startTime)+" ms");
            return ok;
        }
        return true;
    }
}
