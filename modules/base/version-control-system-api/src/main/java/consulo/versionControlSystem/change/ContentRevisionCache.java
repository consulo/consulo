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

import consulo.application.ApplicationManager;
import consulo.application.util.function.Throwable2Computable;
import consulo.project.Project;
import consulo.util.collection.SLRUMap;
import consulo.util.io.CharsetToolkit;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SoftReference;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.VcsKey;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.encoding.EncodingRegistry;
import org.jetbrains.annotations.Contract;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author irengrig
 *         Date: 6/8/11
 *         Time: 6:53 PM
 */
public class ContentRevisionCache {
  private final Object myLock;
  private final SLRUMap<Key, SoftReference<byte[]>> myCache;
  private final SLRUMap<CurrentKey, VcsRevisionNumber> myCurrentRevisionsCache;
  private final SLRUMap<Pair<FilePath, VcsRevisionNumber>, Object> myCustom;
  private long myCounter;

  public ContentRevisionCache() {
    myLock = new Object();
    myCache = new SLRUMap<>(100, 50);
    myCurrentRevisionsCache = new SLRUMap<>(200, 50);
    myCustom = new SLRUMap<>(30, 30);
    myCounter = 0;
  }

  private void put(FilePath path, VcsRevisionNumber number, @Nonnull VcsKey vcsKey, @Nonnull UniqueType type, @Nullable final byte[] bytes) {
    if (bytes == null) return;
    synchronized (myLock) {
      myCache.put(new Key(path, number, vcsKey, type), new SoftReference<>(bytes));
    }
  }

  @Nullable
  public String get(FilePath path, VcsRevisionNumber number, @Nonnull VcsKey vcsKey, @Nonnull UniqueType type) {
    synchronized (myLock) {
      final byte[] bytes = getBytes(path, number, vcsKey, type);
      if (bytes == null) return null;
      return bytesToString(path, bytes);
    }
  }

  public void putCustom(FilePath path, VcsRevisionNumber number, final Object o) {
    synchronized (myLock) {
      myCustom.put(Pair.create(path, number), o);
    }
  }

  @Nullable
  public Object getCustom(FilePath path, VcsRevisionNumber number) {
    synchronized (myLock) {
      return myCustom.get(Pair.create(path, number));
    }
  }

  public void clearAllCurrent() {
    synchronized (myLock) {
      ++ myCounter;
      myCurrentRevisionsCache.clear();
    }
  }

  public void clearScope(final List<VcsDirtyScope> scopes) {
    // VcsDirtyScope.belongsTo() performs some checks under read action. So deadlock could occur if some thread tries to modify
    // ContentRevisionCache (i.e. call getOrLoadCurrentAsBytes()) under write action while other thread invokes clearScope(). To prevent
    // such deadlocks we also perform locking "myLock" (and other logic) under read action.
    // TODO: "myCurrentRevisionsCache" logic should be refactored to be more clear and possibly to avoid creating such wrapping read actions
    ApplicationManager.getApplication().runReadAction(new Runnable() {
      @Override
      public void run() {
        synchronized (myLock) {
          ++myCounter;
          for (final VcsDirtyScope scope : scopes) {
            final Set<CurrentKey> toRemove = new HashSet<>();
            myCurrentRevisionsCache.iterateKeys(currentKey -> {
              if (scope.belongsTo(currentKey.getPath())) {
                toRemove.add(currentKey);
              }
            });
            for (CurrentKey key : toRemove) {
              myCurrentRevisionsCache.remove(key);
            }
          }
        }
      }
    });
  }

  public void clearCurrent(Set<String> paths) {
    final HashSet<String> converted = new HashSet<>();
    for (String path : paths) {
      converted.add(FilePathsHelper.convertPath(path));
    }
    synchronized (myLock) {
      final Set<CurrentKey> toRemove = new HashSet<>();
      myCurrentRevisionsCache.iterateKeys(currentKey -> {
        if (converted.contains(FilePathsHelper.convertPath(currentKey.getPath().getPath()))) {
          toRemove.add(currentKey);
        }
      });
      for (CurrentKey key : toRemove) {
        myCurrentRevisionsCache.remove(key);
      }
    }
  }

  @Nullable
  @Contract("!null, _, _ -> !null")
  public static String getAsString(@Nullable byte[] bytes, @Nonnull FilePath file, @Nullable Charset charset) {
    if (bytes == null) return null;
    if (charset == null) {
      return bytesToString(file, bytes);
    }
    else {
      return CharsetToolkit.bytesToString(bytes, charset);
    }
  }

  @Nullable
  public static String getOrLoadAsString(@Nonnull Project project,
                                         @Nonnull FilePath file,
                                         VcsRevisionNumber number,
                                         @Nonnull VcsKey key,
                                         @Nonnull UniqueType type,
                                         @Nonnull Throwable2Computable<byte[], VcsException, IOException> loader,
                                         @Nullable Charset charset)
          throws VcsException, IOException {
    final byte[] bytes = getOrLoadAsBytes(project, file, number, key, type, loader);
    if (bytes == null) return null;
    return getAsString(bytes, file, charset);
  }


  @Nullable
  public static String getOrLoadAsString(final Project project, FilePath path, VcsRevisionNumber number, @Nonnull VcsKey vcsKey,
                                         @Nonnull UniqueType type, final Throwable2Computable<byte[], VcsException, IOException> loader)
          throws VcsException, IOException {
    return getOrLoadAsString(project, path, number, vcsKey, type, loader, null);
  }

  private static String bytesToString(FilePath path, @Nonnull byte[] bytes) {
    Charset charset = null;
    if (path.getVirtualFile() != null) {
      charset = path.getVirtualFile().getCharset();
    }

    if (charset != null) {
      int bomLength = CharsetToolkit.getBOMLength(bytes, charset);
      final CharBuffer charBuffer = charset.decode(ByteBuffer.wrap(bytes, bomLength, bytes.length - bomLength));
      return charBuffer.toString();
    }

    return CharsetToolkit.bytesToString(bytes, EncodingRegistry.getInstance().getDefaultCharset());
  }

  @Nullable
  public byte[] getBytes(FilePath path, VcsRevisionNumber number, @Nonnull VcsKey vcsKey, @Nonnull UniqueType type) {
    synchronized (myLock) {
      final SoftReference<byte[]> reference = myCache.get(new Key(path, number, vcsKey, type));
      return SoftReference.dereference(reference);
    }
  }

  private boolean putCurrent(FilePath path, VcsRevisionNumber number, @Nonnull VcsKey vcsKey, final long counter) {
    synchronized (myLock) {
      if (myCounter != counter) return false;
      ++ myCounter;
      myCurrentRevisionsCache.put(new CurrentKey(path, vcsKey), number);
    }
    return true;
  }

  private Pair<VcsRevisionNumber, Long> getCurrent(final FilePath path, final VcsKey vcsKey) {
    synchronized (myLock) {
      return new Pair<>(myCurrentRevisionsCache.get(new CurrentKey(path, vcsKey)), myCounter);
    }
  }

  public static byte[] getOrLoadAsBytes(final Project project, FilePath path, VcsRevisionNumber number, @Nonnull VcsKey vcsKey,
                                        @Nonnull UniqueType type, final Throwable2Computable<byte[], VcsException, IOException> loader)
          throws VcsException, IOException {
    ContentRevisionCache cache = ProjectLevelVcsManager.getInstance(project).getContentRevisionCache();
    byte[] bytes = cache.getBytes(path, number, vcsKey, type);
    if (bytes != null) return bytes;

    checkLocalFileSize(path);
    bytes = loader.compute();
    cache.put(path, number, vcsKey, type, bytes);
    return bytes;
  }

  private static void checkLocalFileSize(FilePath path) throws VcsException {
    File ioFile = path.getIOFile();
    if (ioFile.exists()) {
      checkContentsSize(ioFile.getPath(), ioFile.length());
    }
  }

  public static void checkContentsSize(final String path, final long size) throws VcsException {
    if (size > VcsUtil.getMaxVcsLoadedFileSize()) {
      throw new VcsException("Can not show contents of \n'" + path +
                             "'.\nFile size is bigger than " +
                             StringUtil.formatFileSize(VcsUtil.getMaxVcsLoadedFileSize()) +
                             ".\n\nYou can relax this restriction by increasing " + VcsUtil.MAX_VCS_LOADED_SIZE_KB + " property in idea.properties file.");
    }
  }

  private static VcsRevisionNumber putIntoCurrentCache(final ContentRevisionCache cache,
                                                       FilePath path,
                                                       @Nonnull VcsKey vcsKey,
                                                       final CurrentRevisionProvider loader) throws VcsException, IOException {
    VcsRevisionNumber loadedRevisionNumber;
    Pair<VcsRevisionNumber, Long> currentRevision;

    while (true) {
      loadedRevisionNumber = loader.getCurrentRevision();
      currentRevision = cache.getCurrent(path, vcsKey);
      if (loadedRevisionNumber.equals(currentRevision.getFirst())) return loadedRevisionNumber;

      if (cache.putCurrent(path, loadedRevisionNumber, vcsKey, currentRevision.getSecond())) {
        return loadedRevisionNumber;
      }
    }
  }

  public static Pair<VcsRevisionNumber, byte[]> getOrLoadCurrentAsBytes(final Project project, FilePath path, @Nonnull VcsKey vcsKey,
                                                                                          final CurrentRevisionProvider loader) throws VcsException, IOException {
    ContentRevisionCache cache = ProjectLevelVcsManager.getInstance(project).getContentRevisionCache();

    VcsRevisionNumber currentRevision;
    Pair<VcsRevisionNumber, byte[]> loaded;
    while (true) {
      currentRevision = putIntoCurrentCache(cache, path, vcsKey, loader);
      final byte[] cachedCurrent = cache.getBytes(path, currentRevision, vcsKey, UniqueType.REPOSITORY_CONTENT);
      if (cachedCurrent != null) {
        return Pair.create(currentRevision, cachedCurrent);
      }
      checkLocalFileSize(path);
      loaded = loader.get();
      if (loaded.getFirst().equals(currentRevision)) break;
    }

    cache.put(path, currentRevision, vcsKey, UniqueType.REPOSITORY_CONTENT, loaded.getSecond());
    return loaded;
  }

  public static Pair<VcsRevisionNumber, String> getOrLoadCurrentAsString(final Project project, FilePath path, @Nonnull VcsKey vcsKey,
                                                                                           final CurrentRevisionProvider loader) throws VcsException, IOException {
    Pair<VcsRevisionNumber, byte[]> pair = getOrLoadCurrentAsBytes(project, path, vcsKey, loader);
    return Pair.create(pair.getFirst(), bytesToString(path, pair.getSecond()));
  }

  private static class CurrentKey {
    protected final FilePath myPath;
    protected final VcsKey myVcsKey;

    private CurrentKey(FilePath path, VcsKey vcsKey) {
      myPath = path;
      myVcsKey = vcsKey;
    }

    public FilePath getPath() {
      return myPath;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      CurrentKey that = (CurrentKey)o;

      if (myPath != null ? !myPath.equals(that.myPath) : that.myPath != null) return false;
      if (myVcsKey != null ? !myVcsKey.equals(that.myVcsKey) : that.myVcsKey != null) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = myPath != null ? myPath.hashCode() : 0;
      result = 31 * result + (myVcsKey != null ? myVcsKey.hashCode() : 0);
      return result;
    }
  }

  private static class Key extends CurrentKey {
    private final VcsRevisionNumber myNumber;
    protected final UniqueType myType;

    private Key(FilePath path, VcsRevisionNumber number, VcsKey vcsKey, UniqueType type) {
      super(path, vcsKey);
      myNumber = number;
      myType = type;
    }

    public VcsRevisionNumber getNumber() {
      return myNumber;
    }

    public UniqueType getType() {
      return myType;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;

      Key key = (Key)o;

      if (myNumber != null ? !myNumber.equals(key.myNumber) : key.myNumber != null) return false;
      if (!myPath.equals(key.myPath)) return false;
      if (myType != key.myType) return false;
      if (!myVcsKey.equals(key.myVcsKey)) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + myPath.hashCode();
      result = 31 * result + (myNumber != null ? myNumber.hashCode() : 0);
      result = 31 * result + myVcsKey.hashCode();
      result = 31 * result + myType.hashCode();
      return result;
    }
  }

  public static enum UniqueType {
    REPOSITORY_CONTENT,
    REMOTE_CONTENT
  }
}
