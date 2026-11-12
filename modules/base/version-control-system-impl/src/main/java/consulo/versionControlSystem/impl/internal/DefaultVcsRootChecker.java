// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.impl.internal;

import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.VcsDescriptor;
import consulo.versionControlSystem.VcsKey;
import consulo.versionControlSystem.VcsRootChecker;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

final class DefaultVcsRootChecker extends VcsRootChecker {
  private final AbstractVcs myVcs;
  private final @Nullable VcsDescriptor myVcsDescriptor;

  DefaultVcsRootChecker(AbstractVcs vcs, @Nullable VcsDescriptor vcsDescriptor) {
    myVcs = vcs;
    myVcsDescriptor = vcsDescriptor;
  }

  @Override
  public VcsKey getSupportedVcs() {
    return myVcs.getKeyInstanceMethod();
  }

  @Override
  public boolean isRoot(VirtualFile file) {
    if (myVcsDescriptor == null) return false;
    return myVcsDescriptor.probablyUnderVcs(file);
  }

  @Override
  public boolean validateRoot(VirtualFile file) {
    return true;
  }

  @Override
  public boolean isVcsDir(@Nullable String dirName) {
    if (myVcsDescriptor == null || dirName == null) return false;
    return myVcsDescriptor.matchesVcsDirPattern(dirName);
  }

  @Override
  public boolean areChildrenValidMappings() {
    if (myVcsDescriptor == null) return false;
    return myVcsDescriptor.areChildrenValidMappings();
  }
}
