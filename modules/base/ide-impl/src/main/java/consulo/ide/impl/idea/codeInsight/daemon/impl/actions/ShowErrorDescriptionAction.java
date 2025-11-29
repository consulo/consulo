/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInsight.daemon.impl.actions;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.ide.impl.idea.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl;
import consulo.ide.impl.idea.codeInsight.daemon.impl.ShowErrorDescriptionHandler;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.impl.action.BaseCodeInsightAction;
import consulo.language.editor.impl.internal.rawHighlight.HighlightInfoImpl;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.awt.accessibility.ScreenReader;
import consulo.ui.ex.internal.ActionManagerEx;
import jakarta.annotation.Nonnull;

import java.awt.event.KeyEvent;
import java.util.Objects;

@ActionImpl(id = "ShowErrorDescription")
public class ShowErrorDescriptionAction extends BaseCodeInsightAction implements DumbAware {
    private static int width;
    private static boolean shouldShowDescription = false;
    private static boolean descriptionShown = true;
    private boolean myRequestFocus = false;

    public ShowErrorDescriptionAction() {
        super(ActionLocalize.actionShowerrordescriptionText(), ActionLocalize.actionShowerrordescriptionDescription());
        setEnabledInModalContext(true);
    }

    @Nonnull
    @Override
    protected CodeInsightActionHandler getHandler() {
        return new ShowErrorDescriptionHandler(shouldShowDescription ? width : 0, myRequestFocus);
    }

    @Override
    @RequiredReadAction
    protected boolean isValidForFile(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
        return DaemonCodeAnalyzer.getInstance(project).isHighlightingAvailable(file) && isEnabledForFile(project, editor, file);
    }

    @RequiredReadAction
    private static boolean isEnabledForFile(Project project, Editor editor, PsiFile file) {
        DaemonCodeAnalyzer codeAnalyzer = DaemonCodeAnalyzer.getInstance(project);
        HighlightInfoImpl info =
            ((DaemonCodeAnalyzerImpl) codeAnalyzer).findHighlightByOffset(editor.getDocument(), editor.getCaretModel().getOffset(), false);
        return info != null && info.getDescription() != LocalizeValue.empty();
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        // The tooltip gets the focus if using a screen reader and invocation through a keyboard shortcut.
        myRequestFocus = ScreenReader.isActive() && (e.getInputEvent() instanceof KeyEvent);

        changeState();

        super.actionPerformed(e);
    }

    private static void changeState() {
        if (Objects.equals(ActionManagerEx.getInstanceEx().getPrevPreformedActionId(), IdeActions.ACTION_SHOW_ERROR_DESCRIPTION)) {
            shouldShowDescription = descriptionShown;
        }
        else {
            shouldShowDescription = false;
            descriptionShown = true;
        }
    }

    public static void rememberCurrentWidth(int currentWidth) {
        width = currentWidth;
        descriptionShown = !shouldShowDescription;
    }
}
