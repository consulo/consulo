// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.virtualFileSystem.impl.internal.entry;

import consulo.application.ApplicationManager;
import consulo.util.lang.CharArrayUtil;
import consulo.util.lang.LocalTimeCounter;
import consulo.virtualFileSystem.*;
import consulo.virtualFileSystem.encoding.EncodingManager;
import consulo.virtualFileSystem.encoding.EncodingRegistry;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.impl.internal.FileNameCache;
import consulo.virtualFileSystem.impl.internal.PersistentFSImpl;
import consulo.virtualFileSystem.internal.VfsImplUtil;
import consulo.virtualFileSystem.internal.*;
import consulo.virtualFileSystem.localize.VirtualFileSystemLocalize;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Collection;

/**
 * @author max
 */
public abstract class VirtualFileSystemEntry extends InternalNewVirtualFile {
  public static final VirtualFileSystemEntry[] EMPTY_ARRAY = new VirtualFileSystemEntry[0];

  static final PersistentFS ourPersistence = PersistentFS.getInstance();

  static final int IS_WRITABLE_FLAG = 0x01000000;
  static final int IS_HIDDEN_FLAG = 0x02000000;
  private static final int INDEXED_FLAG = 0x04000000;
  static final int CHILDREN_CACHED = 0x08000000; // makes sense for directory only
  static final int SYSTEM_LINE_SEPARATOR_DETECTED = CHILDREN_CACHED; // makes sense for non-directory file only
  private static final int DIRTY_FLAG = 0x10000000;
  static final int IS_SYMLINK_FLAG = 0x20000000;
  private static final int HAS_SYMLINK_FLAG = 0x40000000;
  static final int IS_SPECIAL_FLAG = 0x80000000;

  static final int ALL_FLAGS_MASK = DIRTY_FLAG | IS_SYMLINK_FLAG | HAS_SYMLINK_FLAG | IS_SPECIAL_FLAG | IS_WRITABLE_FLAG | IS_HIDDEN_FLAG | INDEXED_FLAG | CHILDREN_CACHED;

  @Nonnull // except NULL_VIRTUAL_FILE
  final VfsData.Segment mySegment;
  private final VirtualDirectoryImpl myParent;
  final int myId;
  private volatile CachedFileType myFileType;

  static {
    //noinspection ConstantConditions
    assert (~ALL_FLAGS_MASK) == LocalTimeCounter.TIME_MASK;
  }

  VirtualFileSystemEntry(int id, @Nonnull VfsData.Segment segment, @Nullable VirtualDirectoryImpl parent) {
    mySegment = segment;
    myId = id;
    myParent = parent;
    if (id <= 0) {
      throw new IllegalArgumentException("id must be positive but got: " + id);
    }
  }

  // for NULL_FILE
  private VirtualFileSystemEntry() {
    // although in general mySegment is always @NotNull,
    // in this case we made an exception to be able to instantiate special singleton NULL_VIRTUAL_FILE
    //noinspection ConstantConditions
    mySegment = null;
    myParent = null;
    myId = -42;
  }

  void updateLinkStatus() {
    setFlagInt(HAS_SYMLINK_FLAG, is(VFileProperty.SYMLINK) || getParent().getFlagInt(HAS_SYMLINK_FLAG));
  }

  @Override
  @Nonnull
  public String getName() {
    return getNameSequence().toString();
  }

  @Nonnull
  @Override
  public CharSequence getNameSequence() {
    return FileNameCache.getVFileName(getNameId());
  }

  @Override
  public final int getNameId() {
    return mySegment.getNameId(myId);
  }

  @Override
  public VirtualDirectoryImpl getParent() {
    VirtualDirectoryImpl changedParent = mySegment.vfsData.getChangedParent(myId);
    return changedParent != null ? changedParent : myParent;
  }

  @Override
  public boolean isDirty() {
    return getFlagInt(DIRTY_FLAG);
  }

  @Override
  public long getModificationStamp() {
    return isValid() ? mySegment.getModificationStamp(myId) : -1;
  }

  public void setModificationStamp(long modificationStamp) {
    mySegment.setModificationStamp(myId, modificationStamp);
  }

  boolean getFlagInt(int mask) {
    return mySegment.getFlag(myId, mask);
  }

  void setFlagInt(int mask, boolean value) {
    mySegment.setFlag(myId, mask, value);
  }

  @Override
  public boolean isFileIndexed() {
    return getFlagInt(INDEXED_FLAG);
  }

  @Override
  public void setFileIndexed(boolean indexed) {
    setFlagInt(INDEXED_FLAG, indexed);
  }

  @Override
  public void markClean() {
    setFlagInt(DIRTY_FLAG, false);
  }

  @Override
  public void markDirty() {
    if (!isDirty()) {
      markDirtyInternal();
      VirtualFileSystemEntry parent = getParent();
      if (parent != null) parent.markDirty();
    }
  }

  @Override
  public void markDirtyInternal() {
    setFlagInt(DIRTY_FLAG, true);
  }

  @Override
  public void markDirtyRecursively() {
    markDirty();
    for (VirtualFile file : getCachedChildren()) {
      ((NewVirtualFile)file).markDirtyRecursively();
    }
  }

  @Nonnull
  protected char[] appendPathOnFileSystem(int accumulatedPathLength, int[] positionRef) {
    CharSequence name = getNameSequence();

    char[] chars = getParent().appendPathOnFileSystem(accumulatedPathLength + 1 + name.length(), positionRef);
    int i = positionRef[0];
    chars[i] = '/';
    positionRef[0] = copyString(chars, i + 1, name);

    return chars;
  }

  private static int copyString(@Nonnull char[] chars, int pos, @Nonnull CharSequence s) {
    int length = s.length();
    CharArrayUtil.getChars(s, chars, 0, pos, length);
    return pos + length;
  }

  @Override
  @Nonnull
  public String getUrl() {
    String protocol = getFileSystem().getProtocol();
    int prefixLen = protocol.length() + "://".length();
    char[] chars = appendPathOnFileSystem(prefixLen, new int[]{prefixLen});
    copyString(chars, copyString(chars, 0, protocol), "://");
    return new String(chars);
  }

  @Override
  @Nonnull
  public String getPath() {
    return new String(appendPathOnFileSystem(0, new int[]{0}));
  }

  @Override
  public void delete(final Object requestor) throws IOException {
    ApplicationManager.getApplication().assertWriteAccessAllowed();
    ourPersistence.deleteFile(requestor, this);
  }

  @Override
  public void rename(final Object requestor, @Nonnull @NonNls final String newName) throws IOException {
    ApplicationManager.getApplication().assertWriteAccessAllowed();
    if (getName().equals(newName)) return;
    validateName(newName);
    ourPersistence.renameFile(requestor, this, newName);
  }

  @Override
  @Nonnull
  public VirtualFile createChildData(final Object requestor, @Nonnull final String name) throws IOException {
    validateName(name);
    return ourPersistence.createChildFile(requestor, this, name);
  }

  @Override
  public boolean isWritable() {
    return getFlagInt(IS_WRITABLE_FLAG);
  }

  @Override
  public void setWritable(boolean writable) throws IOException {
    ourPersistence.setWritable(this, writable);
  }

  @Override
  public long getTimeStamp() {
    return ourPersistence.getTimeStamp(this);
  }

  @Override
  public void setTimeStamp(final long time) throws IOException {
    ourPersistence.setTimeStamp(this, time);
  }

  @Override
  public long getLength() {
    return ourPersistence.getLength(this);
  }

  @Nonnull
  @Override
  public VirtualFile copy(final Object requestor, @Nonnull final VirtualFile newParent, @Nonnull final String copyName) throws IOException {
    if (getFileSystem() != newParent.getFileSystem()) {
      throw new IOException(VirtualFileSystemLocalize.fileCopyError(newParent.getPresentableUrl()).get());
    }

    if (!newParent.isDirectory()) {
      throw new IOException(VirtualFileSystemLocalize.fileCopyTargetMustBeDirectory().get());
    }

    return EncodingRegistry.doActionAndRestoreEncoding(this, () -> ourPersistence.copyFile(requestor, this, newParent, copyName));
  }

  @Override
  public void move(final Object requestor, @Nonnull final VirtualFile newParent) throws IOException {
    ApplicationManager.getApplication().assertWriteAccessAllowed();

    if (getFileSystem() != newParent.getFileSystem()) {
      throw new IOException(VirtualFileSystemLocalize.fileMoveError(newParent.getPresentableUrl()).get());
    }

    EncodingRegistry.doActionAndRestoreEncoding(this, () -> {
      ourPersistence.moveFile(requestor, this, newParent);
      return this;
    });
  }

  @Override
  public int getId() {
    return myId;
  }

  @Override
  public boolean equals(Object o) {
    return this == o || o instanceof VirtualFileSystemEntry && myId == ((VirtualFileSystemEntry)o).myId;
  }

  @Override
  public int hashCode() {
    return myId;
  }

  @Override
  @Nonnull
  public VirtualFile createChildDirectory(final Object requestor, @Nonnull final String name) throws IOException {
    validateName(name);
    return ourPersistence.createChildDirectory(requestor, this, name);
  }

  private void validateName(@Nonnull String name) throws IOException {
    if (!getFileSystem().isValidName(name)) {
      throw new IOException(VirtualFileSystemLocalize.fileInvalidNameError(name).get());
    }
  }

  @Override
  public boolean exists() {
    return mySegment.vfsData.isFileValid(myId);
  }

  @Override
  public boolean isValid() {
    return exists();
  }

  @Override
  public String toString() {
    return getUrl();
  }

  public void setNewName(@Nonnull String newName) {
    if (!getFileSystem().isValidName(newName)) {
      throw new IllegalArgumentException(VirtualFileSystemLocalize.fileInvalidNameError(newName).get());
    }

    VirtualDirectoryImpl parent = getParent();
    parent.removeChild(this);
    mySegment.setNameId(myId, FileNameCache.storeName(newName));
    parent.addChild(this);
    ((PersistentFSImpl)PersistentFS.getInstance()).incStructuralModificationCount();
  }

  public void setParent(@Nonnull VirtualFile newParent) {
    ApplicationManager.getApplication().assertWriteAccessAllowed();

    VirtualDirectoryImpl parent = getParent();
    parent.removeChild(this);

    VirtualDirectoryImpl directory = (VirtualDirectoryImpl)newParent;
    mySegment.vfsData.changeParent(myId, directory);
    directory.addChild(this);
    updateLinkStatus();
    ((PersistentFSImpl)PersistentFS.getInstance()).incStructuralModificationCount();
  }

  @Override
  public boolean isInLocalFileSystem() {
    return getFileSystem() instanceof LocalFileSystem;
  }

  public void invalidate() {
    mySegment.vfsData.invalidateFile(myId);
  }

  @Nonnull
  @Override
  public Charset getCharset() {
    return isCharsetSet() ? super.getCharset() : computeCharset();
  }

  @Nonnull
  private Charset computeCharset() {
    Charset charset;
    if (isDirectory()) {
      Charset configured = EncodingManager.getInstance().getEncoding(this, true);
      charset = configured == null ? Charset.defaultCharset() : configured;
      setCharset(charset);
    }
    else {
      FileType fileType = getFileType();
      if (isCharsetSet()) {
        // file type detection may have cached the charset, no need to re-detect
        return super.getCharset();
      }
      try {
        final byte[] content = VfsImplUtil.loadBytes(this);
        charset = LoadTextUtil.detectCharsetAndSetBOM(this, content, fileType);
      }
      catch (IOException e) {
        return super.getCharset();
      }
    }
    return charset;
  }

  @Override
  public String getPresentableName() {
    if (VirtualFileSystemInternalHelper.getInstance().isHideKnownExtensionInTabs() && !isDirectory()) {
      final String nameWithoutExtension = getNameWithoutExtension();
      return nameWithoutExtension.isEmpty() ? getName() : nameWithoutExtension;
    }
    return getName();
  }

  @Override
  public boolean is(@Nonnull VFileProperty property) {
    if (property == VFileProperty.SPECIAL) return getFlagInt(IS_SPECIAL_FLAG);
    if (property == VFileProperty.HIDDEN) return getFlagInt(IS_HIDDEN_FLAG);
    if (property == VFileProperty.SYMLINK) return getFlagInt(IS_SYMLINK_FLAG);
    return super.is(property);
  }

  public void updateProperty(@Nonnull String property, boolean value) {
    if (property == PROP_WRITABLE) setFlagInt(IS_WRITABLE_FLAG, value);
    if (property == PROP_HIDDEN) setFlagInt(IS_HIDDEN_FLAG, value);
  }

  @Override
  public String getCanonicalPath() {
    if (getFlagInt(HAS_SYMLINK_FLAG)) {
      if (is(VFileProperty.SYMLINK)) {
        return ourPersistence.resolveSymLink(this);
      }
      VirtualFileSystemEntry parent = getParent();
      if (parent != null) {
        return parent.getCanonicalPath() + "/" + getName();
      }
      return getName();
    }
    return getPath();
  }

  @Override
  public NewVirtualFile getCanonicalFile() {
    if (getFlagInt(HAS_SYMLINK_FLAG)) {
      final String path = getCanonicalPath();
      return path != null ? (NewVirtualFile)getFileSystem().findFileByPath(path) : null;
    }
    return this;
  }

  @Override
  public boolean isRecursiveOrCircularSymLink() {
    if (!is(VFileProperty.SYMLINK)) return false;
    NewVirtualFile resolved = getCanonicalFile();
    // invalid symlink
    if (resolved == null) return false;
    // if it's recursive
    if (VirtualFileUtil.isAncestor(resolved, this, false)) return true;

    // check if it's circular - any symlink above resolves to my target too
    for (VirtualFileSystemEntry p = getParent(); p != null; p = p.getParent()) {
      // optimization: when the file has no symlinks up the hierarchy, it's not circular
      if (!p.getFlagInt(HAS_SYMLINK_FLAG)) return false;
      if (p.is(VFileProperty.SYMLINK)) {
        VirtualFile parentResolved = p.getCanonicalFile();
        if (resolved.equals(parentResolved)) {
          return true;
        }
      }
    }
    return false;
  }

  @Nonnull
  @Override
  public FileType getFileType() {
    CachedFileType cache = myFileType;
    FileType type = cache == null ? null : cache.getUpToDateOrNull();
    if (type == null) {
      type = super.getFileType();
      myFileType = CachedFileType.forType(type);
    }
    return type;
  }

  static final VirtualFileSystemEntry NULL_VIRTUAL_FILE = new VirtualFileSystemEntry() {
    @Override
    public String toString() {
      return "NULL";
    }

    @Nonnull
    @Override
    public NewVirtualFileSystem getFileSystem() {
      throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public NewVirtualFile findChild(@Nonnull String name) {
      throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public NewVirtualFile refreshAndFindChild(@Nonnull String name) {
      throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public NewVirtualFile findChildIfCached(@Nonnull String name) {
      throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public Collection<VirtualFile> getCachedChildren() {
      throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public Iterable<VirtualFile> iterInDbChildren() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDirectory() {
      throw new UnsupportedOperationException();
    }

    @Override
    public VirtualFile[] getChildren() {
      throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp) {
      throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getInputStream() {
      throw new UnsupportedOperationException();
    }
  };
}