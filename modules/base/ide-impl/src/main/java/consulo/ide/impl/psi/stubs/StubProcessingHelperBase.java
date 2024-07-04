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
package consulo.ide.impl.psi.stubs;

import consulo.language.impl.psi.PsiFileImpl;
import consulo.language.psi.PsiBinaryFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.stub.PsiFileWithStubSupport;
import consulo.language.psi.stub.*;
import consulo.language.psi.stub.StubbedSpine;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.content.scope.ProjectAwareSearchScope;
import consulo.util.collection.ContainerUtil;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * Author: dmitrylomov
 */
public abstract class StubProcessingHelperBase {
  private static final Logger LOG = Logger.getInstance(StubProcessingHelperBase.class);

  public <Psi extends PsiElement> boolean processStubsInFile(@Nonnull Project project,
                                                             @Nonnull VirtualFile file,
                                                             @Nonnull StubIdList value,
                                                             @Nonnull Predicate<? super Psi> processor,
                                                             @Nullable ProjectAwareSearchScope scope,
                                                             @Nonnull Class<Psi> requiredClass) {
    PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
    if (psiFile == null) {
      LOG.error("Stub index points to a file without PSI: " + file.getFileType() + ", used scope " + scope);
      onInternalError(file);
      return true;
    }

    if (value.size() == 1 && value.get(0) == 0) {
      //noinspection unchecked
      return !checkType(requiredClass, psiFile, psiFile) || processor.test((Psi)psiFile);
    }

    List<StubbedSpine> spines = getAllSpines(psiFile);
    if (spines.isEmpty()) {
      return handleNonPsiStubs(file, processor, requiredClass, psiFile);
    }

    for (int i = 0, size = value.size(); i < size; i++) {
      PsiElement psi = getStubPsi(spines, value.get(i));
      if (!checkType(requiredClass, psiFile, psi)) break;
      //noinspection unchecked
      if (!processor.test((Psi)psi)) return false;
    }
    return true;
  }

  @Nonnull
  private static List<StubbedSpine> getAllSpines(PsiFile psiFile) {
    if (!(psiFile instanceof PsiFileImpl) && psiFile instanceof PsiFileWithStubSupport psiFileWithStubSupport) {
      return Collections.singletonList(psiFileWithStubSupport.getStubbedSpine());
    }

    return ContainerUtil.map(StubTreeBuilder.getStubbedRoots(psiFile.getViewProvider()), t -> ((PsiFileImpl)t.second).getStubbedSpine());
  }

  private <Psi extends PsiElement> boolean checkType(@Nonnull Class<Psi> requiredClass, PsiFile psiFile, PsiElement psiElement) {
    if (requiredClass.isInstance(psiElement)) return true;

    StubTree stubTree = ((PsiFileWithStubSupport)psiFile).getStubTree();
    if (stubTree == null && psiFile instanceof PsiFileImpl psiFileImpl) {
      stubTree = psiFileImpl.calcStubTree();
    }
    inconsistencyDetected(stubTree, (PsiFileWithStubSupport)psiFile);
    return false;
  }

  private static PsiElement getStubPsi(List<? extends StubbedSpine> spines, int index) {
    if (spines.size() == 1) return spines.get(0).getStubPsi(index);

    for (StubbedSpine spine : spines) {
      int count = spine.getStubCount();
      if (index < count) {
        return spine.getStubPsi(index);
      }
      index -= count;
    }
    return null;
  }

  // e.g. DOM indices
  private <Psi extends PsiElement> boolean handleNonPsiStubs(@Nonnull VirtualFile file, @Nonnull Predicate<? super Psi> processor, @Nonnull Class<Psi> requiredClass, @Nonnull PsiFile psiFile) {
    if (BinaryFileStubBuilder.forFileType(psiFile.getFileType()) == null) {
      LOG.error("unable to get stub builder for " + psiFile.getFileType() + ", " + StubTreeLoader.getFileViewProviderMismatchDiagnostics(psiFile.getViewProvider()));
      onInternalError(file);
      return true;
    }

    if (psiFile instanceof PsiBinaryFile) {
      // a file can be indexed as containing stubs,
      // but then in a specific project FileViewProviderFactory can decide not to create stub-aware PSI
      // because the file isn't in expected location
      return true;
    }

    ObjectStubTree objectStubTree = StubTreeLoader.getInstance().readFromVFile(psiFile.getProject(), file);
    if (objectStubTree == null) {
      LOG.error("Stub index points to a file without indexed stubs: " + psiFile.getFileType());
      onInternalError(file);
      return true;
    }
    if (objectStubTree instanceof StubTree) {
      LOG.error("Stub index points to a file with PSI stubs (instead of non-PSI ones): " + psiFile.getFileType());
      onInternalError(file);
      return true;
    }
    if (!requiredClass.isInstance(psiFile)) {
      inconsistencyDetected(objectStubTree, (PsiFileWithStubSupport)psiFile);
      return true;
    }
    //noinspection unchecked
    return processor.test((Psi)psiFile);
  }

  private void inconsistencyDetected(@Nullable ObjectStubTree stubTree, @Nonnull PsiFileWithStubSupport psiFile) {
    try {
      StubTextInconsistencyException.checkStubTextConsistency(psiFile);
      LOG.error(StubTreeLoader.getInstance().stubTreeAndIndexDoNotMatch(stubTree, psiFile, null));
    }
    finally {
      onInternalError(psiFile.getVirtualFile());
    }
  }

  protected abstract void onInternalError(VirtualFile file);
}
