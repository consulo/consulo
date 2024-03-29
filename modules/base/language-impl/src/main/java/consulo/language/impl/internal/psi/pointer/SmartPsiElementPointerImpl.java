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

package consulo.language.impl.internal.psi.pointer;

import consulo.application.ApplicationManager;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.util.ProperTextRange;
import consulo.document.util.Segment;
import consulo.document.util.TextRange;
import consulo.language.file.FileViewProvider;
import consulo.language.impl.file.FreeThreadedFileViewProvider;
import consulo.language.impl.psi.PsiAnchor;
import consulo.language.impl.psi.PsiFileImpl;
import consulo.language.impl.internal.psi.PsiDocumentManagerBase;
import consulo.language.impl.internal.psi.PsiManagerEx;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.*;
import consulo.language.psi.stub.IStubFileElementType;
import consulo.language.util.LanguageUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.lang.Comparing;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

class SmartPsiElementPointerImpl<E extends PsiElement> implements SmartPointerEx<E> {
  private static final Logger LOG = Logger.getInstance(SmartPsiElementPointerImpl.class);

  private Reference<E> myElement;
  private final SmartPointerElementInfo myElementInfo;
  protected final SmartPointerManagerImpl myManager;
  private byte myReferenceCount = 1;
  @Nullable
  SmartPointerTracker.PointerReference pointerReference;

  SmartPsiElementPointerImpl(SmartPointerManagerImpl manager, @Nonnull E element, @Nullable PsiFile containingFile, boolean forInjected) {
    this(manager, element, createElementInfo(manager, element, containingFile, forInjected));
  }

  SmartPsiElementPointerImpl(SmartPointerManagerImpl manager, @Nonnull E element, @Nonnull SmartPointerElementInfo elementInfo) {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    myElementInfo = elementInfo;
    myManager = manager;
    cacheElement(element);
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof SmartPsiElementPointer && pointsToTheSameElementAs(this, (SmartPsiElementPointer)obj);
  }

  @Override
  public int hashCode() {
    return myElementInfo.elementHashCode();
  }

  @Override
  @Nonnull
  public Project getProject() {
    return myManager.getProject();
  }

  @Override
  @Nullable
  public E getElement() {
    if (getProject().isDisposed()) return null;

    E element = getCachedElement();
    if (element == null || !element.isValid()) {
      element = doRestoreElement();
      cacheElement(element);
    }
    return element;
  }

  @Nullable
  E doRestoreElement() {
    //noinspection unchecked
    E element = (E)myElementInfo.restoreElement(myManager);
    if (element != null && !element.isValid()) {
      return null;
    }
    return element;
  }

  void cacheElement(@Nullable E element) {
    myElement = element == null ? null : PsiManagerEx.getInstanceEx(getProject()).isBatchFilesProcessingMode() ? new WeakReference<>(element) : new SoftReference<>(element);
  }

  @Override
  public E getCachedElement() {
    return consulo.util.lang.ref.SoftReference.dereference(myElement);
  }

  @Override
  public PsiFile getContainingFile() {
    PsiFile file = getElementInfo().restoreFile(myManager);

    if (file != null) {
      return file;
    }

    final Document doc = myElementInfo.getDocumentToSynchronize();
    if (doc == null) {
      final E resolved = getElement();
      return resolved == null ? null : resolved.getContainingFile();
    }
    return PsiDocumentManager.getInstance(getProject()).getPsiFile(doc);
  }

  @Override
  public VirtualFile getVirtualFile() {
    return myElementInfo.getVirtualFile();
  }

  @Override
  public Segment getRange() {
    return myElementInfo.getRange(myManager);
  }

  @Nullable
  @Override
  public Segment getPsiRange() {
    return myElementInfo.getPsiRange(myManager);
  }

  @Nonnull
  private static <E extends PsiElement> SmartPointerElementInfo createElementInfo(SmartPointerManagerImpl manager, @Nonnull E element, PsiFile containingFile, boolean forInjected) {
    SmartPointerElementInfo elementInfo = doCreateElementInfo(manager.getProject(), element, containingFile, forInjected);
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      PsiElement restored = elementInfo.restoreElement(manager);
      if (!element.equals(restored)) {
        // likely cause: PSI having isPhysical==true, but which can't be restored by containing file and range. To fix, make isPhysical return false
        LOG.error("Cannot restore " + element + " of " + element.getClass() + " from " + elementInfo + "; restored=" + restored + " in " + element.getProject());
      }
    }
    return elementInfo;
  }

  @Nonnull
  private static <E extends PsiElement> SmartPointerElementInfo doCreateElementInfo(@Nonnull Project project, @Nonnull E element, PsiFile containingFile, boolean forInjected) {
    if (element instanceof PsiDirectory) {
      return new DirElementInfo((PsiDirectory)element);
    }
    if (element instanceof PsiCompiledElement || containingFile == null || !containingFile.isPhysical() || !element.isPhysical()) {
      if (element instanceof StubBasedPsiElement && element instanceof PsiCompiledElement) {
        if (element instanceof PsiFile) {
          return new FileElementInfo((PsiFile)element);
        }
        PsiAnchor.StubIndexReference stubReference = PsiAnchor.createStubReference(element, containingFile);
        if (stubReference != null) {
          return new ClsElementInfo(stubReference);
        }
      }
      return new HardElementInfo(element);
    }

    FileViewProvider viewProvider = containingFile.getViewProvider();
    if (viewProvider instanceof FreeThreadedFileViewProvider) {
      PsiLanguageInjectionHost hostContext = InjectedLanguageManager.getInstance(containingFile.getProject()).getInjectionHost(containingFile);
      TextRange elementRange = element.getTextRange();
      if (hostContext != null && elementRange != null) {
        SmartPsiElementPointer<PsiLanguageInjectionHost> hostPointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(hostContext);
        return new InjectedSelfElementInfo(project, element, elementRange, containingFile, hostPointer);
      }
    }

    if (element instanceof PsiFile) {
      return new FileElementInfo((PsiFile)element);
    }

    Document document = FileDocumentManager.getInstance().getCachedDocument(viewProvider.getVirtualFile());
    if (document != null && ((PsiDocumentManagerBase)PsiDocumentManager.getInstance(project)).getSynchronizer().isDocumentAffectedByTransactions(document)) {
      LOG.error("Smart pointers shouldn't be created during PSI changes");
    }

    SmartPointerElementInfo info = createAnchorInfo(element, containingFile);
    if (info != null) {
      return info;
    }

    TextRange elementRange = element.getTextRange();
    if (elementRange == null) {
      return new HardElementInfo(element);
    }
    IdentikitImpl.ByTypeImpl identikit = IdentikitImpl.fromPsi(element, LanguageUtil.getRootLanguage(element));
    if (elementRange.isEmpty() && identikit.findPsiElement(containingFile, elementRange.getStartOffset(), elementRange.getEndOffset()) != element) {
      // PSI has empty range, no text, but complicated structure (e.g. PSI built on C-style macro expansions). It can't be reliably
      // restored by just one offset in a file, so hold it on a hard reference
      return new HardElementInfo(element);
    }
    ProperTextRange proper = ProperTextRange.create(elementRange);
    return new SelfElementInfo(proper, identikit, containingFile, forInjected);
  }

  @Nullable
  private static SmartPointerElementInfo createAnchorInfo(@Nonnull PsiElement element, @Nonnull PsiFile containingFile) {
    if (element instanceof StubBasedPsiElement && containingFile instanceof PsiFileImpl) {
      IStubFileElementType stubType = ((PsiFileImpl)containingFile).getElementTypeForStubBuilder();
      if (stubType != null && stubType.shouldBuildStubFor(containingFile.getViewProvider().getVirtualFile())) {
        StubBasedPsiElement stubPsi = (StubBasedPsiElement)element;
        int stubId = PsiAnchor.calcStubIndex(stubPsi);
        if (stubId != -1) {
          return new AnchorElementInfo(element, (PsiFileImpl)containingFile, stubId, stubPsi.getElementType());
        }
      }
    }

    Pair<IdentikitImpl.ByAnchor, PsiElement> pair = IdentikitImpl.withAnchor(element, LanguageUtil.getRootLanguage(containingFile));
    if (pair != null) {
      return new AnchorElementInfo(pair.second, containingFile, pair.first);
    }
    return null;
  }

  @Nonnull
  SmartPointerElementInfo getElementInfo() {
    return myElementInfo;
  }

  static boolean pointsToTheSameElementAs(@Nonnull SmartPsiElementPointer pointer1, @Nonnull SmartPsiElementPointer pointer2) {
    if (pointer1 == pointer2) return true;
    if (pointer1 instanceof SmartPsiElementPointerImpl && pointer2 instanceof SmartPsiElementPointerImpl) {
      SmartPsiElementPointerImpl impl1 = (SmartPsiElementPointerImpl)pointer1;
      SmartPsiElementPointerImpl impl2 = (SmartPsiElementPointerImpl)pointer2;
      SmartPointerElementInfo elementInfo1 = impl1.getElementInfo();
      SmartPointerElementInfo elementInfo2 = impl2.getElementInfo();
      if (!elementInfo1.pointsToTheSameElementAs(elementInfo2, ((SmartPsiElementPointerImpl)pointer1).myManager)) return false;
      PsiElement cachedElement1 = impl1.getCachedElement();
      PsiElement cachedElement2 = impl2.getCachedElement();
      return cachedElement1 == null || cachedElement2 == null || Comparing.equal(cachedElement1, cachedElement2);
    }
    return Comparing.equal(pointer1.getElement(), pointer2.getElement());
  }

  synchronized int incrementAndGetReferenceCount(int delta) {
    if (myReferenceCount == Byte.MAX_VALUE) return Byte.MAX_VALUE; // saturated
    if (myReferenceCount == 0) return -1; // disposed, not to be reused again
    return myReferenceCount += delta;
  }

  @Override
  public String toString() {
    return myElementInfo.toString();
  }
}
