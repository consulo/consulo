/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.versionControlSystem.impl.internal.history;

import consulo.versionControlSystem.RepositoryLocation;
import consulo.versionControlSystem.VcsException;
import consulo.ui.ex.awt.dualView.DualTreeElement;
import consulo.util.lang.TreeItem;
import consulo.versionControlSystem.history.VcsFileRevision;
import consulo.versionControlSystem.history.VcsRevisionNumber;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.IOException;
import java.util.Date;
import java.util.List;

// TODO this class should not implements VcsFileRevision (this is too confusing)
public class TreeNodeOnVcsRevision extends DefaultMutableTreeNode implements VcsFileRevision, DualTreeElement {
  @Nonnull
  private final VcsFileRevision myRevision;

  public TreeNodeOnVcsRevision(@Nullable VcsFileRevision revision, @Nonnull List<TreeItem<VcsFileRevision>> roots) {
    myRevision = revision == null ? VcsFileRevision.NULL : revision;
    for (TreeItem<VcsFileRevision> root : roots) {
      add(new TreeNodeOnVcsRevision(root.getData(), root.getChildren()));
    }
  }

  @Nullable
  @Override
  public RepositoryLocation getChangedRepositoryPath() {
    return myRevision.getChangedRepositoryPath();
  }

  @Nonnull
  public VcsFileRevision getRevision() {
    return myRevision;
  }

  @Override
  public String getAuthor() {
    return myRevision.getAuthor();
  }

  @Override
  public String getCommitMessage() {
    return myRevision.getCommitMessage();
  }

  @Override
  public byte[] loadContent() throws IOException, VcsException {
    return myRevision.loadContent();
  }

  @Override
  public VcsRevisionNumber getRevisionNumber() {
    return myRevision.getRevisionNumber();
  }

  @Override
  public Date getRevisionDate() {
    return myRevision.getRevisionDate();
  }

  @Override
  public String getBranchName() {
    return myRevision.getBranchName();
  }

  @Override
  public byte[] getContent() throws IOException, VcsException {
    return myRevision.getContent();
  }

  @Override
  public String toString() {
    return getRevisionNumber().asString();
  }

  @Override
  public boolean shouldBeInTheFlatView() {
    return myRevision != VcsFileRevision.NULL;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TreeNodeOnVcsRevision that = (TreeNodeOnVcsRevision)o;

    if (myRevision != null ? !myRevision.getRevisionNumber().equals(that.myRevision.getRevisionNumber()) : that.myRevision != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return myRevision != null ? myRevision.getRevisionNumber().hashCode() : 0;
  }
}
