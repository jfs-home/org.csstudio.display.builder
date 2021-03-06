/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * Copyright (C) 2016 European Spallation Source ERIC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.csstudio.display.builder.representation.javafx.widgets;


import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.csstudio.display.builder.model.ArrayWidgetProperty;
import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.macros.MacroHandler;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.display.builder.model.util.ModelThreadPool;
import org.csstudio.display.builder.model.widgets.PVWidget;
import org.csstudio.display.builder.model.widgets.SymbolWidget;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.csstudio.javafx.ImageCache;
import org.csstudio.javafx.Styles;
import org.diirt.util.array.ListInt;
import org.diirt.util.array.ListNumber;
import org.diirt.vtype.VBoolean;
import org.diirt.vtype.VEnum;
import org.diirt.vtype.VEnumArray;
import org.diirt.vtype.VNumber;
import org.diirt.vtype.VNumberArray;
import org.diirt.vtype.VString;
import org.diirt.vtype.VType;

import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Dimension2D;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import se.europeanspallationsource.xaos.components.SVG;


/**
 * @author claudiorosati, European Spallation Source ERIC
 * @version 1.0.0 19 Jun 2017
 */
public class SymbolRepresentation extends RegionBaseRepresentation<StackPane, SymbolWidget> {

    private static final double INDEX_LABEL_SIZE = 32.0;

    private int                                                        arrayIndex                  = 0;
    private volatile boolean                                           autoSize                    = false;
    private final UntypedWidgetPropertyListener                        contentChangedListener      = this::contentChanged;
    private final Symbol                                               defaultSymbol;
    private final DefaultSymbolNode                                    defaultSymbolNode           = new DefaultSymbolNode();
    private final DirtyFlag                                            dirtyContent                = new DirtyFlag();
    private final DirtyFlag                                            dirtyGeometry               = new DirtyFlag();
    private final DirtyFlag                                            dirtyStyle                  = new DirtyFlag();
    private final DirtyFlag                                            dirtyValue                  = new DirtyFlag();
    private volatile boolean                                           enabled                     = true;
    private final UntypedWidgetPropertyListener                        geometryChangedListener     = this::geometryChanged;
    private final ImageView                                            imageView                   = new ImageView();
    private final Label                                                indexLabel                  = new Label();
    private final Circle                                               indexLabelBackground        = new Circle(INDEX_LABEL_SIZE / 2, Color.BLACK.deriveColor(0.0, 0.0, 0.0, 0.75));
    private final WidgetPropertyListener<Integer>                      initialIndexChangedListener = this::initialIndexChanged;
    private Dimension2D                                                maxSize                     = new Dimension2D(0, 0);
    private final UntypedWidgetPropertyListener                        styleChangedListener        = this::styleChanged;
    private Symbol                                                     symbol;
    private final WidgetPropertyListener<String>                       symbolPropertyListener      = this::symbolChanged;
    private final AtomicReference<List<Symbol>>                        symbols                     = new AtomicReference<>(Collections.emptyList());
    private final WidgetPropertyListener<List<WidgetProperty<String>>> symbolsChangedListener      = this::symbolsChanged;
    private final AtomicBoolean                                        updatingValue               = new AtomicBoolean(false);
    private final WidgetPropertyListener<VType>                        valueChangedListener        = this::valueChanged;

    // ---- imageIndex property
    private IntegerProperty imageIndex = new SimpleIntegerProperty(-1);

    private int getImageIndex ( ) {
        return imageIndex.get();
    }

    private IntegerProperty imageIndexProperty ( ) {
        return imageIndex;
    }

    private void setImageIndex ( int imageIndex ) {

        int oldIndex = getImageIndex();
        List<Symbol> symbolsList = symbols.get();

        if ( imageIndex < 0 || symbolsList.isEmpty() ) {
            symbol = getDefaultSymbol();
        } else {
            symbol = symbolsList.get(Math.min(imageIndex, symbolsList.size() - 1));
        }

        if ( oldIndex != imageIndex ) {
            dirtyGeometry.mark();
            toolkit.scheduleUpdate(SymbolRepresentation.this);
        }

        toolkit.execute(() -> {
            this.imageIndex.set(imageIndex);
            jfx_node.getChildren().set(0, getSymbolNode());
        });

    }

    public static String resolveImageFile ( SymbolWidget widget, String imageFileName ) {

        try {

            String expandedFileName = MacroHandler.replace(widget.getMacrosOrProperties(), imageFileName);

            // Resolve new image file relative to the source widget model (not 'top'!).
            // Get the display model from the widget tied to this representation.
            final DisplayModel widgetModel = widget.getDisplayModel();

            // Resolve the image path using the parent model file path.
            return ModelResourceUtil.resolveResource(widgetModel, expandedFileName);

        } catch ( Exception ex ) {

            logger.log(Level.WARNING, "Failure resolving image path: {0} [{1}].", new Object[] { imageFileName, ex.getMessage() });

            return null;

        }

    }

    private static boolean resourceExists ( String fileName ) {

        try {
            ModelResourceUtil.openResourceStream(fileName);
        } catch ( Exception ex ) {
            return false;
        }

        return true;

    }

    public SymbolRepresentation ( ) {

        super();

        //  This initialization must be performed here, to allow defaultSymbolNode
        //  to be initialized first.
        defaultSymbol = new Symbol();

    }

    @Override
    public void dispose ( ) {

        symbol = null;

        symbols.get().clear();
        symbols.set(null);

        super.dispose();

    }

    @Override
    public void updateChanges ( ) {

        super.updateChanges();

        Object value;

        //  Must be the first "if" statement to be executed, because it select the array index for the value.
        if ( dirtyContent.checkAndClear() ) {

            value = model_widget.propArrayIndex().getValue();

            if ( !Objects.equals(value, arrayIndex) ) {
                arrayIndex = Math.max(0, (int) value);
            }

            dirtyValue.mark();

        }

        //  Must be the second "if" statement to be executed, because it select the node to be displayed.
        if ( dirtyValue.checkAndClear() && updatingValue.compareAndSet(false, true) ) {

            int idx = Integer.MIN_VALUE;    // Marker indicating non-valid value.

            try {

                value = model_widget.runtimePropValue().getValue();

                if ( value != null ) {
                    if ( PVWidget.RUNTIME_VALUE_NO_PV == value ) {
                        idx = model_widget.propInitialIndex().getValue();
                    } else if ( value instanceof VBoolean ) {
                        idx = ( (VBoolean) value ).getValue() ? 1 : 0;
                    } else if ( value instanceof VString ) {
                        try {
                            idx = Integer.parseInt(( (VString) value ).getValue());
                        } catch ( NumberFormatException nfex ) {
                            logger.log(Level.FINE, "Failure parsing the string value: {0} [{1}].", new Object[] { ( (VString) value ).getValue(), nfex.getMessage() });
                        }
                    } else if ( value instanceof VNumber ) {
                        idx = ( (VNumber) value ).getValue().intValue();
                    } else if ( value instanceof VEnum ) {
                        idx = ( (VEnum) value ).getIndex();
                    } else if ( value instanceof VNumberArray ) {

                        ListNumber array = ( (VNumberArray) value ).getData();

                        if ( array.size() > 0 ) {
                            idx = array.getInt(Math.min(arrayIndex, array.size() - 1));
                        }

                    } else if ( value instanceof VEnumArray ) {

                        ListInt array = ( (VEnumArray) value ).getIndexes();

                        if ( array.size() > 0 ) {
                            idx = array.getInt(Math.min(arrayIndex, array.size() - 1));
                        }

                    }
                }

            } finally {
                updatingValue.set(false);
            }

            if ( idx != Integer.MIN_VALUE ) {
                // Valid value.
                setImageIndex(idx);
            }

        }

        if ( dirtyGeometry.checkAndClear() ) {

            value = model_widget.propVisible().getValue();

            if ( !Objects.equals(value, jfx_node.isVisible()) ) {
                jfx_node.setVisible((boolean) value);
            }

            value = model_widget.propAutoSize().getValue();

            if ( !Objects.equals(value, autoSize) ) {
                autoSize = (boolean) value;
            }

            if ( autoSize ) {
                model_widget.propWidth().setValue((int) Math.round(maxSize.getWidth()));
                model_widget.propHeight().setValue((int) Math.round(maxSize.getHeight()));
            }

            double w = model_widget.propWidth().getValue();
            double h = model_widget.propHeight().getValue();

            if ( w < INDEX_LABEL_SIZE || h < INDEX_LABEL_SIZE ) {
                jfx_node.getChildren().remove(indexLabel);
                jfx_node.getChildren().remove(indexLabelBackground);
            } else {
                if ( !jfx_node.getChildren().contains(indexLabelBackground) ) {
                    jfx_node.getChildren().add(indexLabelBackground);
                }
                if ( !jfx_node.getChildren().contains(indexLabel) ) {
                    jfx_node.getChildren().add(indexLabel);
                }
            }

            if ( symbol != null ) {
                setSymbolSize(w, h, model_widget.propPreserveRatio().getValue());
            }

            jfx_node.setLayoutX(model_widget.propX().getValue());
            jfx_node.setLayoutY(model_widget.propY().getValue());
            jfx_node.setPrefSize(w, h);

            value = model_widget.propRotation().getValue();

            if ( !Objects.equals(value, jfx_node.getRotate()) ) {
                jfx_node.setRotate((double) value);
            }

        }

        if ( dirtyStyle.checkAndClear() ) {

            value = model_widget.propEnabled().getValue();

            if ( !Objects.equals(value, enabled) ) {

                enabled = (boolean) value;

                Styles.update(jfx_node, Styles.NOT_ENABLED, !enabled);

            }

            value = model_widget.propShowIndex().getValue();

            if ( !Objects.equals(value, indexLabel.isVisible()) ) {
                indexLabel.setVisible((boolean) value);
                indexLabelBackground.setVisible((boolean) value);
            }

            if ( model_widget.propTransparent().getValue() ) {
                jfx_node.setBackground(null);
            } else {
                jfx_node.setBackground(new Background(new BackgroundFill(JFXUtil.convert(model_widget.propBackgroundColor().getValue()), CornerRadii.EMPTY, Insets.EMPTY)));
            }

        }

    }

    @Override
    protected StackPane createJFXNode ( ) throws Exception {

        autoSize = model_widget.propAutoSize().getValue();
        symbol = getDefaultSymbol();

        StackPane symbolPane = new StackPane();

        indexLabelBackground.setStroke(Color.LIGHTGRAY.deriveColor(0.0, 1.0, 1.0, 0.75));
        indexLabelBackground.setVisible(model_widget.propShowIndex().getValue());

        indexLabel.setAlignment(Pos.CENTER);
        indexLabel.setFont(Font.font(indexLabel.getFont().getFamily(), FontWeight.BOLD, 16));
        indexLabel.setTextFill(Color.WHITE);
        indexLabel.setVisible(model_widget.propShowIndex().getValue());
        indexLabel.textProperty().bind(Bindings.convert(imageIndexProperty()));

        symbolPane.getChildren().addAll(getSymbolNode(), indexLabelBackground, indexLabel);

        if ( model_widget.propTransparent().getValue() ) {
            symbolPane.setBackground(null);
        } else {
            symbolPane.setBackground(new Background(new BackgroundFill(JFXUtil.convert(model_widget.propBackgroundColor().getValue()), CornerRadii.EMPTY, Insets.EMPTY)));
        }

        enabled = model_widget.propEnabled().getValue();

        Styles.update(symbolPane, Styles.NOT_ENABLED, !enabled);

        initialIndexChanged(null, null, null);
        symbolChanged(null, null, null);

        return symbolPane;

    }

    @Override
    protected void registerListeners ( ) {

        super.registerListeners();

        model_widget.propArrayIndex().addUntypedPropertyListener(contentChangedListener);
        model_widget.propPVName().addUntypedPropertyListener(contentChangedListener);

        model_widget.propSymbols().addPropertyListener(symbolsChangedListener);
        model_widget.propSymbols().getValue().stream().forEach(p -> p.addPropertyListener(symbolPropertyListener));

        model_widget.propInitialIndex().addPropertyListener(initialIndexChangedListener);

        model_widget.propAutoSize().addUntypedPropertyListener(geometryChangedListener);
        model_widget.propVisible().addUntypedPropertyListener(geometryChangedListener);
        model_widget.propX().addUntypedPropertyListener(geometryChangedListener);
        model_widget.propY().addUntypedPropertyListener(geometryChangedListener);
        model_widget.propWidth().addUntypedPropertyListener(geometryChangedListener);
        model_widget.propHeight().addUntypedPropertyListener(geometryChangedListener);
        model_widget.propPreserveRatio().addUntypedPropertyListener(geometryChangedListener);
        model_widget.propRotation().addUntypedPropertyListener(geometryChangedListener);

        model_widget.propBackgroundColor().addUntypedPropertyListener(styleChangedListener);
        model_widget.propEnabled().addUntypedPropertyListener(styleChangedListener);
        model_widget.propShowIndex().addUntypedPropertyListener(styleChangedListener);
        model_widget.propTransparent().addUntypedPropertyListener(styleChangedListener);

        if ( toolkit.isEditMode() ) {
            dirtyValue.checkAndClear();
        } else {
            model_widget.runtimePropValue().addPropertyListener(valueChangedListener);
            valueChanged(null, null, null);
        }

    }

    @Override
    protected void unregisterListeners ( ) {

        model_widget.propArrayIndex().removePropertyListener(contentChangedListener);
        model_widget.propPVName().removePropertyListener(contentChangedListener);

        model_widget.propSymbols().removePropertyListener(symbolsChangedListener);
        model_widget.propSymbols().getValue().stream().forEach(p -> p.removePropertyListener(symbolPropertyListener));

        model_widget.propInitialIndex().removePropertyListener(initialIndexChangedListener);

        model_widget.propAutoSize().removePropertyListener(geometryChangedListener);
        model_widget.propVisible().removePropertyListener(geometryChangedListener);
        model_widget.propX().removePropertyListener(geometryChangedListener);
        model_widget.propY().removePropertyListener(geometryChangedListener);
        model_widget.propWidth().removePropertyListener(geometryChangedListener);
        model_widget.propHeight().removePropertyListener(geometryChangedListener);
        model_widget.propPreserveRatio().removePropertyListener(geometryChangedListener);
        model_widget.propRotation().removePropertyListener(geometryChangedListener);

        model_widget.propBackgroundColor().removePropertyListener(styleChangedListener);
        model_widget.propEnabled().removePropertyListener(styleChangedListener);
        model_widget.propShowIndex().removePropertyListener(styleChangedListener);
        model_widget.propTransparent().removePropertyListener(styleChangedListener);

        if ( !toolkit.isEditMode() ) {
            model_widget.runtimePropValue().removePropertyListener(valueChangedListener);
        }

        super.unregisterListeners();

    }

    private void contentChanged ( final WidgetProperty<?> property, final Object oldValue, final Object newValue ) {
        dirtyContent.mark();
        toolkit.scheduleUpdate(this);
    }

    /**
     * Fix the file names imported from BOY.
     */
    private void fixImportedSymbolNames ( ) {

        try {

            ArrayWidgetProperty<WidgetProperty<String>> propSymbols = model_widget.propSymbols();
            List<String> fileNames = new ArrayList<>(2);

            switch ( model_widget.getImportedFrom() ) {
                case "org.csstudio.opibuilder.widgets.ImageBoolIndicator":
                    return;
                case "org.csstudio.opibuilder.widgets.symbol.bool.BoolMonitorWidget": {

                        String imageFileName = propSymbols.getElement(0).getValue();
                        int dotIndex = imageFileName.lastIndexOf('.');
                        String onImageFileName = imageFileName.substring(0, dotIndex) + " On" + imageFileName.substring(dotIndex);
                        String offImageFileName = imageFileName.substring(0, dotIndex) + " Off" + imageFileName.substring(dotIndex);

                        if ( resourceExists(resolveImageFile(model_widget, onImageFileName)) ) {
                            fileNames.add(onImageFileName);
                        } else {
                            fileNames.add(imageFileName);
                        }

                        if ( resourceExists(resolveImageFile(model_widget, offImageFileName)) ) {
                            fileNames.add(offImageFileName);
                        } else {
                            fileNames.add(imageFileName);
                        }

                    }
                    break;
                case "org.csstudio.opibuilder.widgets.symbol.multistate.MultistateMonitorWidget": {

                        String imageFileName = propSymbols.getElement(0).getValue();
                        int dotIndex = imageFileName.lastIndexOf('.');
                        int spaceIndex = imageFileName.lastIndexOf(' ');

                        try {
                            model_widget.propInitialIndex().setValue(Integer.parseInt(imageFileName.substring(1 + spaceIndex, dotIndex)));
                        } catch ( NumberFormatException nfex ) {
                            logger.log(Level.WARNING, "Imported image file doesn't contain state value [{0}].", imageFileName);
                        }

                        int index = 0;

                        while ( true ) {

                            String nthImageFileName = MessageFormat.format("{0} {1,number,#########0}{2}", imageFileName.substring(0, spaceIndex), index, imageFileName.substring(dotIndex));

                            if ( resourceExists(resolveImageFile(model_widget, nthImageFileName)) ) {
                                fileNames.add(nthImageFileName);
                            } else {
                                break;
                            }

                            index++;

                        }

                    }
                    break;
                default:
                    logger.log(Level.WARNING, "Invalid imported type [{0}].", model_widget.getImportedFrom());
                    return;
            }

            for ( int i = 0; i < fileNames.size(); i++ ) {
                model_widget.addOrReplaceSymbol(i, fileNames.get(i));
            }

        } finally {
            model_widget.clearImportedFrom();
        }

    }

    private void geometryChanged ( final WidgetProperty<?> property, final Object oldValue, final Object newValue ) {
        dirtyGeometry.mark();
        toolkit.scheduleUpdate(this);
    }

    private Symbol getDefaultSymbol ( ) {
        return defaultSymbol;
    }

    private DefaultSymbolNode getDefaultSymbolNode ( ) {
        return defaultSymbolNode;
    }

    Node getSymbolNode ( ) {

        Image image = symbol.getImage();

        if ( image == null ) {
            return getDefaultSymbolNode();
        } else {

            imageView.setImage(image);

            return imageView;

        }

    }

    private void initialIndexChanged ( final WidgetProperty<Integer> property, final Object oldValue, final Object newValue ) {
        dirtyValue.mark();
        toolkit.scheduleUpdate(this);
    }

    void setSymbolSize ( double width, double height, boolean preserveRatio ) {
        if ( symbol != null ) {
            if ( symbol.getImage() == null ) {
                getDefaultSymbolNode().setSize(width, height);
            } else {
                imageView.setFitWidth(width);
                imageView.setFitHeight(height);
                imageView.setPreserveRatio(preserveRatio);
            }
        }
    }

    private void styleChanged ( final WidgetProperty<?> property, final Object oldValue, final Object newValue ) {
        dirtyStyle.mark();
        toolkit.scheduleUpdate(this);
    }

    private void symbolChanged ( final WidgetProperty<String> property, final String oldValue, final String newValue ) {
        ModelThreadPool.getExecutor().execute(this::updateSymbols);
    }

    private void symbolsChanged ( final WidgetProperty<List<WidgetProperty<String>>> property, final List<WidgetProperty<String>> oldValue, final List<WidgetProperty<String>> newValue ) {
        ModelThreadPool.getExecutor().execute( ( ) -> {

            if ( oldValue != null ) {
                oldValue.stream().forEach(p -> p.removePropertyListener(symbolPropertyListener));
            }

            if ( newValue != null ) {
                newValue.stream().forEach(p -> p.addPropertyListener(symbolPropertyListener));
            }

            updateSymbols();

        });
    }

    private synchronized void updateSymbols ( ) {

        List<WidgetProperty<String>> fileNames = model_widget.propSymbols().getValue();
        List<Symbol> symbolsList = new ArrayList<>(fileNames.size());
        Map<String, Symbol> symbolsMap = new HashMap<>(fileNames.size());
        Map<String, Symbol> currentSymbolsMap = symbols.get().stream().distinct().collect(Collectors.toMap(Symbol::getFileName, sc -> sc));
        int currentIndex = getImageIndex();

        try {

            if ( model_widget.getImportedFrom() != null ) {
                fixImportedSymbolNames();
            }

            fileNames.stream().forEach(f -> {

                String fileName = f.getValue();
                Symbol s = symbolsMap.get(fileName);

                if ( s == null ) {     // Symbol not yet loaded...

                    s = currentSymbolsMap.get(fileName);

                    if ( s == null ) { // Neither previously loaded.
                        s = new Symbol(fileName);
                    }

                    symbolsMap.put(fileName, s);

                }

                symbolsList.add(s);

            });

        } finally {

            int newImageIndex = Math.min(Math.max(currentIndex, 0), symbolsList.size() - 1);

            maxSize = new Dimension2D(
                symbolsList.stream().mapToDouble(Symbol::getOriginalWidth).max().orElse(0.0),
                symbolsList.stream().mapToDouble(Symbol::getOriginalHeight).max().orElse(0.0)
            );

            symbols.set(symbolsList);

            setImageIndex(-1);
            setImageIndex(newImageIndex);

            dirtyGeometry.mark();
            dirtyValue.mark();
            toolkit.scheduleUpdate(this);

        }

    }

    private void valueChanged ( final WidgetProperty<? extends VType> property, final VType oldValue, final VType newValue ) {
        dirtyValue.mark();
        toolkit.scheduleUpdate(this);
    }

    private class DefaultSymbolNode extends Group {

        private final Rectangle r;
        private final Line l1;
        private final Line l2;

        DefaultSymbolNode() {

            setManaged(true);

            int w = 100;
            int h = 100;

            r = new Rectangle(0, 0, w, h);

            r.setFill(null);
            r.setArcHeight(0);
            r.setArcWidth(0);
            r.setStroke(Color.BLACK);
            r.setStrokeType(StrokeType.INSIDE);

            l1 = new Line(0.5, 0.5, w - 0.5, h - 0.5);

            l1.setStroke(Color.BLACK);
            l1.setStrokeLineCap(StrokeLineCap.BUTT);

            l2 = new Line(0.5, h - 0.5, w - 0.5, 0.5);

            l2.setStroke(Color.BLACK);
            l2.setStrokeLineCap(StrokeLineCap.BUTT);

            getChildren().add(r);
            getChildren().add(l1);
            getChildren().add(l2);

        }

        public void setSize ( double w, double h ) {

            r.setWidth(w);
            r.setHeight(h);

            l1.setEndX(w - 0.5);
            l1.setEndY(h - 0.5);

            l2.setEndX(w - 0.5);
            l2.setStartY(h - 0.5);

        }

    }

    private class Symbol {

        private final String fileName;
        private Image image = null;
        private double originalHeight = 100;
        private double originalWidth = 100;

        Symbol ( ) {
            fileName = null;
        }

        Symbol ( String fileName ) {

            this.fileName = fileName;

            String imageFileName = resolveImageFile(model_widget, fileName);

            if ( imageFileName != null ) {

                if ( toolkit.isEditMode() ) {
                    ImageCache.remove(imageFileName);
                }

                image = ImageCache.get(imageFileName);

                if ( image != null ) {
                    originalWidth = image.getWidth();
                    originalHeight = image.getHeight();
                } else {

                    boolean loadFailed = true;

                    if ( imageFileName.toLowerCase().endsWith(".svg") ) {
                        try {

                            // Open the image from the stream created from the
                            // resource file.
                            SVG svg = SVG.load(ModelResourceUtil.openResourceStream(imageFileName));
                            Bounds bounds = svg.getLayoutBounds();

                            image = new WritableImage((int) Math.round(bounds.getWidth()), (int) Math.round(bounds.getHeight()));
                            originalWidth = image.getWidth();
                            originalHeight = image.getHeight();
                            loadFailed = false;

                            toolkit.execute( ( ) -> {

                                SnapshotParameters sp = new SnapshotParameters();

                                sp.setFill(Color.TRANSPARENT);
                                svg.snapshot(sp, (WritableImage) image);

                            });

                        } catch ( Exception ex ) {
                            logger.log(Level.WARNING, "Failure loading SVG image file: ({0}) {1} [{2}].", new Object[] { fileName, imageFileName, ex.getMessage() });
                        }
                    } else {
                        try {

                            // Open the image from the stream created from the
                            // resource file.
                            image = new Image(ModelResourceUtil.openResourceStream(imageFileName));
                            originalWidth = image.getWidth();
                            originalHeight = image.getHeight();
                            loadFailed = false;

                        } catch ( Exception ex ) {
                            logger.log(Level.WARNING, "Failure loading image: ({0}) {1} [{2}].", new Object[] { fileName, imageFileName, ex.getMessage() });
                        }
                    }

                    if ( !loadFailed ) {
                        ImageCache.put(imageFileName, image);
                    }

                }
            }

        }

        String getFileName ( ) {
            return fileName;
        }

        double getOriginalHeight ( ) {
            return originalHeight;
        }

        double getOriginalWidth ( ) {
            return originalWidth;
        }

        Image getImage ( ) {
            return image;
        }

    }

}
