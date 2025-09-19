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
import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;
import consulo.fileEditor.util.FileContentUtil;
import consulo.language.Language;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.IntentionMetaData;
import consulo.language.editor.intention.LowPriorityAction;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.inject.advanced.Configuration;
import consulo.language.inject.advanced.LanguageInjectionSupport;
import consulo.language.inject.advanced.TemporaryPlacesRegistry;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Dmitry Avdeev
 */
@ExtensionImpl
@IntentionMetaData(ignoreId = "platform.inject.language", fileExtensions = "txt", categories = "Language Injection")
public class UnInjectLanguageAction implements IntentionAction, LowPriorityAction {

    @Override
    @Nonnull
    public String getText() {
        return "Un-inject Language";
    }

    @Override
    public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
        int offset = editor.getCaretModel().getOffset();
        PsiFile psiFile = InjectedLanguageManager.getInstance(project).findInjectedPsiNoCommit(file, offset);
        if (psiFile == null) {
            return false;
        }
        LanguageInjectionSupport support = psiFile.getUserData(LanguageInjectionSupport.INJECTOR_SUPPORT);
        return support != null;
    }

    @Override
    public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        ApplicationManager.getApplication().runReadAction(() -> invokeImpl(project, editor, file));
    }

    private static void invokeImpl(Project project, Editor editor, PsiFile file) {
        InjectedLanguageManager manager = InjectedLanguageManager.getInstance(project);
        PsiFile psiFile = manager.findInjectedPsiNoCommit(file, editor.getCaretModel().getOffset());
        if (psiFile == null) {
            return;
        }
        PsiLanguageInjectionHost host = manager.getInjectionHost(psiFile);
        if (host == null) {
            return;
        }
        LanguageInjectionSupport support = psiFile.getUserData(LanguageInjectionSupport.INJECTOR_SUPPORT);
        if (support == null) {
            return;
        }
        try {
            if (psiFile.getUserData(LanguageInjectionSupport.TEMPORARY_INJECTED_LANGUAGE) != null) {
                // temporary injection
                TemporaryPlacesRegistry temporaryPlacesRegistry = TemporaryPlacesRegistry.getInstance(project);
                for (PsiLanguageInjectionHost.Shred shred : manager.getShreds(psiFile)) {
                    if (temporaryPlacesRegistry.removeHostWithUndo(project, shred.getHost())) {
                        break;
                    }
                }
            }
            else if (!support.removeInjectionInPlace(host)) {
                defaultFunctionalityWorked(host);
            }
        }
        finally {
            FileContentUtil.reparseFiles(project, Collections.<VirtualFile>emptyList(), true);
        }
    }

    private static boolean defaultFunctionalityWorked(PsiLanguageInjectionHost host) {
        Set<String> languages = new HashSet<String>();
        List<Pair<PsiElement, TextRange>> files = InjectedLanguageManager.getInstance(host.getProject()).getInjectedPsiFiles(host);
        if (files == null) {
            return false;
        }
        for (Pair<PsiElement, TextRange> pair : files) {
            for (Language lang = pair.first.getLanguage(); lang != null; lang = lang.getBaseLanguage()) {
                languages.add(lang.getID());
            }
        }
        // todo there is a problem: host i.e. literal expression is confused with "target" i.e. parameter
        // todo therefore this part doesn't work for java
        return Configuration.getProjectInstance(host.getProject()).setHostInjectionEnabled(host, languages, false);
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }
}
