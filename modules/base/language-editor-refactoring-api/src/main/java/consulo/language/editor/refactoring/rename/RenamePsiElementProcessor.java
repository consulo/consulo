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

package consulo.language.editor.refactoring.rename;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.codeEditor.Editor;
import consulo.content.scope.SearchScope;
import consulo.language.editor.refactoring.RefactoringSettings;
import consulo.language.editor.refactoring.event.RefactoringElementListener;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileSystemItem;
import consulo.language.psi.PsiReference;
import consulo.language.psi.search.ReferencesSearch;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;
import consulo.usage.UsageInfo;
import consulo.util.collection.MultiMap;
import consulo.util.lang.Pair;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author yole
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class RenamePsiElementProcessor {
  public abstract boolean canProcessElement(@Nonnull PsiElement element);

  public RenameDialog createRenameDialog(Project project, PsiElement element, PsiElement nameSuggestionContext, Editor editor) {
    return new RenameDialog(project, element, nameSuggestionContext, editor);
  }

  public void renameElement(PsiElement element, String newName, UsageInfo[] usages,
                            @Nullable RefactoringElementListener listener) throws IncorrectOperationException {
    RenameUtil.doRenameGenericNamedElement(element, newName, usages, listener);
  }

  @Nonnull
  public Collection<PsiReference> findReferences(PsiElement element, boolean searchInCommentsAndStrings) {
    return findReferences(element);
  }

  @Nonnull
  public Collection<PsiReference> findReferences(PsiElement element) {
    return ReferencesSearch.search(element).findAll();
  }

  @Nullable
  public Pair<String, String> getTextOccurrenceSearchStrings(@Nonnull PsiElement element, @Nonnull String newName) {
    return null;
  }

  @Nullable
  public String getQualifiedNameAfterRename(PsiElement element, String newName, boolean nonJava) {
    return null;
  }

  /**
   * Builds the complete set of elements to be renamed during the refactoring.
   *
   * @param element the base element for the refactoring.
   * @param newName the name into which the element is being renamed.
   * @param allRenames the map (from element to its new name) into which all additional elements to be renamed should be stored.
   */
  public void prepareRenaming(PsiElement element, String newName, Map<PsiElement, String> allRenames) {
    prepareRenaming(element, newName, allRenames, element.getUseScope());
  }

  public void prepareRenaming(PsiElement element, String newName, Map<PsiElement, String> allRenames, SearchScope scope) {
  }

  public void findExistingNameConflicts(PsiElement element, String newName, MultiMap<PsiElement,String> conflicts) {
  }

  public void findExistingNameConflicts(PsiElement element, String newName, MultiMap<PsiElement,String> conflicts, Map<PsiElement, String> allRenames) {
    findExistingNameConflicts(element, newName, conflicts);
  }

  public boolean isInplaceRenameSupported() {
    return true;
  }

  public static List<RenamePsiElementProcessor> allForElement(@Nonnull PsiElement element) {
    List<RenamePsiElementProcessor> result = new ArrayList<>();
    for (RenamePsiElementProcessor processor : element.getProject().getApplication().getExtensionList(RenamePsiElementProcessor.class)) {
      if (processor.canProcessElement(element)) {
        result.add(processor);
      }
    }
    return result;
  }

  @Nonnull
  public static RenamePsiElementProcessor forElement(@Nonnull PsiElement element) {
    for (RenamePsiElementProcessor processor : element.getProject().getApplication().getExtensionList(RenamePsiElementProcessor.class)) {
      if (processor.canProcessElement(element)) {
        return processor;
      }
    }
    return DEFAULT;
  }

  @Nullable
  public Runnable getPostRenameCallback(PsiElement element, String newName, RefactoringElementListener elementListener) {
    return null;
  }

  @Nullable
  @NonNls
  public String getHelpID(PsiElement element) {
    return element instanceof PsiFile ? "refactoring.renameFile" : "refactoring.renameDialogs";
  }

  public boolean isToSearchInComments(PsiElement element) {
    return element instanceof PsiFileSystemItem && RefactoringSettings.getInstance().RENAME_SEARCH_IN_COMMENTS_FOR_FILE;
  }

  public void setToSearchInComments(PsiElement element, boolean enabled) {
    if (element instanceof PsiFileSystemItem) {
      RefactoringSettings.getInstance().RENAME_SEARCH_IN_COMMENTS_FOR_FILE = enabled;
    }
  }

  public boolean isToSearchForTextOccurrences(PsiElement element) {
    return element instanceof PsiFileSystemItem && RefactoringSettings.getInstance().RENAME_SEARCH_FOR_TEXT_FOR_FILE;
  }

  public void setToSearchForTextOccurrences(PsiElement element, boolean enabled) {
    if (element instanceof PsiFileSystemItem) {
      RefactoringSettings.getInstance().RENAME_SEARCH_FOR_TEXT_FOR_FILE = enabled;
    }
  }

  public boolean showRenamePreviewButton(PsiElement psiElement){
    return true;
  }

  /**
   * Returns the element to be renamed instead of the element on which the rename refactoring was invoked (for example, a super method
   * of an inherited method).
   *
   * @param element the element on which the refactoring was invoked.
   * @param editor the editor in which the refactoring was invoked.
   * @return the element to rename, or null if the rename refactoring should be canceled.
   */
  @Nullable
  public PsiElement substituteElementToRename(PsiElement element, @Nullable Editor editor) {
    return element;
  }

  /**
   * Substitutes element to be renamed and initiate rename procedure. Should be used in order to prevent modal dialogs to appear during inplace rename
   * @param element the element on which refactoring was invoked
   * @param editor the editor in which inplace refactoring was invoked
   * @param renameCallback rename procedure which should be called on the chosen substitution
   */
  public void substituteElementToRename(@Nonnull PsiElement element, @Nonnull Editor editor, @Nonnull Consumer<PsiElement> renameCallback) {
    PsiElement psiElement = substituteElementToRename(element, editor);
    if (psiElement == null) return;
    if (!PsiElementRenameHandler.canRename(psiElement.getProject(), editor, psiElement)) return;
    renameCallback.accept(psiElement);
  }

  public void findCollisions(PsiElement element, String newName, Map<? extends PsiElement, String> allRenames,
                             List<UsageInfo> result) {
  }

  public static final RenamePsiElementProcessor DEFAULT = new RenamePsiElementProcessor() {
    @Override
    public boolean canProcessElement(@Nonnull PsiElement element) {
      return true;
    }
  };

  /**
   * Use this method to force showing preview for custom processors.
   * This method is always called after prepareRenaming()
   * @return force show preview
   */
  public boolean forcesShowPreview() {
    return false;
  }

  @Nullable
  public PsiElement getElementToSearchInStringsAndComments(PsiElement element) {
    return element;
  }
}
