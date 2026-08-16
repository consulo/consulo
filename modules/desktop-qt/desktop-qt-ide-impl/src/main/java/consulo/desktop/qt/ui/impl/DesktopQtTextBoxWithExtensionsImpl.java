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
import consulo.ui.TextBoxWithExtensions;
import consulo.ui.event.ClickEvent;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.image.Image;
import io.qt.gui.QAction;
import io.qt.gui.QIcon;
import io.qt.widgets.QLineEdit;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
@SuppressWarnings("deprecation")
public class DesktopQtTextBoxWithExtensionsImpl extends DesktopQtTextBoxImpl implements TextBoxWithExtensions, DesktopQtIconOwner {
    private final List<Extension> myExtensions = new ArrayList<>();

    private final List<QAction> myActions = new ArrayList<>();

    public DesktopQtTextBoxWithExtensionsImpl(@Nullable String text) {
        super(text);
    }

    @Override
    protected void initialize(QLineEdit component) {
        super.initialize(component);

        updateExtensions();
    }

    @Override
    public void refreshIcons() {
        updateExtensions();
    }

    /**
     * Every extension is rewritten at once rather than the new one being appended, because a line edit keeps its
     * actions in the order they were added and the api may place one before the text after one was placed after it.
     */
    private void updateExtensions() {
        QLineEdit component = myComponent;
        if (component == null) {
            return;
        }

        for (QAction action : myActions) {
            component.removeAction(action);
        }
        myActions.clear();

        for (Extension extension : myExtensions) {
            QAction action = component.addAction(
                toQIcon(extension),
                extension.isLeft() ? QLineEdit.ActionPosition.LeadingPosition : QLineEdit.ActionPosition.TrailingPosition
            );

            ComponentEventListener<consulo.ui.Component, ClickEvent> clickListener = extension.getClickListener();
            // an extension without a listener is a plain marker - a magnifier of a search field - and stays
            // enabled, since a disabled action is drawn greyed out by qt
            if (clickListener != null) {
                action.triggered.connect(() -> clickListener.onEvent(new ClickEvent(this, null)));
            }

            myActions.add(action);
        }
    }

    /**
     * The hovered icon of the api is the {@code Active} mode of a qt icon, which is what a line edit asks for while
     * the pointer is over the action - so qt swaps the two on its own instead of a filter watching for the hover.
     */
    private static QIcon toQIcon(Extension extension) {
        Image icon = extension.getIcon();
        if (!(icon instanceof DesktopQtImage qtIcon)) {
            return new QIcon();
        }

        QIcon result = new QIcon(qtIcon.toQPixmap());

        Image hoveredIcon = extension.getHoveredIcon();
        if (hoveredIcon != icon && hoveredIcon instanceof DesktopQtImage qtHoveredIcon) {
            result.addPixmap(qtHoveredIcon.toQPixmap(), QIcon.Mode.Active);
        }

        return result;
    }

    @Override
    public TextBoxWithExtensions setExtensions(Extension... extensions) {
        myExtensions.clear();
        myExtensions.addAll(List.of(extensions));

        updateExtensions();
        return this;
    }

    @Override
    public TextBoxWithExtensions addFirstExtension(Extension extension) {
        myExtensions.add(0, extension);

        updateExtensions();
        return this;
    }

    @Override
    public TextBoxWithExtensions addLastExtension(Extension extension) {
        myExtensions.add(extension);

        updateExtensions();
        return this;
    }
}
