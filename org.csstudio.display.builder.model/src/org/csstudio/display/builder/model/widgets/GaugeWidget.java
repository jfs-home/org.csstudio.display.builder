/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * Copyright (C) 2016 European Spallation Source ERIC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.csstudio.display.builder.model.widgets;


import java.util.List;

import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.properties.EnumWidgetProperty;


/**
 * Widget displaying and editing a numeric PV value.
 *
 * @author Claudio Rosati, European Spallation Source ERIC
 * @version 1.0.0 25 Jan 2017
 */
public class GaugeWidget extends BaseGaugeWidget {

    public static final WidgetDescriptor WIDGET_DESCRIPTOR = new WidgetDescriptor(
        "gauge",
        WidgetCategory.CONTROL,
        "Gauge",
        "platform:/plugin/org.csstudio.display.builder.model/icons/gauge.png",
        "Gauge that can read/write a numeric PV"
    ) {
        @Override
        public Widget createWidget ( ) {
            return new GaugeWidget();
        }
    };

    /**
     * This enumeration is used to reduce the choices from the original enumeration.
     *
     * @author claudiorosati, European Spallation Source ERIC
     * @version 1.0.0 25 Jan 2017
     */
    public enum Skin {
        DASHBOARD,
        DIGITAL,
        FLAT,
        MODERN,
        SIMPLE_DIGITAL,
        SIMPLE_SECTION,
        SLIM
    }

    public static final WidgetPropertyDescriptor<Skin> propSkin = new WidgetPropertyDescriptor<Skin>(WidgetPropertyCategory.WIDGET, "skin", Messages.WidgetProperties_Skin) {
        @Override
        public EnumWidgetProperty<Skin> createProperty ( Widget widget, Skin defaultValue ) {
            return new EnumWidgetProperty<>(this, widget, defaultValue);
        }
    };

    private volatile WidgetProperty<Skin> skin;

    public GaugeWidget ( ) {
        super(WIDGET_DESCRIPTOR.getType(), 160, 160);
    }

    public WidgetProperty<Skin> propSkin ( ) {
        return skin;
    }

    @Override
    protected void defineProperties ( final List<WidgetProperty<?>> properties ) {

        super.defineProperties(properties);

        properties.add(skin = propSkin.createProperty(this, Skin.SIMPLE_SECTION));

    }

}
