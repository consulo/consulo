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
package consulo.language.editor.impl.internal.documentation;

import consulo.language.editor.hint.HintManager;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;

/**
 * @author VISTALL
 * @since 2026-10-09
 */
public final class DocumentationOpenInToolWindowAction extends DumbAwareAction implements AnActionWithSyncUpdate, HintManager.ActionToIgnore {
    private final DocumentationBrowser myBrowser;
    private final Runnable myAfterOpen;

    public DocumentationOpenInToolWindowAction(DocumentationBrowser browser, Runnable afterOpen) {
        super(CodeInsightLocalize.javadocActionOpenInToolWindow(), LocalizeValue.empty(), PlatformIconGroup.toolwindowsDocumentation());
        myBrowser = browser;
        myAfterOpen = afterOpen;
    }

    @Override
    public void update(AnActionEvent e) {
        DocumentationPage page = myBrowser.getPage();
        e.getPresentation().setEnabled(page != null && page.element() != null);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        DocumentationPage page = myBrowser.getPage();
        SmartPsiElementPointer<? extends PsiElement> element = page == null ? null : page.element();
        if (element == null) {
            return;
        }

        DocumentationToolWindowManager toolWindowManager = myBrowser.getProject().getInstance(DocumentationToolWindowManager.class);
        if (toolWindowManager.show(new DocumentationElementSource(element, null), false)) {
            myAfterOpen.run();
        }
    }
}
