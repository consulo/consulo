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
package consulo.codeEditor.impl.internal.popup;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorPopupHelper;
import consulo.codeEditor.internal.CaretPixelLocationProvider;
import consulo.codeEditor.internal.CaretPixelLocationProvider.CaretPixelLocation;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.internal.AnchoredPopup;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.popup.JBPopup;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-08
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
public class UnifiedEditorPopupHelperImpl implements EditorPopupHelper {
    @Override
    public RelativePoint guessBestPopupLocation(Editor editor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isBestPopupLocationVisible(Editor editor) {
        return caretLocation(editor) != null;
    }

    @Override
    @RequiredUIAccess
    public void showPopupInBestPositionFor(Editor editor, JBPopup popup) {
        if (!(popup instanceof AnchoredPopup anchoredPopup)) {
            popup.showCenteredInCurrentWindow(editor.getProject());
            return;
        }

        CaretPixelLocation location = caretLocation(editor);

        if (location == null) {
            anchoredPopup.showAtPoint(editor.getUIComponent(), 0, 0, 0);
        }
        else {
            anchoredPopup.showAtPoint(editor.getUIComponent(), location.x(), location.y(), location.height());
        }
    }

    private static @Nullable CaretPixelLocation caretLocation(Editor editor) {
        return editor instanceof CaretPixelLocationProvider provider ? provider.getCaretPixelLocation() : null;
    }
}
