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
import io.qt.widgets.QWidget;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtEditorComponent extends QtComponentDelegate<DesktopQtEditorWidget> {
    private final DesktopQtEditorImpl myEditor;

    public DesktopQtEditorComponent(DesktopQtEditorImpl editor) {
        myEditor = editor;
    }

    @Override
    protected DesktopQtEditorWidget createQt(QWidget parent) {
        return new DesktopQtEditorWidget(parent, myEditor);
    }

    @Override
    protected void initialize(DesktopQtEditorWidget component) {
        super.initialize(component);

        // the editor was asked for these before it had a widget to answer with, so the widget catches up here
        component.getErrorStripe().setStripeVisible(((EditorMarkupModel) myEditor.getMarkupModel()).isErrorStripeVisible());
        component.getErrorStripe().listenToMarkup();
        component.getStatusPanel().refresh();

        component.updateSideAreas();
        component.updateScrollRanges();
    }
}
