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

package consulo.codeInsight;

import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.ide.util.EditSourceUtil;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.pom.Navigatable;
import com.intellij.pom.PomDeclarationSearcher;
import com.intellij.pom.PomTarget;
import com.intellij.pom.PsiDeclaredTarget;
import com.intellij.pom.references.PomService;
import com.intellij.psi.*;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Consumer;
import com.intellij.util.containers.ContainerUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * @author VISTALL
 * @since 20.04.2015
 */
public class TargetElementUtil {
  @Nonnull
  public static Set<String> getAllAccepted() {
    Set<String> flags = new LinkedHashSet<String>();
    TargetElementUtilEx.EP_NAME.composite().collectAllAccepted(flags);
    return flags;
  }

  @Nonnull
  public static Set<String> getDefinitionSearchFlags() {
    Set<String> flags = new LinkedHashSet<String>();
    TargetElementUtilEx.EP_NAME.composite().collectDefinitionSearchFlags(flags);
    return flags;
  }

  @Nonnull
  public static Set<String> getReferenceSearchFlags() {
    Set<String> flags = new LinkedHashSet<String>();
    TargetElementUtilEx.EP_NAME.composite().collectReferenceSearchFlags(flags);
    return flags;
  }

  @Nullable
  public static PsiReference findReference(Editor editor) {
    int offset = editor.getCaretModel().getOffset();
    PsiReference result = findReference(editor, offset);
    if (result == null) {
      int expectedCaretOffset = editor instanceof EditorEx ? ((EditorEx)editor).getExpectedCaretOffset() : offset;
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
  public static int adjustOffset(Document document, final int offset) {
    return adjustOffset(null, document, offset);
  }

  public static int adjustOffset(@Nullable PsiFile file, Document document, final int offset) {
    CharSequence text = document.getCharsSequence();
    int correctedOffset = offset;
    int textLength = document.getTextLength();
    if (offset >= textLength) {
      correctedOffset = textLength - 1;
    }
    else if (!isIdentifierPart(file, text, offset)) {
      correctedOffset--;
    }
    if (correctedOffset < 0 || !isIdentifierPart(file, text, correctedOffset)) return offset;
    return correctedOffset;
  }

  @Nullable
  public static PsiElement adjustReference(@Nonnull PsiReference ref) {
    return TargetElementUtilEx.EP_NAME.composite().adjustReference(ref);
  }

  @Nullable
  @RequiredReadAction
  public static PsiReference findReference(Editor editor, int offset) {
    Project project = editor.getProject();
    if (project == null) return null;

    Document document = editor.getDocument();
    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(document);
    if (file == null) return null;

    offset = adjustOffset(file, document, offset);

    if (file instanceof PsiCompiledFile) {
      return ((PsiCompiledFile)file).getDecompiledPsiFile().findReferenceAt(offset);
    }

    return file.findReferenceAt(offset);
  }

  @Nullable
  @RequiredUIAccess
  public static PsiElement findTargetElement(Editor editor, @Nonnull Set<String> flags) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    int offset = editor.getCaretModel().getOffset();
    final PsiElement result = findTargetElement(editor, flags, offset);
    if (result != null) return result;

    int expectedCaretOffset = editor instanceof EditorEx ? ((EditorEx)editor).getExpectedCaretOffset() : offset;
    if (expectedCaretOffset != offset) {
      return findTargetElement(editor, flags, expectedCaretOffset);
    }
    return null;
  }

  @Nullable
  public static PsiElement findTargetElement(@Nonnull Editor editor, @Nonnull Set<String> flags, int offset) {
    PsiElement targetElement = findTargetElementImpl(editor, flags, offset);
    if (targetElement == null) {
      return null;
    }
    PsiElement target = TargetElementUtilEx.EP_NAME.composite().modifyTargetElement(targetElement, flags);
    if (target != null) {
      return target;
    }
    return targetElement;
  }

  @Nullable
  private static PsiElement findTargetElementImpl(@Nonnull Editor editor, @Nonnull Set<String> flags, int offset) {
    Project project = editor.getProject();
    if (project == null) return null;

    if (flags.contains(TargetElementUtilEx.LOOKUP_ITEM_ACCEPTED)) {
      PsiElement element = getTargetElementFromLookup(project);
      if (element != null) {
        return element;
      }
    }

    Document document = editor.getDocument();

    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(document);
    if (file == null) return null;

    offset = adjustOffset(file, document, offset);

    if (file instanceof PsiCompiledFile) {
      file = ((PsiCompiledFile)file).getDecompiledPsiFile();
    }
    PsiElement element = file.findElementAt(offset);
    if (flags.contains(TargetElementUtilEx.REFERENCED_ELEMENT_ACCEPTED)) {
      final PsiElement referenceOrReferencedElement = getReferenceOrReferencedElement(file, editor, flags, offset);
      //if (referenceOrReferencedElement == null) {
      //  return getReferenceOrReferencedElement(file, editor, flags, offset);
      //}
      if (isAcceptableReferencedElement(element, referenceOrReferencedElement)) {
        return referenceOrReferencedElement;
      }
    }

    if (element == null) return null;

    if (flags.contains(TargetElementUtilEx.ELEMENT_NAME_ACCEPTED)) {
      if (element instanceof PsiNamedElement) return element;
      return getNamedElement(element, offset - element.getTextRange().getStartOffset());
    }
    return null;
  }

  @Nullable
  private static PsiElement getReferenceOrReferencedElement(PsiFile file, Editor editor, Set<String> flags, int offset) {
    PsiElement referenceOrReferencedElement = getReferenceOrReferencedElementImpl(file, editor, flags, offset);

    PsiElement psiElement =
            TargetElementUtilEx.EP_NAME.composite().modifyReferenceOrReferencedElement(referenceOrReferencedElement, file, editor, flags, offset);
    if (psiElement != null) {
      return psiElement;
    }
    return referenceOrReferencedElement;
  }

  @Nullable
  private static PsiElement getReferenceOrReferencedElementImpl(PsiFile file, Editor editor, Set<String> flags, int offset) {
    PsiReference ref = findReference(editor, offset);
    if (ref == null) return null;

    PsiElement referenceOrReferencedElement = TargetElementUtilEx.EP_NAME.composite().getReferenceOrReferencedElement(ref, flags);
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
      final PsiElement psi = item == null ? null : CompletionUtil.getTargetElement(item);
      if (psi != null && psi.isValid()) {
        return psi;
      }
    }
    return null;
  }

  private static boolean isAcceptableReferencedElement(final PsiElement element, final PsiElement referenceOrReferencedElement) {
    if(referenceOrReferencedElement == null || !referenceOrReferencedElement.isValid()) {
      return false;
    }
    if(!TargetElementUtilEx.EP_NAME.composite().isAcceptableReferencedElement(element, referenceOrReferencedElement)) {
      return false;
    }
    return true;
  }

  @Nullable
  public static PsiElement getNamedElement(@Nullable final PsiElement element, final int offsetInElement) {
    if (element == null) return null;

    final List<PomTarget> targets = ContainerUtil.newArrayList();
    final Consumer<PomTarget> consumer = new Consumer<PomTarget>() {
      @Override
      public void consume(PomTarget target) {
        if (target instanceof PsiDeclaredTarget) {
          final PsiDeclaredTarget declaredTarget = (PsiDeclaredTarget)target;
          final PsiElement navigationElement = declaredTarget.getNavigationElement();
          final TextRange range = declaredTarget.getNameIdentifierRange();
          if (range != null &&
              !range.shiftRight(navigationElement.getTextRange().getStartOffset()).contains(element.getTextRange().getStartOffset() + offsetInElement)) {
            return;
          }
        }
        targets.add(target);
      }
    };

    PsiElement parent = element;

    int offset = offsetInElement;
    while (parent != null) {
      for (PomDeclarationSearcher searcher : PomDeclarationSearcher.EP_NAME.getExtensions()) {
        searcher.findDeclarationsAt(parent, offset, consumer);
        if (!targets.isEmpty()) {
          final PomTarget target = targets.get(0);
          return target == null ? null : PomService.convertToPsi(element.getProject(), target);
        }
      }
      offset += parent.getStartOffsetInParent();
      parent = parent.getParent();
    }

    return getNamedElement(element);
  }


  @Nullable
  private static PsiElement getNamedElement(@Nullable final PsiElement element) {
    PsiElement parent;
    if ((parent = PsiTreeUtil.getParentOfType(element, PsiNamedElement.class, false)) != null) {
      // A bit hacky depends on navigation offset correctly overridden
      assert element != null : "notnull parent?";
      if (parent.getTextOffset() == element.getTextRange().getStartOffset()) {
        return parent;
      }
    }
    if(element == null) {
      return null;
    }
    return TargetElementUtilEx.EP_NAME.composite().getNamedElement(element);
  }

  @Nullable
  public static PsiElement adjustElement(final Editor editor, final Set<String> flags, final PsiElement element, final PsiElement contextElement) {
    return TargetElementUtilEx.EP_NAME.composite().adjustElement(editor, flags, element, contextElement);
  }

  public static boolean inVirtualSpace(@Nonnull Editor editor, int offset) {
    if (offset == editor.getCaretModel().getOffset()) {
      return EditorUtil.inVirtualSpace(editor, editor.getCaretModel().getLogicalPosition());
    }

    return false;
  }

  private static boolean isIdentifierPart(@Nullable PsiFile file, CharSequence text, int offset) {
    if (file != null) {
      if (TargetElementUtilEx.EP_NAME.composite().isIdentifierPart(file, text, offset)) {
        return true;
      }
    }
    return Character.isJavaIdentifierPart(text.charAt(offset));
  }

  public static boolean acceptImplementationForReference(PsiReference reference, PsiElement element) {
    return true;
  }

  @Nonnull
  public static Collection<PsiElement> getTargetCandidates(PsiReference reference) {
    if (reference instanceof PsiPolyVariantReference) {
      final ResolveResult[] results = ((PsiPolyVariantReference)reference).multiResolve(false);
      final ArrayList<PsiElement> navigatableResults = new ArrayList<PsiElement>(results.length);

      for (ResolveResult r : results) {
        PsiElement element = r.getElement();
        if (EditSourceUtil.canNavigate(element) || element instanceof Navigatable && ((Navigatable)element).canNavigateToSource()) {
          navigatableResults.add(element);
        }
      }

      return navigatableResults;
    }
    PsiElement resolved = reference.resolve();
    if (resolved instanceof NavigationItem) {
      return Collections.singleton(resolved);
    }
    Collection<PsiElement> targetCandidates = TargetElementUtilEx.EP_NAME.composite().getTargetCandidates(reference);
    if (targetCandidates != null) {
      return targetCandidates;
    }
    return Collections.emptyList();
  }

  @Nullable
  public static PsiElement getGotoDeclarationTarget(final PsiElement element, final PsiElement navElement) {
    PsiElement gotoDeclarationTarget = TargetElementUtilEx.EP_NAME.composite().getGotoDeclarationTarget(element, navElement);
    if (gotoDeclarationTarget != null) {
      return gotoDeclarationTarget;
    }
    return navElement;
  }

  public static boolean includeSelfInGotoImplementation(final PsiElement element) {
    return TargetElementUtilEx.EP_NAME.composite().includeSelfInGotoImplementation(element);
  }

  public static SearchScope getSearchScope(Editor editor, PsiElement element) {
    return PsiSearchHelper.SERVICE.getInstance(element.getProject()).getUseScope(element);
  }
}
