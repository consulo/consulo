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
package consulo.language.editor.impl.internal.intention;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.WriteAction;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.Editor;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.inject.EditorWindow;
import consulo.language.editor.inject.InjectedEditorManager;
import consulo.language.editor.inspection.SuppressIntentionActionFromFix;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.IntentionActionDelegate;
import consulo.language.editor.intention.PsiElementBaseIntentionAction;
import consulo.language.impl.internal.psi.stub.StubTextInconsistencyException;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.CommandProcessor;
import consulo.util.lang.Pair;
import consulo.util.lang.ThreeState;
import org.jspecify.annotations.Nullable;

import java.util.function.BiPredicate;

/**
 * Runs an intention against the right file - host or injected - the way the alt-enter popup would.
 * Lives on the platform side so the intention list ui can be shared by every frontend.
 *
 * @author VISTALL
 * @since 2026-08-17
 */
public final class IntentionActionInvoker {
    private IntentionActionInvoker() {
    }

    @RequiredReadAction
    public static boolean availableFor(PsiFile psiFile, Editor editor, IntentionAction action) {
        if (!psiFile.isValid()) {
            return false;
        }

        try {
            Project project = psiFile.getProject();
            action = IntentionActionDelegate.unwrap(action);
            if (action instanceof SuppressIntentionActionFromFix suppressIntentionActionFromFix) {
                ThreeState shouldBeAppliedToInjectionHost = suppressIntentionActionFromFix.isShouldBeAppliedToInjectionHost();
                if (editor instanceof EditorWindow && shouldBeAppliedToInjectionHost == ThreeState.YES) {
                    return false;
                }
                if (!(editor instanceof EditorWindow) && shouldBeAppliedToInjectionHost == ThreeState.NO) {
                    return false;
                }
            }

            if (action instanceof PsiElementBaseIntentionAction psiAction) {
                if (!psiAction.checkFile(psiFile)) {
                    return false;
                }
                PsiElement leaf = psiFile.findElementAt(editor.getCaretModel().getOffset());
                if (leaf == null || !psiAction.isAvailable(project, editor, leaf)) {
                    return false;
                }
            }
            else if (!action.isAvailable(project, editor, psiFile)) {
                return false;
            }
        }
        catch (IndexNotReadyException e) {
            return false;
        }
        return true;
    }

    public static @Nullable Pair<PsiFile, Editor> chooseBetweenHostAndInjected(
        PsiFile hostFile,
        Editor hostEditor,
        @Nullable PsiFile injectedFile,
        BiPredicate<? super PsiFile, ? super Editor> predicate
    ) {
        Editor editorToApply = null;
        PsiFile fileToApply = null;

        Editor injectedEditor = null;
        if (injectedFile != null) {
            injectedEditor = InjectedEditorManager.getInstance(hostFile.getProject()).getInjectedEditorForInjectedFile(hostEditor, injectedFile);
            if (predicate.test(injectedFile, injectedEditor)) {
                editorToApply = injectedEditor;
                fileToApply = injectedFile;
            }
        }

        if (editorToApply == null && hostEditor != injectedEditor && predicate.test(hostFile, hostEditor)) {
            editorToApply = hostEditor;
            fileToApply = hostFile;
        }
        if (editorToApply == null) {
            return null;
        }
        return Pair.create(fileToApply, editorToApply);
    }

    public static @Nullable Pair<PsiFile, Editor> chooseFileForAction(
        PsiFile hostFile,
        @Nullable Editor hostEditor,
        IntentionAction action
    ) {
        if (hostEditor == null) {
            return Pair.create(hostFile, null);
        }

        PsiFile injectedFile = InjectedLanguageManager.getInstance(hostEditor.getProject()).findInjectedPsiNoCommit(hostFile, hostEditor.getCaretModel().getOffset());
        return chooseBetweenHostAndInjected(hostFile, hostEditor, injectedFile, (psiFile, editor) -> availableFor(psiFile, editor, action));
    }

    @RequiredUIAccess
    public static boolean chooseActionAndInvoke(
        PsiFile hostFile,
        @Nullable Editor hostEditor,
        IntentionAction action,
        String text
    ) {
        return chooseActionAndInvoke(hostFile, hostEditor, action, text, hostFile.getProject());
    }

    @RequiredUIAccess
    public static boolean chooseActionAndInvoke(
        PsiFile hostFile,
        @Nullable Editor hostEditor,
        IntentionAction action,
        String text,
        Project project
    ) {
        FeatureUsageTracker.getInstance().triggerFeatureUsed("codeassists.quickFix");

        FeatureUsageTracker.getInstance().getFixesStats().registerInvocation();

        PsiDocumentManager.getInstance(project).commitAllDocuments();

        Pair<PsiFile, Editor> pair = chooseFileForAction(hostFile, hostEditor, action);
        if (pair == null) {
            return false;
        }

        CommandProcessor.getInstance().newCommand()
            .project(project)
            .name(LocalizeValue.ofNullable(text))
            .run(() -> invokeIntention(action, pair.second, pair.first));

        checkPsiTextConsistency(hostFile);

        return true;
    }

    private static void checkPsiTextConsistency(PsiFile hostFile) {
        if (Registry.is("ide.check.stub.text.consistency") || Application.get().isUnitTestMode()) {
            if (hostFile.isValid()) {
                StubTextInconsistencyException.checkStubTextConsistency(hostFile);
            }
        }
    }

    private static void invokeIntention(IntentionAction action, @Nullable Editor editor, PsiFile file) {
        PsiElement elementToMakeWritable = action.getElementToMakeWritable(file);
        if (elementToMakeWritable != null && !FileModificationService.getInstance().preparePsiElementsForWrite(elementToMakeWritable)) {
            return;
        }

        Runnable r = () -> action.invoke(file.getProject(), editor, file);
        if (action.startInWriteAction()) {
            WriteAction.run(r::run);
        }
        else {
            r.run();
        }
    }
}
