/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.desktop.qt.ui.impl;

import consulo.desktop.qt.ui.impl.image.DesktopQtIconOwner;
import consulo.desktop.qt.ui.impl.image.DesktopQtImage;
import consulo.ui.ComboBoxBase;
import consulo.ui.ComboBoxStyle;
import consulo.ui.ComponentItemRender;
import consulo.ui.Length;
import consulo.ui.RenderItem;
import consulo.ui.TextItemRender;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.model.FlatDataModel;
import io.qt.widgets.QAbstractItemView;
import io.qt.widgets.QComboBox;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public abstract class DesktopQtComboBoxBaseImpl<E, W extends QComboBox> extends QtComponentDelegate<W>
    implements ComboBoxBase<E>, DesktopQtIconOwner {
    protected final FlatDataModel<E> myModel;

    private TextItemRender<E> myRenderer = TextItemRender.defaultRender();

    private final Set<ComboBoxStyle> myStyles = EnumSet.noneOf(ComboBoxStyle.class);

    protected boolean myRebuilding;

    protected DesktopQtComboBoxBaseImpl(FlatDataModel<E> model) {
        myModel = model;

        // a component is bound more than once, so the model is followed from here rather than from a bind
        myModel.addListener(event -> onModelChanged());
    }

    protected void onModelChanged() {
        rebuildIfBound();
    }

    protected abstract boolean isSelectedItem(int index, E element);

    protected abstract void afterRebuild(W component);

    protected final void rebuild(W component) {
        myRebuilding = true;
        try {
            component.clear();

            for (int i = 0; i < myModel.getSize(); i++) {
                E element = myModel.get(i);

                DesktopQtTextItemPresentation presentation = new DesktopQtTextItemPresentation();

                myRenderer.render(presentation, RenderItem.of(element, isSelectedItem(i, element)));

                Image image = presentation.getImage();
                if (image instanceof DesktopQtImage) {
                    component.addItem(toHostQIcon(image), presentation.toString());
                }
                else {
                    component.addItem(presentation.toString());
                }
            }

            afterRebuild(component);

            component.updateGeometry();
        }
        finally {
            myRebuilding = false;
        }
    }

    protected final void rebuildIfBound() {
        W component = myComponent;
        if (component != null && !component.isDisposed()) {
            rebuild(component);
        }
    }

    @Override
    @RequiredUIAccess
    public void refreshIcons() {
        rebuildIfBound();
    }

    @RequiredUIAccess
    @Override
    public void repaintAnimatedImage() {
        super.repaintAnimatedImage();

        W component = myComponent;
        if (component == null || component.isDisposed()) {
            return;
        }

        QAbstractItemView view = component.view();
        if (view == null || view.isDisposed() || !view.isVisible()) {
            return;
        }

        QWidget viewport = view.viewport();
        if (viewport != null && !viewport.isDisposed()) {
            viewport.update();
        }
    }

    @Override
    public void addStyle(ComboBoxStyle style) {
        myStyles.add(style);

        W component = myComponent;
        if (component != null && !component.isDisposed()) {
            applyStyle(component, style);
        }
    }

    protected final void applyStyles(W component) {
        for (ComboBoxStyle style : myStyles) {
            applyStyle(component, style);
        }
    }

    private static void applyStyle(QComboBox component, ComboBoxStyle style) {
        switch (style) {
            case INPLACE -> component.setFrame(false);
            case TRANSPARENT_BACKGROUND -> component.setAutoFillBackground(false);
        }
    }

    @Override
    public FlatDataModel<E> getDataModel() {
        return myModel;
    }

    @Override
    public void setRender(TextItemRender<E> render) {
        myRenderer = render;

        rebuildIfBound();
    }

    @Override
    public void setRender(ComponentItemRender<E> render) {
    }

    @Override
    public void setSpeedSearchConverter(@Nullable Function<E, String> converter) {
    }

    @Override
    public @Nullable String getSpeedSearchText() {
        return null;
    }

    @Override
    public void setItemHeightGetter(@Nullable Function<E, Length> getter) {
    }
}
