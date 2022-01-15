/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.psi.util;

import com.intellij.lang.ASTNode;
import com.intellij.lang.FileASTNode;
import com.intellij.lang.Language;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Computable;
import consulo.util.dataholder.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.meta.PsiMetaData;
import com.intellij.psi.meta.PsiMetaOwner;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.tree.IElementType;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.TimeoutUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.lang.LanguageVersion;
import consulo.logging.Logger;
import consulo.psi.PsiElementWithSubtreeChangeNotifier;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author yole
 */
public class PsiUtilCore {
  private static final Logger LOG = Logger.getInstance(PsiUtilCore.class);
  public static final PsiElement NULL_PSI_ELEMENT = new NullPsiElement();
  private static class NullPsiElement implements PsiElement {
    @Override
    @Nonnull
    public Project getProject() {
      throw createException();
    }

    @Override
    @Nonnull
    public Language getLanguage() {
      throw createException();
    }

    @Nonnull
    @Override
    public LanguageVersion getLanguageVersion() {
      throw createException();
    }

    @Override
    public PsiManager getManager() {
      throw createException();
    }

    @Override
    @Nonnull
    public PsiElement[] getChildren() {
      throw createException();
    }

    @Override
    public PsiElement getParent() {
      throw createException();
    }

    @Override
    @Nullable
    public PsiElement getFirstChild() {
      throw createException();
    }

    @Override
    @Nullable
    public PsiElement getLastChild() {
      throw createException();
    }

    @Override
    @Nullable
    public PsiElement getNextSibling() {
      throw createException();
    }

    @Override
    @Nullable
    public PsiElement getPrevSibling() {
      throw createException();
    }

    @Override
    public PsiFile getContainingFile() {
      throw createException();
    }

    @Override
    public TextRange getTextRange() {
      throw createException();
    }

    @Override
    public int getStartOffsetInParent() {
      throw createException();
    }

    @Override
    public int getTextLength() {
      throw createException();
    }

    @Override
    public PsiElement findElementAt(int offset) {
      throw createException();
    }

    @Override
    @Nullable
    public PsiReference findReferenceAt(int offset) {
      throw createException();
    }

    @Override
    public int getTextOffset() {
      throw createException();
    }

    @Override
    public String getText() {
      throw createException();
    }

    @Override
    @Nonnull
    public char[] textToCharArray() {
      throw createException();
    }

    @Override
    public PsiElement getNavigationElement() {
      throw createException();
    }

    @Override
    public PsiElement getOriginalElement() {
      throw createException();
    }

    @Override
    public boolean textMatches(@Nonnull CharSequence text) {
      throw createException();
    }

    @Override
    public boolean textMatches(@Nonnull PsiElement element) {
      throw createException();
    }

    @Override
    public boolean textContains(char c) {
      throw createException();
    }

    @Override
    public void accept(@Nonnull PsiElementVisitor visitor) {
      throw createException();
    }

    @Override
    public void acceptChildren(@Nonnull PsiElementVisitor visitor) {
      throw createException();
    }

    @Override
    public PsiElement copy() {
      throw createException();
    }

    @Override
    public PsiElement add(@Nonnull PsiElement element) {
      throw createException();
    }

    @Override
    public PsiElement addBefore(@Nonnull PsiElement element, PsiElement anchor) {
      throw createException();
    }

    @Override
    public PsiElement addAfter(@Nonnull PsiElement element, PsiElement anchor) {
      throw createException();
    }

    @Override
    public void checkAdd(@Nonnull PsiElement element) {
      throw createException();
    }

    @Override
    public PsiElement addRange(PsiElement first, PsiElement last) {
      throw createException();
    }

    @Override
    public PsiElement addRangeBefore(@Nonnull PsiElement first, @Nonnull PsiElement last, PsiElement anchor) {
      throw createException();
    }

    @Override
    public PsiElement addRangeAfter(PsiElement first, PsiElement last, PsiElement anchor) {
      throw createException();
    }

    @Override
    public void delete() {
      throw createException();
    }

    @Override
    public void checkDelete() {
      throw createException();
    }

    @Override
    public void deleteChildRange(PsiElement first, PsiElement last) {
      throw createException();
    }

    @Override
    public PsiElement replace(@Nonnull PsiElement newElement) {
      throw createException();
    }

    @Override
    public boolean isValid() {
      throw createException();
    }

    @Override
    public boolean isWritable() {
      throw createException();
    }

    protected PsiInvalidElementAccessException createException() {
      return new PsiInvalidElementAccessException(this, toString(), null);
    }

    @Override
    @Nullable
    public PsiReference getReference() {
      throw createException();
    }

    @Override
    @Nonnull
    public PsiReference[] getReferences() {
      throw createException();
    }

    @Override
    public <T> T getCopyableUserData(Key<T> key) {
      throw createException();
    }

    @Override
    public <T> void putCopyableUserData(Key<T> key, T value) {
      throw createException();
    }

    @Override
    public boolean processDeclarations(@Nonnull PsiScopeProcessor processor,
                                       @Nonnull ResolveState state,
                                       PsiElement lastParent,
                                       @Nonnull PsiElement place) {
      throw createException();
    }

    @Override
    public PsiElement getContext() {
      throw createException();
    }

    @Override
    public boolean isPhysical() {
      throw createException();
    }

    @Override
    @Nonnull
    public GlobalSearchScope getResolveScope() {
      throw createException();
    }

    @Override
    @Nonnull
    public SearchScope getUseScope() {
      throw createException();
    }

    @Override
    public ASTNode getNode() {
      throw createException();
    }

    @Override
    public <T> T getUserData(@Nonnull Key<T> key) {
      throw createException();
    }

    @Override
    public <T> void putUserData(@Nonnull Key<T> key, T value) {
      throw createException();
    }

    @Override
    public boolean isEquivalentTo(final PsiElement another) {
      return this == another;
    }

    @Override
    public String toString() {
      return "NULL_PSI_ELEMENT";
    }
  }

  public static final PsiFile NULL_PSI_FILE = new NullPsiFile();
  private static class NullPsiFile extends NullPsiElement implements PsiFile, PsiElementWithSubtreeChangeNotifier {
    @Override
    public FileASTNode getNode() {
      throw createException();
    }

    @Override
    public PsiDirectory getParent() {
      throw createException();
    }

    @Override
    public VirtualFile getVirtualFile() {
      throw createException();
    }

    @Override
    public PsiDirectory getContainingDirectory() {
      throw createException();
    }

    @Override
    public long getModificationStamp() {
      throw createException();
    }

    @Nonnull
    @Override
    public PsiFile getOriginalFile() {
      throw createException();
    }

    @Nonnull
    @Override
    public FileType getFileType() {
      throw createException();
    }

    @Nonnull
    @Override
    public PsiFile[] getPsiRoots() {
      throw createException();
    }

    @Nonnull
    @Override
    public FileViewProvider getViewProvider() {
      throw createException();
    }

    @Override
    public void subtreeChanged() {
      throw createException();
    }

    @Override
    public boolean isDirectory() {
      throw createException();
    }

    @RequiredReadAction
    @Nonnull
    @Override
    public String getName() {
      throw createException();
    }

    @Override
    public boolean processChildren(PsiElementProcessor<PsiFileSystemItem> processor) {
      throw createException();
    }

    @Nullable
    @Override
    public ItemPresentation getPresentation() {
      throw createException();
    }

    @Override
    public void navigate(boolean requestFocus) {
      throw createException();
    }

    @Override
    public boolean canNavigate() {
      throw createException();
    }

    @Override
    public boolean canNavigateToSource() {
      throw createException();
    }

    @Override
    public void checkSetName(String name) throws IncorrectOperationException {
      throw createException();
    }

    @RequiredWriteAction
    @Override
    public PsiElement setName(@NonNls @Nonnull String name) throws IncorrectOperationException {
      throw createException();
    }

    @Override
    public String toString() {
      return "NULL_PSI_FILE";
    }
  }

  @Nonnull
  public static PsiElement[] toPsiElementArray(@Nonnull Collection<? extends PsiElement> collection) {
    if (collection.isEmpty()) return PsiElement.EMPTY_ARRAY;
    //noinspection SSBasedInspection
    return collection.toArray(new PsiElement[collection.size()]);
  }

  public static Language getNotAnyLanguage(ASTNode node) {
    if (node == null) return Language.ANY;

    final Language lang = node.getElementType().getLanguage();
    return lang == Language.ANY ? getNotAnyLanguage(node.getTreeParent()) : lang;
  }

  @Nullable
  public static VirtualFile getVirtualFile(@Nullable PsiElement element) {
    // optimisation: call isValid() on file only to reduce walks up and down
    if (element == null) {
      return null;
    }
    if (element instanceof PsiFileSystemItem) {
      return element.isValid() ? ((PsiFileSystemItem)element).getVirtualFile() : null;
    }
    final PsiFile containingFile = element.getContainingFile();
    if (containingFile == null || !containingFile.isValid()) {
      return null;
    }

    VirtualFile file = containingFile.getVirtualFile();
    if (file == null) {
      PsiFile originalFile = containingFile.getOriginalFile();
      if (originalFile != containingFile && originalFile.isValid()) {
        file = originalFile.getVirtualFile();
      }
    }
    return file;
  }

  public static int compareElementsByPosition(final PsiElement element1, final PsiElement element2) {
    if (element1 != null && element2 != null) {
      final PsiFile psiFile1 = element1.getContainingFile();
      final PsiFile psiFile2 = element2.getContainingFile();
      if (Comparing.equal(psiFile1, psiFile2)){
        final TextRange textRange1 = element1.getTextRange();
        final TextRange textRange2 = element2.getTextRange();
        if (textRange1 != null && textRange2 != null) {
          return textRange1.getStartOffset() - textRange2.getStartOffset();
        }
      } else if (psiFile1 != null && psiFile2 != null){
        final String name1 = psiFile1.getName();
        final String name2 = psiFile2.getName();
        return name1.compareToIgnoreCase(name2);
      }
    }
    return 0;
  }

  public static boolean hasErrorElementChild(@Nonnull PsiElement element) {
    for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof PsiErrorElement) return true;
    }
    return false;
  }

  @Nonnull
  public static PsiElement getElementAtOffset(@Nonnull PsiFile file, int offset) {
    PsiElement elt = file.findElementAt(offset);
    if (elt == null && offset > 0) {
      elt = file.findElementAt(offset - 1);
    }
    if (elt == null) {
      return file;
    }
    return elt;
  }

  @Nullable
  public static PsiFile getTemplateLanguageFile(final PsiElement element) {
    if (element == null) return null;
    final PsiFile containingFile = element.getContainingFile();
    if (containingFile == null) return null;

    final FileViewProvider viewProvider = containingFile.getViewProvider();
    return viewProvider.getPsi(viewProvider.getBaseLanguage());
  }

  @Nonnull
  public static PsiFile[] toPsiFileArray(@Nonnull Collection<? extends PsiFile> collection) {
    if (collection.isEmpty()) return PsiFile.EMPTY_ARRAY;
    //noinspection SSBasedInspection
    return collection.toArray(new PsiFile[collection.size()]);
  }

  /**
   * @return name for element using element structure info
   */
  @Nullable
  public static String getName(PsiElement element) {
    String name = null;
    if (element instanceof PsiMetaOwner) {
      final PsiMetaData data = ((PsiMetaOwner) element).getMetaData();
      if (data != null) {
        name = data.getName(element);
      }
    }
    if (name == null && element instanceof PsiNamedElement) {
      name = ((PsiNamedElement) element).getName();
    }
    return name;
  }

  public static String getQualifiedNameAfterRename(String qName, String newName) {
    if (qName == null) return newName;
    int index = qName.lastIndexOf('.');
    return index < 0 ? newName : qName.substring(0, index + 1) + newName;
  }

  public static Language getDialect(@Nonnull PsiElement element) {
    return narrowLanguage(element.getLanguage(), element.getContainingFile().getLanguage());
  }

  protected static Language narrowLanguage(final Language language, final Language candidate) {
    if (candidate.isKindOf(language)) return candidate;
    return language;
  }

  public static void ensureValid(@Nonnull PsiElement element) {
    if (!element.isValid()) {
      TimeoutUtil.sleep(1); // to see if processing in another thread suddenly makes the element valid again (which is a bug)
      if (element.isValid()) {
        LOG.error("PSI resurrected: " + element + " of " + element.getClass());
        return;
      }
      throw new PsiInvalidElementAccessException(element);
    }
  }

  /**
   * Tries to find PSI file for a virtual file and throws assertion error with debug info if it is null.
   */
  @Nonnull
  public static PsiFile getPsiFile(@Nonnull Project project, @Nonnull VirtualFile file) {
    PsiManager psiManager = PsiManager.getInstance(project);
    PsiFile psi = psiManager.findFile(file);
    if (psi != null) return psi;
    FileType fileType = file.getFileType();
    FileViewProvider viewProvider = psiManager.findViewProvider(file);
    Document document = FileDocumentManager.getInstance().getDocument(file);
    boolean ignored = !(file instanceof LightVirtualFile) && FileTypeRegistry.getInstance().isFileIgnored(file);
    VirtualFile vDir = file.getParent();
    PsiDirectory psiDir = vDir == null ? null : PsiManager.getInstance(project).findDirectory(vDir);
    FileIndexFacade indexFacade = FileIndexFacade.getInstance(project);
    StringBuilder sb = new StringBuilder();
    sb.append("valid=").append(file.isValid()).
            append(" isDirectory=").append(file.isDirectory()).
            append(" hasDocument=").append(document != null).
            append(" length=").append(file.getLength());
    sb.append("\nproject=").append(project.getName()).
            append(" default=").append(project.isDefault()).
            append(" open=").append(project.isOpen());;
    sb.append("\nfileType=").append(fileType.getName()).append("/").append(fileType.getClass().getName());
    sb.append("\nisIgnored=").append(ignored);
    sb.append(" underIgnored=").append(indexFacade.isUnderIgnored(file));
    sb.append(" inLibrary=").append(indexFacade.isInLibrarySource(file) || indexFacade.isInLibraryClasses(file));
    sb.append(" parentDir=").append(vDir == null ? "no-vfs" : vDir.isDirectory() ? "has-vfs-dir" : "has-vfs-file").
            append("/").append(psiDir == null ? "no-psi" : "has-psi");
    sb.append("\nviewProvider=").append(viewProvider == null ? "null" : viewProvider.getClass().getName());
    if (viewProvider != null) {
      List<PsiFile> files = viewProvider.getAllFiles();
      sb.append(" language=").append(viewProvider.getBaseLanguage().getID());
      sb.append(" physical=").append(viewProvider.isPhysical());
      sb.append(" rootCount=").append(files.size());
      for (PsiFile o : files) {
        sb.append("\n  root=").append(o.getLanguage().getID()).append("/").append(o.getClass().getName());
      }
    }
    LOG.error("no PSI for file '" + file.getName() + "'", new Attachment(file.getPresentableUrl(), sb.toString()));
    throw new AssertionError();
  }

  /**
   * @deprecated use CompletionUtil#getOriginalElement where appropriate instead
   */
  @Nullable
  public static <T extends PsiElement> T getOriginalElement(@Nonnull T psiElement, final Class<? extends T> elementClass) {
    final PsiFile psiFile = psiElement.getContainingFile();
    final PsiFile originalFile = psiFile.getOriginalFile();
    if (originalFile == psiFile) return psiElement;
    final TextRange range = psiElement.getTextRange();
    final PsiElement element = originalFile.findElementAt(range.getStartOffset());
    final int maxLength = range.getLength();
    T parent = PsiTreeUtil.getParentOfType(element, elementClass, false);
    T next = parent ;
    while (next != null && next.getTextLength() <= maxLength) {
      parent = next;
      next = PsiTreeUtil.getParentOfType(next, elementClass, true);
    }
    return parent;
  }

  @Nonnull
  public static Project getProjectInReadAction(@Nonnull final PsiElement element) {
    return ApplicationManager.getApplication().runReadAction(new Computable<Project>() {
      @Override
      public Project compute() {
        return element.getProject();
      }
    });
  }

  @Contract("null -> null;!null -> !null")
  public static IElementType getElementType(@Nullable ASTNode node) {
    return node == null ? null : node.getElementType();
  }

  @Contract("null -> null;!null -> !null")
  public static IElementType getElementType(@Nullable PsiElement element) {
    return element == null ? null : getElementType(element.getNode());
  }

  @Nonnull
  public static Language getLanguageAtOffset (@Nonnull PsiFile file, int offset) {
    final PsiElement elt = file.findElementAt(offset);
    if (elt == null) return file.getLanguage();
    if (elt instanceof PsiWhiteSpace) {
      final int decremented = elt.getTextRange().getStartOffset() - 1;
      if (decremented >= 0) {
        return getLanguageAtOffset(file, decremented);
      }
    }
    return findLanguageFromElement(elt);
  }

  @Nonnull
  public static Language findLanguageFromElement(final PsiElement elt) {
    if (elt.getFirstChild() == null) { //is leaf
      final PsiElement parent = elt.getParent();
      if (parent != null) {
        return parent.getLanguage();
      }
    }

    return elt.getLanguage();
  }

  @Nonnull
  public static LanguageVersion findLanguageVersionFromElement(final PsiElement elt) {
    if (elt.getFirstChild() == null) { //is leaf
      final PsiElement parent = elt.getParent();
      if (parent != null) {
        return parent.getLanguageVersion();
      }
    }

    return elt.getLanguageVersion();
  }

  @Nonnull
  public static PsiFile[] virtualToPsiFiles(@Nonnull final VirtualFile[] files, @Nonnull Project project) {
    PsiManager manager = PsiManager.getInstance(project);
    List<PsiFile> result = new ArrayList<PsiFile>();
    for (VirtualFile virtualFile : files) {
      PsiFile psiFile = manager.findFile(virtualFile);
      if (psiFile != null) result.add(psiFile);
    }
    return PsiUtilCore.toPsiFileArray(result);
  }

  @Nonnull
  public static PsiFile[] virtualToPsiFiles(@Nonnull final List<VirtualFile> files, @Nonnull Project project) {
    PsiManager manager = PsiManager.getInstance(project);
    List<PsiFile> result = new ArrayList<PsiFile>();
    for (VirtualFile virtualFile : files) {
      PsiFile psiFile = manager.findFile(virtualFile);
      if (psiFile != null) result.add(psiFile);
    }
    return PsiUtilCore.toPsiFileArray(result);
  }

  @Nullable
  public static PsiFileSystemItem findFileSystemItem(@Nullable Project project, @Nullable VirtualFile file) {
    if (project == null || file == null) return null;
    if (project.isDisposed() || !file.isValid()) return null;
    PsiManager psiManager = PsiManager.getInstance(project);
    return file.isDirectory() ? psiManager.findDirectory(file) : psiManager.findFile(file);
  }
}
