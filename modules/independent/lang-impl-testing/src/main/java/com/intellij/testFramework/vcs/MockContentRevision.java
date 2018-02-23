
package com.intellij.testFramework.vcs;

import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;

import javax.annotation.Nonnull;

/**
 * @author yole
 */
public class MockContentRevision implements ContentRevision {
  private final FilePath myPath;
  private final VcsRevisionNumber myRevisionNumber;

  public MockContentRevision(final FilePath path, final VcsRevisionNumber revisionNumber) {
    myPath = path;
    myRevisionNumber = revisionNumber;
  }

  @Override
  @javax.annotation.Nullable
  public String getContent() throws VcsException {
    return null;
  }

  @Override
  @Nonnull
  public FilePath getFile() {
    return myPath;
  }

  @Override
  @Nonnull
  public VcsRevisionNumber getRevisionNumber() {
    return myRevisionNumber;
  }

  @Override
  public String toString() {
    return myPath.getName() + ":" + myRevisionNumber;
  }
}
