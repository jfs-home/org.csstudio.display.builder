/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.javafx;

import javafx.geometry.Bounds;
import javafx.geometry.Dimension2D;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/** Helper for text
 *  @author Kay Kasemir
 */
public class TextUtils
{
    private final static Text helper = new Text();

    /** Compute the preferred size for a text
     *
     *  <p>Must be called on the UI thread,
     *  because it's using one shared text helper.
     *
     *  @param font Font
     *  @param text Text
     *  @return Width, height
     */
    public static Dimension2D computeTextSize(final Font font, final String text)
    {
        // com.sun.javafx.scene.control.skin.Utils contains related code,
        // but is private
        helper.setFont(font);
        // With default line spacing of 0.0,
        // height of multi-line text is too small...
        helper.setLineSpacing(1.0);
        helper.setText(text);

        final Bounds measure = helper.getLayoutBounds();
        return new Dimension2D(measure.getWidth(), measure.getHeight());
    }
}
