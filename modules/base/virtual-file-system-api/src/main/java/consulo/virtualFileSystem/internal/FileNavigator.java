// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.virtualFileSystem.internal;

import consulo.logging.Logger;
import consulo.virtualFileSystem.NewVirtualFile;
import consulo.virtualFileSystem.VFileProperty;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

/**
 * Interface abstracts out walking through VirtualFile hierarchy.
 * Implementations of this interface provide a way to 'move' up(=parent) and down(=child) the hierarchy, while the path
 * resolution methods scan the path string, and use the {@link FileNavigator} methods to 'interpret' path segments.
 */
public interface FileNavigator<F extends VirtualFile> {
    Logger LOG = Logger.getInstance(FileNavigator.class);

    @Nullable
    F parentOf(F file);

    /**
     * @return a child file with a given childName, or null, if a child can't be resolved for given childName;
     * (definition of 'resolved' is implementation-dependent)
     */
    @Nullable
    F childOf(F parent, String childName);

    /** Don't resolve symlinks or canonicalize the path -- just walk the path segments as they are. */
    FileNavigator<NewVirtualFile> LEXICAL = new FileNavigator<>() {
        @Override
        public @Nullable NewVirtualFile parentOf(NewVirtualFile file) {
            return file.getParent();
        }

        @Override
        public @Nullable NewVirtualFile childOf(NewVirtualFile parent, String childName) {
            return parent.findChild(childName);
        }
    };

    /**
     * POSIX path resolution requires resolving _each_ path segment against the file system -- e.g. resolve all symlinks.
     * This implementation resolves symlink _only_ before '..' (i.e. in {@link #parentOf(VirtualFile)}), but not in other cases
     * -- which is why it is 'light'.
     * It is an optimization, to reduce # of accesses to the actual underlying FS. But it has downsides: the path resolution
     * sometimes gives unexpected result.
     * E.g. 'a/b/c' and 'a/../a/b/c' paths one would expect them to resolve to the same file, since `a/../a` segment should just
     * collapse to `a`. This is true for POSIX resolution, but not for 'POSIX light': under 'POSIX light' result of the path
     * resolution depends on is `a` a symlink or not. If `a` is a symlink (->`/home/user/AAA`), then the symlink will be resolved
     * during 'a/../a/b/c' resolution, but during 'a/b/c' resolution symlink will NOT be resolved -- hence, 'a/../a/b/c' resolves
     * to VirtualFile[`/home/user/AAA/b/c`], while `a/b/c` resolves to just VirtualFile[`a/b/c`].
     * This is important only on the level of VirtualFile operations -- e.g. if one reads VirtualFile[`a/b/c`] content, the read
     * goes through underlying FS, which evaluates the path via true POSIX path resolution, so the `/home/user/AAA/b/c` content
     * is really read. But as long as you work with VirtualFiles, the VirtualFile[`/home/user/AAA/b/c`] != VirtualFile[`a/b/c`],
     * and this difference may sometimes bite.
     */
    FileNavigator<NewVirtualFile> POSIX_LIGHT = new FileNavigator<>() {
        @Override
        public @Nullable NewVirtualFile parentOf(NewVirtualFile file) {
            //Here we do 'partial canonicalization' of the path: we resolve symlinks, but _only_ in getParent, i.e. before
            // '..' segments. It makes the resolution "unstable" so to say: 'a/b/c' and 'a/../a/b/c' could be resolved to different
            // files, depending on is 'a' a symlink or not.
            // The result differs from regular canonicalization, via VirtualFile.getCanonicalPath() or Path.toRealPath(),
            // there _all_ symlinks are resolved -- but the result also differs from Path.toRealPath(NOFOLLOW_LINKS) where _none_
            // of symlinks are resolved.
            NewVirtualFile resolved = file;
            if (resolved.is(VFileProperty.SYMLINK)) {
                NewVirtualFile canonicalFile = resolved.getCanonicalFile();
                if (LOG.isTraceEnabled()) {
                    LOG.trace("[" + resolved.getPath() + "]: symlink resolved to [" + canonicalFile + "]");
                }
                if (canonicalFile == null) {
                    return null;
                }

                resolved = canonicalFile;
            }
            return resolved.getParent();
        }

        @Override
        public @Nullable NewVirtualFile childOf(NewVirtualFile parent, String childName) {
            return parent.findChild(childName);
        }
    };
}
