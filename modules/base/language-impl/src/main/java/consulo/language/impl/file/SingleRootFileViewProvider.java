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
package consulo.language.impl.file;

import consulo.component.ProcessCanceledException;
import consulo.language.Language;
import consulo.language.file.FileViewProvider;
import consulo.language.impl.DebugUtil;
import consulo.language.impl.ast.FileElement;
import consulo.language.impl.internal.psi.PsiManagerEx;
import consulo.language.psi.PsiFileEx;
import consulo.language.impl.psi.PsiFileImpl;
import consulo.language.plain.PlainTextLanguage;
import consulo.language.psi.*;
import consulo.language.util.LanguageUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.reflect.ReflectionUtil;
import consulo.virtualFileSystem.RawFileLoaderHelper;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;

import org.jspecify.annotations.Nullable;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SingleRootFileViewProvider extends AbstractFileViewProvider implements FileViewProvider {
  private static final Logger LOG = Logger.getInstance(SingleRootFileViewProvider.class);
  @SuppressWarnings("unused")
  private volatile PsiFile myPsiFile;
  private static final VarHandle ourPsiFileUpdater;

  static {
    try {
      ourPsiFileUpdater = MethodHandles.lookup().findVarHandle(SingleRootFileViewProvider.class, "myPsiFile", PsiFile.class);
    }
    catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  
  private final Language myBaseLanguage;

  public SingleRootFileViewProvider(PsiManager manager, VirtualFile file) {
    this(manager, file, true);
  }

  public SingleRootFileViewProvider(PsiManager manager, VirtualFile virtualFile, boolean eventSystemEnabled) {
    this(manager, virtualFile, eventSystemEnabled, calcBaseLanguage(virtualFile, manager.getProject(), virtualFile.getFileType()));
  }

  public SingleRootFileViewProvider(PsiManager manager, VirtualFile virtualFile, boolean eventSystemEnabled, FileType fileType) {
    this(manager, virtualFile, eventSystemEnabled, calcBaseLanguage(virtualFile, manager.getProject(), fileType));
  }

  protected SingleRootFileViewProvider(PsiManager manager, VirtualFile virtualFile, boolean eventSystemEnabled, Language language) {
    super(manager, virtualFile, eventSystemEnabled);
    myBaseLanguage = language;
  }

  @Override
  
  public Language getBaseLanguage() {
    return myBaseLanguage;
  }

  private static Language calcBaseLanguage(VirtualFile file, Project project, FileType fileType) {
    if (fileType.isBinary()) return Language.ANY;
    if (RawFileLoaderHelper.isTooLargeForIntelligence(file)) return PlainTextLanguage.INSTANCE;

    Language language = LanguageUtil.getLanguageForPsi(project, file);

    return language != null ? language : PlainTextLanguage.INSTANCE;
  }

  @Override
  
  public Set<Language> getLanguages() {
    return Collections.singleton(getBaseLanguage());
  }

  @Override
  
  public List<PsiFile> getAllFiles() {
    return ContainerUtil.createMaybeSingletonList(getPsi(getBaseLanguage()));
  }

  @Override
  protected @Nullable PsiFile getPsiInner(Language target) {
    if (target != getBaseLanguage()) {
      return null;
    }
    PsiFile psiFile = myPsiFile;
    if (psiFile == null) {
      psiFile = createFile();
      if (psiFile == null) {
        psiFile = PsiUtilCore.NULL_PSI_FILE;
      }
      boolean set = ourPsiFileUpdater.compareAndSet(this, null, psiFile);
      if (!set && psiFile != PsiUtilCore.NULL_PSI_FILE) {
        PsiFile alreadyCreated = myPsiFile;
        if (alreadyCreated == psiFile) {
          LOG.error(this + ".createFile() must create new file instance but got the same: " + psiFile);
        }
        if (psiFile instanceof PsiFileEx) {
          PsiFile finalPsiFile = psiFile;
          DebugUtil.performPsiModification("invalidating throw-away copy", () -> ((PsiFileEx)finalPsiFile).markInvalidated());
        }
        psiFile = alreadyCreated;
      }
    }
    return psiFile == PsiUtilCore.NULL_PSI_FILE ? null : psiFile;
  }

  @Override
  public final PsiFile getCachedPsi(Language target) {
    if (target != getBaseLanguage()) return null;
    PsiFile file = myPsiFile;
    return file == PsiUtilCore.NULL_PSI_FILE ? null : file;
  }

  
  @Override
  public final List<PsiFile> getCachedPsiFiles() {
    return ContainerUtil.createMaybeSingletonList(getCachedPsi(getBaseLanguage()));
  }

  @Override
  
  public final List<FileElement> getKnownTreeRoots() {
    PsiFile psiFile = getCachedPsi(getBaseLanguage());
    if (!(psiFile instanceof PsiFileImpl)) return Collections.emptyList();
    FileElement element = ((PsiFileImpl)psiFile).getTreeElement();
    return ContainerUtil.createMaybeSingletonList(element);
  }

  private PsiFile createFile() {
    try {
      return shouldCreatePsi() ? createFile(getManager().getProject(), getVirtualFile(), getFileType()) : null;
    }
    catch (ProcessCanceledException e) {
      throw e;
    }
    catch (Throwable e) {
      LOG.error(e);
      return null;
    }
  }

  
  @Override
  public SingleRootFileViewProvider createCopy(VirtualFile copy) {
    return new SingleRootFileViewProvider(getManager(), copy, false, getBaseLanguage());
  }

  @Override
  public PsiReference findReferenceAt(int offset) {
    PsiFile psiFile = getPsi(getBaseLanguage());
    return findReferenceAt(psiFile, offset);
  }

  @Override
  public PsiElement findElementAt(int offset) {
    return findElementAt(getPsi(getBaseLanguage()), offset);
  }

  @Override
  public PsiElement findElementAt(int offset, Class<? extends Language> lang) {
    if (!ReflectionUtil.isAssignable(lang, getBaseLanguage().getClass())) return null;
    return findElementAt(offset);
  }

  public final void forceCachedPsi(PsiFile psiFile) {
    while (true) {
      PsiFile prev = myPsiFile;
      if (ourPsiFileUpdater.compareAndSet(this, prev, psiFile)) {
        if (prev != psiFile && prev instanceof PsiFileEx) {
          ((PsiFileEx)prev).markInvalidated();
        }
        break;
      }
    }
    ((PsiManagerEx)getManager()).getFileManager().setViewProvider(getVirtualFile(), this);
  }

  // region deprecated methods

  @Deprecated
  public static boolean isTooLargeForIntelligence(VirtualFile vFile) {
    return RawFileLoaderHelper.isTooLargeForIntelligence(vFile);
  }

  @Deprecated
  public static boolean isTooLargeForContentLoading(VirtualFile vFile) {
    return RawFileLoaderHelper.isTooLargeForContentLoading(vFile);
  }

  @Deprecated
  public static void doNotCheckFileSizeLimit(VirtualFile vFile) {
    RawFileLoaderHelper.doNotCheckFileSizeLimit(vFile);
  }

  @Deprecated
  public static boolean isTooLargeForIntelligence(VirtualFile vFile, long contentSize) {
    return RawFileLoaderHelper.isTooLargeForIntelligence(vFile, contentSize);
  }

  @Deprecated
  public static boolean isTooLargeForContentLoading(VirtualFile vFile, long contentSize) {
    return RawFileLoaderHelper.isTooLargeForContentLoading(vFile, contentSize);
  }

  @Deprecated
  public static boolean fileSizeIsGreaterThan(VirtualFile vFile, long maxBytes) {
    return RawFileLoaderHelper.fileSizeIsGreaterThan(vFile, maxBytes);
  }

  // endregion
}
