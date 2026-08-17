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
package consulo.desktop.qt.editor.impl;

import consulo.desktop.qt.ui.impl.QtComponentDelegate;
import consulo.language.editor.impl.internal.markup.EditorMarkupModel;
import io.qt.widgets.QHBoxLayout;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

/**
 * The editor and the error strip side by side. The strip is a sibling of the scroll area rather than something
 * inside it, so it stands after the scroll bar and outside the scrolling area - which is where the awt frontend
 * puts it, as {@code BorderLayout.EAST} of the editor panel.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtEditorComponent extends QtComponentDelegate<QWidget> {
    private final DesktopQtEditorImpl myEditor;

    private @Nullable DesktopQtEditorWidget mySurface;
    private @Nullable DesktopQtEditorErrorStripeWidget myErrorStripe;

    public DesktopQtEditorComponent(DesktopQtEditorImpl editor) {
        myEditor = editor;
    }

    public @Nullable DesktopQtEditorWidget getSurface() {
        return mySurface;
    }

    public @Nullable DesktopQtEditorErrorStripeWidget getErrorStripe() {
        return myErrorStripe;
    }

    @Override
    protected QWidget createQt(QWidget parent) {
        QWidget container = new QWidget(parent);

        mySurface = new DesktopQtEditorWidget(container, myEditor);
        myErrorStripe = new DesktopQtEditorErrorStripeWidget(container, myEditor);

        QHBoxLayout layout = new QHBoxLayout(container);
        layout.setContentsMargins(0, 0, 0, 0);
        layout.setSpacing(0);
        layout.addWidget(mySurface, 1);
        layout.addWidget(myErrorStripe);

        return container;
    }

    @Override
    protected void initialize(QWidget component) {
        super.initialize(component);

        DesktopQtEditorWidget surface = mySurface;
        DesktopQtEditorErrorStripeWidget errorStripe = myErrorStripe;

        if (surface == null || errorStripe == null) {
            return;
        }

        // the editor was asked for these before it had a widget to answer with, so the widget catches up here
        errorStripe.setStripeVisible(((EditorMarkupModel) myEditor.getMarkupModel()).isErrorStripeVisible());
        errorStripe.listenToMarkup();
        errorStripe.refresh();

        surface.updateSideAreas();
        surface.updateScrollRanges();
    }
}
