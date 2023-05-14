// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.inject.impl.internal;

import consulo.language.inject.ReferenceInjector;
import consulo.language.psi.PsiFile;
import consulo.util.lang.Pair;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

class InjectionResult implements Supplier<InjectionResult> {
  @Nullable
  final List<? extends PsiFile> files;
  @Nullable
  final List<? extends Pair<ReferenceInjector, PlaceImpl>> references;
  private final long myModificationCount;

  InjectionResult(@Nonnull PsiFile hostFile, @Nullable List<? extends PsiFile> files, @Nullable List<? extends Pair<ReferenceInjector, PlaceImpl>> references) {
    this.files = files;
    this.references = references;
    myModificationCount = calcModCount(hostFile);
  }

  @Override
  public InjectionResult get() {
    return this;
  }

  boolean isEmpty() {
    return files == null && references == null;
  }

  boolean isValid() {
    if (files != null) {
      for (PsiFile file : files) {
        if (!file.isValid()) return false;
      }
    }
    else if (references != null) {
      for (Pair<ReferenceInjector, PlaceImpl> pair : references) {
        PlaceImpl place = pair.getSecond();
        if (!place.isValid()) return false;
      }
    }
    return true;
  }

  boolean isModCountUpToDate(@Nonnull PsiFile hostPsiFile) {
    return myModificationCount == calcModCount(hostPsiFile);
  }

  private static long calcModCount(@Nonnull PsiFile hostPsiFile) {
    return (hostPsiFile.getModificationStamp() << 32) + hostPsiFile.getManager().getModificationTracker().getModificationCount();
  }
}
