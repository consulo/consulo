// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.inject.impl.internal;

import consulo.document.Document;
import consulo.document.util.Segment;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.file.FileViewProvider;
import consulo.document.DocumentWindow;
import consulo.language.file.inject.VirtualFileWindow;
import consulo.language.impl.file.AbstractFileViewProvider;
import consulo.language.impl.file.FreeThreadedFileViewProvider;
import consulo.language.impl.file.MultiplePsiFilesPerDocumentFileViewProvider;
import consulo.language.impl.internal.psi.PsiManagerEx;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.language.template.TemplateLanguageFileViewProvider;
import consulo.util.lang.ref.Ref;

import jakarta.annotation.Nonnull;
import java.util.List;

public interface InjectedFileViewProvider extends FileViewProvider, FreeThreadedFileViewProvider {
  default void rootChangedImpl(@Nonnull PsiFile psiFile) {
    if (!isPhysical()) return; // injected PSI change happened inside reparse; ignore
    if (getPatchingLeaves()) return;

    DocumentWindowImpl documentWindow = getDocument();
    List<PsiLanguageInjectionHost.Shred> shreds = documentWindow.getShreds();
    assert documentWindow.getHostRanges().length == shreds.size();
    String[] changes = documentWindow.calculateMinEditSequence(psiFile.getNode().getText());
    assert changes.length == shreds.size();
    for (int i = 0; i < changes.length; i++) {
      String change = changes[i];
      if (change != null) {
        PsiLanguageInjectionHost.Shred shred = shreds.get(i);
        PsiLanguageInjectionHost host = shred.getHost();
        TextRange rangeInsideHost = shred.getRangeInsideHost();
        String newHostText = rangeInsideHost.replace(host.getText(), change);
        //shred.host =
        host.updateText(newHostText);
      }
    }
  }

  default FileViewProvider cloneImpl() {
    DocumentWindow oldDocumentWindow = ((VirtualFileWindow)getVirtualFile()).getDocumentWindow();
    Document hostDocument = oldDocumentWindow.getDelegate();
    PsiDocumentManager documentManager = PsiDocumentManager.getInstance(getManager().getProject());
    PsiFile hostFile = documentManager.getPsiFile(hostDocument);
    Language language = getBaseLanguage();
    PsiFile file = getPsi(language);
    Language hostFileLanguage = InjectedLanguageManager.getInstance(file.getProject()).getTopLevelFile(file).getLanguage();
    PsiFile hostPsiFileCopy = (PsiFile)hostFile.copy();
    Segment firstTextRange = oldDocumentWindow.getHostRanges()[0];
    PsiElement hostElementCopy = hostPsiFileCopy.getViewProvider().findElementAt(firstTextRange.getStartOffset(), hostFileLanguage);
    assert hostElementCopy != null;
    Ref<FileViewProvider> provider = new Ref<>();
    PsiLanguageInjectionHost.InjectedPsiVisitor visitor = (injectedPsi, places) -> {
      Document document = documentManager.getCachedDocument(injectedPsi);
      if (document instanceof DocumentWindowImpl && oldDocumentWindow.areRangesEqual((DocumentWindowImpl)document)) {
        provider.set(injectedPsi.getViewProvider());
      }
    };
    for (PsiElement current = hostElementCopy; current != null && current != hostPsiFileCopy; current = current.getParent()) {
      current.putUserData(SingleRootInjectedFileViewProvider.LANGUAGE_FOR_INJECTED_COPY_KEY, language);
      try {
        InjectedLanguageManager.getInstance(hostPsiFileCopy.getProject()).enumerateEx(current, hostPsiFileCopy, false, visitor);
      }
      finally {
        current.putUserData(SingleRootInjectedFileViewProvider.LANGUAGE_FOR_INJECTED_COPY_KEY, null);
      }
      if (provider.get() != null) break;
    }
    return provider.get();
  }

  // returns true if shreds were set, false if old ones were reused
  default boolean setShreds(@Nonnull PlaceImpl newShreds) {
    synchronized (getLock()) {
      PlaceImpl oldShreds = getDocument().getShreds();
      // try to reuse shreds, otherwise there are too many range markers disposals/re-creations
      if (same(oldShreds, newShreds)) {
        return false;
      }
      getDocument().setShreds(newShreds);
      return true;
    }
  }

  static boolean same(PlaceImpl oldShreds, PlaceImpl newShreds) {
    if (oldShreds == newShreds) return true;
    if (oldShreds.size() != newShreds.size()) return false;
    for (int i = 0; i < oldShreds.size(); i++) {
      PsiLanguageInjectionHost.Shred oldShred = oldShreds.get(i);
      PsiLanguageInjectionHost.Shred newShred = newShreds.get(i);
      if (!oldShred.equals(newShred)) return false;
    }
    return true;
  }

  default boolean isPhysicalImpl() {
    return isEventSystemEnabled();
  }

  default void performNonPhysically(Runnable runnable) {
    synchronized (getLock()) {
      SingleRootInjectedFileViewProvider.disabledTemporarily.set(true);
      try {
        runnable.run();
      }
      finally {
        SingleRootInjectedFileViewProvider.disabledTemporarily.set(false);
      }
    }
  }

  boolean getPatchingLeaves();

  void forceCachedPsi(@Nonnull PsiFile file);

  Object getLock();

  default boolean isValid() {
    return getShreds().isValid();
  }

  default boolean isDisposed() {
    return getManager().getProject().isDisposed();
  }

  default PlaceImpl getShreds() {
    return getDocument().getShreds();
  }

  default boolean isEventSystemEnabledImpl() {
    return !SingleRootInjectedFileViewProvider.disabledTemporarily.get();
  }

  @Override
  @Nonnull
  DocumentWindowImpl getDocument();

  static InjectedFileViewProvider create(@Nonnull PsiManagerEx manager, @Nonnull VirtualFileWindowImpl file, @Nonnull DocumentWindowImpl window, @Nonnull Language language) {
    AbstractFileViewProvider original = (AbstractFileViewProvider)manager.getFileManager().createFileViewProvider(file, false);
    return original instanceof TemplateLanguageFileViewProvider
           ? new MultipleRootsInjectedFileViewProvider.Template(manager, file, window, language, original)
           : original instanceof MultiplePsiFilesPerDocumentFileViewProvider
             ? new MultipleRootsInjectedFileViewProvider(manager, file, window, language, original)
             : new SingleRootInjectedFileViewProvider(manager, file, window, language);
  }
}
