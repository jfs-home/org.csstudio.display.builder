/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.behaviorPVName;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.displayBackgroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.displayBorderAlarmSensitive;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.displayFont;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.displayForegroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newBooleanPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newRuntimeValue;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newStringPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.positionWidth;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.widgetName;
import static org.csstudio.display.builder.model.widgets.plots.PlotWidgetProperties.displayToolbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.csstudio.display.builder.model.ArrayWidgetProperty;
import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.RuntimeWidgetProperty;
import org.csstudio.display.builder.model.StructuredWidgetProperty;
import org.csstudio.display.builder.model.StructuredWidgetProperty.Descriptor;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetConfigurator;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.NamedWidgetFonts;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.persist.XMLUtil;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.properties.WidgetFont;
import org.diirt.util.array.ListDouble;
import org.diirt.util.array.ListNumber;
import org.diirt.vtype.VTable;
import org.diirt.vtype.VType;
import org.osgi.framework.Version;
import org.w3c.dom.Element;

/** Widget that displays a string table
 *
 *  <p>The 'value' can be either a VTable
 *  or a <code>List&lt;List&lt;String>></code>.
 *  The latter includes 2-D string arrays written
 *  by Jython scripts.
 *
 *  TODO Some API for script to setCellText(row, column)
 *  TODO Some API for script to setCellBackground(row, column)
 *  TODO Some API for script to setCellColor(row, column)
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TableWidget extends VisibleWidget
{
    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("table", WidgetCategory.MONITOR,
            "Table",
            "platform:/plugin/org.csstudio.display.builder.model/icons/table.gif",
            "A table",
            Arrays.asList("org.csstudio.opibuilder.widgets.table"))
    {
        @Override
        public Widget createWidget()
        {
            return new TableWidget();
        }
    };

    /** Structure for column configuration */
    private static final WidgetPropertyDescriptor<Boolean> behaviorEditable =
        newBooleanPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "editable", "Editable");

    private static final WidgetPropertyDescriptor<String> columnOption =
            newStringPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "option", "Option");

    private static final ArrayWidgetProperty.Descriptor<WidgetProperty<String>> columnOptions =
        new ArrayWidgetProperty.Descriptor<>(WidgetPropertyCategory.DISPLAY, "options", "Options",
                                             (widget, index) ->
                                             columnOption.createProperty(widget, "Option " + (index + 1)),
                                             /* minimum size */ 0);

    private static final StructuredWidgetProperty.Descriptor displayColumn =
        new Descriptor(WidgetPropertyCategory.DISPLAY, "column", "Column");

    public static class ColumnProperty extends StructuredWidgetProperty
    {
        public ColumnProperty(final Widget widget, final String name)
        {
            super(displayColumn, widget,
                  Arrays.asList(widgetName.createProperty(widget, name),
                                positionWidth.createProperty(widget, 50),
                                behaviorEditable.createProperty(widget, true),
                                columnOptions.createProperty(widget, Collections.emptyList())));
        }

        public WidgetProperty<String> name()                          { return getElement(0); }
        public WidgetProperty<Integer> width()                        { return getElement(1); }
        public WidgetProperty<Boolean> editable()                     { return getElement(2); }
        public ArrayWidgetProperty<WidgetProperty<String>> options()  { final WidgetProperty<List<WidgetProperty<String>>> prop = getElement(3);
                                                                        return (ArrayWidgetProperty<WidgetProperty<String>>)prop;
                                                                      }
    };

    /** 'columns' array */
    public static final ArrayWidgetProperty.Descriptor<ColumnProperty> displayColumns =
        new ArrayWidgetProperty.Descriptor<>(WidgetPropertyCategory.DISPLAY, "columns", "Columns",
                                             (widget, index) ->
                                             new ColumnProperty(widget, "Column " + (index + 1)));


    /** 'value', but compared to usual value not limited to VType */
    private static final WidgetPropertyDescriptor<Object> runtimeValue =
        new WidgetPropertyDescriptor<Object>(WidgetPropertyCategory.RUNTIME, "value", Messages.WidgetProperties_Value)
        {
            @Override
            public WidgetProperty<Object> createProperty(final Widget widget, final Object value)
            {
                return new RuntimeWidgetProperty<Object>(this, widget, value)
                {
                    @Override
                    public void setValueFromObject(final Object value) throws Exception
                    {
                        if (value instanceof VType)
                            setValue(value);
                        else if (value instanceof List)
                            setValue(value);
                        else if (value == null)
                            setValue(null);
                        else
                            throw new Exception("Need VType or List<List<String>, got " + value);
                    }
                };
            }
        };

    /** PV for runtime info about selection */
    private static final WidgetPropertyDescriptor<String> selectionPV =
        newStringPropertyDescriptor(WidgetPropertyCategory.MISC, "selection_pv", Messages.WidgetProperties_SelectionPV);

    /** Runtime info about selection */
    private static final WidgetPropertyDescriptor<VType> selectionInfo =
        newRuntimeValue("selection", Messages.WidgetProperties_Selection);

    /** Configurator for legacy XML files */
    private static class CustomConfigurator extends WidgetConfigurator
    {
        public CustomConfigurator(final Version xml_version)
        {
            super(xml_version);
        }

        @Override
        public boolean configureFromXML(final ModelReader model_reader, final Widget widget,
                                        final Element xml) throws Exception
        {
            if (! super.configureFromXML(model_reader, widget, xml))
                return false;

            if (xml_version.getMajor() < 2)
                configureLegacyColumns((TableWidget) widget, xml);

            return true;
        }
    };

    /** Configure column information from legacy XML
     *  @param widget
     *  @param xml
     *  @throws Exception
     */
    private static void configureLegacyColumns(final TableWidget widget, final Element xml) throws Exception
    {
        // Legacy column information:
        // <column_headers>
        //   <row>  <col>Name</col>  <col>width</col>  <col>Edit: No?</col>  <col>TEXT|CHECKBOX|DROPDOWN</col>
        final Element el = XMLUtil.getChildElement(xml, "column_headers");
        if (el == null)
            return;
        int col = -1;
        final ArrayWidgetProperty<ColumnProperty> columns = widget.displayColumns();
        for (Element row : XMLUtil.getChildElements(el, "row"))
        {
            ++col;
            while (columns.size() <= col)
                columns.addElement();
            final ColumnProperty column = columns.getElement(col);

            int idx = -1;
            for (Element item : XMLUtil.getChildElements(row, "col"))
            {
                switch (++idx)
                {
                case 0: column.name().setValue(XMLUtil.getString(item));
                        break;
                case 1: final String text = XMLUtil.getString(item);
                        if (text.length() > 0)
                            try
                            {
                                column.width().setValue(Integer.parseInt(text));
                            }
                            catch (NumberFormatException ex)
                            {
                                throw new Exception("Error in legacy table column width", ex);
                            }
                        break;
                case 2: if ("No".equalsIgnoreCase(XMLUtil.getString(item)))
                            column.editable().setValue(false);
                        break;
                case 3: // Ignore editor type
                        break;
                }
            }
        }
    }

    private volatile WidgetProperty<WidgetColor> background;
    private volatile WidgetProperty<WidgetColor> foreground;
    private volatile WidgetProperty<WidgetFont> font;
    private volatile WidgetProperty<Boolean> show_toolbar;
    private volatile ArrayWidgetProperty<ColumnProperty> columns;
    private volatile WidgetProperty<String> pv_name;
    private volatile WidgetProperty<Object> value;
    private volatile WidgetProperty<Boolean> editable;
    private volatile WidgetProperty<String> selection_pv;
    private volatile WidgetProperty<VType> selection;

    public TableWidget()
    {
        super(WIDGET_DESCRIPTOR.getType(), 500, 300);
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(displayBorderAlarmSensitive.createProperty(this, true));
        properties.add(background = displayBackgroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.BACKGROUND)));
        properties.add(foreground = displayForegroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.TEXT)));
        properties.add(font = displayFont.createProperty(this, NamedWidgetFonts.DEFAULT));
        properties.add(show_toolbar = displayToolbar.createProperty(this,false));
        properties.add(columns = displayColumns.createProperty(this, Arrays.asList(  new ColumnProperty(this, "Column 1") )));
        properties.add(pv_name = behaviorPVName.createProperty(this, ""));
        properties.add(value = runtimeValue.createProperty(this, null));
        properties.add(editable = behaviorEditable.createProperty(this, true));
        properties.add(selection_pv = selectionPV.createProperty(this, ""));
        properties.add(selection = selectionInfo.createProperty(this, null));
    }

    @Override
    public WidgetConfigurator getConfigurator(final Version persisted_version) throws Exception
    {
        return new CustomConfigurator(persisted_version);
    }

    /** @return Display 'background_color' */
    public WidgetProperty<WidgetColor> displayBackgroundColor()
    {
        return background;
    }

    /** @return Display 'foreground_color' */
    public WidgetProperty<WidgetColor> displayForegroundColor()
    {
        return foreground;
    }

    /** @return Display 'font' */
    public WidgetProperty<WidgetFont> displayFont()
    {
        return font;
    }

    /** @return Display 'show_toolbar' */
    public WidgetProperty<Boolean> displayToolbar()
    {
        return show_toolbar;
    }

    /** @return Display 'columns' */
    public ArrayWidgetProperty<ColumnProperty> displayColumns()
    {
        return columns;
    }

    /** Get options for a column's values
     *
     *  <p>Convenience routines for <code>displayColumns()</code>
     *
     *  @param column Column index, must be 0 .. <code>displayColumns().size()-1</code>
     *  @return Options that combo editor will present for the cells in that column
     */
    public  List<String> getColumnOptions(final int column)
    {
        final List<WidgetProperty<String>> options = columns.getElement(column).options().getValue();
        if (options.isEmpty())
            return Collections.emptyList();
        final List<String> values = new ArrayList<>(options.size());
        for (WidgetProperty<String> option : options)
            values.add(option.getValue());
        return values;
    }

    /** Set options for a column's values
     *
     *  <p>Convenience routines for <code>displayColumns()</code>
     *
     *  @param column Column index, must be 0 .. <code>displayColumns().size()-1</code>
     *  @param options Options to present in combo editor for the cells in that column
     */
    public void setColumnOptions(final int column, final List<String> options)
    {
        final ArrayWidgetProperty<WidgetProperty<String>> options_prop = columns.getElement(column).options();
        final int num = options.size();
        while (options_prop.size() > num)
            options_prop.removeElement();
        while (options_prop.size() < num)
            options_prop.addElement();
        // Received list is supposed to contain strings,
        // but script might actually send PyString, or anything else.
        // Calling toString on the object will allow anything
        // to be passed without errors like
        // "org.python.core.PyString cannot be cast to java.lang.String"
        for (int i=0; i<num; ++i)
            options_prop.getElement(i).setValue(Objects.toString(options.get(i)));
    }

    /** @return Behavior 'pv_name' */
    public WidgetProperty<String> behaviorPVName()
    {
        return pv_name;
    }

    /** @return Runtime 'value' */
    public WidgetProperty<Object> runtimeValue()
    {
        return value;
    }

    /** Set value, i.e. content of cells in table
     *
     *  <p>Convenience routine for <code>runtimeValue()</code>.
     *  and converting the data to a 2D list of strings.
     *
     *  <p>Accepts either a {@link List} of rows,
     *  where each row is a {@link List} of column {@link String}s,
     *  or a {@link VTable}
     *
     *  @param data {@link List} of rows, or {@link VTable}
     */
    public void setValue(final Object data)
    {
        value.setValue(data);
    }

    /** Fetch value, i.e. content of cells in table
     *
     *  <p>Convenience routine for <code>runtimeValue()</code>
     *  and converting the data to a 2D list of strings.
     *
     *  @return {@link List} of rows, where each row is a {@link List} of column {@link String}s
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<List<String>> getValue()
    {
        final Object the_value = value.getValue();
        if (the_value instanceof List)
        {   // Create deep copy:
            // * Avoid changes to widget's value
            // * If a script fetches the value, modifies it, then sets that as a new value,
            //   passing out a deep copy asserts that the 'new' value is indeed recognized
            //   as new, instead of referencing the old value and thus seem a no-change OP.
            final List<List<String>> deep_copy = new ArrayList<>(((List)the_value).size());
            for (List<String> row : (List<List<String>>)the_value)
            {
                final List<String> row_copy = new ArrayList<>(row);
                deep_copy.add(row_copy);
            }
            return deep_copy;
        }
        else if (the_value instanceof VTable)
        {
            final VTable table = (VTable) the_value;
            final int rows = table.getRowCount();
            final int cols = table.getColumnCount();
            // Extract 2D string matrix for data
            final List<List<String>> data = new ArrayList<>(rows);
            for (int r=0; r<rows; ++r)
            {
                final List<String> row = new ArrayList<>(cols);
                for (int c=0; c<cols; ++c)
                {
                    final Object col_data = table.getColumnData(c);
                    if (col_data instanceof List)
                        row.add( Objects.toString(((List)col_data).get(r)) );
                    else if (col_data instanceof ListDouble)
                        row.add( Double.toString(((ListDouble)col_data).getDouble(r)) );
                    else if (col_data instanceof ListNumber)
                        row.add( Long.toString(((ListNumber)col_data).getLong(r)) );
                    else
                        row.add( Objects.toString(col_data) );
                }
                data.add(row);
            }
            return data;
        }
        else
            return Arrays.asList(Arrays.asList(Objects.toString(the_value)));
    }

    /** @return Behavior 'editable' */
    public WidgetProperty<Boolean> behaviorEditable()
    {
        return editable;
    }

    /** @return Misc. 'selection_pv' */
    public WidgetProperty<String> miscSelectionPV()
    {
        return selection_pv;
    }

    /** @return Runtime 'selection' */
    public WidgetProperty<VType> runtimeSelection()
    {
        return selection;
    }
}