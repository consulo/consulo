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

import consulo.annotation.access.RequiredReadAction;
import consulo.application.dumb.IndexNotReadyException;
import consulo.codeEditor.Editor;
import consulo.language.editor.TargetElementUtil;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.documentation.DocumentationProvider;
import consulo.language.editor.documentation.DocumentationProviderEx;
import consulo.language.editor.impl.internal.completion.CompletionUtil;
import consulo.language.editor.internal.DocumentationManagerHelper;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiDocCommentBase;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiPolyVariantReference;
import consulo.language.psi.PsiReference;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.lang.ObjectUtil;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-09
 */
public final class DocumentationTargetFinder {
    private static final Logger LOG = Logger.getInstance(DocumentationTargetFinder.class);

    private DocumentationTargetFinder() {
    }

    @RequiredReadAction
    public static @Nullable DocumentationElementSource findAtOffset(
        Project project,
        Editor editor,
        PsiFile file,
        int offset,
        @Nullable LookupElement lookupItem
    ) {
        PsiElement originalElement = file.findElementAt(offset);

        PsiElement element = lookupItem != null
            ? findForLookupItem(project, editor, file, offset, lookupItem)
            : findTargetElement(project, editor, offset, file, originalElement);

        if (element == null) {
            if (originalElement == null) {
                return null;
            }

            PsiComment comment = PsiTreeUtil.getParentOfType(originalElement, PsiComment.class);
            if (comment == null) {
                return null;
            }

            element = comment instanceof PsiDocCommentBase docComment ? docComment.getOwner() : comment.getParent();
            if (element == null) {
                return null;
            }
        }

        if (!element.isValid() || element.getProject() != project) {
            return null;
        }

        SmartPointerManager pointerManager = SmartPointerManager.getInstance(project);
        return new DocumentationElementSource(
            pointerManager.createSmartPsiElementPointer(element),
            originalElement == null ? null : pointerManager.createSmartPsiElementPointer(originalElement)
        );
    }

    @RequiredReadAction
    public static @Nullable PsiElement findTargetElement(
        Project project,
        Editor editor,
        int offset,
        @Nullable PsiFile file,
        @Nullable PsiElement contextElement
    ) {
        try {
            return findTargetElementUnsafe(project, editor, offset, file, contextElement);
        }
        catch (IndexNotReadyException e) {
            LOG.warn("Index not ready");
            return null;
        }
    }

    @RequiredReadAction
    private static @Nullable PsiElement findTargetElementUnsafe(
        Project project,
        Editor editor,
        int offset,
        @Nullable PsiFile file,
        @Nullable PsiElement contextElement
    ) {
        PsiElement element = null;
        if (file != null) {
            DocumentationProvider provider = DocumentationManagerHelper.getProviderFromElement(file);
            if (provider instanceof DocumentationProviderEx providerEx) {
                element = sameProject(project, providerEx.getCustomDocumentationElement(editor, file, contextElement));
            }
        }

        if (element == null) {
            element = sameProject(project, TargetElementUtil.findTargetElement(editor, TargetElementUtil.getAllAccepted(), offset));

            if (element != null || contextElement != null) {
                PsiElement adjusted = sameProject(
                    project,
                    TargetElementUtil.adjustElement(editor, TargetElementUtil.getAllAccepted(), element, contextElement)
                );
                if (adjusted != null) {
                    element = adjusted;
                }
            }
        }

        if (element == null) {
            PsiReference reference = TargetElementUtil.findReference(editor, offset);
            if (reference != null) {
                element = sameProject(project, TargetElementUtil.adjustReference(reference));
                if (reference instanceof PsiPolyVariantReference) {
                    element = sameProject(project, reference.getElement());
                }
            }
        }

        if (contextElement != null && element != null) {
            DocumentationManagerHelper.storeOriginalElement(project, contextElement, element);
        }
        return element;
    }

    @RequiredReadAction
    public static @Nullable PsiElement findForLookupItem(Project project, Editor editor, PsiFile file, int offset, LookupElement item) {
        int lookupOffset = offset > 0 && offset == editor.getDocument().getTextLength() ? offset - 1 : offset;

        PsiReference reference = TargetElementUtil.findReference(editor, lookupOffset);
        PsiElement contextElement = ObjectUtil.coalesce(file.findElementAt(lookupOffset), file);
        PsiElement targetElement = reference != null ? reference.getElement() : contextElement;

        DocumentationProvider provider = DocumentationManagerHelper.getProviderFromElement(file);
        PsiElement fromProvider = targetElement == null || !targetElement.isValid()
            ? null
            : provider.getDocumentationElementForLookupItem(PsiManager.getInstance(project), item.getObject(), targetElement);
        return sameProject(project, fromProvider != null ? fromProvider : CompletionUtil.getTargetElement(item));
    }

    @RequiredReadAction
    private static @Nullable PsiElement sameProject(Project project, @Nullable PsiElement element) {
        if (element != null && element.isValid() && element.getProject() != project) {
            return null;
        }
        return element;
    }
}
