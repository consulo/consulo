// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.openapi.vcs.changes;

import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.change.VcsDirtyScope;
import jakarta.annotation.Nonnull;

public interface FileHolder {
  /**
   * Notify that CLM refresh has started, everything is dirty
   */
  void cleanAll();

  /**
   * Notify that CLM refresh has started for particular scope
   */
  void cleanUnderScope(@Nonnull VcsDirtyScope scope);

  FileHolder copy();

  default void notifyVcsStarted(@Nonnull AbstractVcs vcs) {
  }
}
