/*
 * Copyright 2013-2016 consulo.io
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
package consulo.language.editor;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.codeEditor.util.EditorUtil;
import consulo.content.scope.SearchScope;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.editor.completion.CompletionUtilCore;
import consulo.language.editor.completion.lookup.Lookup;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.pom.PomDeclarationSearcher;
import consulo.language.pom.PomService;
import consulo.language.pom.PomTarget;
import consulo.language.pom.PsiDeclaredTarget;
import consulo.language.psi.*;
import consulo.language.psi.scope.PsiSearchScopeUtil;
import consulo.language.psi.util.EditSourceUtil;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.navigation.Navigatable;
import consulo.navigation.NavigationItem;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2015-04-20
 */
public class TargetElementUtil {
    @Nonnull
    public static Set<String> getAllAccepted() {
        Set<String> flags = new LinkedHashSet<>();
        TargetElementUtilExtender.EP.forEachExtensionSafe(Application.get(), it -> it.collectAllAccepted(flags));
        return flags;
    }

    @Nonnull
    public static Set<String> getDefinitionSearchFlags() {
        Set<String> flags = new LinkedHashSet<>();
        TargetElementUtilExtender.EP.forEachExtensionSafe(Application.get(), it -> it.collectDefinitionSearchFlags(flags));
        return flags;
    }

    @Nonnull
    public static Set<String> getReferenceSearchFlags() {
        Set<String> flags = new LinkedHashSet<>();
        TargetElementUtilExtender.EP.forEachExtensionSafe(Application.get(), it -> it.collectReferenceSearchFlags(flags));
        return flags;
    }

    @Nullable
    @RequiredReadAction
    public static PsiReference findReference(Editor editor) {
        int offset = editor.getCaretModel().getOffset();
        PsiReference result = findReference(editor, offset);
        if (result == null) {
            int expectedCaretOffset = editor.getExpectedCaretOffset();
            if (expectedCaretOffset != offset) {
                result = findReference(editor, expectedCaretOffset);
            }
        }
        return result;
    }

    /**
     * @param document
     * @param offset
     * @return
     * @deprecated adjust offset with PsiElement should be used instead to provide correct checking for identifier part
     */
    public static int adjustOffset(Document document, int offset) {
        return adjustOffset(null, document, offset);
    }

    public static int adjustOffset(@Nullable PsiFile file, Document document, int offset) {
        CharSequence text = document.getCharsSequence();
        int correctedOffset = offset;
        int textLength = document.getTextLength();
        if (offset >= textLength) {
            correctedOffset = textLength - 1;
        }
        else if (!isIdentifierPart(file, text, offset)) {
            correctedOffset--;
        }
        if (correctedOffset < 0 || !isIdentifierPart(file, text, correctedOffset)) {
            return offset;
        }
        return correctedOffset;
    }

    @Nullable
    public static PsiElement adjustReference(@Nonnull PsiReference ref) {
        return TargetElementUtilExtender.EP.computeSafeIfAny(Application.get(), it -> it.adjustReference(ref));
    }

    @Nullable
    @RequiredReadAction
    public static PsiReference findReference(Editor editor, int offset) {
        Project project = editor.getProject();
        if (project == null) {
            return null;
        }

        Document document = editor.getDocument();
        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(document);
        if (file == null) {
            return null;
        }

        offset = adjustOffset(file, document, offset);

        return file instanceof PsiCompiledFile compiledFile
            ? compiledFile.getDecompiledPsiFile().findReferenceAt(offset)
            : file.findReferenceAt(offset);
    }

    @Nullable
    @RequiredReadAction
    public static PsiElement findTargetElement(Editor editor, @Nonnull Set<String> flags) {
        int offset = editor.getCaretModel().getOffset();
        PsiElement result = findTargetElement(editor, flags, offset);
        if (result != null) {
            return result;
        }

        int expectedCaretOffset = editor.getExpectedCaretOffset();
        if (expectedCaretOffset != offset) {
            return findTargetElement(editor, flags, expectedCaretOffset);
        }
        return null;
    }

    @Nullable
    @RequiredReadAction
    public static PsiElement findTargetElement(@Nonnull Editor editor, @Nonnull Set<String> flags, int offset) {
        PsiElement targetElement = findTargetElementImpl(editor, flags, offset);
        if (targetElement == null) {
            return null;
        }

        PsiElement target =
            TargetElementUtilExtender.EP.computeSafeIfAny(Application.get(), it -> it.modifyTargetElement(targetElement, flags));
        if (target != null) {
            return target;
        }
        return targetElement;
    }

    @Nullable
    @RequiredReadAction
    private static PsiElement findTargetElementImpl(@Nonnull Editor editor, @Nonnull Set<String> flags, int offset) {
        Project project = editor.getProject();
        if (project == null) {
            return null;
        }

        if (flags.contains(TargetElementUtilExtender.LOOKUP_ITEM_ACCEPTED)) {
            PsiElement element = getTargetElementFromLookup(project);
            if (element != null) {
                return element;
            }
        }

        Document document = editor.getDocument();

        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(document);
        if (file == null) {
            return null;
        }

        offset = adjustOffset(file, document, offset);

        if (file instanceof PsiCompiledFile compiledFile) {
            file = compiledFile.getDecompiledPsiFile();
        }
        PsiElement element = file.findElementAt(offset);
        if (flags.contains(TargetElementUtilExtender.REFERENCED_ELEMENT_ACCEPTED)) {
            PsiElement referenceOrReferencedElement = getReferenceOrReferencedElement(file, editor, flags, offset);
            //if (referenceOrReferencedElement == null) {
            //    return getReferenceOrReferencedElement(file, editor, flags, offset);
            //}
            if (isAcceptableReferencedElement(element, referenceOrReferencedElement)) {
                return referenceOrReferencedElement;
            }
        }

        if (element == null) {
            return null;
        }

        if (flags.contains(TargetElementUtilExtender.ELEMENT_NAME_ACCEPTED)) {
            if (element instanceof PsiNamedElement) {
                return element;
            }
            return getNamedElement(element, offset - element.getTextRange().getStartOffset());
        }
        return null;
    }

    @Nullable
    @RequiredReadAction
    private static PsiElement getReferenceOrReferencedElement(PsiFile file, Editor editor, Set<String> flags, int offset) {
        PsiElement referenceOrReferencedElement = getReferenceOrReferencedElementImpl(file, editor, flags, offset);

        PsiElement psiElement = TargetElementUtilExtender.EP.computeSafeIfAny(
            Application.get(),
            it -> it.modifyReferenceOrReferencedElement(referenceOrReferencedElement, file, editor, flags, offset)
        );
        if (psiElement != null) {
            return psiElement;
        }
        return referenceOrReferencedElement;
    }

    @Nullable
    @RequiredReadAction
    private static PsiElement getReferenceOrReferencedElementImpl(PsiFile file, Editor editor, Set<String> flags, int offset) {
        PsiReference ref = findReference(editor, offset);
        if (ref == null) {
            return null;
        }

        PsiElement referenceOrReferencedElement =
            TargetElementUtilExtender.EP.computeSafeIfAny(Application.get(), it -> it.getReferenceOrReferencedElement(ref, flags));
        if (referenceOrReferencedElement != null) {
            return referenceOrReferencedElement;
        }

        return null;
    }

    @Nullable
    private static PsiElement getTargetElementFromLookup(Project project) {
        Lookup activeLookup = LookupManager.getInstance(project).getActiveLookup();
        if (activeLookup != null) {
            LookupElement item = activeLookup.getCurrentItem();
            PsiElement psi = item == null ? null : CompletionUtilCore.getTargetElement(item);
            if (psi != null && psi.isValid()) {
                return psi;
            }
        }
        return null;
    }

    private static boolean isAcceptableReferencedElement(PsiElement element, PsiElement referenceOrReferencedElement) {
        if (referenceOrReferencedElement == null || !referenceOrReferencedElement.isValid()) {
            return false;
        }
        for (TargetElementUtilExtender extender : TargetElementUtilExtender.EP.getExtensionList(Application.get())) {
            if (!extender.isAcceptableReferencedElement(element, referenceOrReferencedElement)) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    @RequiredReadAction
    public static PsiElement getNamedElement(@Nullable PsiElement element, int offsetInElement) {
        if (element == null) {
            return null;
        }

        List<PomTarget> targets = new ArrayList<>();
        @RequiredReadAction
        Consumer<PomTarget> consumer = target -> {
            if (target instanceof PsiDeclaredTarget declaredTarget) {
                PsiElement navigationElement = declaredTarget.getNavigationElement();
                TextRange range = declaredTarget.getNameIdentifierRange();
                if (range != null && !range.shiftRight(navigationElement.getTextRange().getStartOffset())
                    .contains(element.getTextRange().getStartOffset() + offsetInElement)) {
                    return;
                }
            }
            targets.add(target);
        };

        PsiElement parent = element;

        int offset = offsetInElement;
        while (parent != null) {
            for (PomDeclarationSearcher searcher : PomDeclarationSearcher.EP_NAME.getExtensionList()) {
                searcher.findDeclarationsAt(parent, offset, consumer);
                if (!targets.isEmpty()) {
                    PomTarget target = targets.get(0);
                    return target == null ? null : PomService.convertToPsi(element.getProject(), target);
                }
            }
            offset += parent.getStartOffsetInParent();
            parent = parent.getParent();
        }

        return getNamedElement(element);
    }


    @Nullable
    @RequiredReadAction
    private static PsiElement getNamedElement(@Nullable PsiElement element) {
        PsiElement parent;
        if ((parent = PsiTreeUtil.getParentOfType(element, PsiNamedElement.class, false)) != null) {
            // A bit hacky depends on navigation offset correctly overridden
            assert element != null : "notnull parent?";
            if (parent.getTextOffset() == element.getTextRange().getStartOffset()) {
                return parent;
            }
        }
        if (element == null) {
            return null;
        }
        return TargetElementUtilExtender.EP.computeSafeIfAny(Application.get(), it -> it.getNamedElement(element));
    }

    @Nullable
    public static PsiElement adjustElement(Editor editor, Set<String> flags, PsiElement element, PsiElement contextElement) {
        return TargetElementUtilExtender.EP.computeSafeIfAny(
            Application.get(),
            it -> it.adjustElement(editor, flags, element, contextElement)
        );
    }

    public static boolean inVirtualSpace(@Nonnull Editor editor, int offset) {
        return offset == editor.getCaretModel().getOffset()
            && EditorUtil.inVirtualSpace(editor, editor.getCaretModel().getLogicalPosition());
    }

    private static boolean isIdentifierPart(@Nullable PsiFile file, CharSequence text, int offset) {
        if (file != null) {
            if (TargetElementUtilExtender.EP.findFirstSafe(Application.get(), it -> it.isIdentifierPart(file, text, offset)) != null) {
                return true;
            }
        }
        return Character.isJavaIdentifierPart(text.charAt(offset));
    }

    public static boolean acceptImplementationForReference(PsiReference reference, PsiElement element) {
        for (TargetElementUtilExtender extender : TargetElementUtilExtender.EP.getExtensionList(Application.get())) {
            if (!extender.acceptImplementationForReference(reference, element)) {
                return false;
            }
        }
        return true;
    }

    @Nonnull
    @RequiredReadAction
    public static Collection<PsiElement> getTargetCandidates(PsiReference reference) {
        if (reference instanceof PsiPolyVariantReference polyVariantReference) {
            ResolveResult[] results = polyVariantReference.multiResolve(false);
            ArrayList<PsiElement> navigatableResults = new ArrayList<>(results.length);

            for (ResolveResult r : results) {
                PsiElement element = r.getElement();
                if (EditSourceUtil.canNavigate(element)
                    || element instanceof Navigatable navigatable && navigatable.canNavigateToSource()) {
                    navigatableResults.add(element);
                }
            }

            return navigatableResults;
        }
        PsiElement resolved = reference.resolve();
        if (resolved instanceof NavigationItem) {
            return Collections.singleton(resolved);
        }
        Collection<PsiElement> targetCandidates =
            TargetElementUtilExtender.EP.computeSafeIfAny(Application.get(), it -> it.getTargetCandidates(reference));
        if (targetCandidates != null) {
            return targetCandidates;
        }
        return Collections.emptyList();
    }

    @Nullable
    public static PsiElement getGotoDeclarationTarget(PsiElement element, PsiElement navElement) {
        PsiElement gotoDeclarationTarget =
            TargetElementUtilExtender.EP.computeSafeIfAny(Application.get(), it -> it.getGotoDeclarationTarget(element, navElement));
        if (gotoDeclarationTarget != null) {
            return gotoDeclarationTarget;
        }
        return navElement;
    }

    public static boolean includeSelfInGotoImplementation(PsiElement element) {
        for (TargetElementUtilExtender extender : TargetElementUtilExtender.EP.getExtensionList(Application.get())) {
            if (!extender.includeSelfInGotoImplementation(element)) {
                return false;
            }
        }
        return true;
    }

    public static SearchScope getSearchScope(Editor editor, PsiElement element) {
        return PsiSearchScopeUtil.getUseScope(element);
    }
}
