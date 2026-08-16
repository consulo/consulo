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
package consulo.desktop.qt.codeInsight.intention;

import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.Editor;
import consulo.ide.impl.idea.codeInsight.intention.impl.IntentionListStep;
import consulo.language.editor.internal.intention.CachedIntentions;
import consulo.language.editor.internal.intention.IntentionsUI;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.ListPopupStep;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
@ServiceImpl
@Singleton
public class DesktopQtIntentionsUIImpl extends IntentionsUI {
    private volatile @Nullable JBPopup myLastPopup;

    @Inject
    public DesktopQtIntentionsUIImpl(Project project) {
        super(project);
    }

    @Override
    @RequiredUIAccess
    public void update(CachedIntentions cachedIntentions, boolean actionsChanged) {
        Editor editor = cachedIntentions.getEditor();
        if (editor == null || !actionsChanged) {
            return;
        }

        hide();

        if (editor.getSettings().isShowIntentionBulb()
            && editor.getCaretModel().getCaretCount() == 1
            && cachedIntentions.showBulb()) {
            showHint(cachedIntentions.getFile(), editor, cachedIntentions);
        }
    }

    @Override
    @RequiredUIAccess
    public void hide() {
        JBPopup popup = myLastPopup;
        myLastPopup = null;

        if (popup != null && !popup.isDisposed()) {
            popup.cancel();
        }
    }

    @Override
    @RequiredUIAccess
    public void showHint(PsiFile file, Editor editor, CachedIntentions cachedIntentions) {
        // the bulb of the other frontends hangs off the caret, and the qt editor is no
        // CaretPixelLocationProvider yet - until it is, there is nothing to place it against
    }

    @Override
    @RequiredUIAccess
    public void showPopup(PsiFile file, Editor editor, CachedIntentions cachedIntentions) {
        hide();

        ListPopupStep step =
            new IntentionListStep(null, editor, file, cachedIntentions.getProject(), cachedIntentions);

        ListPopup popup = JBPopupFactory.getInstance().createListPopup(cachedIntentions.getProject(), step);
        myLastPopup = popup;

        editor.showPopupInBestPositionFor(popup);
    }
}
