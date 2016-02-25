/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.builder.editor.undo;

import org.csstudio.display.builder.editor.Messages;
import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.util.undo.UndoableAction;

/** Action to add widget
 *  @author Kay Kasemir
 */
public class AddWidgetAction extends UndoableAction
{
    private final Widget container;
    private final Widget widget;

    public AddWidgetAction(final Widget container, final Widget widget)
    {
        super(Messages.AddWidget);
        this.container = container;
        this.widget = widget;
    }

    @Override
    public void run()
    {
        ChildrenProperty.getChildren(container).addChild(widget);
    }

    @Override
    public void undo()
    {
        ChildrenProperty.getChildren(container).removeChild(widget);
    }
}
