// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.impl.internal.change;

import consulo.logging.Logger;
import consulo.project.Project;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.change.ContentRevision;
import consulo.versionControlSystem.change.CurrentContentRevision;
import consulo.versionControlSystem.diff.DiffProvider;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Change content stored in {@link ChangeListManagerSerialization} to keep change-to-changelist mappings between IDE restarts.
 * These are going to be replaced by real content revision after the next CLM refresh.
 */
public class FakeRevision implements ContentRevision {
  private static final Logger LOG = Logger.getInstance(FakeRevision.class);

  private final Project myProject;
  private final FilePath myFile;
  private final boolean myCurrentRevision;

  /**
   * @deprecated Consider this class platform-only, use own ContentRevision implementation when needed.
   */
  @Deprecated
  public FakeRevision(@Nonnull Project project, @Nonnull FilePath file) {
    this(project, file, false);
  }

  public FakeRevision(@Nonnull Project project, @Nonnull FilePath file, boolean isCurrentRevision) {
    myProject = project;
    myFile = file;
    myCurrentRevision = isCurrentRevision;
  }

  @Override
  @Nullable
  public String getContent() throws VcsException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("FakeRevision queried for " + myFile.getPath() + (myCurrentRevision ? " (current)" : ""), new Throwable());
    }

    if (myCurrentRevision) {
      return new CurrentContentRevision(myFile).getContent();
    }

    VirtualFile virtualFile = myFile.getVirtualFile();
    if (virtualFile == null) return null;

    AbstractVcs vcs = ProjectLevelVcsManager.getInstance(myProject).getVcsFor(virtualFile);
    DiffProvider diffProvider = vcs != null ? vcs.getDiffProvider() : null;
    if (diffProvider == null) return null;

    ContentRevision delegateContent = diffProvider.createCurrentFileContent(virtualFile);
    if (delegateContent == null) return null;

    return delegateContent.getContent();
  }

  @Override
  @Nonnull
  public FilePath getFile() {
    return myFile;
  }

  @Override
  @Nonnull
  public VcsRevisionNumber getRevisionNumber() {
    return VcsRevisionNumber.NULL;
  }

  @Override
  public String toString() {
    return myFile.getPath();
  }
}
