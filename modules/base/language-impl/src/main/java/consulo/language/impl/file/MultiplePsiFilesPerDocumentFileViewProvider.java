/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.language.impl.file;

import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.impl.ast.FileElement;
import consulo.language.impl.psi.PsiFileImpl;
import consulo.language.impl.internal.psi.PsiDocumentManagerBase;
import consulo.language.impl.internal.psi.SharedPsiElementImplUtil;
import consulo.language.psi.*;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Maps;
import consulo.util.lang.reflect.ReflectionUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.annotations.TestOnly;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

/**
 * @author max
 */
public abstract class MultiplePsiFilesPerDocumentFileViewProvider extends AbstractFileViewProvider {
  protected final ConcurrentMap<Language, PsiFileImpl> myRoots = ContainerUtil.newConcurrentMap(1, 0.75f, 1);
  private MultiplePsiFilesPerDocumentFileViewProvider myOriginal;

  public MultiplePsiFilesPerDocumentFileViewProvider(@Nonnull PsiManager manager, @Nonnull VirtualFile virtualFile, boolean eventSystemEnabled) {
    super(manager, virtualFile, eventSystemEnabled);
  }

  @Override
  @Nonnull
  public abstract Language getBaseLanguage();

  @Override
  @Nonnull
  public List<PsiFile> getAllFiles() {
    List<PsiFile> roots = new ArrayList<>();
    for (Language language : getLanguages()) {
      PsiFile psi = getPsi(language);
      if (psi != null) roots.add(psi);
    }
    PsiFile base = getPsi(getBaseLanguage());
    if (!roots.isEmpty() && roots.get(0) != base) {
      roots.remove(base);
      roots.add(0, base);
    }
    return roots;
  }

  protected final void removeFile(@Nonnull Language language) {
    PsiFileImpl file = myRoots.remove(language);
    if (file != null) {
      file.markInvalidated();
    }
  }

  @Override
  protected PsiFile getPsiInner(@Nonnull Language target) {
    PsiFileImpl file = myRoots.get(target);
    if (file == null) {
      if (!shouldCreatePsi()) return null;
      if (target != getBaseLanguage() && !getLanguages().contains(target)) {
        return null;
      }
      file = createPsiFileImpl(target);
      if (file == null) return null;
      if (myOriginal != null) {
        PsiFile originalFile = myOriginal.getPsi(target);
        if (originalFile != null) {
          file.setOriginalFile(originalFile);
        }
      }
      file = Maps.cacheOrGet(myRoots, target, file);
    }
    return file;
  }

  @Nullable
  protected PsiFileImpl createPsiFileImpl(@Nonnull Language target) {
    return (PsiFileImpl)createFile(target);
  }

  @Override
  public final PsiFile getCachedPsi(@Nonnull Language target) {
    return myRoots.get(target);
  }

  @Nonnull
  @Override
  public final List<PsiFile> getCachedPsiFiles() {
    return ContainerUtil.mapNotNull(myRoots.keySet(), this::getCachedPsi);
  }

  @Nonnull
  @Override
  public final List<FileElement> getKnownTreeRoots() {
    List<FileElement> files = new ArrayList<>(myRoots.size());
    for (PsiFile file : myRoots.values()) {
      FileElement treeElement = ((PsiFileImpl)file).getTreeElement();
      if (treeElement != null) {
        files.add(treeElement);
      }
    }

    return files;
  }

  @TestOnly
  public void checkAllTreesEqual() {
    Collection<PsiFileImpl> roots = myRoots.values();
    PsiDocumentManager documentManager = PsiDocumentManager.getInstance(getManager().getProject());
    documentManager.commitAllDocuments();
    for (PsiFile root : roots) {
      Document document = documentManager.getDocument(root);
      assert document != null;
      documentManager.checkConsistency(root, document);
      assert root.getText().equals(document.getText());
    }
  }

  @Nonnull
  @Override
  public final MultiplePsiFilesPerDocumentFileViewProvider createCopy(@Nonnull VirtualFile fileCopy) {
    MultiplePsiFilesPerDocumentFileViewProvider copy = cloneInner(fileCopy);
    copy.myOriginal = myOriginal == null ? this : myOriginal;
    return copy;
  }

  @Nonnull
  protected abstract MultiplePsiFilesPerDocumentFileViewProvider cloneInner(@Nonnull VirtualFile fileCopy);

  @Override
  @Nullable
  public PsiElement findElementAt(int offset, @Nonnull Class<? extends Language> lang) {
    PsiFile mainRoot = getPsi(getBaseLanguage());
    PsiElement ret = null;
    for (Language language : getLanguages()) {
      if (!ReflectionUtil.isAssignable(lang, language.getClass())) continue;
      if (lang.equals(Language.class) && !getLanguages().contains(language)) continue;

      PsiFile psiRoot = getPsi(language);
      PsiElement psiElement = findElementAt(psiRoot, offset);
      if (psiElement == null || psiElement instanceof OuterLanguageElement) continue;
      if (ret == null || psiRoot != mainRoot) {
        ret = psiElement;
      }
    }
    return ret;
  }

  @Override
  @Nullable
  public PsiElement findElementAt(int offset) {
    return findElementAt(offset, Language.class);
  }

  @Override
  @Nullable
  public PsiReference findReferenceAt(int offset) {
    TextRange minRange = new TextRange(0, getContents().length());
    PsiReference ret = null;
    for (Language language : getLanguages()) {
      PsiElement psiRoot = getPsi(language);
      PsiReference reference = SharedPsiElementImplUtil.findReferenceAt(psiRoot, offset, language);
      if (reference == null) continue;
      TextRange textRange = reference.getRangeInElement().shiftRight(reference.getElement().getTextRange().getStartOffset());
      if (minRange.contains(textRange) && (!textRange.contains(minRange) || ret == null)) {
        minRange = textRange;
        ret = reference;
      }
    }
    return ret;
  }

  @Override
  public void contentsSynchronized() {
    Set<Language> languages = getLanguages();
    for (Iterator<Map.Entry<Language, PsiFileImpl>> iterator = myRoots.entrySet().iterator(); iterator.hasNext(); ) {
      Map.Entry<Language, PsiFileImpl> entry = iterator.next();
      if (!languages.contains(entry.getKey())) {
        PsiFileImpl file = entry.getValue();
        iterator.remove();
        file.markInvalidated();
      }
    }
    super.contentsSynchronized();
  }

}
