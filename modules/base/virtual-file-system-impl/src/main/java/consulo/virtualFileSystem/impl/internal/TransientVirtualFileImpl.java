// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.virtualFileSystem.impl.internal;

import consulo.logging.Logger;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.io.FileAttributes;
import consulo.util.lang.StringUtil;
import consulo.util.lang.SystemProperties;
import consulo.virtualFileSystem.BaseVirtualFile;
import consulo.virtualFileSystem.NewVirtualFileSystem;
import consulo.virtualFileSystem.RefreshQueue;
import consulo.virtualFileSystem.VFileProperty;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileSystem;
import consulo.virtualFileSystem.internal.CacheAvoidingVirtualFile;
import consulo.virtualFileSystem.internal.VirtualFileWithAttributes;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;

/**
 * Non-cached implementation of {@link VirtualFile}.
 * <p/>
 * This implementation is used to deal with file-trees outside the main project tree: regular {@link VirtualFile} implementations
 * puts quite a lot of data in the underlying VFS cache (both in-memory and persistent parts) -- to provide faster access, but
 * also to satisfy some contract statements about equality and {@link consulo.util.dataholder.UserDataHolder}, etc. VFS cache
 * could be quite large and expensive to maintain if there are really a lot of such files. For some tasks we do want to use
 * the {@link VirtualFile} VFS API, but we don't want to clutter the VFS cache with the data of all the files accessed -- e.g.
 * because this is a big, but rarely accessed file-tree. So this implementation is an experimental approach to deal with such
 * cases -- by sacrificing part of the contract.
 * <p/>
 * The implementation violates specific part of the {@link VirtualFile}'s contract: "A particular file is represented by
 * equal {@code VirtualFile} instances for the entire lifetime of the IDE process" -- it is not true for this implementation,
 * the same real file can be represented by several instances of {@link TransientVirtualFileImpl} AND by instances of different
 * {@link VirtualFile} implementations -- and those instances are not guaranteed to be equal.
 * Specifically: 'transient' VirtualFile is NOT equal to the 'cached' VirtualFile for the same path/url.
 * <p/>
 * This class inherits {@link consulo.util.dataholder.UserDataHolder} from its superclass, but it is not advised to use it.
 * The data stored via this interface is not guaranteed to live for the whole IDE session. Even more: since it could be >1
 * instance of this class for a particular 'real' file, the data could be set differently on different instances of this class,
 * and on the instances of a regular {@link VirtualFile}, representing the same real file. Basically, it is advised _not_ to
 * use {@link consulo.util.dataholder.UserDataHolder} methods on the instances of this class, because the behavior could
 * be unreliable.
 */
public final class TransientVirtualFileImpl extends BaseVirtualFile implements CacheAvoidingVirtualFile, VirtualFileWithAttributes {
    private static final Logger LOG = Logger.getInstance(TransientVirtualFileImpl.class);

    /**
     * Dedicated value to be used in {@link #cachedAttributes} if {@link NewVirtualFileSystem#getAttributes(VirtualFile)} returns null,
     * to separate this result (='attributes are known to be null') from 'attributes not yet cached'
     */
    private static final FileAttributes CACHED_NULL = new FileAttributes(false, false, false, false, 0, -1, false);

    private final String name;
    private final transient String path;

    private final VirtualFile parent;

    private final NewVirtualFileSystem fileSystem;

    /**
     * {@code fileSystem.getAttributes(this)} cached result
     */
    private volatile @Nullable FileAttributes cachedAttributes = null;

    public TransientVirtualFileImpl(String name,
                                    String path,
                                    NewVirtualFileSystem fileSystem,
                                    VirtualFile parent) {
        this.name = name;
        this.path = path;
        this.fileSystem = fileSystem;
        this.parent = parent;
    }

    TransientVirtualFileImpl(String name,
                             String path,
                             NewVirtualFileSystem fileSystem,
                             VirtualFile parent,
                             @Nullable FileAttributes attributes) {
        this(name, path, fileSystem, parent);
        this.cachedAttributes = attributes;
    }

    @Override
    public @Nullable VirtualFile asCacheable() {
        //or maybe fileSystem.refreshAndFindFileByPath(getPath()) ?
        return fileSystem.findFileByPath(getPath());
    }

    @Override
    public boolean isCached() {
        return false;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public VirtualFileSystem getFileSystem() {
        return fileSystem;
    }

    @Override
    public VirtualFile getParent() {
        return parent;
    }

    @Override
    public boolean isValid() {
        return exists();
    }

    @Override
    public boolean exists() {
        FileAttributes attributes = fetchAttributes();
        if (attributes != null) {
            return true;
        }
        else {
            return fileSystem.exists(this);
        }
    }

    @Override
    public boolean isDirectory() {
        FileAttributes attributes = fetchAttributes();
        if (attributes == null) {
            return fileSystem.isDirectory(this);
        }
        else {
            return attributes.isDirectory();
        }
    }

    @Override
    public boolean is(VFileProperty property) {
        return switch (property) {
            case SYMLINK -> {
                FileAttributes attributes = fetchAttributes();
                yield attributes != null && attributes.isSymLink();
            }
            case HIDDEN -> {
                FileAttributes attributes = fetchAttributes();
                yield attributes != null && attributes.isHidden();
            }
            case SPECIAL -> {
                FileAttributes attributes = fetchAttributes();
                yield attributes != null && attributes.isSpecial();
            }
        };
    }

    @Override
    public boolean isWritable() {
        FileAttributes attributes = fetchAttributes();
        return attributes != null && attributes.isWritable();
    }

    @Override
    public VirtualFile[] getChildren() {
        //MAYBE RC: cache children once calculated?
        // TODO: on Win/NTFS .listWithAttributes() has almost the same cost as plain .list() -- but for other OSes it is not true.
        //      So, the eager loading of all children's attributes may be an overhead on non-Win OSes, in use-cases there most of the loaded children are not really used.
        //      In such cases lazy loading of children attributes may be preferable. So far we don't know how frequent such use-cases are, so eager loading is the default (if available) -- this may need adjustments after enough statistics will be collecte
        if (fileSystem instanceof BatchingFileSystem bfs) {
            return bfs.listWithAttributes(this).entrySet().stream()
                .map(entry -> {
                    var childName = entry.getKey();
                    var attributes = entry.getValue();
                    return new TransientVirtualFileImpl(childName, path + '/' + childName, fileSystem, this, attributes);
                }).toArray(VirtualFile[]::new);
        }
        String[] childNames = fileSystem.list(this);
        return Arrays.stream(childNames)
            .map(childName -> new TransientVirtualFileImpl(childName, path + '/' + childName, fileSystem, this))
            .toArray(VirtualFile[]::new);
    }

    @Override
    public OutputStream getOutputStream(Object requestor,
                                        long newModificationStamp,
                                        long newTimeStamp) throws IOException {
        return fileSystem.getOutputStream(this, requestor, newModificationStamp, newTimeStamp);
    }

    @Override
    public byte[] contentsToByteArray() throws IOException {
        return fileSystem.contentsToByteArray(this);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return fileSystem.getInputStream(this);
    }

    @Override
    public long getTimeStamp() {
        FileAttributes attributes = fetchAttributes();
        if (attributes == null) {
            return fileSystem.getTimeStamp(this);
        }
        else {
            return attributes.lastModified;
        }
    }

    @Override
    public long getLength() {
        FileAttributes attributes = fetchAttributes();
        if (attributes == null) {
            return fileSystem.getLength(this);
        }
        else {
            return attributes.length;
        }
    }

    @Override
    public void setWritable(boolean writable) throws IOException {
        fileSystem.setWritable(this, writable);
    }

    @Override
    public long getModificationStamp() {
        return 1;//we do not track modifications for transient files
    }

    private @Nullable FileAttributes fetchAttributes() {
        //isDirectory, isWriteable, getLength() and is(hidden/special/symlink) are all delegate to .getAttributes()
        //in _most_ FS implementations. Even .exists() == (getAttributes() != null) in most FS implementations.
        //The noticeable exception is ArchiveFileSystem: without it all those methods could be boiled down to a
        // single IO getAttributes() call, with caching it -- and .refresh() could just drop the cache then.
        //To avoid the issue with ArchiveFileSystem we do following: request getAttribute(), and fallback to apt
        // fs.getXXX() if getAttribute() == null


        FileAttributes cachedAttributes = this.cachedAttributes;
        if (cachedAttributes == CACHED_NULL) {
            return null;
        }
        else if (cachedAttributes != null) {
            return cachedAttributes;
        }

        FileAttributes attributes = fileSystem.getAttributes(this);
        this.cachedAttributes = Objects.requireNonNullElse(attributes, CACHED_NULL);
        return attributes;
    }

    //<editor-fold desc="UserDataHolder overrides: prohibit access"> =========================================================================

    /**
     * All {@link consulo.util.dataholder.UserDataHolder} methods is overwritten in this class, to log error if this
     * feature-flag is true (default: false).
     * <p/>
     * Because it could be >1 instance of {@link TransientVirtualFileImpl} for the same path, and those instances do NOT share
     * userData storage -- {@link consulo.util.dataholder.UserDataHolder} methods for {@link TransientVirtualFileImpl} behave
     * in a way (likely) unexpected for the client. So even though some accesses {@link consulo.util.dataholder.UserDataHolder}
     * are harmless, some may cause incorrect behavior -- it is worth having a way to trace the accesses.
     * <p/>
     * This flag is intended to be used in tests/debug, to trace the code that does access {@link consulo.util.dataholder.UserDataHolder}
     * methods.
     */
    static final boolean LOG_USER_DATA_HOLDER_ACCESS = SystemProperties.getBooleanProperty(
        "consulo.virtualFileSystem.impl.internal.TransientVirtualFileImpl.LOG_USER_DATA_HOLDER_ACCESS",
        false
    );

    @Override
    public void copyUserDataTo(UserDataHolderBase other) {
        logUnsupportedIfNeeded(LOG_USER_DATA_HOLDER_ACCESS);
        super.copyUserDataTo(other);
    }

    @Override
    public <T> @Nullable T getUserData(Key<T> key) {
        logUnsupportedIfNeeded(LOG_USER_DATA_HOLDER_ACCESS);
        return super.getUserData(key);
    }

    @Override
    public <T> void putUserData(Key<T> key, @Nullable T value) {
        logUnsupportedIfNeeded(LOG_USER_DATA_HOLDER_ACCESS);
        super.putUserData(key, value);
    }

    @Override
    public <T> @Nullable T getCopyableUserData(Key<T> key) {
        logUnsupportedIfNeeded(LOG_USER_DATA_HOLDER_ACCESS);
        return super.getCopyableUserData(key);
    }

    @Override
    public <T> void putCopyableUserData(Key<T> key, @Nullable T value) {
        logUnsupportedIfNeeded(LOG_USER_DATA_HOLDER_ACCESS);
        super.putCopyableUserData(key, value);
    }

    @Override
    public <T> boolean replace(Key<T> key, @Nullable T oldValue, @Nullable T newValue) {
        logUnsupportedIfNeeded(LOG_USER_DATA_HOLDER_ACCESS);
        return super.replace(key, oldValue, newValue);
    }

    @Override
    public <T> T putUserDataIfAbsent(Key<T> key, T value) {
        logUnsupportedIfNeeded(LOG_USER_DATA_HOLDER_ACCESS);
        return super.putUserDataIfAbsent(key, value);
    }

    @Override
    public void copyCopyableDataTo(UserDataHolderBase clone) {
        logUnsupportedIfNeeded(LOG_USER_DATA_HOLDER_ACCESS);
        super.copyCopyableDataTo(clone);
    }

    @Override
    public boolean isUserDataEmpty() {
        logUnsupportedIfNeeded(LOG_USER_DATA_HOLDER_ACCESS);
        return super.isUserDataEmpty();
    }

    @Override
    public @Nullable FileAttributes getAttributes() {
        return fetchAttributes();
    }

    private static void logUnsupportedIfNeeded(boolean logUserDataHolderAccess) {
        if (logUserDataHolderAccess) {
            //will also log a stacktrace
            LOG.error("TransientVirtualFileImpl does NOT really support UserDataHolder (see javadoc)");
        }
    }

    //</editor-fold> =========================================================================================================================

    @Override
    public void refresh(boolean asynchronous,
                        boolean recursive,
                        @Nullable Runnable postRunnable) {
        cachedAttributes = null;
        //TODO RC: Seems like we don't need real VFS refresh for non-cached?
        //         Maybe dropping the cachedAttributes (and .children, if will decide to cache them too), is enough?
        RefreshQueue.getInstance().refresh(asynchronous, recursive, postRunnable, this);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }


        TransientVirtualFileImpl file = (TransientVirtualFileImpl) o;
        boolean caseSensitive = fileSystem.isCaseSensitive();
        return (caseSensitive ? name.equals(file.name) : name.equalsIgnoreCase(file.name))
            && parent.equals(file.parent)
            && fileSystem.equals(file.fileSystem);
    }

    @Override
    public int hashCode() {
        boolean caseSensitive = fileSystem.isCaseSensitive();
        int result = caseSensitive ? name.hashCode() : StringUtil.stringHashCodeInsensitive(name);
        result = 31 * result + parent.hashCode();
        result = 31 * result + fileSystem.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "TransientVirtualFileImpl[" + path + "][fileSystem: " + fileSystem + ']';
    }
}
