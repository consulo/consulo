// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.impl.internal.change;

import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.VcsListener;
import consulo.versionControlSystem.change.Change;
import jakarta.annotation.Nonnull;

import java.util.Collection;

public interface ChangesOnServerTracker extends VcsListener {
  // todo add vcs parameter???
  void invalidate(final Collection<String> paths);

  boolean isUpToDate(@Nonnull Change change, @Nonnull AbstractVcs vcs);

  boolean updateStep();

  void changeUpdated(@Nonnull String path, @Nonnull AbstractVcs vcs);

  void changeRemoved(@Nonnull String path, @Nonnull AbstractVcs vcs);
}
