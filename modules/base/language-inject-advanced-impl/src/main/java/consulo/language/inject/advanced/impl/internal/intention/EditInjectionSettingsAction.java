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

package consulo.language.inject.advanced.impl.internal.intention;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.configurable.internal.ShowConfigurableService;
import consulo.fileEditor.util.FileContentUtil;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.IntentionMetaData;
import consulo.language.editor.intention.LowPriorityAction;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.inject.advanced.LanguageInjectionSupport;
import consulo.language.inject.advanced.impl.internal.InjectionsSettingsUI;
import consulo.language.inject.impl.internal.InjectedLanguageUtil;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.Collections;

/**
 * @author Gregory.Shrago
 */
@ExtensionImpl
@IntentionMetaData(ignoreId = "platform.inject.language", fileExtensions = "txt", categories = "Language Injection")
public class EditInjectionSettingsAction implements IntentionAction, LowPriorityAction {
    @Override
    @Nonnull
    public String getText() {
        return "Edit Injection Settings";
    }

    @Override
    public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
        int offset = editor.getCaretModel().getOffset();
        PsiFile psiFile = InjectedLanguageUtil.findInjectedPsiNoCommit(file, offset);
        if (psiFile == null) {
            return false;
        }
        LanguageInjectionSupport support = psiFile.getUserData(LanguageInjectionSupport.SETTINGS_EDITOR);
        return support != null;
    }

    @Override
    public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        project.getApplication().runReadAction(() -> invokeImpl(project, editor, file));
    }

    private static void invokeImpl(Project project, Editor editor, PsiFile file) {
        PsiFile psiFile = InjectedLanguageUtil.findInjectedPsiNoCommit(file, editor.getCaretModel().getOffset());
        if (psiFile == null) {
            return;
        }
        PsiLanguageInjectionHost host = InjectedLanguageManager.getInstance(project).getInjectionHost(psiFile);
        if (host == null) {
            return;
        }
        LanguageInjectionSupport support = psiFile.getUserData(LanguageInjectionSupport.SETTINGS_EDITOR);
        if (support == null) {
            return;
        }
        try {
            if (!support.editInjectionInPlace(host)) {
                project.getApplication().getInstance(ShowConfigurableService.class).showAndSelect(project, InjectionsSettingsUI.class);
            }
        }
        finally {
            FileContentUtil.reparseFiles(project, Collections.<VirtualFile>emptyList(), true);
        }
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }
}