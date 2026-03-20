// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.impl.internal.psi.pointer;

import consulo.document.Document;
import consulo.document.util.Segment;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import org.jspecify.annotations.Nullable;

abstract class SmartPointerElementInfo {
  @Nullable Document getDocumentToSynchronize() {
    return null;
  }

  void fastenBelt(SmartPointerManagerImpl manager) {
  }

  abstract @Nullable PsiElement restoreElement(SmartPointerManagerImpl manager);

  abstract @Nullable PsiFile restoreFile(SmartPointerManagerImpl manager);

  abstract int elementHashCode(); // must be immutable

  abstract boolean pointsToTheSameElementAs(SmartPointerElementInfo other, SmartPointerManagerImpl manager);

  abstract VirtualFile getVirtualFile();

  abstract @Nullable Segment getRange(SmartPointerManagerImpl manager);

  void cleanup() {
  }

  abstract @Nullable Segment getPsiRange(SmartPointerManagerImpl manager);
}
