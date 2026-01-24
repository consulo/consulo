// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.stream.psi.impl;

import consulo.execution.debug.stream.psi.DebuggerPositionResolver;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.XSourcePosition;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public final class DebuggerPositionResolverImpl implements DebuggerPositionResolver {
  @Override
  public @Nullable PsiElement getNearestElementToBreakpoint(@Nonnull XDebugSession session) {
    final XSourcePosition position = session.getCurrentPosition();
    if (position == null) return null;

    int offset = position.getOffset();
    final VirtualFile file = position.getFile();
    if (file.isValid() && 0 <= offset && offset < file.getLength()) {
      @Nullable PsiFile psiFile = PsiManager.getInstance(session.getProject()).findFile(file);
      return psiFile != null && psiFile.isValid() ? psiFile.findElementAt(offset) : null;
    }

    return null;
  }
}
