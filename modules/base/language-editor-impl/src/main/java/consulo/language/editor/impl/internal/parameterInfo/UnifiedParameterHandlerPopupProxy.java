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
package consulo.language.editor.impl.internal.parameterInfo;

import consulo.codeEditor.Editor;
import consulo.language.editor.completion.lookup.Lookup;
import consulo.ui.LightPopup;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;
import consulo.language.editor.internal.parameterInfo.ParameterHandlerPopupProxy;
import consulo.language.editor.internal.parameterInfo.ParameterInfoAnchor;
import consulo.language.editor.internal.parameterInfo.ParameterInfoModel;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
public class UnifiedParameterHandlerPopupProxy implements ParameterHandlerPopupProxy {
    private final Editor myEditor;
    private @Nullable LightPopup myPopup;
    private @Nullable UnifiedCaretAnchor myAnchor;

    public UnifiedParameterHandlerPopupProxy(Editor editor) {
        myEditor = editor;
    }

    @Override
    public boolean isVisible() {
        return myPopup != null;
    }

    @Override
    @RequiredUIAccess
    public void show(ParameterInfoModel model, @Nullable ParameterInfoAnchor anchor, boolean requestFocus, boolean hideByTextChange) {
        hide();

        LightPopup popup = UnifiedParameterInfoContent.createPopup(model);
        popup.addCloseListener(event -> {
            if (myPopup == popup) {
                myPopup = null;
                myAnchor = null;
            }
        });

        UnifiedCaretAnchor caretAnchor = new UnifiedCaretAnchor(myEditor, popup);

        myPopup = popup;
        myAnchor = caretAnchor;

        caretAnchor.relocate();
    }

    @Override
    @RequiredUIAccess
    public void update(ParameterInfoModel model, @Nullable ParameterInfoAnchor anchor) {
        LightPopup popup = myPopup;
        UnifiedCaretAnchor caretAnchor = myAnchor;
        if (popup == null || caretAnchor == null) {
            return;
        }

        popup.setContent(UnifiedParameterInfoContent.create(model));
        caretAnchor.relocate();
    }

    @Override
    public void adjustForLookup(Lookup lookup) {
    }

    @Override
    @RequiredUIAccess
    public void hide() {
        LightPopup popup = myPopup;
        myPopup = null;
        myAnchor = null;
        if (popup != null) {
            popup.close();
        }
    }
}
