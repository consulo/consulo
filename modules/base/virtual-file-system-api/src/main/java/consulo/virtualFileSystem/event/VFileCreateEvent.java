// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.virtualFileSystem.event;

import consulo.util.io.FileAttributes;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.VirtualFileSystem;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * @author max
 */
public final class VFileCreateEvent extends VFileEvent {
  private final VirtualFile myParent;
  private final boolean myDirectory;
  private final @Nullable FileAttributes myAttributes;
  private final @Nullable String mySymlinkTarget;
  private final ChildInfo @Nullable [] myChildren;
  private final boolean myAllChildren;
  private final int myChildNameId;
  private @Nullable VirtualFile myCreatedFile = null;

  /**
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
                          VirtualFile parent,
                          String childName,
                          boolean isDirectory,
                          @Nullable FileAttributes attributes,
                          @Nullable String symlinkTarget,
                          boolean isFromRefresh,
                          ChildInfo @Nullable [] children) {
    this(requestor, parent, childName, isDirectory, attributes, symlinkTarget, isFromRefresh, children, children != null);
  }

  /**
   * @param children    null means children not available (e.g. the created file is not a directory) or unknown
   * @param allChildren whether {@code children} contains all children of the created directory
   */
  public VFileCreateEvent(Object requestor,
                          VirtualFile parent,
                          String childName,
                          boolean isDirectory,
                          @Nullable FileAttributes attributes,
                          @Nullable String symlinkTarget,
                          boolean isFromRefresh,
                          ChildInfo @Nullable [] children,
                          boolean allChildren) {
    super(requestor, isFromRefresh);
    myParent = parent;
    myDirectory = isDirectory;
    myAttributes = attributes;
    mySymlinkTarget = symlinkTarget;
    myChildren = children;
    myAllChildren = children != null && allChildren;
    myChildNameId = VirtualFileManager.getInstance().storeName(childName);
  }

  public String getChildName() {
    return VirtualFileManager.getInstance().getVFileName(myChildNameId).toString();
  }

  public boolean isDirectory() {
    return myDirectory;
  }

  public VirtualFile getParent() {
    return myParent;
  }

  public @Nullable FileAttributes getAttributes() {
    return myAttributes;
  }

  public @Nullable String getSymlinkTarget() {
    return mySymlinkTarget;
  }

  /** @return {@code true} if the newly created file is a directory that has no children. */
  public boolean isEmptyDirectory() {
    return isDirectory() && myAllChildren && myChildren != null && myChildren.length == 0;
  }

  @Override
  protected String computePath() {
    String parentPath = myParent.getPath();
    // jar file returns "x.jar!/"
    return StringUtil.endsWithChar(parentPath, '/') ? parentPath + getChildName() : parentPath + "/" + getChildName();
  }

  @Override
  public @Nullable VirtualFile getFile() {
    VirtualFile createdFile = myCreatedFile;
    if (createdFile == null && myParent.isValid()) {
      myCreatedFile = createdFile = myParent.findChild(getChildName());
    }
    return createdFile;
  }

  /**
   * Children of the created file if it's a directory.
   * <br/>
   * <code>null</code> is returned if the file is not a directory or the children are not known.
   * If {@link #isAllChildren()} returns {@code false}, the returned array contains only some children.
   *
   * @return children of the created file if it's a directory
   */
  public ChildInfo @Nullable [] getChildren() {
    return myChildren;
  }

  public boolean isAllChildren() {
    return myAllChildren;
  }

  public void resetCache() {
    myCreatedFile = null;
  }

  @Override
  public VirtualFileSystem getFileSystem() {
    return myParent.getFileSystem();
  }

  @Override
  public boolean isValid() {
    return myParent.isValid() && myParent.findChild(getChildName()) == null;
  }

  @Override
  public boolean equals(@Nullable Object o) {
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