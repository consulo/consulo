/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package consulo.virtualFileSystem.impl.internal.temp;

import consulo.annotation.component.ExtensionImpl;
import consulo.util.collection.ArrayUtil;
import consulo.util.io.BufferExposingByteArrayInputStream;
import consulo.util.io.FileAttributes;
import consulo.util.lang.LocalTimeCounter;
import consulo.virtualFileSystem.*;
import consulo.virtualFileSystem.impl.internal.FSRecords;
import consulo.virtualFileSystem.internal.FakeVirtualFile;
import consulo.virtualFileSystem.internal.VfsImplUtil;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author max
 */
@ExtensionImpl
public class TempFileSystemImpl extends TempFileSystem implements RefreshableFileSystem, VirtualFilePointerCapableFileSystem{
  private final FSItem myRoot = new FSDir(null, "/");

  @Nonnull
  @Override
  public String extractRootPath(@Nonnull String path) {
    //return path.startsWith("/") ? "/" : "";
    return "/";
  }

  @Override
  public int getRank() {
    return 1;
  }

  @Nullable
  private FSItem convert(VirtualFile file) {
    VirtualFile parentFile = file.getParent();
    if (parentFile == null) return myRoot;
    FSItem parentItem = convert(parentFile);
    if (parentItem == null || !parentItem.isDirectory()) {
      return null;
    }

    return parentItem.findChild(file.getName());
  }

  @Override
  public VirtualFile copyFile(Object requestor, @Nonnull VirtualFile file, @Nonnull VirtualFile newParent, @Nonnull String copyName)
      throws IOException {
    return VirtualFileUtil.copyFile(requestor, file, newParent, copyName);
  }

  @Override
  @Nonnull
  public VirtualFile createChildDirectory(Object requestor, @Nonnull VirtualFile parent, @Nonnull String dir) throws IOException {
    FSItem fsItem = convert(parent);
    assert fsItem != null && fsItem.isDirectory();

    FSDir fsDir = (FSDir)fsItem;
    FSItem existingDir = fsDir.findChild(dir);
    if (existingDir == null) {
      fsDir.addChild(new FSDir(fsDir, dir));
    }
    else if (!existingDir.isDirectory()) {
      throw new IOException("Directory already contains a file named " + dir);
    }


    return new FakeVirtualFile(parent, dir);
  }

  @Override
  public VirtualFile createChildFile(Object requestor, @Nonnull VirtualFile parent, @Nonnull String file) throws IOException {
    FSItem fsItem = convert(parent);
    if (fsItem == null) {
      FSRecords.invalidateCaches();
      throw new IllegalStateException("cannot find parent directory: " + parent.getPath());
    }
    assert fsItem.isDirectory(): "parent is not a directory: " + parent.getPath();

    FSDir fsDir = (FSDir)fsItem;

    assert fsDir.findChild(file) == null: "File " + file + " already exists in " + parent.getPath();
    fsDir.addChild(new FSFile(fsDir, file));

    return new FakeVirtualFile(parent, file);
  }

  @Override
  public void deleteFile(Object requestor, @Nonnull VirtualFile file) throws IOException {
    FSItem fsItem = convert(file);
    if (fsItem == null) {
      FSRecords.invalidateCaches();
      throw new IllegalStateException("failed to delete file " + file.getPath());
    }
    fsItem.getParent().removeChild(fsItem);
  }

  @Override
  public void moveFile(Object requestor, @Nonnull VirtualFile file, @Nonnull VirtualFile newParent) throws IOException {
    FSItem fsItem = convert(file);
    assert fsItem != null: "failed to move file " + file.getPath();
    FSItem newParentItem = convert(newParent);
    assert newParentItem != null && newParentItem.isDirectory(): "failed to find move target " + file.getPath();
    FSDir newDir = (FSDir) newParentItem;
    if (newDir.findChild(file.getName()) != null) {
      throw new IOException("Directory already contains a file named " + file.getName());
    }

    fsItem.getParent().removeChild(fsItem);
    ((FSDir) newParentItem).addChild(fsItem);
  }

  @Override
  public void renameFile(Object requestor, @Nonnull VirtualFile file, @Nonnull String newName) throws IOException {
    FSItem fsItem = convert(file);
    assert fsItem != null;

    fsItem.setName(newName);
  }

  @Override
  @Nonnull
  public String getProtocol() {
    return PROTOCOL;
  }

  @Override
  public boolean exists(@Nonnull VirtualFile fileOrDirectory) {
    return convert(fileOrDirectory) != null;
  }

  @Override
  @Nonnull
  public String[] list(@Nonnull VirtualFile file) {
    FSItem fsItem = convert(file);
    assert fsItem != null;

    return fsItem.list();
  }

  @Override
  public boolean isDirectory(@Nonnull VirtualFile file) {
    return convert(file) instanceof FSDir;
  }

  @Override
  public long getTimeStamp(@Nonnull VirtualFile file) {
    FSItem fsItem = convert(file);
    assert fsItem != null: "cannot find item for path " + file.getPath();

    return fsItem.myTimestamp;
  }

  @Override
  public void setTimeStamp(@Nonnull VirtualFile file, long timeStamp) throws IOException {
    FSItem fsItem = convert(file);
    assert fsItem != null;

    fsItem.myTimestamp = timeStamp > 0 ? timeStamp : LocalTimeCounter.currentTime();
  }

  @Override
  public boolean isWritable(@Nonnull VirtualFile file) {
    FSItem fsItem = convert(file);
    assert fsItem != null;

    return fsItem.myWritable;
  }

  @Override
  public void setWritable(@Nonnull VirtualFile file, boolean writableFlag) throws IOException {
    FSItem fsItem = convert(file);
    assert fsItem != null;

    fsItem.myWritable = writableFlag;
  }

  @Override
  @Nonnull
  public byte[] contentsToByteArray(@Nonnull VirtualFile file) throws IOException {
    FSItem fsItem = convert(file);
    if (fsItem == null) throw new FileNotFoundException("Cannot find temp for " + file.getPath());
    
    assert fsItem instanceof FSFile;

    return ((FSFile)fsItem).myContent;
  }

  @Override
  @Nonnull
  public InputStream getInputStream(@Nonnull VirtualFile file) throws IOException {
    return new BufferExposingByteArrayInputStream(contentsToByteArray(file));
  }

  @Override
  @Nonnull
  public OutputStream getOutputStream(@Nonnull final VirtualFile file, Object requestor, final long modStamp, long timeStamp)
      throws IOException {
    return new ByteArrayOutputStream() {
      @Override
      public void close() throws IOException {
        super.close();
        FSItem fsItem = convert(file);
        assert fsItem instanceof FSFile;

        ((FSFile)fsItem).myContent = toByteArray();
        setTimeStamp(file, modStamp);
      }
    };
  }

  @Override
  public void refresh(boolean asynchronous) {
    RefreshQueue.getInstance().refresh(asynchronous, true, null, ManagingFS.getInstance().getRoots(this));
  }

  @Override
  public VirtualFile findFileByPath(@Nonnull @NonNls String path) {
    return VfsImplUtil.findFileByPath(this, path);
  }

  @Override
  public VirtualFile findFileByPathIfCached(@Nonnull @NonNls String path) {
    return VfsImplUtil.findFileByPathIfCached(this, path);
  }

  @Override
  public VirtualFile refreshAndFindFileByPath(@Nonnull String path) {
    return VfsImplUtil.refreshAndFindFileByPath(this, path);
  }

  @Override
  public long getLength(@Nonnull VirtualFile file) {
    try {
      return contentsToByteArray(file).length;
    }
    catch (IOException e) {
      return 0;
    }
  }

  private abstract static class FSItem {
    private final FSDir myParent;
    private String myName;
    private long myTimestamp;
    private boolean myWritable;

    protected FSItem(FSDir parent, String name) {
      myParent = parent;
      myName = name;
      myTimestamp = LocalTimeCounter.currentTime();
      myWritable = true;
    }

    public abstract boolean isDirectory();

    @Nullable
    public FSItem findChild(String name) {
      return null;
    }

    public void setName(String name) {
      myName = name;
    }

    public FSDir getParent() {
      return myParent;
    }

    public String[] list() {
      return ArrayUtil.EMPTY_STRING_ARRAY;
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() + ": " + myName;
    }
  }

  private static class FSDir extends FSItem {
    private final List<FSItem> myChildren = new ArrayList<FSItem>();

    public FSDir(FSDir parent, String name) {
      super(parent, name);
    }

    @Override
    @Nullable
    public FSItem findChild(String name) {
      for (FSItem child : myChildren) {
        if (name.equals(child.myName)) {
          return child;
        }
      }

      return null;
    }

    @Override
    public boolean isDirectory() {
      return true;
    }

    public void addChild(FSItem item) {
      myChildren.add(item);
    }

    public void removeChild(FSItem fsItem) {
      if (fsItem.myName.equals("src") && getParent() == null) {
        throw new RuntimeException("removing src directory");
      }
      myChildren.remove(fsItem);
    }

    @Override
    public String[] list() {
      String[] names = ArrayUtil.newStringArray(myChildren.size());
      for (int i = 0; i < names.length; i++) {
        names[i] = myChildren.get(i).myName;
      }
      return names;
    }
  }

  private static class FSFile extends FSItem {
    public FSFile(FSDir parent, String name) {
      super(parent, name);
    }

    private byte[] myContent = new byte[0];

    @Override
    public boolean isDirectory() {
      return false;
    }
  }

  @Override
  public FileAttributes getAttributes(@Nonnull VirtualFile file) {
    FSItem item = convert(file);
    if (item == null) return null;
    long length = item instanceof FSFile ? ((FSFile)item).myContent.length : 0;
    return new FileAttributes(item.isDirectory(), false, false, false, length, item.myTimestamp, item.myWritable);
  }
}
