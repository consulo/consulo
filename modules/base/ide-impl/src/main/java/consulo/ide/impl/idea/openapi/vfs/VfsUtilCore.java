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
package consulo.ide.impl.idea.openapi.vfs;

import consulo.annotation.DeprecationInfo;
import consulo.content.ContentIterator;
import consulo.ide.impl.idea.util.containers.DistinctRootsCollection;
import consulo.platform.Platform;
import consulo.util.collection.ContainerUtil;
import consulo.util.io.FileUtil;
import consulo.util.io.URLUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.*;
import consulo.virtualFileSystem.event.VirtualFileEvent;
import consulo.virtualFileSystem.util.VirtualFilePathUtil;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.util.VirtualFileVisitor;
import org.jspecify.annotations.Nullable;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import static consulo.virtualFileSystem.util.VirtualFileVisitor.VisitorException;

@Deprecated
@DeprecationInfo("use VirtualFileUtil")
public class VfsUtilCore {
    public static final String LOCALHOST_URI_PATH_PREFIX = URLUtil.LOCALHOST_URI_PATH_PREFIX;
    public static final char VFS_SEPARATOR_CHAR = VirtualFileUtil.VFS_SEPARATOR_CHAR;

    /**
     * Checks whether the <code>ancestor {@link VirtualFile}</code> is parent of <code>file
     * {@link VirtualFile}</code>.
     *
     * @param ancestor the file
     * @param file     the file
     * @param strict   if <code>false</code> then this method returns <code>true</code> if <code>ancestor</code>
     *                 and <code>file</code> are equal
     * @return <code>true</code> if <code>ancestor</code> is parent of <code>file</code>; <code>false</code> otherwise
     */
    public static boolean isAncestor(VirtualFile ancestor, VirtualFile file, boolean strict) {
        return VirtualFileUtil.isAncestor(ancestor, file, strict);
    }

    /**
     * @return {@code true} if {@code file} is located under one of {@code roots} or equal to one of them
     */
    public static boolean isUnder(VirtualFile file, @Nullable Set<VirtualFile> roots) {
        return VirtualFileUtil.isUnder(file, roots);
    }

    /**
     * @return {@code true} if {@code url} is located under one of {@code rootUrls} or equal to one of them
     */
    public static boolean isUnder(String url, @Nullable Collection<String> rootUrls) {
        return VirtualFileUtil.isUnder(url, rootUrls);
    }

    public static boolean isEqualOrAncestor(String ancestorUrl, String fileUrl) {
        if (ancestorUrl.equals(fileUrl)) {
            return true;
        }
        if (StringUtil.endsWithChar(ancestorUrl, '/')) {
            return fileUrl.startsWith(ancestorUrl);
        }
        else {
            return StringUtil.startsWithConcatenation(fileUrl, ancestorUrl, "/");
        }
    }

    public static boolean isAncestor(File ancestor, File file, boolean strict) {
        return FileUtil.isAncestor(ancestor, file, strict);
    }

    /**
     * Gets relative path of <code>file</code> to <code>root</code> when it's possible
     * This method is designed to be used for file descriptions (in trees, lists etc.)
     *
     * @param file the file
     * @param root candidate to be parent file (Project base dir, any content roots etc.)
     * @return relative path of {@code file} or full path if {@code root} is not actual ancestor of {@code file}
     */
    public static @Nullable String getRelativeLocation(@Nullable VirtualFile file, VirtualFile root) {
        return VirtualFileUtil.getRelativeLocation(file, root);
    }

    public static @Nullable String getRelativePath(VirtualFile file, VirtualFile ancestor) {
        return VirtualFileUtil.getRelativePath(file, ancestor);
    }

    /**
     * Gets the relative path of <code>file</code> to its <code>ancestor</code>. Uses <code>separator</code> for
     * separating files.
     *
     * @param file      the file
     * @param ancestor  parent file
     * @param separator character to use as files separator
     * @return the relative path or {@code null} if {@code ancestor} is not ancestor for {@code file}
     */
    public static @Nullable String getRelativePath(VirtualFile file, VirtualFile ancestor, char separator) {
        return VirtualFileUtil.getRelativePath(file, ancestor, separator);
    }

    public static @Nullable VirtualFile getVirtualFileForJar(@Nullable VirtualFile entryVFile) {
        if (entryVFile == null) {
            return null;
        }
        String path = entryVFile.getPath();
        int separatorIndex = path.indexOf("!/");
        if (separatorIndex < 0) {
            return null;
        }

        String localPath = path.substring(0, separatorIndex);
        return VirtualFileManager.getInstance().findFileByUrl("file://" + localPath);
    }

    /**
     * Makes a copy of the <code>file</code> in the <code>toDir</code> folder and returns it.
     *
     * @param requestor any object to control who called this method. Note that
     *                  it is considered to be an external change if <code>requestor</code> is <code>null</code>.
     *                  See {@link VirtualFileEvent#getRequestor}
     * @param file      file to make a copy of
     * @param toDir     directory to make a copy in
     * @return a copy of the file
     * @throws IOException if file failed to be copied
     */
    
    public static VirtualFile copyFile(Object requestor, VirtualFile file, VirtualFile toDir) throws IOException {
        return VirtualFileUtil.copyFile(requestor, file, toDir);
    }

    /**
     * Makes a copy of the <code>file</code> in the <code>toDir</code> folder with the <code>newName</code> and returns it.
     *
     * @param requestor any object to control who called this method. Note that
     *                  it is considered to be an external change if <code>requestor</code> is <code>null</code>.
     *                  See {@link VirtualFileEvent#getRequestor}
     * @param file      file to make a copy of
     * @param toDir     directory to make a copy in
     * @param newName   new name of the file
     * @return a copy of the file
     * @throws IOException if file failed to be copied
     */
    
    public static VirtualFile copyFile(
        Object requestor,
        VirtualFile file,
        VirtualFile toDir,
        String newName
    ) throws IOException {
        return VirtualFileUtil.copyFile(requestor, file, toDir, newName);
    }

    
    public static InputStream byteStreamSkippingBOM(byte[] buf, VirtualFile file) throws IOException {
        return VirtualFileUtil.byteStreamSkippingBOM(buf, file);
    }

    
    public static InputStream inputStreamSkippingBOM(
        InputStream stream,
        @SuppressWarnings("UnusedParameters") VirtualFile file
    ) throws IOException {
        return VirtualFileUtil.inputStreamSkippingBOM(stream, file);
    }

    
    public static OutputStream outputStreamAddingBOM(OutputStream stream, VirtualFile file) throws IOException {
        return VirtualFileUtil.outputStreamAddingBOM(stream, file);
    }

    public static boolean iterateChildrenRecursively(
        VirtualFile root,
        @Nullable VirtualFileFilter filter,
        ContentIterator iterator
    ) {
        return VirtualFileUtil.iterateChildrenRecursively(root, filter, iterator);
    }

    @SuppressWarnings({"UnsafeVfsRecursion", "Duplicates"})
    
    public static VirtualFileVisitor.Result visitChildrenRecursively(
        VirtualFile file,
        VirtualFileVisitor<?> visitor
    ) throws VisitorException {
        return VirtualFileUtil.visitChildrenRecursively(file, visitor);
    }

    public static <E extends Exception> VirtualFileVisitor.Result visitChildrenRecursively(
        VirtualFile file,
        VirtualFileVisitor visitor,
        Class<E> eClass
    ) throws E {
        return VirtualFileUtil.visitChildrenRecursively(file, visitor, eClass);
    }

    /**
     * Returns {@code true} if given virtual file represents broken symbolic link (which points to non-existent file).
     */
    public static boolean isBrokenLink(VirtualFile file) {
        return VirtualFileUtil.isBrokenLink(file);
    }

    /**
     * Returns {@code true} if given virtual file represents broken or recursive symbolic link.
     */
    public static boolean isInvalidLink(VirtualFile link) {
        return VirtualFileUtil.isInvalidLink(link);
    }

    
    public static String loadText(VirtualFile file) throws IOException {
        return loadText(file, (int) file.getLength());
    }

    
    public static String loadText(VirtualFile file, int length) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(file.getInputStream(), file.getCharset())) {
            return new String(consulo.ide.impl.idea.openapi.util.io.FileUtil.loadText(reader, length));
        }
    }

    
    public static byte[] loadBytes(VirtualFile file) throws IOException {
        RawFileLoader rawFileLoader = RawFileLoader.getInstance();

        return rawFileLoader.isTooLarge(file.getLength())
            ? FileUtil.loadFirstAndClose(file.getInputStream(), rawFileLoader.getLargeFilePreviewSize())
            : file.contentsToByteArray();
    }

    
    public static VirtualFile[] toVirtualFileArray(Collection<? extends VirtualFile> files) {
        return VirtualFileUtil.toVirtualFileArray(files);
    }

    
    public static String urlToPath(@Nullable String url) {
        return VirtualFileUtil.urlToPath(url);
    }

    
    public static File virtualToIoFile(VirtualFile file) {
        return new File(VirtualFilePathUtil.toPresentableUrl(file.getUrl()));
    }

    
    public static String pathToUrl(String path) {
        return VirtualFileUtil.pathToUrl(path);
    }

    public static List<File> virtualToIoFiles(Collection<VirtualFile> scope) {
        return ContainerUtil.map2List(scope, VfsUtilCore::virtualToIoFile);
    }

    
    public static String toIdeaUrl(String url) {
        return toIdeaUrl(url, true);
    }

    
    public static String toIdeaUrl(String url, boolean removeLocalhostPrefix) {
        return URLUtil.toIdeaUrl(url, removeLocalhostPrefix);
    }

    
    public static String fixURLforIDEA(String url) {
        // removeLocalhostPrefix - false due to backward compatibility reasons
        return toIdeaUrl(url, false);
    }

    
    public static String convertFromUrl(URL url) {
        return VirtualFileUtil.convertFromUrl(url);
    }

    /**
     * Converts VsfUrl info {@link URL}.
     *
     * @param vfsUrl VFS url (as constructed by {@link VirtualFile#getUrl()}
     * @return converted URL or null if error has occurred.
     */
    public static @Nullable URL convertToURL(String vfsUrl) {
        return VirtualFileUtil.convertToURL(vfsUrl);
    }

    
    public static String fixIDEAUrl(String ideaUrl) {
        String ideaProtocolMarker = "://";
        int idx = ideaUrl.indexOf(ideaProtocolMarker);
        if (idx >= 0) {
            String s = ideaUrl.substring(0, idx);

            if (s.equals("jar") || s.equals(StandardFileSystems.ZIP_PROTOCOL)) {
                s = "jar:file";
            }
            String urlWithoutProtocol = ideaUrl.substring(idx + ideaProtocolMarker.length());
            ideaUrl = s + ":" + (urlWithoutProtocol.startsWith("/") ? "" : "/") + urlWithoutProtocol;
        }

        return ideaUrl;
    }

    public static @Nullable VirtualFile findRelativeFile(String uri, @Nullable VirtualFile base) {
        return VirtualFileUtil.findRelativeFile(uri, base);
    }

    public static boolean processFilesRecursively(VirtualFile root, Predicate<VirtualFile> processor) {
        if (!processor.test(root)) {
            return false;
        }

        if (root.isDirectory()) {
            LinkedList<VirtualFile[]> queue = new LinkedList<>();

            queue.add(root.getChildren());

            do {
                VirtualFile[] files = queue.removeFirst();

                for (VirtualFile file : files) {
                    if (!processor.test(file)) {
                        return false;
                    }
                    if (file.isDirectory()) {
                        queue.add(file.getChildren());
                    }
                }
            }
            while (!queue.isEmpty());
        }

        return true;
    }

    /**
     * Gets the common ancestor for passed files, or null if the files do not have common ancestors.
     *
     * @param file1 fist file
     * @param file2 second file
     * @return common ancestor for the passed files. Returns <code>null</code> if
     * the files do not have common ancestor
     */
    public static @Nullable VirtualFile getCommonAncestor(VirtualFile file1, VirtualFile file2) {
        if (!file1.getFileSystem().equals(file2.getFileSystem())) {
            return null;
        }

        VirtualFile[] path1 = getPathComponents(file1);
        VirtualFile[] path2 = getPathComponents(file2);

        int lastEqualIdx = -1;
        for (int i = 0; i < path1.length && i < path2.length; i++) {
            if (path1[i].equals(path2[i])) {
                lastEqualIdx = i;
            }
            else {
                break;
            }
        }
        return lastEqualIdx == -1 ? null : path1[lastEqualIdx];
    }

    /**
     * Gets an array of files representing paths from root to the passed file.
     *
     * @param file the file
     * @return virtual files which represents paths from root to the passed file
     */
    
    static VirtualFile[] getPathComponents(VirtualFile file) {
        ArrayList<VirtualFile> componentsList = new ArrayList<>();
        while (file != null) {
            componentsList.add(file);
            file = file.getParent();
        }
        int size = componentsList.size();
        VirtualFile[] components = new VirtualFile[size];
        for (int i = 0; i < size; i++) {
            components[i] = componentsList.get(size - i - 1);
        }
        return components;
    }

    public static boolean hasInvalidFiles(Iterable<VirtualFile> files) {
        for (VirtualFile file : files) {
            if (!file.isValid()) {
                return true;
            }
        }
        return false;
    }

    public static @Nullable VirtualFile findContainingDirectory(VirtualFile file, CharSequence name) {
        VirtualFile parent = file.isDirectory() ? file : file.getParent();
        while (parent != null) {
            if (Comparing.equal(parent.getNameSequence(), name, Platform.current().fs().isCaseSensitive())) {
                return parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    /**
     * this collection will keep only distinct files/folders, e.g. C:\foo\bar will be removed when C:\foo is added
     */
    public static class DistinctVFilesRootsCollection extends DistinctRootsCollection<VirtualFile> {
        public DistinctVFilesRootsCollection() {
        }

        public DistinctVFilesRootsCollection(Collection<VirtualFile> virtualFiles) {
            super(virtualFiles);
        }

        public DistinctVFilesRootsCollection(VirtualFile[] collection) {
            super(collection);
        }

        @Override
        protected boolean isAncestor(VirtualFile ancestor, VirtualFile virtualFile) {
            return VfsUtilCore.isAncestor(ancestor, virtualFile, false);
        }
    }

    public static void processFilesRecursively(
        VirtualFile root,
        Predicate<VirtualFile> processor,
        Function<VirtualFile, Boolean> directoryFilter
    ) {
        if (!processor.test(root)) {
            return;
        }

        if (root.isDirectory() && directoryFilter.apply(root)) {
            List<VirtualFile[]> queue = new LinkedList<>();

            queue.add(root.getChildren());

            do {
                VirtualFile[] files = queue.removeFirst();

                for (VirtualFile file : files) {
                    if (!processor.test(file)) {
                        return;
                    }
                    if (file.isDirectory() && directoryFilter.apply(file)) {
                        queue.add(file.getChildren());
                    }
                }
            }
            while (!queue.isEmpty());
        }
    }
}
