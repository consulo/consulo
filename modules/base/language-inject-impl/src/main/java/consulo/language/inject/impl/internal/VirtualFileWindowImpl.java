// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.inject.impl.internal;

import consulo.language.Language;
import consulo.language.file.inject.VirtualFileWindow;
import consulo.language.file.light.LightVirtualFile;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;

class VirtualFileWindowImpl extends LightVirtualFile implements VirtualFileWindow {
  private final VirtualFile myDelegate;
  private final DocumentWindowImpl myDocumentWindow;

  VirtualFileWindowImpl(@Nonnull String name, @Nonnull VirtualFile delegate, @Nonnull DocumentWindowImpl window, @Nonnull Language language, @Nonnull CharSequence text) {
    super(name, language, text);
    setCharset(delegate.getCharset());
    setFileType(language.getAssociatedFileType());
    if (delegate instanceof VirtualFileWindow) throw new IllegalArgumentException(delegate + " must not be injected");
    myDelegate = delegate;
    myDocumentWindow = window;
  }

  @Nonnull
  @Override
  public VirtualFile getDelegate() {
    return myDelegate;
  }

  @Nonnull
  @Override
  public DocumentWindowImpl getDocumentWindow() {
    return myDocumentWindow;
  }

  @Override
  public boolean isValid() {
    return myDelegate.isValid() && myDocumentWindow.isValid();
  }

  @Override
  public String toString() {
    return "VirtualFileWindow in " + myDelegate.getPresentableUrl();
  }
}