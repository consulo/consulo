// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.smartPointers;

import com.intellij.openapi.application.ApplicationManager;
import consulo.logging.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.impl.FrozenDocument;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import consulo.util.dataholder.Key;
import com.intellij.openapi.util.ProperTextRange;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiDocumentManagerBase;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.reference.SoftReference;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.TestOnly;

import java.lang.ref.Reference;
import java.util.List;

@Singleton
public final class SmartPointerManagerImpl extends SmartPointerManager {
  private static final Logger LOG = Logger.getInstance(SmartPointerManagerImpl.class);
  private final Project myProject;
  private final Key<SmartPointerTracker> POINTERS_KEY;
  private final PsiDocumentManagerBase myPsiDocManager;

  @Inject
  public SmartPointerManagerImpl(@Nonnull Project project) {
    myProject = project;
    myPsiDocManager = (PsiDocumentManagerBase)PsiDocumentManager.getInstance(project);
    POINTERS_KEY = Key.create("SMART_POINTERS " + (project.isDefault() ? "default" : project.hashCode()));
  }

  @Nonnull
  private static String anonymize(@Nonnull Project project) {
    return (project.isDisposed() ? "(Disposed)" : "") + (project.isDefault() ? "(Default)" : "") + project.hashCode();
  }

  public void fastenBelts(@Nonnull VirtualFile file) {
    SmartPointerTracker pointers = getTracker(file);
    if (pointers != null) pointers.fastenBelts(this);
  }

  private static final Key<Reference<SmartPsiElementPointerImpl<?>>> CACHED_SMART_POINTER_KEY = Key.create("CACHED_SMART_POINTER_KEY");

  @Override
  @Nonnull
  public <E extends PsiElement> SmartPsiElementPointer<E> createSmartPsiElementPointer(@Nonnull E element) {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    PsiFile containingFile = element.getContainingFile();
    return createSmartPsiElementPointer(element, containingFile);
  }

  @Override
  @Nonnull
  public <E extends PsiElement> SmartPsiElementPointer<E> createSmartPsiElementPointer(@Nonnull E element, PsiFile containingFile) {
    return createSmartPsiElementPointer(element, containingFile, false);
  }

  @Nonnull
  public <E extends PsiElement> SmartPsiElementPointer<E> createSmartPsiElementPointer(@Nonnull E element, PsiFile containingFile, boolean forInjected) {
    ensureValid(element, containingFile);
    SmartPointerTracker.processQueue();
    ensureMyProject(containingFile != null ? containingFile.getProject() : element.getProject());
    SmartPsiElementPointerImpl<E> pointer = getCachedPointer(element);
    if (pointer != null &&
        (!(pointer.getElementInfo() instanceof SelfElementInfo) || ((SelfElementInfo)pointer.getElementInfo()).isForInjected() == forInjected) &&
        pointer.incrementAndGetReferenceCount(1) > 0) {
      return pointer;
    }

    pointer = new SmartPsiElementPointerImpl<>(this, element, containingFile, forInjected);
    if (containingFile != null) {
      trackPointer(pointer, containingFile.getViewProvider().getVirtualFile());
    }
    element.putUserData(CACHED_SMART_POINTER_KEY, new SoftReference<>(pointer));
    return pointer;
  }

  private void ensureMyProject(@Nonnull Project project) {
    if (project != myProject) {
      throw new IllegalArgumentException("Element from alien project: " + anonymize(project) + " expected: " + anonymize(myProject));
    }
  }

  private static void ensureValid(@Nonnull PsiElement element, @Nullable PsiFile containingFile) {
    boolean valid = containingFile != null ? containingFile.isValid() : element.isValid();
    if (!valid) {
      PsiUtilCore.ensureValid(element);
      if (containingFile != null && !containingFile.isValid()) {
        throw new PsiInvalidElementAccessException(containingFile, "Element " + element.getClass() + "(" + element.getLanguage() + ")" + " claims to be valid but returns invalid containing file ");
      }
    }
  }

  private static <E extends PsiElement> SmartPsiElementPointerImpl<E> getCachedPointer(@Nonnull E element) {
    Reference<SmartPsiElementPointerImpl<?>> data = element.getUserData(CACHED_SMART_POINTER_KEY);
    SmartPsiElementPointerImpl<?> cachedPointer = SoftReference.dereference(data);
    if (cachedPointer != null) {
      PsiElement cachedElement = cachedPointer.getElement();
      if (cachedElement != element) {
        return null;
      }
    }
    //noinspection unchecked
    return (SmartPsiElementPointerImpl<E>)cachedPointer;
  }

  @Override
  @Nonnull
  public SmartPsiFileRange createSmartPsiFileRangePointer(@Nonnull PsiFile file, @Nonnull TextRange range) {
    return createSmartPsiFileRangePointer(file, range, false);
  }

  @Nonnull
  public SmartPsiFileRange createSmartPsiFileRangePointer(@Nonnull PsiFile file, @Nonnull TextRange range, boolean forInjected) {
    PsiUtilCore.ensureValid(file);
    SmartPointerTracker.processQueue();
    SmartPsiFileRangePointerImpl pointer = new SmartPsiFileRangePointerImpl(this, file, ProperTextRange.create(range), forInjected);
    trackPointer(pointer, file.getViewProvider().getVirtualFile());

    return pointer;
  }

  private <E extends PsiElement> void trackPointer(@Nonnull SmartPsiElementPointerImpl<E> pointer, @Nonnull VirtualFile containingFile) {
    SmartPointerElementInfo info = pointer.getElementInfo();
    if (!(info instanceof SelfElementInfo)) return;

    SmartPointerTracker.PointerReference reference = new SmartPointerTracker.PointerReference(pointer, containingFile, POINTERS_KEY);
    while (true) {
      SmartPointerTracker pointers = getTracker(containingFile);
      if (pointers == null) {
        pointers = containingFile.putUserDataIfAbsent(POINTERS_KEY, new SmartPointerTracker());
      }
      if (pointers.addReference(reference, pointer)) {
        break;
      }
    }
  }

  @Override
  public void removePointer(@Nonnull SmartPsiElementPointer pointer) {
    if (!(pointer instanceof SmartPsiElementPointerImpl) || myProject.isDisposed()) {
      return;
    }
    ensureMyProject(pointer.getProject());
    int refCount = ((SmartPsiElementPointerImpl<?>)pointer).incrementAndGetReferenceCount(-1);
    if (refCount == -1) {
      LOG.error("Double smart pointer removal");
      return;
    }

    if (refCount == 0) {
      PsiElement element = ((SmartPointerEx<?>)pointer).getCachedElement();
      if (element != null) {
        element.putUserData(CACHED_SMART_POINTER_KEY, null);
      }

      SmartPointerElementInfo info = ((SmartPsiElementPointerImpl<?>)pointer).getElementInfo();
      info.cleanup();

      SmartPointerTracker.PointerReference reference = ((SmartPsiElementPointerImpl<?>)pointer).pointerReference;
      if (reference != null) {
        if (reference.get() != pointer) {
          throw new IllegalStateException("Reference points to " + reference.get());
        }
        if (reference.key != POINTERS_KEY) {
          throw new IllegalStateException("Reference from wrong project: " + reference.key + " vs " + POINTERS_KEY);
        }
        SmartPointerTracker pointers = getTracker(reference.file);
        if (pointers != null) {
          pointers.removeReference(reference);
        }
      }
    }
  }

  @Nullable
  SmartPointerTracker getTracker(@Nonnull VirtualFile containingFile) {
    return containingFile.getUserData(POINTERS_KEY);
  }

  @TestOnly
  public int getPointersNumber(@Nonnull PsiFile containingFile) {
    VirtualFile file = containingFile.getViewProvider().getVirtualFile();
    SmartPointerTracker pointers = getTracker(file);
    return pointers == null ? 0 : pointers.getSize();
  }

  @Override
  public boolean pointToTheSameElement(@Nonnull SmartPsiElementPointer pointer1, @Nonnull SmartPsiElementPointer pointer2) {
    return SmartPsiElementPointerImpl.pointsToTheSameElementAs(pointer1, pointer2);
  }

  public void updatePointers(@Nonnull Document document, @Nonnull FrozenDocument frozen, @Nonnull List<? extends DocumentEvent> events) {
    VirtualFile file = FileDocumentManager.getInstance().getFile(document);
    SmartPointerTracker list = file == null ? null : getTracker(file);
    if (list != null) list.updateMarkers(frozen, events);
  }

  public void updatePointerTargetsAfterReparse(@Nonnull VirtualFile file) {
    SmartPointerTracker list = getTracker(file);
    if (list != null) list.updatePointerTargetsAfterReparse();
  }

  @Nonnull
  Project getProject() {
    return myProject;
  }

  @Nonnull
  PsiDocumentManagerBase getPsiDocumentManager() {
    return myPsiDocManager;
  }
}
