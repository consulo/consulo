// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.virtualFileSystem.event;

import consulo.util.io.FileAttributes;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.VirtualFileSystem;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author max
 */
public class VFileCreateEvent extends VFileEvent {
  private final
  @Nonnull
  VirtualFile myParent;
  private final boolean myDirectory;
  private final FileAttributes myAttributes;
  private final String mySymlinkTarget;
  private final ChildInfo[] myChildren;
  private final int myChildNameId;
  private VirtualFile myCreatedFile;

  /**
   *
   * @param requestor
   * @param parent
   * @param childName
   * @param isDirectory
   * @param attributes null means should read from the created file
   * @param symlinkTarget
   * @param isFromRefresh
   * @param children null means children not available (e.g. the created file is not a directory) or unknown
   */
  public VFileCreateEvent(Object requestor,
                          @Nonnull VirtualFile parent,
                          @Nonnull String childName,
                          boolean isDirectory,
                          @Nullable FileAttributes attributes,
                          @Nullable String symlinkTarget,
                          boolean isFromRefresh,
                          @Nullable ChildInfo[] children) {
    super(requestor, isFromRefresh);
    myParent = parent;
    myDirectory = isDirectory;
    myAttributes = attributes;
    mySymlinkTarget = symlinkTarget;
    myChildren = children;
    myChildNameId = VirtualFileManager.getInstance().storeName(childName);
  }

  @Nonnull
  public String getChildName() {
    return VirtualFileManager.getInstance().getVFileName(myChildNameId).toString();
  }

  public boolean isDirectory() {
    return myDirectory;
  }

  @Nonnull
  public VirtualFile getParent() {
    return myParent;
  }

  @Nullable
  public FileAttributes getAttributes() {
    return myAttributes;
  }

  @Nullable
  public String getSymlinkTarget() {
    return mySymlinkTarget;
  }

  /**
   * @return true if the newly created file is a directory which has no children.
   */
  public boolean isEmptyDirectory() {
    return isDirectory() && myChildren != null && myChildren.length == 0;
  }

  @Nonnull
  @Override
  protected String computePath() {
    String parentPath = myParent.getPath();
    // jar file returns "x.jar!/"
    return StringUtil.endsWithChar(parentPath, '/') ? parentPath + getChildName() : parentPath + "/" + getChildName();
  }

  @Override
  public VirtualFile getFile() {
    VirtualFile createdFile = myCreatedFile;
    if (createdFile == null && myParent.isValid()) {
      myCreatedFile = createdFile = myParent.findChild(getChildName());
    }
    return createdFile;
  }

  /**
   *
   * @return null means children not available (e.g. the created file is not a directory) or unknown
   */
  @Nullable
  public ChildInfo[] getChildren() {
    return myChildren;
  }

  public void resetCache() {
    myCreatedFile = null;
  }

  @Nonnull
  @Override
  public VirtualFileSystem getFileSystem() {
    return myParent.getFileSystem();
  }

  @Override
  public boolean isValid() {
    return myParent.isValid() && myParent.findChild(getChildName()) == null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    VFileCreateEvent event = (VFileCreateEvent)o;

    return myDirectory == event.myDirectory && getChildName().equals(event.getChildName()) && myParent.equals(event.myParent);
  }

  @Override
  public int hashCode() {
    int result = myParent.hashCode();
    result = 31 * result + (myDirectory ? 1 : 0);
    result = 31 * result + getChildName().hashCode();
    return result;
  }

  @Override
  public String toString() {
    String kind = myDirectory ? (isEmptyDirectory() ? "(empty) " : "") + "dir " : "file ";
    return "VfsEvent[create " + kind + myParent.getUrl() + "/" + getChildName() + "]" + (myChildren == null ? "" : " with " + myChildren.length + " children");
  }

  /**
   * @return the nameId (obtained via FileNameCache.storeName()) of the myChildName or -1 if the nameId wasn't computed.
   */
  public int getChildNameId() {
    return myChildNameId;
  }
}