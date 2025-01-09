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

import consulo.application.progress.ProgressManager;
import consulo.codeEditor.Editor;
import consulo.language.Language;
import consulo.language.editor.TargetElementUtil;
import consulo.language.editor.refactoring.BaseRefactoringProcessor;
import consulo.language.editor.refactoring.inline.GenericInlineHandler;
import consulo.language.editor.refactoring.inline.InlineHandler;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.ui.ConflictsDialog;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.search.ReferencesSearch;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.CommandProcessor;
import consulo.usage.UsageInfo;
import consulo.util.collection.MultiMap;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2025-01-09
 */
public class GenericInlineHandlerImpl {
    @RequiredUIAccess
    public static boolean invoke(final PsiElement element, @Nullable Editor editor, final InlineHandler languageSpecific) {
        final PsiReference invocationReference = editor != null ? TargetElementUtil.findReference(editor) : null;
        final InlineHandler.Settings settings = languageSpecific.prepareInlineElement(element, editor, invocationReference != null);
        if (settings == null || settings == InlineHandler.Settings.CANNOT_INLINE_SETTINGS) {
            return settings != null;
        }

        final Collection<? extends PsiReference> allReferences;

        if (settings.isOnlyOneReferenceToInline()) {
            allReferences = Collections.singleton(invocationReference);
        }
        else {
            final SimpleReference<Collection<? extends PsiReference>> usagesRef = new SimpleReference<>();
            ProgressManager.getInstance().runProcessWithProgressSynchronously(
                () -> usagesRef.set(ReferencesSearch.search(element).findAll()),
                "Find Usages",
                false,
                element.getProject()
            );
            allReferences = usagesRef.get();
        }

        final MultiMap<PsiElement, String> conflicts = new MultiMap<>();
        final Map<Language, InlineHandler.Inliner> inliners = GenericInlineHandler.initializeInliners(element, settings, allReferences);

        for (PsiReference reference : allReferences) {
            GenericInlineHandler.collectConflicts(reference, element, inliners, conflicts);
        }

        final Project project = element.getProject();
        if (!conflicts.isEmpty()) {
            if (project.getApplication().isUnitTestMode()) {
                throw new BaseRefactoringProcessor.ConflictsInTestsException(conflicts.values());
            }
            else {
                final ConflictsDialog conflictsDialog = new ConflictsDialog(project, conflicts);
                conflictsDialog.show();
                if (!conflictsDialog.isOK()) {
                    return true;
                }
            }
        }

        HashSet<PsiElement> elements = new HashSet<>();
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
            return true;
        }
        project.getApplication().runWriteAction(() -> {
            final String subj = element instanceof PsiNamedElement namedElement ? namedElement.getName() : "element";

            CommandProcessor.getInstance().newCommand()
                .project(project)
                .name(RefactoringLocalize.inlineCommand(StringUtil.notNullize(subj, "<nameless>")))
                .run(() -> {
                    final PsiReference[] references = GenericInlineHandler.sortDepthFirstRightLeftOrder(allReferences);

                    final UsageInfo[] usages = new UsageInfo[references.length];
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
        return true;
    }
}
