/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package consulo.versionControlSystem.change;

import consulo.platform.Platform;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.lang.Comparing;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsPathPresenter;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatus;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author max
 */
public class Change {
  private int myHash;

  public enum Type {
    MODIFICATION,
    NEW,
    DELETED,
    MOVED
  }

  private final ContentRevision myBeforeRevision;
  private final ContentRevision myAfterRevision;
  private final FileStatus myFileStatus;
  protected String myMoveRelativePath;
  protected boolean myRenamed;
  protected boolean myMoved;
  protected boolean myRenameOrMoveCached = false;
  private boolean myIsReplaced;
  private Type myType;
  private final Map<String, Change> myOtherLayers;
  // if null, vcs's is used. intended: for property conflict case
  private Supplier<MergeTexts> myMergeProvider;

  public Change(@Nullable final ContentRevision beforeRevision, @Nullable final ContentRevision afterRevision) {
    this(beforeRevision, afterRevision, convertStatus(beforeRevision, afterRevision));
  }

  public Change(@Nullable final ContentRevision beforeRevision, @Nullable final ContentRevision afterRevision, @Nullable FileStatus fileStatus) {
    assert beforeRevision != null || afterRevision != null;
    myBeforeRevision = beforeRevision;
    myAfterRevision = afterRevision;
    myFileStatus = fileStatus == null ? convertStatus(beforeRevision, afterRevision) : fileStatus;
    myHash = -1;
    myOtherLayers = new HashMap<String, Change>(0);
  }

  private static FileStatus convertStatus(@Nullable ContentRevision beforeRevision, @Nullable ContentRevision afterRevision) {
    if (beforeRevision == null) return FileStatus.ADDED;
    if (afterRevision == null) return FileStatus.DELETED;
    return FileStatus.MODIFIED;
  }

  public Supplier<MergeTexts> getMergeProvider() {
    return myMergeProvider;
  }

  public void setMergeProvider(Supplier<MergeTexts> mergeProvider) {
    myMergeProvider = mergeProvider;
  }

  public void addAdditionalLayerElement(final String name, final Change change) {
    myOtherLayers.put(name, change);
  }

  public Map<String, Change> getOtherLayers() {
    return myOtherLayers;
  }

  public boolean isTreeConflict() {
    return false;
  }

  public boolean isPhantom() {
    return false;
  }

  public boolean hasOtherLayers() {
    return ! myOtherLayers.isEmpty();
  }

  public Type getType() {
    if (myType == null) {
      if (myBeforeRevision == null) return Type.NEW;
      if (myAfterRevision == null) return Type.DELETED;

      FilePath bFile = myBeforeRevision.getFile();
      FilePath aFile = myAfterRevision.getFile();
      if (!Comparing.equal(bFile, aFile)) return Type.MOVED;

      // enforce case-sensitive check
      if (!Platform.current().fs().isCaseSensitive()) {
        String bPath = bFile.getPath();
        String aPath = aFile.getPath();
        if (!bPath.equals(aPath) && bPath.equalsIgnoreCase(aPath)) return Type.MOVED;
      }

      return Type.MODIFICATION;
    }
    return myType;
  }

  @Nullable
  public ContentRevision getBeforeRevision() {
    return myBeforeRevision;
  }

  @Nullable
  public ContentRevision getAfterRevision() {
    return myAfterRevision;
  }

  public FileStatus getFileStatus() {
    return myFileStatus;
  }

  @Nullable
  public VirtualFile getVirtualFile() {
    return myAfterRevision == null ? null : myAfterRevision.getFile().getVirtualFile();
  }

  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || (! (o instanceof Change))) return false;
    final Change otherChange = ((Change)o);

    final ContentRevision br1 = getBeforeRevision();
    final ContentRevision br2 = otherChange.getBeforeRevision();
    final ContentRevision ar1 = getAfterRevision();
    final ContentRevision ar2 = otherChange.getAfterRevision();

    FilePath fbr1 = br1 != null ? br1.getFile() : null;
    FilePath fbr2 = br2 != null ? br2.getFile() : null;

    FilePath far1 = ar1 != null ? ar1.getFile() : null;
    FilePath far2 = ar2 != null ? ar2.getFile() : null;

    return Comparing.equal(fbr1, fbr2) && Comparing.equal(far1, far2);
  }

  public int hashCode() {
    if (myHash == -1) {
      myHash = calculateHash();
    }
    return myHash;
  }

  private int calculateHash() {
    return revisionHashCode(getBeforeRevision()) * 27 + revisionHashCode(getAfterRevision());
  }

  private static int revisionHashCode(ContentRevision rev) {
    if (rev == null) return 0;
    return rev.getFile().getIOFile().getPath().hashCode();
  }

  public boolean affectsFile(File ioFile) {
    if (myBeforeRevision != null && myBeforeRevision.getFile().getIOFile().equals(ioFile)) return true;
    if (myAfterRevision != null && myAfterRevision.getFile().getIOFile().equals(ioFile)) return true;
    return false;
  }

  public boolean isRenamed() {
    cacheRenameOrMove(null);
    return myRenamed;
  }

  public boolean isMoved() {
    cacheRenameOrMove(null);
    return myMoved;
  }

  public String getMoveRelativePath(Project project) {
    cacheRenameOrMove(project);
    return myMoveRelativePath;
  }

  private void cacheRenameOrMove(final Project project) {
    if (myBeforeRevision != null && myAfterRevision != null && (! revisionPathsSame())) {
      if (!myRenameOrMoveCached) {
        myRenameOrMoveCached = true;
        if (Comparing.equal(myBeforeRevision.getFile().getParentPath(), myAfterRevision.getFile().getParentPath())) {
          myRenamed = true;
        }
        else {
          myMoved = true;
        }
      }
      if (myMoved && myMoveRelativePath == null && project != null) {
        myMoveRelativePath = VcsPathPresenter.getInstance(project).getPresentableRelativePath(myBeforeRevision, myAfterRevision);
      }
    }
  }

  private boolean revisionPathsSame() {
    final String path1 = myBeforeRevision.getFile().getIOFile().getAbsolutePath();
    final String path2 = myAfterRevision.getFile().getIOFile().getAbsolutePath();
    return path1.equals(path2);
  }

  @NonNls
  public String toString() {
    final Type type = getType();
    //noinspection EnumSwitchStatementWhichMissesCases
    switch (type) {
      case NEW: return "A: " + myAfterRevision;
      case DELETED: return "D: " + myBeforeRevision;
      case MOVED: return "M: " + myBeforeRevision + " -> " + myAfterRevision;
      default: return "M: " + myAfterRevision;
    }
  }

  @Nullable
  public String getOriginText(final Project project) {
    cacheRenameOrMove(project);
    if (isMoved()) {
      return getMovedText(project);
    } else if (isRenamed()) {
      return getRenamedText();
    }
    return myIsReplaced ? VcsLocalize.changeFileReplacedText().get() : null;
  }

  @Nullable
  protected String getRenamedText() {
    return VcsLocalize.changeFileRenamedFromText(myBeforeRevision.getFile().getName()).get();
  }

  @Nullable
  protected String getMovedText(final Project project) {
    return VcsLocalize.changeFileMovedFromText(getMoveRelativePath(project)).get();
  }

  public boolean isIsReplaced() {
    return myIsReplaced;
  }

  public void setIsReplaced(final boolean isReplaced) {
    myIsReplaced = isReplaced;
  }

  @Nullable
  public Image getAdditionalIcon() {
    return null;
  }

  @Nullable
  public String getDescription() {
    return null;
  }
}
