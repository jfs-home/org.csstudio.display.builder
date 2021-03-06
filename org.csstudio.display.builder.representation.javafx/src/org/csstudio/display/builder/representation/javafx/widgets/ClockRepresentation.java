/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * Copyright (C) 2016 European Spallation Source ERIC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.csstudio.display.builder.representation.javafx.widgets;


import java.util.Objects;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.widgets.ClockWidget;
import org.csstudio.display.builder.representation.javafx.JFXUtil;

import eu.hansolo.medusa.Clock;
import eu.hansolo.medusa.Clock.ClockSkinType;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;


/**
 * @author Claudio Rosati, European Spallation Source ERIC
 * @version 1.0.0 18 Jan 2017
 */
@SuppressWarnings("nls")
public class ClockRepresentation extends BaseClockRepresentation<ClockWidget> {

    private final DirtyFlag dirtyBehavior = new DirtyFlag();
    private final DirtyFlag dirtyLook     = new DirtyFlag();

    private final UntypedWidgetPropertyListener behaviorChangedListener = this::behaviorChanged;
    private final UntypedWidgetPropertyListener lookChangedListener     = this::lookChanged;

    @Override
    public void updateChanges ( ) {

        super.updateChanges();

        Object value;

        if ( dirtyBehavior.checkAndClear() ) {

            value = model_widget.propDiscreteHours().getValue();

            if ( !Objects.equals(value, jfx_node.isDiscreteHours()) ) {
                jfx_node.setDiscreteHours((boolean) value);
            }

            value = model_widget.propDiscreteMinutes().getValue();

            if ( !Objects.equals(value, jfx_node.isDiscreteMinutes()) ) {
                jfx_node.setDiscreteMinutes((boolean) value);
            }

            value = model_widget.propDiscreteSeconds().getValue();

            if ( !Objects.equals(value, jfx_node.isDiscreteSeconds()) ) {
                jfx_node.setDiscreteSeconds((boolean) value);
            }

        }

        if ( dirtyLook.checkAndClear() ) {

            value = ClockSkinType.valueOf(model_widget.propSkin().getValue().name());

            if ( !Objects.equals(value, jfx_node.getSkinType()) ) {
                jfx_node.setSkinType((ClockSkinType) value);
            }

            value = JFXUtil.convert(model_widget.propBackgroundColor().getValue());

            if ( model_widget.propTransparent().getValue() ) {
                value = ((Color) value).deriveColor(0, 1, 1, 0);
            }

            if ( !Objects.equals(value, jfx_node.getBackgroundPaint()) ) {
                jfx_node.setBackgroundPaint((Paint) value);
            }

            value = JFXUtil.convert(model_widget.propDateColor().getValue());

            if ( !Objects.equals(value, jfx_node.getDateColor()) ) {
                jfx_node.setDateColor((Color) value);
            }

            value = JFXUtil.convert(model_widget.propHourColor().getValue());

            if ( !Objects.equals(value, jfx_node.getHourColor()) ) {
                jfx_node.setHourColor((Color) value);
            }

            value = JFXUtil.convert(model_widget.propHourTickMarkColor().getValue());

            if ( !Objects.equals(value, jfx_node.getHourTickMarkColor()) ) {
                jfx_node.setHourTickMarkColor((Color) value);
            }

            value = model_widget.propHourTickMarkVisible().getValue();

            if ( !Objects.equals(value, jfx_node.isHourTickMarksVisible()) ) {
                jfx_node.setHourTickMarksVisible((boolean) value);
            }

            value = JFXUtil.convert(model_widget.propKnobColor().getValue());

            if ( !Objects.equals(value, jfx_node.getKnobColor()) ) {
                jfx_node.setKnobColor((Color) value);
            }

            value = JFXUtil.convert(model_widget.propMinuteColor().getValue());

            if ( !Objects.equals(value, jfx_node.getMinuteColor()) ) {
                jfx_node.setMinuteColor((Color) value);
            }

            value = JFXUtil.convert(model_widget.propMinuteTickMarkColor().getValue());

            if ( !Objects.equals(value, jfx_node.getMinuteTickMarkColor()) ) {
                jfx_node.setMinuteTickMarkColor((Color) value);
            }

            value = model_widget.propMinuteTickMarkVisible().getValue();

            if ( !Objects.equals(value, jfx_node.isMinuteTickMarksVisible()) ) {
                jfx_node.setMinuteTickMarksVisible((boolean) value);
            }

            value = JFXUtil.convert(model_widget.propRingColor().getValue());

            if ( !Objects.equals(value, jfx_node.getBorderPaint()) ) {
                jfx_node.setBorderPaint((Paint) value);
            }

            value = model_widget.propRingWidth().getValue();

            if ( !Objects.equals(value, jfx_node.getBorderWidth()) ) {
                jfx_node.setBorderWidth((double) value);
            }

            value = JFXUtil.convert(model_widget.propSecondColor().getValue());

            if ( !Objects.equals(value, jfx_node.getSecondColor()) ) {
                jfx_node.setSecondColor((Color) value);
            }

            value = JFXUtil.convert(model_widget.propTextColor().getValue());

            if ( !Objects.equals(value, jfx_node.getTextColor()) ) {
                jfx_node.setTextColor((Color) value);
            }

            value = model_widget.propTextVisible().getValue();

            if ( !Objects.equals(value, jfx_node.isTextVisible()) ) {
                jfx_node.setTextVisible((boolean) value);
            }

            value = JFXUtil.convert(model_widget.propTickLabelColor().getValue());

            if ( !Objects.equals(value, jfx_node.getTickLabelColor()) ) {
                jfx_node.setTickLabelColor((Color) value);
            }

            value = model_widget.propTickLabelsVisible().getValue();

            if ( !Objects.equals(value, jfx_node.isTickLabelsVisible()) ) {
                jfx_node.setTickLabelsVisible((boolean) value);
            }

            value = JFXUtil.convert(model_widget.propTitleColor().getValue());

            if ( !Objects.equals(value, jfx_node.getTitleColor()) ) {
                jfx_node.setTitleColor((Color) value);
            }

        }

    }

    @Override
    protected Clock createJFXNode ( ) throws Exception {

        Clock.ClockSkinType skinType = ClockSkinType.valueOf(model_widget.propSkin().getValue().name());
        Clock clock = super.createJFXNode(skinType);

        clock.setBackgroundPaint(model_widget.propTransparent().getValue() ? Color.TRANSPARENT : JFXUtil.convert(model_widget.propBackgroundColor().getValue()));
        clock.setBorderPaint(JFXUtil.convert(model_widget.propRingColor().getValue()));
        clock.setBorderWidth(model_widget.propRingWidth().getValue());
        clock.setDateColor(JFXUtil.convert(model_widget.propDateColor().getValue()));
        clock.setDiscreteHours(model_widget.propDiscreteHours().getValue());
        clock.setDiscreteMinutes(model_widget.propDiscreteMinutes().getValue());
        clock.setDiscreteSeconds(model_widget.propDiscreteSeconds().getValue());
        clock.setHourColor(JFXUtil.convert(model_widget.propHourColor().getValue()));
        clock.setHourTickMarkColor(JFXUtil.convert(model_widget.propHourTickMarkColor().getValue()));
        clock.setHourTickMarksVisible(model_widget.propHourTickMarkVisible().getValue());
        clock.setKnobColor(JFXUtil.convert(model_widget.propKnobColor().getValue()));
        clock.setMinuteColor(JFXUtil.convert(model_widget.propMinuteColor().getValue()));
        clock.setMinuteTickMarkColor(JFXUtil.convert(model_widget.propMinuteTickMarkColor().getValue()));
        clock.setMinuteTickMarksVisible(model_widget.propMinuteTickMarkVisible().getValue());
        clock.setSecondColor(JFXUtil.convert(model_widget.propSecondColor().getValue()));
        clock.setTextColor(JFXUtil.convert(model_widget.propTextColor().getValue()));
        clock.setTextVisible(model_widget.propTextVisible().getValue());
        clock.setTickLabelColor(JFXUtil.convert(model_widget.propTickLabelColor().getValue()));
        clock.setTickLabelsVisible(model_widget.propTickLabelsVisible().getValue());
        clock.setTitleColor(JFXUtil.convert(model_widget.propTitleColor().getValue()));

        clock.backgroundPaintProperty().addListener( ( s, o, n ) -> {
            if ( !Objects.equals(n, JFXUtil.convert(model_widget.propBackgroundColor().getValue())) ) {
                model_widget.propBackgroundColor().setValue(JFXUtil.convert((Color) n));
            }
        });
        clock.borderPaintProperty().addListener( ( s, o, n ) -> {
            if ( !Objects.equals(n, JFXUtil.convert(model_widget.propRingColor().getValue())) ) {
                model_widget.propRingColor().setValue(JFXUtil.convert((Color) n));
            }
        });
        clock.borderWidthProperty().addListener( ( s, o, n ) -> {
            if ( !Objects.equals(n, model_widget.propRingWidth().getValue()) ) {
                model_widget.propRingWidth().setValue(n.doubleValue());
            }
        });
        clock.dateColorProperty().addListener( ( s, o, n ) -> {
            if ( !Objects.equals(n, JFXUtil.convert(model_widget.propDateColor().getValue())) ) {
                model_widget.propDateColor().setValue(JFXUtil.convert(n));
            }
        });
        clock.discreteHoursProperty().addListener( ( s, o, n ) -> {
            if ( !Objects.equals(n, model_widget.propDiscreteHours().getValue()) ) {
                model_widget.propDiscreteHours().setValue(n);
            }
        });
        clock.discreteMinutesProperty().addListener( ( s, o, n ) -> {
            if ( !Objects.equals(n, model_widget.propDiscreteMinutes().getValue()) ) {
                model_widget.propDiscreteMinutes().setValue(n);
            }
        });
        clock.discreteSecondsProperty().addListener( ( s, o, n ) -> {
            if ( !Objects.equals(n, model_widget.propDiscreteSeconds().getValue()) ) {
                model_widget.propDiscreteSeconds().setValue(n);
            }
        });
        clock.hourColorProperty().addListener( ( s, o, n ) -> {
            if ( !Objects.equals(n, JFXUtil.convert(model_widget.propHourColor().getValue())) ) {
                model_widget.propHourColor().setValue(JFXUtil.convert(n));
            }
        });
        clock.hourTickMarkColorProperty().addListener( ( s, o, n ) -> {
            if ( !Objects.equals(n, JFXUtil.convert(model_widget.propHourTickMarkColor().getValue())) ) {
                model_widget.propHourTickMarkColor().setValue(JFXUtil.convert(n));
            }
        });
        clock.hourTickMarksVisibleProperty().addListener( ( s, o, n ) -> {
            if ( !Objects.equals(n, model_widget.propHourTickMarkVisible().getValue()) ) {
                model_widget.propHourTickMarkVisible().setValue(n);
            }
        });
        clock.knobColorProperty().addListener( ( s, o, n ) -> {
            if ( !Objects.equals(n, JFXUtil.convert(model_widget.propKnobColor().getValue())) ) {
                model_widget.propKnobColor().setValue(JFXUtil.convert(n));
            }
        });
        clock.minuteColorProperty().addListener( ( s, o, n ) -> {
            if ( !Objects.equals(n, JFXUtil.convert(model_widget.propMinuteColor().getValue())) ) {
                model_widget.propMinuteColor().setValue(JFXUtil.convert(n));
            }
        });
        clock.minuteTickMarkColorProperty().addListener( ( s, o, n ) -> {
            if ( !Objects.equals(n, JFXUtil.convert(model_widget.propMinuteTickMarkColor().getValue())) ) {
                model_widget.propMinuteTickMarkColor().setValue(JFXUtil.convert(n));
            }
        });
        clock.minuteTickMarksVisibleProperty().addListener( ( s, o, n ) -> {
            if ( !Objects.equals(n, model_widget.propMinuteTickMarkVisible().getValue()) ) {
                model_widget.propMinuteTickMarkVisible().setValue(n);
            }
        });
        clock.secondColorProperty().addListener( ( s, o, n ) -> {
            if ( !Objects.equals(n, JFXUtil.convert(model_widget.propSecondColor().getValue())) ) {
                model_widget.propSecondColor().setValue(JFXUtil.convert(n));
            }
        });
        clock.textColorProperty().addListener( ( s, o, n ) -> {
            if ( !Objects.equals(n, JFXUtil.convert(model_widget.propTextColor().getValue())) ) {
                model_widget.propTextColor().setValue(JFXUtil.convert(n));
            }
        });
        clock.textVisibleProperty().addListener( ( s, o, n ) -> {
            if ( !Objects.equals(n, model_widget.propTextVisible().getValue()) ) {
                model_widget.propTextVisible().setValue(n);
            }
        });
        clock.tickLabelColorProperty().addListener( ( s, o, n ) -> {
            if ( !Objects.equals(n, JFXUtil.convert(model_widget.propTickLabelColor().getValue())) ) {
                model_widget.propTickLabelColor().setValue(JFXUtil.convert(n));
            }
        });
        clock.tickLabelsVisibleProperty().addListener( ( s, o, n ) -> {
            if ( !Objects.equals(n, model_widget.propTickLabelsVisible().getValue()) ) {
                model_widget.propTickLabelsVisible().setValue(n);
            }
        });
        clock.titleColorProperty().addListener( ( s, o, n ) -> {
            if ( !Objects.equals(n, JFXUtil.convert(model_widget.propTitleColor().getValue())) ) {
                model_widget.propTitleColor().setValue(JFXUtil.convert(n));
            }
        });

        return clock;

    }

    @Override
    protected void registerListeners ( ) {

        super.registerListeners();

        model_widget.propBackgroundColor().addUntypedPropertyListener(lookChangedListener);
        model_widget.propDateColor().addUntypedPropertyListener(lookChangedListener);
        model_widget.propHourColor().addUntypedPropertyListener(lookChangedListener);
        model_widget.propHourTickMarkColor().addUntypedPropertyListener(lookChangedListener);
        model_widget.propHourTickMarkVisible().addUntypedPropertyListener(lookChangedListener);
        model_widget.propKnobColor().addUntypedPropertyListener(lookChangedListener);
        model_widget.propMinuteColor().addUntypedPropertyListener(lookChangedListener);
        model_widget.propMinuteTickMarkColor().addUntypedPropertyListener(lookChangedListener);
        model_widget.propMinuteTickMarkVisible().addUntypedPropertyListener(lookChangedListener);
        model_widget.propRingColor().addUntypedPropertyListener(lookChangedListener);
        model_widget.propRingWidth().addUntypedPropertyListener(lookChangedListener);
        model_widget.propSecondColor().addUntypedPropertyListener(lookChangedListener);
        model_widget.propSkin().addUntypedPropertyListener(lookChangedListener);
        model_widget.propTextColor().addUntypedPropertyListener(lookChangedListener);
        model_widget.propTextVisible().addUntypedPropertyListener(lookChangedListener);
        model_widget.propTickLabelColor().addUntypedPropertyListener(lookChangedListener);
        model_widget.propTickLabelsVisible().addUntypedPropertyListener(lookChangedListener);
        model_widget.propTitleColor().addUntypedPropertyListener(lookChangedListener);
        model_widget.propTransparent().addUntypedPropertyListener(lookChangedListener);

        model_widget.propDiscreteHours().addUntypedPropertyListener(behaviorChangedListener);
        model_widget.propDiscreteMinutes().addUntypedPropertyListener(behaviorChangedListener);
        model_widget.propDiscreteSeconds().addUntypedPropertyListener(behaviorChangedListener);

    }

    @Override
    protected void unregisterListeners ( ) {

        model_widget.propBackgroundColor().removePropertyListener(lookChangedListener);
        model_widget.propDateColor().removePropertyListener(lookChangedListener);
        model_widget.propHourColor().removePropertyListener(lookChangedListener);
        model_widget.propHourTickMarkColor().removePropertyListener(lookChangedListener);
        model_widget.propHourTickMarkVisible().removePropertyListener(lookChangedListener);
        model_widget.propKnobColor().removePropertyListener(lookChangedListener);
        model_widget.propMinuteColor().removePropertyListener(lookChangedListener);
        model_widget.propMinuteTickMarkColor().removePropertyListener(lookChangedListener);
        model_widget.propMinuteTickMarkVisible().removePropertyListener(lookChangedListener);
        model_widget.propRingColor().removePropertyListener(lookChangedListener);
        model_widget.propRingWidth().removePropertyListener(lookChangedListener);
        model_widget.propSecondColor().removePropertyListener(lookChangedListener);
        model_widget.propSkin().removePropertyListener(lookChangedListener);
        model_widget.propTextColor().removePropertyListener(lookChangedListener);
        model_widget.propTextVisible().removePropertyListener(lookChangedListener);
        model_widget.propTickLabelColor().removePropertyListener(lookChangedListener);
        model_widget.propTickLabelsVisible().removePropertyListener(lookChangedListener);
        model_widget.propTitleColor().removePropertyListener(lookChangedListener);
        model_widget.propTransparent().removePropertyListener(lookChangedListener);

        model_widget.propDiscreteHours().removePropertyListener(behaviorChangedListener);
        model_widget.propDiscreteMinutes().removePropertyListener(behaviorChangedListener);
        model_widget.propDiscreteSeconds().removePropertyListener(behaviorChangedListener);

        super.unregisterListeners();

    }

    private void behaviorChanged ( final WidgetProperty<?> property, final Object old_value, final Object new_value ) {
        dirtyBehavior.mark();
        toolkit.scheduleUpdate(this);
    }

    private void lookChanged ( final WidgetProperty<?> property, final Object old_value, final Object new_value ) {
        dirtyLook.mark();
        toolkit.scheduleUpdate(this);
    }

}
