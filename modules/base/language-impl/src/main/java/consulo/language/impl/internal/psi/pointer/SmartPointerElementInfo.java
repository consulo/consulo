// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.impl.internal.psi.pointer;

import consulo.document.Document;
import consulo.document.util.Segment;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
