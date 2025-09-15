/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInsight.navigation;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.progress.ProgressManager;
import consulo.application.util.function.CommonProcessors;
import consulo.application.util.query.Query;
import consulo.codeEditor.Editor;
import consulo.content.scope.SearchScope;
import consulo.language.editor.CodeInsightBundle;
import consulo.language.editor.TargetElementUtil;
import consulo.language.editor.TargetElementUtilExtender;
import consulo.language.psi.PsiElement;
import consulo.language.psi.resolve.PsiElementProcessor;
import consulo.language.psi.resolve.PsiElementProcessorAdapter;
import consulo.language.psi.search.DefinitionsScopedSearch;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.util.collection.ArrayUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class ImplementationSearcher {
    public static final String SEARCHING_FOR_IMPLEMENTATIONS = CodeInsightBundle.message("searching.for.implementations");

    @Nullable
    PsiElement[] searchImplementations(Editor editor, PsiElement element, int offset) {
        boolean onRef = Application.get().runReadAction((Supplier<Boolean>) () -> {
            Set<String> flags = new HashSet<>(getFlags());
            flags.remove(TargetElementUtilExtender.REFERENCED_ELEMENT_ACCEPTED);
            flags.remove(TargetElementUtilExtender.LOOKUP_ITEM_ACCEPTED);

            return TargetElementUtil.findTargetElement(editor, flags, offset) == null;
        });
        return searchImplementations(
            element,
            editor,
            onRef && Application.get().runReadAction(
                (Supplier<Boolean>) () -> element == null || TargetElementUtil.includeSelfInGotoImplementation(element)
            ),
            onRef
        );
    }

    @Nullable
    public PsiElement[] searchImplementations(PsiElement element, Editor editor, boolean includeSelfAlways, boolean includeSelfIfNoOthers) {
        if (element == null) {
            return PsiElement.EMPTY_ARRAY;
        }
        PsiElement[] elements = searchDefinitions(element, editor);
        if (elements == null) {
            return null; //the search has been cancelled
        }
        if (elements.length > 0) {
            return filterElements(element, includeSelfAlways ? ArrayUtil.prepend(element, elements) : elements);
        }
        if (includeSelfAlways || includeSelfIfNoOthers) {
            return new PsiElement[]{element};
        }
        return PsiElement.EMPTY_ARRAY;
    }

    protected static SearchScope getSearchScope(PsiElement element, Editor editor) {
        return Application.get().runReadAction((Supplier<SearchScope>) () -> TargetElementUtil.getSearchScope(editor, element));
    }

    /**
     * @param element
     * @param editor
     * @return For the case the search has been cancelled
     */
    @Nullable
    protected PsiElement[] searchDefinitions(PsiElement element, Editor editor) {
        PsiElement[][] result = new PsiElement[1][];
        if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            try {
                result[0] = search(element, editor).toArray(PsiElement.EMPTY_ARRAY);
            }
            catch (IndexNotReadyException e) {
                dumbModeNotification(element);
                result[0] = null;
            }
        }, SEARCHING_FOR_IMPLEMENTATIONS, true, element.getProject())) {
            return null;
        }
        return result[0];
    }

    protected Query<PsiElement> search(PsiElement element, Editor editor) {
        return DefinitionsScopedSearch.search(element, getSearchScope(element, editor), isSearchDeep());
    }

    protected boolean isSearchDeep() {
        return true;
    }

    private static void dumbModeNotification(@Nonnull PsiElement element) {
        Project project = Application.get().runReadAction((Supplier<Project>) element::getProject);
        DumbService.getInstance(project).showDumbModeNotification("Implementation information isn't available while indices are built");
    }

    protected PsiElement[] filterElements(PsiElement element, PsiElement[] targetElements) {
        return targetElements;
    }

    @Nonnull
    public static Set<String> getFlags() {
        return TargetElementUtil.getDefinitionSearchFlags();
    }

    public static class FirstImplementationsSearcher extends ImplementationSearcher {
        @Override
        protected PsiElement[] searchDefinitions(PsiElement element, Editor editor) {
            if (canShowPopupWithOneItem(element)) {
                return new PsiElement[]{element};
            }

            PsiElementProcessor.FindElement<PsiElement> collectProcessor = new PsiElementProcessor.FindElement<>();
            PsiElement[] result = new PsiElement[1];
            if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
                @Override
                public void run() {
                    try {
                        search(element, editor).forEach(new PsiElementProcessorAdapter<>(collectProcessor) {
                            @Override
                            @RequiredReadAction
                            public boolean processInReadAction(PsiElement element) {
                                return !accept(element) || super.processInReadAction(element);
                            }
                        });
                        result[0] = collectProcessor.getFoundElement();
                    }
                    catch (IndexNotReadyException e) {
                        ImplementationSearcher.dumbModeNotification(element);
                        result[0] = null;
                    }
                }
            }, SEARCHING_FOR_IMPLEMENTATIONS, true, element.getProject())) {
                return null;
            }
            PsiElement foundElement = result[0];
            return foundElement != null ? new PsiElement[]{foundElement} : PsiElement.EMPTY_ARRAY;
        }

        protected boolean canShowPopupWithOneItem(PsiElement element) {
            return accept(element);
        }

        protected boolean accept(PsiElement element) {
            return true;
        }
    }

    public abstract static class BackgroundableImplementationSearcher extends ImplementationSearcher {
        @Override
        protected PsiElement[] searchDefinitions(PsiElement element, Editor editor) {
            CommonProcessors.CollectProcessor<PsiElement> processor = new CommonProcessors.CollectProcessor<>() {
                @Override
                public boolean process(PsiElement element) {
                    processElement(element);
                    return super.process(element);
                }
            };
            try {
                search(element, editor).forEach(processor);
            }
            catch (IndexNotReadyException e) {
                ImplementationSearcher.dumbModeNotification(element);
                return null;
            }
            return processor.toArray(PsiElement.EMPTY_ARRAY);
        }

        protected abstract void processElement(PsiElement element);
    }
}