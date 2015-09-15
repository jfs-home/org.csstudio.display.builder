/*******************************************************************************
 * Copyright (c) 2015 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal;

import org.csstudio.javafx.rtplot.data.PlotDataItem;

/** Action to hide/show legend.
 *
 *  @param <XTYPE> Data type used for the {@link PlotDataItem}
 *  @author Kunal Shroff - Original author
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ToggleLegendAction<XTYPE extends Comparable<XTYPE>> // extends Action
{
    // TODO Implement for TBD context menu

//    final private RTPlot<XTYPE> plot;
//
//    public ToggleLegendAction(final RTPlot<XTYPE> plot, final boolean is_visible)
//    {
//        super(is_visible ? Messages.Legend_Hide : Messages.Legend_Show,
//              Activator.getIcon("legend"));
//        this.plot = plot;
//    }
//
//    public void updateText()
//    {
//        setText(plot.isLegendVisible() ? Messages.Legend_Hide : Messages.Legend_Show);
//    }
//
//    @Override
//    public void run()
//    {
//        plot.showLegend(! plot.isLegendVisible());
//    }
}
