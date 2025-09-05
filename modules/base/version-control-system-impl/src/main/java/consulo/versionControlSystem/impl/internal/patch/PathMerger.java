/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.patch;

import consulo.platform.Platform;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.base.FilePathImpl;
import consulo.versionControlSystem.impl.internal.util.RelativePathCalculator;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class PathMerger {
  private PathMerger() {
  }

  @Nullable
  public static VirtualFile getFile(VirtualFile base, String path) {
    return getFile(new VirtualFilePathMerger(base), path);
  }

  @Nullable
  public static VirtualFile getFile(VirtualFile base, String path, List<String> tail) {
    return getFile(new VirtualFilePathMerger(base), path, tail);
  }

  @Nullable
  public static File getFile(File base, String path) {
    return getFile(new IoFilePathMerger(base), path);
  }

  @Nullable
  public static File getFile(File base, String path, List<String> tail) {
    return getFile(new IoFilePathMerger(base), path, tail);
  }
  
  @Nullable
  public static FilePath getFile(FilePath base, String path) {
    return getFile(new FilePathPathMerger(base), path);
  }

  @Nullable
  public static FilePath getFile(FilePath base, String path, List<String> tail) {
    return getFile(new FilePathPathMerger(base), path, tail);
  }

  @Nullable
  public static <T> T getFile(FilePathMerger<T> merger, String path) {
    if (path == null) {
      return null;
    }
    List<String> tail = new ArrayList<String>();
    T file = getFile(merger, path, tail);
    if (tail.isEmpty()) {
      return file;
    }
    return null;
  }

  @Nullable
  public static <T> T getFile(FilePathMerger<T> merger, String path, List<String> tail) {
    String[] pieces = RelativePathCalculator.split(path);

    for (int i = 0; i < pieces.length; i++) {
      String piece = pieces[i];
      if ("".equals(piece) || ".".equals(piece)) {
        continue;
      }
      if ("..".equals(piece)) {
        boolean upResult = merger.up();
        if (! upResult) return null;
        continue;
      }

      boolean downResult = merger.down(piece);
      if (! downResult) {
        if (tail != null) {
          for (int j = i; j < pieces.length; j++) {
            String pieceInner = pieces[j];
            tail.add(pieceInner);
          }
        }
        return merger.getResult();
      }
    }

    return merger.getResult();
  }

  @Nullable
  public static VirtualFile getBase(VirtualFile base, String path) {
    return getBase(new VirtualFilePathMerger(base), path);
  }

  @Nullable
  public static <T> T getBase(FilePathMerger<T> merger, String path) {
    boolean caseSensitive = Platform.current().fs().isCaseSensitive();
    String[] parts = path.replace("\\", "/").split("/");
    for (int i = parts.length - 1; i >=0; --i) {
      String part = parts[i];
      if ("".equals(part) || ".".equals(part)) {
        continue;
      } else if ("..".equals(part)) {
        if (! merger.up()) return null;
        continue;
      }
      String vfName = merger.getCurrentName();
      if (vfName == null) return null;
      if ((caseSensitive && vfName.equals(part)) || ((! caseSensitive) && vfName.equalsIgnoreCase(part))) {
        if (! merger.up()) return null;
      } else {
        return null;
      }
    }
    return merger.getResult();
  }

  public static class VirtualFilePathMerger implements FilePathMerger<VirtualFile> {
    private VirtualFile myCurrent;

    public VirtualFilePathMerger(VirtualFile current) {
      myCurrent = current;
    }

    @Override
    public boolean up() {
      myCurrent = myCurrent.getParent();
      return myCurrent != null;
    }

    @Override
    public boolean down(String name) {
      VirtualFile nextChild = myCurrent.findChild(name);
      if (nextChild == null) {
        nextChild = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(myCurrent.getPath(), name));
      }
      if (nextChild != null) {
        myCurrent = nextChild;
        return true;
      }
      return false;
    }

    public VirtualFile getResult() {
      return myCurrent;
    }

    public String getCurrentName() {
      return myCurrent == null ? null : myCurrent.getName();
    }
  }

  // ! does not check result for existence!
  public static class IoFilePathMerger implements FilePathMerger<File> {
    private File myBase;
    private final List<String> myChildPathElements;

    public IoFilePathMerger(File base) {
      myBase = base;
      myChildPathElements = new LinkedList<String>();
    }

    public boolean up() {
      if (! myChildPathElements.isEmpty()) {
        myChildPathElements.remove(myChildPathElements.size() - 1);
        return true;
      }
      myBase = myBase.getParentFile();
      return myBase != null;
    }

    public boolean down(String name) {
      myChildPathElements.add(name);
      return true;
    }

    public File getResult() {
      StringBuilder sb = new StringBuilder();
      for (String element : myChildPathElements) {
        if (sb.length() > 0) {
          sb.append(File.separatorChar);
        }
        sb.append(element);
      }
      return new File(myBase, sb.toString());
    }

    @Nullable
    public String getCurrentName() {
      if (! myChildPathElements.isEmpty()) {
        return myChildPathElements.get(myChildPathElements.size() - 1);
      }
      return myBase == null ? null : myBase.getName();
    }
  }

  public static class FilePathPathMerger implements FilePathMerger<FilePath> {
    private final IoFilePathMerger myIoDelegate;
    private boolean myIsDirectory;

    public FilePathPathMerger(FilePath base) {
      myIoDelegate = new IoFilePathMerger(base.getIOFile());
    }

    @Override
    public boolean down(String name) {
      return myIoDelegate.down(name);
    }

    @Override
    public boolean up() {
      return myIoDelegate.up();
    }

    @Override
    public FilePath getResult() {
      return new FilePathImpl(myIoDelegate.getResult(), myIsDirectory);
    }

    @Override
    public String getCurrentName() {
      return myIoDelegate.getCurrentName();
    }

    public void setIsDirectory(boolean isDirectory) {
      myIsDirectory = isDirectory;
    }
  }

  public interface FilePathMerger<T> {
    boolean up();

    /**
     * !!! should not go down (to null state), if can't find corresponding child
     * @param name
     * @return
     */
    boolean down(String name);
    T getResult();
    @Nullable
    String getCurrentName();
  }
}
