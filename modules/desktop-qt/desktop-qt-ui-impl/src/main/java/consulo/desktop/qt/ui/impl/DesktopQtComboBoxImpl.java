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
import consulo.ui.Length;
import consulo.ui.ComboBox;
import consulo.ui.ComboBoxStyle;
import consulo.ui.ComponentItemRender;
import consulo.ui.RenderItem;
import consulo.ui.TextItemRender;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.image.Image;
import consulo.ui.model.FlatDataModel;
import io.qt.widgets.QAbstractItemView;
import io.qt.core.QRect;
import io.qt.core.QSize;
import io.qt.core.Qt;
import io.qt.widgets.QComboBox;
import io.qt.widgets.QStyle;
import io.qt.widgets.QStyleOptionComboBox;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtComboBoxImpl<E> extends QtComponentDelegate<QComboBox> implements ComboBox<E>, DesktopQtIconOwner {
    private static final int MINIMUM_VISIBLE_CHARS = 8;
    private static final int ICON_TEXT_GAP = 4;

    private TextItemRender<E> myRenderer = TextItemRender.defaultRender();

    private final FlatDataModel<E> myModel;

    private int mySelectedIndex = 0;
    private boolean myFireListeners = true;
    private boolean myRebuilding;

    public DesktopQtComboBoxImpl(FlatDataModel<E> model) {
        myModel = model;

        // a component is bound more than once, so the model is followed from here rather than from a bind
        myModel.addListener(event -> rebuildIfBound());
    }

    @Override
    protected QComboBox createQt(QWidget parent) {
        return new QComboBox(parent) {
            @Override
            public QSize minimumSizeHint() {
                QSize hint = super.minimumSizeHint();

                QStyleOptionComboBox option = new QStyleOptionComboBox();
                initStyleOption(option);

                int contentWidth = fontMetrics().horizontalAdvance("x".repeat(MINIMUM_VISIBLE_CHARS)) + iconSize().width();
                QSize minimum = style().sizeFromContents(QStyle.ContentsType.CT_ComboBox, option, new QSize(contentWidth, hint.height()), this);
                return new QSize(Math.min(hint.width(), minimum.width()), hint.height());
            }

            @Override
            protected void initStyleOption(QStyleOptionComboBox option) {
                super.initStyleOption(option);

                QRect textRect = style().subControlRect(QStyle.ComplexControl.CC_ComboBox, option, QStyle.SubControl.SC_ComboBoxEditField, this);
                int available = textRect.width() - (option.currentIcon().isNull() ? 0 : iconSize().width() + ICON_TEXT_GAP);
                if (available > 0) {
                    option.setCurrentText(fontMetrics().elidedText(option.currentText(), Qt.TextElideMode.ElideRight, available));
                }
            }
        };
    }

    @Override
    protected void initialize(QComboBox component) {
        component.currentIndexChanged.connect(index -> {
            // emptying the widget reports a choice of nothing, which would take the stored index with it
            if (myRebuilding) {
                return;
            }

            mySelectedIndex = index;

            if (myFireListeners) {
                getListenerDispatcher(ValueComponentEvent.class)
                    .onEvent(new ValueComponentEvent(this, getValue(), DesktopQtCurrentInput.current(component)));
            }
        });

        rebuild(component);
    }

    private void rebuild(QComboBox component) {
        myRebuilding = true;
        try {
            component.clear();

            for (int i = 0; i < myModel.getSize(); i++) {
                E element = myModel.get(i);

                DesktopQtTextItemPresentation presentation = new DesktopQtTextItemPresentation();

                myRenderer.render(presentation, RenderItem.of(element, i == mySelectedIndex));

                Image image = presentation.getImage();
                if (image instanceof DesktopQtImage) {
                    component.addItem(toHostQIcon(image), presentation.toString());
                }
                else {
                    component.addItem(presentation.toString());
                }
            }

            int count = component.count();
            if (count > 0 && mySelectedIndex >= count) {
                mySelectedIndex = count - 1;
            }

            component.setCurrentIndex(mySelectedIndex >= 0 && mySelectedIndex < count ? mySelectedIndex : -1);
            component.updateGeometry();
        }
        finally {
            myRebuilding = false;
        }
    }

    private void rebuildIfBound() {
        QComboBox component = myComponent;
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

        QComboBox component = myComponent;
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
    public void setValueByIndex(int index) {
        setValueByIndex(index, true);
    }

    private void setValueByIndex(int index, boolean fireListeners) {
        int oldIndex = mySelectedIndex;
        mySelectedIndex = index;

        QComboBox component = myComponent;
        if (component == null) {
            if (fireListeners && oldIndex != index) {
                getListenerDispatcher(ValueComponentEvent.class).onEvent(new ValueComponentEvent(this, getValue()));
            }
            return;
        }

        // the widget answers a set index with the same signal a click raises, so the guard is what keeps a
        // programmatic set - a reset writing the stored value back - from reading as a user's choice
        myFireListeners = fireListeners;
        try {
            component.setCurrentIndex(index);
        }
        finally {
            myFireListeners = true;
        }
    }

    @Override
    public @Nullable E getValue() {
        return mySelectedIndex >= 0 && mySelectedIndex < myModel.getSize() ? myModel.get(mySelectedIndex) : null;
    }

    @Override
    @RequiredUIAccess
    public void setValue(E value, boolean fireListeners) {
        setValueByIndex(myModel.indexOf(value), fireListeners);
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
