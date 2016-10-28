/*******************************************************************************
 * Copyright (c) 2010-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal;

import static org.csstudio.javafx.rtplot.Activator.logger;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import org.csstudio.javafx.rtplot.Trace;
import org.csstudio.javafx.rtplot.YAxis;
import org.csstudio.javafx.rtplot.data.PlotDataItem;
import org.csstudio.javafx.rtplot.internal.util.GraphicsUtils;
import org.csstudio.javafx.rtplot.internal.util.IntList;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Paint;

/** A 'Y' or 'vertical' axis.
 *  <p>
 *  The plot maintains one or more Y axes.
 *  Each trace to plot needs to be assigned to a Y axis.
 *
 *  @param <XTYPE> Data type of the {@link PlotDataItem}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class YAxisImpl<XTYPE extends Comparable<XTYPE>> extends NumericAxis implements YAxis<XTYPE>
{
    /** How to label the axis */
    final private AxisLabelProvider<XTYPE> label_provider;

    /** Computed in getPixelWidth:
     *  Location of labels, and Y-separation between labels,
     *  used to show SEPARATOR.
     *
     *  Calls to getPixelWidth and paint will come from the same
     *  thread which updates the plot.
     *
     *  Number of label_provider entries should match the size of
     *  label_x and label_y entries after they're set in getDesiredPixelSize,
     *  but note that label_provider could change at any time:
     *  getPixelWidth ran with N labels,
     *  labels change, requesting new layout and thus call to getDesiredPixelSize,
     *  but paint() is called before that happened.
     */
    final private IntList label_x = new IntList(2), label_y = new IntList(2);
    private int label_y_separation;

    /** Show on right side? */
    private volatile boolean is_right = false;

    /** Traces on this axis.
     *
     *  <p>{@link CopyOnWriteArrayList} adds thread safety.
     *  In addition, SYNC on traces when adding a trace
     *  to avoid duplicates
     */
    final private List<TraceImpl<XTYPE>> traces = new CopyOnWriteArrayList<>();

    /** Construct a new Y axis.
     *  <p>
     *  Note that end users will typically <b>not</b> create new Y axes,
     *  but instead ask the <code>Chart</code> for a new or existing axis.
     *  <p>
     *  @param name The axis name.
     *  @param listener Listener.
     */
    public YAxisImpl(final String name, final PlotPartListener listener)
    {
        super(name, listener,
                false,      // vertical
                0.0, 10.0); // Initial range
        label_provider = new AxisLabelProvider<XTYPE>(this);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isUsingAxisName()
    {
        return label_provider.isUsingAxisName();
    }

    /** {@inheritDoc} */
    @Override
    public void useAxisName(final boolean use_axis_name)
    {
        if (label_provider.isUsingAxisName() == use_axis_name)
            return;
        label_provider.useAxisName(use_axis_name);
        requestLayout();
        requestRefresh();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isUsingTraceNames()
    {
        return label_provider.isUsingTraceNames();
    }

    /** {@inheritDoc} */
    @Override
    public void useTraceNames(final boolean use_trace_names)
    {
        if (label_provider.isUsingTraceNames() == use_trace_names)
            return;
        label_provider.useTraceNames(use_trace_names);
        requestLayout();
        requestRefresh();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isOnRight()
    {
        return is_right;
    }

    /** {@inheritDoc} */
    @Override
    public void setOnRight(final boolean right)
    {
        if (is_right == right)
            return;
        is_right = right;
        requestLayout();
        requestRefresh();
    }

    /** Add trace to axis
     *  @param trace {@link Trace}
     *  @throws IllegalArgumentException if trace already on axis
     */
    void addTrace(final TraceImpl<XTYPE> trace)
    {
        Objects.requireNonNull(trace);
        // CopyOnWriteArrayList is thread-safe, but race between 2 threads
        // could still end up with duplicate.
        // Could synchronized here, but then FindBugs complains.
        traces.add(trace);
        // So checking afterwards if there are more than the one expected entries
        if (traces.stream().filter((existing) -> existing == trace).count() > 1)
            throw new IllegalArgumentException("Trace " + trace.getName() + " already on Y Axis " + getName());
        requestLayout();
    }

    /** Remove a trace from the axis.
     *  <p>
     *  Not meant for end users, the trace is supposed to remove itself.
     */
    public void removeTrace(final Trace<XTYPE> trace)
    {
        Objects.requireNonNull(trace);
        if (! traces.remove(trace))
            throw new Error("Internal YAxis error. Axis " + getName() + " does not hold trace " + trace.getName());
        requestLayout();
    }

    /** 'Current' list of traces as thread-safe, read-only iterable,
     *  a snapshot of the underlying {@link CopyOnWriteArrayList}
     *
     * @return Current list of traces for an axis.
     */
    Iterable<TraceImpl<XTYPE>> getTraces()
    {
        return traces;
    }

    /** {@inheritDoc */
    @Override
    public int getDesiredPixelSize(final Rectangle region, final Graphics2D gc)
    {
        logger.log(Level.FINE, "YAxis({0}) layout for {1}", new Object[] { getName(),  region });

        if (! isVisible())
            return 0;

        gc.setFont(label_font);
        FontMetrics metrics = gc.getFontMetrics();
        final int x_sep = metrics.getHeight();
        // Start layout of labels at x=0, 'left',
        // to determine how many fit into one line.
        // Later update these relative x positions based on 'left' or 'right' axis.
        int x = 0;
        int lines = 0;

        // Compute layout of labels
        label_provider.start();
        label_x.clear();
        label_y.clear();
        final IntList label_length = new IntList(2);
        while (label_provider.hasNext())
        {
            label_y_separation = metrics.stringWidth(label_provider.getSeparator());
            label_length.add(metrics.stringWidth(label_provider.getLabel()));
        }
        while (label_provider.hasNext());

        // Compute location of each label
        int next = 0;
        final int N = label_length.size();
        while (next < N)
        {   // Determine how many can fit on one line
            int many = 0;
            int height = 0;
            for (int i=next; i<N; ++i)
                if (height + label_length.get(i) < region.height)
                {
                    ++many;
                    height += label_length.get(i);
                    if (i > 0)
                        height += label_y_separation;
                }
                else
                    break;
            // Can't fit any? Show one, will be clipped
            if (many == 0)
            {
                many = 1;
                height = region.height;
            }
            // Draw one line
            int y = region.y + (region.height+height)/2;
            for (int i=next; i<next+many; ++i)
            {
                y -= label_length.get(i);
                label_x.add(x);
                label_y.add(y);
                if (i < N-1)
                    y -= label_y_separation;
            }
            x += x_sep;
            next += many;
            ++lines;
        }

        final int x_correction = is_right ? region.x + region.width - lines*x_sep : region.x;
        for (int i=label_x.size()-1; i>=0; --i)
            label_x.set(i, label_x.get(i) + x_correction);

        gc.setFont(scale_font);
        metrics = gc.getFontMetrics();
        final int scale_size = metrics.getHeight();

        // Width of labels, width of (rotated) axis text, tick markers.
        return lines * x_sep + scale_size + TICK_LENGTH;
    }

    /** {@inheritDoc} */
    @Override
    public int[] getPixelGaps(final Graphics2D gc)
    {
        if (! isVisible())
            return super.getPixelGaps(gc);

        gc.setFont(scale_font);
        final FontMetrics metrics = gc.getFontMetrics();

        // Measure first tick
        double tick = ticks.getStart();
        String mark = ticks.format(tick);
        int low = metrics.stringWidth(mark);
        // System.out.println("Initial tick: " + mark + " (" + low + ")");

        // Get last tick
        final double low_value = range.getLow();
        final double high_value = range.getHigh();
        final boolean normal = low_value <= high_value;
        double last = tick;
        while ((normal ? tick <= high_value : tick >= high_value)  &&  Double.isFinite(tick))
        {
            last = tick;
            tick = ticks.getNext(tick);
        }
        // Measure last tick
        mark = ticks.format(last);
        final int high = metrics.stringWidth(mark);
        // System.out.println("Final tick: " + mark + " (" + high + ")");

        return new int[] { low / 2, high / 2 };
    }

    /** {@inheritDoc} */
    @Override
    public void paint(final Graphics2D gc, final Rectangle plot_bounds)
    {
        if (! isVisible())
            return;

        super.paint(gc);
        final Rectangle region = getBounds();

        final Stroke old_width = gc.getStroke();
        final Color old_bg = gc.getBackground();
        final Color old_fg = gc.getColor();
        gc.setColor(GraphicsUtils.convert(getColor()));
        gc.setFont(scale_font);

        // Simple line for the axis
        final int line_x, tick_x, minor_x;
        if (is_right)
        {
            line_x = region.x;
            tick_x = region.x + TICK_LENGTH;
            minor_x = region.x + MINOR_TICK_LENGTH;
        }
        else
        {
            line_x = region.x + region.width-1;
            tick_x = region.x + region.width-1 - TICK_LENGTH;
            minor_x = region.x + region.width-1 - MINOR_TICK_LENGTH;
        }
        gc.drawLine(line_x, region.y, line_x, region.y + region.height-1);
        computeTicks(gc);

        final double low_value = range.getLow();
        final double high_value = range.getHigh();
        final boolean normal = low_value <= high_value;
        final int minor_ticks = ticks.getMinorTicks();
        double tick = ticks.getStart();
        double prev = ticks.getPrevious(tick);
        for (/**/;
                (normal ? tick <= high_value : tick >= high_value)  &&  Double.isFinite(tick);
                tick = ticks.getNext(tick))
        {
            // Minor ticks?
            for (int i=1; i<minor_ticks; ++i)
            {
                final double minor = prev + ((tick - prev)*i)/minor_ticks;
                if (normal ? minor < low_value : minor > low_value)
                    continue;
                final int y = getScreenCoord(minor);
                gc.drawLine(minor_x, y, line_x, y);
            }

            // Major tick marks
            gc.setStroke(new BasicStroke(TICK_WIDTH));
            int y = getScreenCoord(tick);
            gc.drawLine(tick_x+1, y, line_x-1, y);

            // Grid line
            if (show_grid)
            {
                gc.setStroke(new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 1, new float[] { 5 }, 0));
                gc.drawLine(plot_bounds.x, y, plot_bounds.x + plot_bounds.width-1, y);
            }
            gc.setStroke(old_width);

            // Tick Label
            drawTickLabel(gc, tick);

            prev = tick;
        }
        // Minor ticks after last major tick?
        if (Double.isFinite(tick))
            for (int i=1; i<minor_ticks; ++i)
            {
                final double minor = prev + ((tick - prev)*i)/minor_ticks;
                if (normal ? minor > high_value : minor < high_value)
                    break;
                final int y = getScreenCoord(minor);
                gc.drawLine(minor_x, y, line_x, y);
            }

        gc.setColor(old_fg);
        gc.setBackground(old_bg);

        gc.setFont(label_font);
        paintLabels(gc);
    }

    protected void paintLabels(final Graphics2D gc)
    {
        if (label_y == null)
            return;
        final Color old_fg = gc.getColor();
        label_provider.start();
        int i = 0;
        while (label_provider.hasNext()  &&  i < label_x.size())
        {   // Draw labels at pre-computed locations
            if (i > 0)
                GraphicsUtils.drawVerticalText(gc, label_x.get(i-1), label_y.get(i-1) - label_y_separation,
                        label_provider.getSeparator(), !is_right);
            gc.setColor(GraphicsUtils.convert(label_provider.getColor()));
            GraphicsUtils.drawVerticalText(gc,
                    label_x.get(i), label_y.get(i), label_provider.getLabel(), !is_right);
            gc.setColor(old_fg);
            ++i;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void drawTickLabel(final Graphics2D gc, final Double tick)
    {
        final Rectangle region = getBounds();
        final String mark = ticks.format(tick);
        final FontMetrics metrics = gc.getFontMetrics();
        final int mark_height = metrics.stringWidth(mark);
        final int mark_width = metrics.getHeight();
        final int x = is_right ? region.x + TICK_LENGTH : region.x + region.width - TICK_LENGTH - mark_width;
        final int y = getScreenCoord(tick)  - mark_height/2;
        // gc.drawRect(x,  y, mark_width, mark_height); // Debug outline of tick label
        GraphicsUtils.drawVerticalText(gc, x, y, mark, !is_right);
    }

    /** {@inheritDoc} */
    @Override
    public void drawFloatingTickLabel(final GraphicsContext gc, final Double tick)
    {
        final Rectangle region = getBounds();
        final String mark = ticks.formatDetailed(tick);
        gc.setFont(GraphicsUtils.convert(scale_font));
        final Rectangle metrics = GraphicsUtils.measureText(gc, mark);
        final int mark_height = metrics.width;
        final int mark_width = metrics.height;
        final int y = getScreenCoord(tick);

        // 'Tick mark' for floating label
        final int tx;
        if (is_right)
        {
            gc.strokeLine(region.x, y, region.x + TICK_LENGTH, y);
            tx = region.x + TICK_LENGTH;
        }
        else
        {
            gc.strokeLine(region.x + region.width - TICK_LENGTH, y, region.x + region.width, y);
            tx = region.x + region.width - TICK_LENGTH - mark_width;
        }

        // Box around label
        final int ty = y - mark_height/2;
        final Paint orig_fill = gc.getFill();
        gc.setFill(javafx.scene.paint.Color.WHITE);
        gc.fillRect(tx-BORDER, ty-BORDER, mark_width+2*BORDER, mark_height+2*BORDER);
        gc.setFill(orig_fill);
        gc.strokeRect(tx-BORDER, ty-BORDER, mark_width+2*BORDER, mark_height+2*BORDER);

        // Label
        if (is_right)
        {
            gc.translate(tx, ty);
            gc.rotate(90.0);
            gc.fillText(mark, 0, 0);
            gc.rotate(-90.0);
            gc.translate(-tx, -ty);
        }
        else
        {
            gc.translate(tx, ty);
            gc.rotate(-90);
            gc.fillText(mark, -mark_height, mark_width);
            gc.rotate(90);
            gc.translate(-tx, -ty);
        }
    }
}
