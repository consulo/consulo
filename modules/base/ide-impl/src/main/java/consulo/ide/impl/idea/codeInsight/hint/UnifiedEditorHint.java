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
package consulo.ide.impl.idea.codeInsight.hint;

import consulo.codeEditor.Editor;
import consulo.codeEditor.event.CaretEvent;
import consulo.codeEditor.event.CaretListener;
import consulo.codeEditor.internal.CaretPixelLocationProvider;
import consulo.codeEditor.internal.CaretPixelLocationProvider.CaretPixelLocation;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.language.editor.inject.EditorWindow;
import consulo.localize.LocalizeValue;
import consulo.ui.LightPopup;
import consulo.ui.PopupOptions;
import consulo.ui.PopupPosition;
import consulo.ui.Toast;
import consulo.ui.ToastStyle;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.util.TraverseUtil;

import java.awt.Rectangle;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-11-10
 */
final class UnifiedEditorHint implements Disposable {
    private final Editor myEditor;
    private final LightPopup myPopup;
    private final Consumer<UnifiedEditorHint> myOnHide;

    private boolean myHidden;

    static boolean isUnifiedEditor(Editor editor) {
        return editor.getComponent().getRootPane() == null && TraverseUtil.getWindowAncestor(editor.getContentUIComponent()) != null;
    }

    @RequiredUIAccess
    UnifiedEditorHint(Editor editor, LocalizeValue text, ToastStyle style, Consumer<UnifiedEditorHint> onHide) {
        myEditor = editor instanceof EditorWindow editorWindow ? editorWindow.getDelegate() : editor;
        myOnHide = onHide;

        Toast toast = Toast.create();
        toast.setText(text);
        toast.addStyle(style);

        myPopup = LightPopup.create(PopupOptions.builder().position(PopupPosition.TOP).disableRequestFocus().build());
        myPopup.setContent(toast);
        myPopup.addCloseListener(event -> hide());

        myEditor.getCaretModel().addCaretListener(new CaretListener() {
            @Override
            public void caretPositionChanged(CaretEvent event) {
                hide();
            }
        }, this);

        myEditor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(DocumentEvent event) {
                hide();
            }
        }, this);

        myEditor.getScrollingModel().addVisibleAreaListener(event -> {
            Rectangle oldRectangle = event.getOldRectangle();
            if (oldRectangle != null && oldRectangle.y != event.getNewRectangle().y) {
                hide();
            }
        }, this);
    }

    @RequiredUIAccess
    void show() {
        CaretPixelLocation location = myEditor instanceof CaretPixelLocationProvider provider ? provider.getCaretPixelLocation() : null;
        if (location == null) {
            myPopup.showAt(myEditor.getContentUIComponent(), 0, 0, 0);
        }
        else {
            myPopup.showAt(myEditor.getContentUIComponent(), location.x(), location.y(), location.height());
        }
    }

    Editor getEditor() {
        return myEditor;
    }

    @RequiredUIAccess
    void hide() {
        if (myHidden) {
            return;
        }
        myHidden = true;

        Disposer.dispose(this);
        myPopup.close();
        myOnHide.accept(this);
    }

    @Override
    public void dispose() {
    }
}
