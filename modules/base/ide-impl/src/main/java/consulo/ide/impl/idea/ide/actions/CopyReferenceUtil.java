// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorColors;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributes;
import consulo.dataContext.DataContext;
import consulo.language.editor.IdentifierUtil;
import consulo.language.editor.QualifiedNameProviderUtil;
import consulo.language.editor.TargetElementUtil;
import consulo.language.editor.highlight.HighlightManager;
import consulo.language.psi.*;
import consulo.project.Project;
import consulo.project.ui.internal.StatusBarEx;
import consulo.project.ui.wm.WindowManager;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CopyReferenceUtil {
    static void highlight(Editor editor, Project project, List<? extends PsiElement> elements) {
        TextAttributes attributes =
            EditorColorsManager.getInstance().getGlobalScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);

        HighlightManager highlightManager = HighlightManager.getInstance(project);
        if (elements.size() == 1 && editor != null && project != null) {
            PsiElement element = elements.get(0);
            PsiElement nameIdentifier = IdentifierUtil.getNameIdentifier(element);
            if (nameIdentifier != null) {
                highlightManager.addOccurrenceHighlights(editor, new PsiElement[]{nameIdentifier}, attributes, true, null);
            }
            else {
                PsiReference reference = TargetElementUtil.findReference(editor, editor.getCaretModel().getOffset());
                if (reference != null) {
                    highlightManager.addOccurrenceHighlights(editor, new PsiReference[]{reference}, attributes, true, null);
                }
                else if (element != PsiDocumentManager.getInstance(project).getCachedPsiFile(editor.getDocument())) {
                    highlightManager.addOccurrenceHighlights(editor, new PsiElement[]{element}, attributes, true, null);
                }
            }
        }
    }

    @Nonnull
    @RequiredReadAction
    static List<PsiElement> getElementsToCopy(@Nullable Editor editor, DataContext dataContext) {
        List<PsiElement> elements = new ArrayList<>();
        if (editor != null) {
            PsiReference reference = TargetElementUtil.findReference(editor);
            if (reference != null) {
                ContainerUtil.addIfNotNull(elements, reference.getElement());
            }
        }

        if (elements.isEmpty()) {
            PsiElement[] psiElements = dataContext.getData(PsiElement.KEY_OF_ARRAY);
            if (psiElements != null) {
                Collections.addAll(elements, psiElements);
            }
        }

        if (elements.isEmpty()) {
            ContainerUtil.addIfNotNull(elements, dataContext.getData(PsiElement.KEY));
        }

        if (elements.isEmpty() && editor == null) {
            Project project = dataContext.getData(Project.KEY);
            VirtualFile[] files = dataContext.getData(VirtualFile.KEY_OF_ARRAY);
            if (project != null && files != null) {
                for (VirtualFile file : files) {
                    ContainerUtil.addIfNotNull(elements, PsiManager.getInstance(project).findFile(file));
                }
            }
        }

        return ContainerUtil.mapNotNull(
            elements,
            element -> element instanceof PsiFile psiFile && !psiFile.getViewProvider().isPhysical() ? null : adjustElement(element)
        );
    }

    static PsiElement adjustElement(PsiElement element) {
        PsiElement adjustedElement = QualifiedNameProviderUtil.adjustElementToCopy(element);
        return adjustedElement != null ? adjustedElement : element;
    }

    static void setStatusBarText(Project project, String message) {
        if (project != null) {
            StatusBarEx statusBar = (StatusBarEx)WindowManager.getInstance().getStatusBar(project);
            if (statusBar != null) {
                statusBar.setInfo(message);
            }
        }
    }

    @Nullable
    static String getQualifiedNameFromProviders(@Nullable PsiElement element) {
        return QualifiedNameProviderUtil.getQualifiedNameDumbAware(element);
    }

    static String doCopy(List<? extends PsiElement> elements, @Nullable Editor editor) {
        if (elements.isEmpty()) {
            return null;
        }

        List<String> fqns = new ArrayList<>();
        for (PsiElement element : elements) {
            String fqn = elementToFqn(element, editor);
            if (fqn == null) {
                return null;
            }

            fqns.add(fqn);
        }

        return StringUtil.join(fqns, "\n");
    }

    @Nullable
    static String elementToFqn(@Nullable PsiElement element, @Nullable Editor editor) {
        return QualifiedNameProviderUtil.elementToFqn(element, editor);
    }
}