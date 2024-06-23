// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.change;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ExtensionAPI(ComponentScope.PROJECT)
public interface ChangeListChangeAssigner {
  @Nullable
  String getChangeListIdFor(@Nonnull Change change, @Nonnull ChangeListManagerGate gate);

  void beforeChangesProcessing(@Nullable VcsDirtyScope dirtyScope);

  void markChangesProcessed(@Nullable VcsDirtyScope dirtyScope);
}
