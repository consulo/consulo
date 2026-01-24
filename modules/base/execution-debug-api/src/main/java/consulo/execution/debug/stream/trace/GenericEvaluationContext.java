// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.stream.trace;

import consulo.project.Project;

import jakarta.annotation.Nonnull;

public interface GenericEvaluationContext {
  @Nonnull
  Project getProject();
}
