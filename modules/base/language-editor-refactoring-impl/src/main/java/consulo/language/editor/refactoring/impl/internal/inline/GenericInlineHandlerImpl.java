/*
 * Copyright 2013-2025 consulo.io
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
package consulo.language.editor.refactoring.impl.internal.inline;

import consulo.application.ReadAction;
import consulo.codeEditor.Editor;
import consulo.language.Language;
import consulo.language.editor.TargetElementUtil;
import consulo.language.editor.refactoring.inline.GenericInlineHandler;
import consulo.language.editor.refactoring.inline.InlineHandler;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.ui.ConflictsDialogDescriptor;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.search.ReferencesSearch;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.CommandProcessor;
import consulo.usage.UsageInfo;
import consulo.util.collection.MultiMap;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SimpleReference;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2025-01-09
 */
public class GenericInlineHandlerImpl {
    /**
     * @return completes with whether the handler takes the element - known once its settings are; the inline itself goes
     * on after that
     */
    @RequiredUIAccess
    public static CompletableFuture<Boolean> invoke(PsiElement element, @Nullable Editor editor, InlineHandler languageSpecific) {
        PsiReference invocationReference = editor != null ? TargetElementUtil.findReference(editor) : null;
        return languageSpecific.prepareInlineElementAsync(element, editor, invocationReference != null).thenApply(settings -> {
            if (settings == null) {
                return Boolean.FALSE;
            }
            if (settings != InlineHandler.Settings.CANNOT_INLINE_SETTINGS) {
                inline(element, invocationReference, languageSpecific, settings);
            }
            return Boolean.TRUE;
        });
    }

    @RequiredUIAccess
    private static void inline(
        PsiElement element,
        @Nullable PsiReference invocationReference,
        InlineHandler languageSpecific,
        InlineHandler.Settings settings
    ) {
        Project project = element.getProject();
        SimpleReference<Collection<? extends PsiReference>> references = SimpleReference.create();
        SimpleReference<Map<Language, InlineHandler.Inliner>> inliners = SimpleReference.create();
        Coroutine.first(UIAction.<@Nullable Object, @Nullable Collection<? extends PsiReference>>await(
                ignored -> findReferences(element, invocationReference, settings)
            ))
            .then(UIAction.<@Nullable Collection<? extends PsiReference>, Boolean>await(allReferences -> {
                if (allReferences == null) {
                    return CompletableFuture.completedFuture(Boolean.FALSE);
                }
                references.set(allReferences);
                inliners.set(GenericInlineHandler.initializeInliners(element, settings, allReferences));

                MultiMap<PsiElement, LocalizeValue> conflicts = new MultiMap<>();
                for (PsiReference reference : allReferences) {
                    GenericInlineHandler.collectConflicts(reference, element, inliners.get(), conflicts);
                }
                return conflicts.isEmpty()
                    ? CompletableFuture.completedFuture(Boolean.TRUE)
                    : new ConflictsDialogDescriptor(project, conflicts).showAsync();
            }))
            .then(UIAction.apply(proceed -> {
                if (proceed) {
                    performInline(element, languageSpecific, settings, references.get(), inliners.get());
                }
                return null;
            }))
            .runAsync(CoroutineScope.of(project.coroutineContext()), null);
    }

    @RequiredUIAccess
    private static CompletableFuture<@Nullable Collection<? extends PsiReference>> findReferences(
        PsiElement element,
        @Nullable PsiReference invocationReference,
        InlineHandler.Settings settings
    ) {
        if (settings.isOnlyOneReferenceToInline()) {
            return CompletableFuture.completedFuture(Collections.singleton(invocationReference));
        }

        SimpleReference<Collection<? extends PsiReference>> usagesRef = new SimpleReference<>();
        return CommonRefactoringUtil.runWithProgress(
            element.getProject(),
            LocalizeValue.localizeTODO("Find Usages"),
            () -> usagesRef.set(ReadAction.compute(() -> ReferencesSearch.search(element).findAll()))
        ).thenApply(completed -> completed ? usagesRef.get() : null);
    }

    @RequiredUIAccess
    private static void performInline(
        PsiElement element,
        InlineHandler languageSpecific,
        InlineHandler.Settings settings,
        Collection<? extends PsiReference> allReferences,
        Map<Language, InlineHandler.Inliner> inliners
    ) {
        Project project = element.getProject();

        Set<PsiElement> elements = new HashSet<>();
        for (PsiReference reference : allReferences) {
            PsiElement refElement = reference.getElement();
            if (refElement != null) {
                elements.add(refElement);
            }
        }
        if (!settings.isOnlyOneReferenceToInline()) {
            elements.add(element);
        }

        if (!CommonRefactoringUtil.checkReadOnlyStatusRecursively(project, elements, true)) {
            return;
        }
        project.getApplication().runWriteAction(() -> {
            String subj = element instanceof PsiNamedElement namedElement ? namedElement.getName() : "element";

            CommandProcessor.getInstance().newCommand()
                .project(project)
                .name(RefactoringLocalize.inlineCommand(StringUtil.notNullize(subj, "<nameless>")))
                .run(() -> {
                    PsiReference[] references = GenericInlineHandler.sortDepthFirstRightLeftOrder(allReferences);

                    UsageInfo[] usages = new UsageInfo[references.length];
                    for (int i = 0; i < references.length; i++) {
                        usages[i] = new UsageInfo(references[i]);
                    }

                    for (UsageInfo usage : usages) {
                        GenericInlineHandler.inlineReference(usage, element, inliners);
                    }

                    if (!settings.isOnlyOneReferenceToInline()) {
                        languageSpecific.removeDefinition(element, settings);
                    }
                });
        });
    }
}
