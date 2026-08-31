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

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.language.editor.impl.internal.hint.EditorMouseHoverPopupManager;
import consulo.language.editor.rawHighlight.HighlightInfo;
import jakarta.inject.Singleton;

/**
 * The hover documentation popup is not built for the unified frontends yet. The service still has to answer:
 * every action performed closes the hover hint through it, so an absent implementation costs a logged failure
 * per keystroke rather than a missing tooltip.
 *
 * @author VISTALL
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
public class UnifiedEditorMouseHoverPopupManager implements EditorMouseHoverPopupManager {
    @Override
    public void showInfoTooltip(Editor editor, HighlightInfo info, int offset, boolean requestFocus, boolean showImmediately) {
    }

    @Override
    public void handleMouseMoved(EditorMouseEvent e) {
    }

    @Override
    public void skipNextMovement() {
    }

    @Override
    public void cancelProcessingAndCloseHint() {
    }

    @Override
    public void cancelCurrentProcessing() {
    }
}
