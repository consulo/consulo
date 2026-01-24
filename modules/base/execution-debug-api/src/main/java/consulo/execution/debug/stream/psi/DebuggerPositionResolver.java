// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.psi;

import consulo.language.psi.PsiElement;
import consulo.execution.debug.XDebugSession;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Vitaliy.Bibaev
 */
public interface DebuggerPositionResolver {
  @Nullable
  PsiElement getNearestElementToBreakpoint(@Nonnull XDebugSession session);
}
