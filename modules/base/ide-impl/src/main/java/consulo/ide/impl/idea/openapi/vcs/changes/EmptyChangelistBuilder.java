// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.openapi.vcs.changes;

import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsKey;
import consulo.versionControlSystem.change.*;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.function.Supplier;

public class EmptyChangelistBuilder implements ChangelistBuilder {
  @Override
  public void processChange(@NotNull Change change, VcsKey vcsKey) {
  }

  @Override
  public void processChangeInList(@NotNull Change change, @Nullable ChangeList changeList, VcsKey vcsKey) {
  }

  @Override
  public void processChangeInList(@NotNull Change change, String changeListName, VcsKey vcsKey) {
  }

  @Override
  public void removeRegisteredChangeFor(FilePath path) {
  }

  @Override
  public void processUnversionedFile(FilePath file) {
  }

  @Override
  public void processLocallyDeletedFile(final FilePath file) {
  }

  @Override
  public void processLocallyDeletedFile(LocallyDeletedChange locallyDeletedChange) {
  }

  @Override
  public void processModifiedWithoutCheckout(final VirtualFile file) {
  }

  @Override
  public void processIgnoredFile(FilePath file) {
  }

  @Override
  public void processLockedFolder(final VirtualFile file) {
  }

  @Override
  public void processLogicallyLockedFolder(VirtualFile file, LogicalLock logicalLock) {
  }

  @Override
  public void processSwitchedFile(final VirtualFile file, final String branch, final boolean recursive) {
  }

  @Override
  public void processRootSwitch(VirtualFile file, String branch) {
  }

  @Override
  public boolean reportChangesOutsideProject() {
    return false;
  }

  @Override
  public void reportAdditionalInfo(final String text) {
  }

  @Override
  public void reportAdditionalInfo(Supplier<JComponent> infoComponent) {
  }

  public void reportWarningMessage(final String message) {
  }
}
