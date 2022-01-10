// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.psi.impl.smartPointers;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.Segment;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

abstract class SmartPointerElementInfo {
  @Nullable
  Document getDocumentToSynchronize() {
    return null;
  }

  void fastenBelt(@Nonnull SmartPointerManagerImpl manager) {
  }

  @Nullable
  abstract PsiElement restoreElement(@Nonnull SmartPointerManagerImpl manager);

  @Nullable
  abstract PsiFile restoreFile(@Nonnull SmartPointerManagerImpl manager);

  abstract int elementHashCode(); // must be immutable

  abstract boolean pointsToTheSameElementAs(@Nonnull SmartPointerElementInfo other, @Nonnull SmartPointerManagerImpl manager);

  abstract VirtualFile getVirtualFile();

  @Nullable
  abstract Segment getRange(@Nonnull SmartPointerManagerImpl manager);

  void cleanup() {
  }

  @Nullable
  abstract Segment getPsiRange(@Nonnull SmartPointerManagerImpl manager);
}
