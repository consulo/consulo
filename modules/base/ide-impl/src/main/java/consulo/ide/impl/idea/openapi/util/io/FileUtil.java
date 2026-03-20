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
package consulo.ide.impl.idea.openapi.util.io;

import consulo.annotation.DeprecationInfo;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.platform.base.localize.CommonLocalize;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.HashingStrategy;
import consulo.util.io.FilePermissionCopier;
import consulo.util.io.FileTooBigException;
import consulo.util.io.NioFiles;
import consulo.util.io.URLUtil;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ThreeState;
import consulo.virtualFileSystem.util.FilePathHashingStrategy;
import org.jspecify.annotations.Nullable;
import org.intellij.lang.annotations.RegExp;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.TestOnly;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Deprecated
@DeprecationInfo("Use consulo.util.io.FileUtil")
@SuppressWarnings("ALL")
public class FileUtil extends FileUtilRt {
    private static final int KILOBYTE = 1024;

    public static final int MEGABYTE = KILOBYTE * KILOBYTE;

    public static final int REGEX_PATTERN_FLAGS = Platform.current().fs().isCaseSensitive() ? 0 : Pattern.CASE_INSENSITIVE;

    public static final HashingStrategy<String> PATH_HASHING_STRATEGY = FilePathHashingStrategy.create();
    public static final HashingStrategy<CharSequence> PATH_CHAR_SEQUENCE_HASHING_STRATEGY = FilePathHashingStrategy.createForCharSequence();

    public static final HashingStrategy<File> FILE_HASHING_STRATEGY =
        Platform.current().fs().isCaseSensitive() ? HashingStrategy.canonical() : new HashingStrategy<>() {
            @Override
            public int hashCode(File object) {
                return fileHashCode(object);
            }

            @Override
            public boolean equals(File o1, File o2) {
                return filesEqual(o1, o2);
            }
        };

    private static final Logger LOG = Logger.getInstance(FileUtil.class);

    
    public static String join(String... parts) {
        return StringUtil.join(parts, File.separator);
    }

    public static @Nullable String getRelativePath(File base, File file) {
        return consulo.util.io.FileUtil.getRelativePath(base, file);
    }

    public static @Nullable String getRelativePath(String basePath, String filePath, char separator) {
        return consulo.util.io.FileUtil.getRelativePath(basePath, filePath, separator);
    }

    public static @Nullable String getRelativePath(
        String basePath,
        String filePath,
        char separator,
        boolean caseSensitive
    ) {
        return consulo.util.io.FileUtil.getRelativePath(basePath, filePath, separator, caseSensitive);
    }

    public static boolean isAbsolute(String path) {
        return new File(path).isAbsolute();
    }

    public static boolean exists(@Nullable String path) {
        return path != null && new File(path).exists();
    }

    /**
     * Check if the {@code ancestor} is an ancestor of {@code file}.
     *
     * @param ancestor supposed ancestor.
     * @param file     supposed descendant.
     * @param strict   if {@code false} then this method returns {@code true} if {@code ancestor} equals to {@code file}.
     * @return {@code true} if {@code ancestor} is parent of {@code file}; {@code false} otherwise.
     */
    public static boolean isAncestor(File ancestor, File file, boolean strict) {
        return isAncestor(ancestor.getPath(), file.getPath(), strict);
    }

    public static boolean isAncestor(String ancestor, String file, boolean strict) {
        return !ThreeState.NO.equals(isAncestorThreeState(ancestor, file, strict));
    }

    /**
     * Checks if the {@code ancestor} is an ancestor of the {@code file}, and if it is an immediate parent or not.
     *
     * @param ancestor supposed ancestor.
     * @param file     supposed descendant.
     * @param strict   if {@code false}, the file can be ancestor of itself,
     *                 i.e. the method returns {@code ThreeState.YES} if {@code ancestor} equals to {@code file}.
     * @return {@code ThreeState.YES} if ancestor is an immediate parent of the file,
     * {@code ThreeState.UNSURE} if ancestor is not immediate parent of the file,
     * {@code ThreeState.NO} if ancestor is not a parent of the file at all.
     */
    public static ThreeState isAncestorThreeState(String ancestor, String file, boolean strict) {
        String ancestorPath = toCanonicalPath(ancestor);
        String filePath = toCanonicalPath(file);
        return startsWith(filePath, ancestorPath, strict, Platform.current().fs().isCaseSensitive(), true);
    }

    public static boolean startsWith(String path, String start) {
        return !ThreeState.NO.equals(startsWith(path, start, false, Platform.current().fs().isCaseSensitive(), false));
    }

    public static boolean startsWith(String path, String start, boolean caseSensitive) {
        return !ThreeState.NO.equals(startsWith(path, start, false, caseSensitive, false));
    }

    
    private static ThreeState startsWith(String path, String prefix, boolean strict, boolean caseSensitive, boolean checkImmediateParent) {
        int pathLength = path.length();
        int prefixLength = prefix.length();
        if (prefixLength == 0) {
            return pathLength == 0 ? ThreeState.YES : ThreeState.UNSURE;
        }
        if (prefixLength > pathLength) {
            return ThreeState.NO;
        }
        if (!path.regionMatches(!caseSensitive, 0, prefix, 0, prefixLength)) {
            return ThreeState.NO;
        }
        if (pathLength == prefixLength) {
            return strict ? ThreeState.NO : ThreeState.YES;
        }
        char lastPrefixChar = prefix.charAt(prefixLength - 1);
        int slashOrSeparatorIdx = prefixLength;
        if (lastPrefixChar == '/' || lastPrefixChar == File.separatorChar) {
            slashOrSeparatorIdx = prefixLength - 1;
        }
        char next1 = path.charAt(slashOrSeparatorIdx);
        if (next1 == '/' || next1 == File.separatorChar) {
            if (!checkImmediateParent) {
                return ThreeState.YES;
            }

            if (slashOrSeparatorIdx == pathLength - 1) {
                return ThreeState.YES;
            }
            int idxNext = path.indexOf(next1, slashOrSeparatorIdx + 1);
            idxNext = idxNext == -1 ? path.indexOf(next1 == '/' ? '\\' : '/', slashOrSeparatorIdx + 1) : idxNext;
            return idxNext == -1 ? ThreeState.YES : ThreeState.UNSURE;
        }
        else {
            return ThreeState.NO;
        }
    }

    /**
     * @param removeProcessor parent, child
     */
    public static <T> Collection<T> removeAncestors(
        Collection<T> files,
        Function<T, String> convertor,
        BiPredicate<T, T> removeProcessor
    ) {
        if (files.isEmpty()) {
            return files;
        }
        TreeMap<String, T> paths = new TreeMap<>();
        for (T file : files) {
            String path = convertor.apply(file);
            assert path != null;
            String canonicalPath = toCanonicalPath(path);
            paths.put(canonicalPath, file);
        }
        List<Map.Entry<String, T>> ordered = new ArrayList<>(paths.entrySet());
        List<T> result = new ArrayList<>(ordered.size());
        result.add(ordered.get(0).getValue());
        for (int i = 1; i < ordered.size(); i++) {
            Map.Entry<String, T> entry = ordered.get(i);
            String child = entry.getKey();
            boolean parentNotFound = true;
            for (int j = i - 1; j >= 0; j--) {
                // possible parents
                String parent = ordered.get(j).getKey();
                if (parent == null) {
                    continue;
                }
                if (startsWith(child, parent) && removeProcessor.test(ordered.get(j).getValue(), entry.getValue())) {
                    parentNotFound = false;
                    break;
                }
            }
            if (parentNotFound) {
                result.add(entry.getValue());
            }
        }
        return result;
    }

    public static @Nullable File findAncestor(File f1, File f2) {
        File ancestor = f1;
        while (ancestor != null && !isAncestor(ancestor, f2, false)) {
            ancestor = ancestor.getParentFile();
        }
        return ancestor;
    }

    public static @Nullable File getParentFile(File file) {
        return consulo.util.io.FileUtil.getParentFile(file);
    }

    
    public static byte[] loadFirst(InputStream stream, int maxLength) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        copy(stream, maxLength, buffer);
        buffer.close();
        return buffer.toByteArray();
    }

    
    public static byte[] loadFirstAndClose(InputStream stream, int maxLength) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            copy(stream, maxLength, buffer);
        }
        finally {
            stream.close();
        }
        return buffer.toByteArray();
    }

    
    public static String loadTextAndClose(InputStream stream) throws IOException {
        //noinspection IOResourceOpenedButNotSafelyClosed
        return loadTextAndClose(new InputStreamReader(stream));
    }

    
    public static String loadTextAndClose(InputStream inputStream, boolean convertLineSeparators) throws IOException {
        String text = loadTextAndClose(inputStream);
        return convertLineSeparators ? StringUtil.convertLineSeparators(text) : text;
    }

    
    public static String loadTextAndClose(Reader reader) throws IOException {
        try {
            return new String(adaptiveLoadText(reader));
        }
        finally {
            reader.close();
        }
    }

    
    public static char[] adaptiveLoadText(Reader reader) throws IOException {
        char[] chars = new char[4096];
        List<char[]> buffers = null;
        int count = 0;
        int total = 0;
        while (true) {
            int n = reader.read(chars, count, chars.length - count);
            if (n <= 0) {
                break;
            }
            count += n;
            if (total > 1024 * 1024 * 10) {
                throw new FileTooBigException("File too big " + reader);
            }
            total += n;
            if (count == chars.length) {
                if (buffers == null) {
                    buffers = new ArrayList<>();
                }
                buffers.add(chars);
                int newLength = Math.min(1024 * 1024, chars.length * 2);
                chars = new char[newLength];
                count = 0;
            }
        }
        char[] result = new char[total];
        if (buffers != null) {
            for (char[] buffer : buffers) {
                System.arraycopy(buffer, 0, result, result.length - total, buffer.length);
                total -= buffer.length;
            }
        }
        System.arraycopy(chars, 0, result, result.length - total, total);
        return result;
    }

    
    public static byte[] adaptiveLoadBytes(InputStream stream) throws IOException {
        byte[] bytes = getThreadLocalBuffer();
        List<byte[]> buffers = null;
        int count = 0;
        int total = 0;
        while (true) {
            int n = stream.read(bytes, count, bytes.length - count);
            if (n <= 0) {
                break;
            }
            count += n;
            if (total > 1024 * 1024 * 10) {
                throw new FileTooBigException("File too big " + stream);
            }
            total += n;
            if (count == bytes.length) {
                if (buffers == null) {
                    buffers = new ArrayList<>();
                }
                buffers.add(bytes);
                int newLength = Math.min(1024 * 1024, bytes.length * 2);
                bytes = new byte[newLength];
                count = 0;
            }
        }
        byte[] result = new byte[total];
        if (buffers != null) {
            for (byte[] buffer : buffers) {
                System.arraycopy(buffer, 0, result, result.length - total, buffer.length);
                total -= buffer.length;
            }
        }
        System.arraycopy(bytes, 0, result, result.length - total, total);
        return result;
    }

    public static boolean createParentDirs(File file) {
        return consulo.util.io.FileUtil.createParentDirs(file);
    }

    public static boolean createDirectory(File path) {
        return consulo.util.io.FileUtil.createDirectory(path);
    }

    public static boolean createIfDoesntExist(File file) {
        return consulo.util.io.FileUtil.createIfNotExists(file);
    }

    public static boolean ensureCanCreateFile(File file) {
        return consulo.util.io.FileUtil.ensureCanCreateFile(file);
    }

    public static void copy(File fromFile, File toFile) throws IOException {
        performCopy(fromFile, toFile, true);
    }

    public static void copyContent(File fromFile, File toFile) throws IOException {
        performCopy(fromFile, toFile, false);
    }

    @SuppressWarnings("Duplicates")
    private static void performCopy(File fromFile, File toFile, boolean syncTimestamp) throws IOException {
        if (filesEqual(fromFile, toFile)) {
            return;
        }

        try (FileOutputStream fos = openOutputStream(toFile); FileInputStream fis = new FileInputStream(fromFile)) {
            copy(fis, fos);
        }

        if (syncTimestamp) {
            long timeStamp = fromFile.lastModified();
            if (timeStamp < 0) {
                LOG.warn("Invalid timestamp " + timeStamp + " of '" + fromFile + "'");
            }
            else if (!toFile.setLastModified(timeStamp)) {
                LOG.warn("Unable to set timestamp " + timeStamp + " to '" + toFile + "'");
            }
        }

        FilePermissionCopier.BY_NIO2.clonePermissions(fromFile.getPath(), toFile.getPath(), true);
    }

    private static FileOutputStream openOutputStream(File file) throws IOException {
        try {
            return new FileOutputStream(file);
        }
        catch (FileNotFoundException e) {
            File parentFile = file.getParentFile();
            if (parentFile == null) {
                throw new IOException("Parent file is null for " + file.getPath(), e);
            }
            createParentDirs(file);
            return new FileOutputStream(file);
        }
    }

    public static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        if (USE_FILE_CHANNELS && inputStream instanceof FileInputStream && outputStream instanceof FileOutputStream) {
            try (FileChannel fromChannel = ((FileInputStream) inputStream).getChannel()) {
                try (FileChannel toChannel = ((FileOutputStream) outputStream).getChannel()) {
                    fromChannel.transferTo(0, Long.MAX_VALUE, toChannel);
                }
            }
        }
        else {
            byte[] buffer = getThreadLocalBuffer();
            while (true) {
                int read = inputStream.read(buffer);
                if (read < 0) {
                    break;
                }
                outputStream.write(buffer, 0, read);
            }
        }
    }

    public static void copy(InputStream inputStream, int maxSize, OutputStream outputStream) throws IOException {
        byte[] buffer = getThreadLocalBuffer();
        int toRead = maxSize;
        while (toRead > 0) {
            int read = inputStream.read(buffer, 0, Math.min(buffer.length, toRead));
            if (read < 0) {
                break;
            }
            toRead -= read;
            outputStream.write(buffer, 0, read);
        }
    }

    public static void copyFileOrDir(File from, File to) throws IOException {
        copyFileOrDir(from, to, from.isDirectory());
    }

    public static void copyFileOrDir(File from, File to, boolean isDir) throws IOException {
        if (isDir) {
            copyDir(from, to);
        }
        else {
            copy(from, to);
        }
    }

    public static void copyDir(File fromDir, File toDir) throws IOException {
        copyDir(fromDir, toDir, true);
    }

    /**
     * Copies content of {@code fromDir} to {@code toDir}.
     * It's equivalent to "cp -r fromDir/* toDir" unix command.
     *
     * @param fromDir source directory
     * @param toDir   destination directory
     * @throws IOException in case of any IO troubles
     */
    public static void copyDirContent(File fromDir, File toDir) throws IOException {
        File[] children = ObjectUtil.notNull(fromDir.listFiles(), ArrayUtil.EMPTY_FILE_ARRAY);
        for (File child : children) {
            copyFileOrDir(child, new File(toDir, child.getName()));
        }
    }

    public static void copyDir(File fromDir, File toDir, boolean copySystemFiles) throws IOException {
        copyDir(fromDir, toDir, copySystemFiles ? null : file -> !StringUtil.startsWithChar(file.getName(), '.'));
    }

    public static void copyDir(File fromDir, File toDir, @Nullable FileFilter filter) throws IOException {
        ensureExists(toDir);
        if (isAncestor(fromDir, toDir, true)) {
            LOG.error(fromDir.getAbsolutePath() + " is ancestor of " + toDir + ". Can't copy to itself.");
            return;
        }
        File[] files = fromDir.listFiles();
        if (files == null) {
            throw new IOException(CommonLocalize.exceptionDirectoryIsInvalid(fromDir.getPath()).get());
        }
        if (!fromDir.canRead()) {
            throw new IOException(CommonLocalize.exceptionDirectoryIsNotReadable(fromDir.getPath()).get());
        }
        for (File file : files) {
            if (filter != null && !filter.accept(file)) {
                continue;
            }
            if (file.isDirectory()) {
                copyDir(file, new File(toDir, file.getName()), filter);
            }
            else {
                copy(file, new File(toDir, file.getName()));
            }
        }
    }

    public static void ensureExists(File dir) throws IOException {
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException(CommonLocalize.exceptionDirectoryCanNotCreate(dir.getPath()).get());
        }
    }

    
    public static String getNameWithoutExtension(File file) {
        return getNameWithoutExtension(file.getName());
    }

    
    public static String getNameWithoutExtension(String name) {
        return consulo.util.io.FileUtil.getNameWithoutExtension(name);
    }

    public static String createSequentFileName(File aParentFolder, String aFilePrefix, String aExtension) {
        return findSequentNonexistentFile(aParentFolder, aFilePrefix, aExtension).getName();
    }

    public static File findSequentNonexistentFile(File parentFolder, String filePrefix, String extension) {
        int postfix = 0;
        String ext = extension.isEmpty() ? "" : '.' + extension;
        File candidate = new File(parentFolder, filePrefix + ext);
        while (candidate.exists()) {
            postfix++;
            candidate = new File(parentFolder, filePrefix + Integer.toString(postfix) + ext);
        }
        return candidate;
    }

    
    public static String toSystemDependentName(String aFileName) {
        return consulo.util.io.FileUtil.toSystemDependentName(aFileName);
    }

    
    public static String toSystemIndependentName(String aFileName) {
        return consulo.util.io.FileUtil.toSystemIndependentName(aFileName);
    }

    /**
     * @deprecated to be removed in IDEA 17
     */
    @SuppressWarnings({"unused", "StringToUpperCaseOrToLowerCaseWithoutLocale"})
    public static String nameToCompare(String name) {
        return (Platform.current().fs().isCaseSensitive() ? name : name.toLowerCase()).replace('\\', '/');
    }

    /**
     * Converts given path to canonical representation by eliminating '.'s, traversing '..'s, and omitting duplicate separators.
     * Please note that this method is symlink-unfriendly (i.e. result of "/path/to/link/../next" most probably will differ from
     * what {@link File#getCanonicalPath()} will return) - so use with care.<br>
     * <br>
     * If the path may contain symlinks, use {@link FileUtil#toCanonicalPath(String, boolean)} instead.
     */
    @Contract("null -> null")
    public static String toCanonicalPath(@Nullable String path) {
        return toCanonicalPath(path, File.separatorChar, true);
    }

    /**
     * When relative ../ parts do not escape outside of symlinks, the links are not expanded.<br>
     * That is, in the best-case scenario the original non-expanded path is preserved.<br>
     * <br>
     * Otherwise, returns a fully resolved path using {@link File#getCanonicalPath()}.<br>
     * <br>
     * Consider the following case:
     * <pre>
     * root/
     *   dir1/
     *     link_to_dir1
     *   dir2/
     * </pre>
     * 'root/dir1/link_to_dir1/../dir2' should be resolved to 'root/dir2'
     */
    @Contract("null, _ -> null")
    public static String toCanonicalPath(@Nullable String path, boolean resolveSymlinksIfNecessary) {
        return toCanonicalPath(path, File.separatorChar, true, resolveSymlinksIfNecessary);
    }

    private static final SymlinkResolver SYMLINK_RESOLVER = new SymlinkResolver() {
        
        @Override
        public String resolveSymlinksAndCanonicalize(String path, char separatorChar, boolean removeLastSlash) {
            try {
                return new File(path).getCanonicalPath().replace(separatorChar, '/');
            }
            catch (IOException ignore) {
                // fall back to the default behavior
                return toCanonicalPath(path, separatorChar, removeLastSlash, false);
            }
        }

        @Override
        public boolean isSymlink(CharSequence path) {
            try {
                BasicFileAttributes att = NioFiles.readAttributes(Path.of(path.toString()));
                return att != null && att.isSymbolicLink();
            }
            catch (IOException e) {
                return false;
            }
        }
    };

    @Contract("null, _, _, _ -> null")
    private static String toCanonicalPath(@Nullable String path, char separatorChar, boolean removeLastSlash, boolean resolveSymlinks) {
        SymlinkResolver symlinkResolver = resolveSymlinks ? SYMLINK_RESOLVER : null;
        return toCanonicalPath(path, separatorChar, removeLastSlash, symlinkResolver);
    }

    @Contract("null, _ -> null")
    public static String toCanonicalPath(@Nullable String path, char separatorChar) {
        return toCanonicalPath(path, separatorChar, true);
    }

    @Contract("null -> null")
    public static String toCanonicalUriPath(@Nullable String path) {
        return toCanonicalPath(path, '/', false);
    }

    /**
     * converts back slashes to forward slashes
     * removes double slashes inside the path, e.g. "x/y//z" => "x/y/z"
     */
    public static String normalize(String path) {
        int start = 0;
        boolean separator = false;
        if (Platform.current().os().isWindows()) {
            if (path.startsWith("//")) {
                start = 2;
                separator = true;
            }
            else if (path.startsWith("\\\\")) {
                return normalizeTail(0, path, false);
            }
        }

        for (int i = start; i < path.length(); ++i) {
            char c = path.charAt(i);
            if (c == '/') {
                if (separator) {
                    return normalizeTail(i, path, true);
                }
                separator = true;
            }
            else if (c == '\\') {
                return normalizeTail(i, path, separator);
            }
            else {
                separator = false;
            }
        }

        return path;
    }

    
    private static String normalizeTail(int prefixEnd, String path, boolean separator) {
        StringBuilder result = new StringBuilder(path.length());
        result.append(path, 0, prefixEnd);
        int start = prefixEnd;
        if (start == 0 && Platform.current().os().isWindows() && (path.startsWith("//") || path.startsWith("\\\\"))) {
            start = 2;
            result.append("//");
            separator = true;
        }

        for (int i = start; i < path.length(); ++i) {
            char c = path.charAt(i);
            if (c == '/' || c == '\\') {
                if (!separator) {
                    result.append('/');
                }
                separator = true;
            }
            else {
                result.append(c);
                separator = false;
            }
        }

        return result.toString();
    }

    
    public static String unquote(String urlString) {
        urlString = urlString.replace('/', File.separatorChar);
        return URLUtil.unescapePercentSequences(urlString);
    }

    public static boolean isFilePathAcceptable(File root, @Nullable FileFilter fileFilter) {
        if (fileFilter == null) {
            return true;
        }
        File file = root;
        do {
            if (!fileFilter.accept(file)) {
                return false;
            }
            file = file.getParentFile();
        }
        while (file != null);
        return true;
    }

    public static boolean rename(File source, String newName) throws IOException {
        File target = new File(source.getParent(), newName);
        if (!Platform.current().fs().isCaseSensitive() && newName.equalsIgnoreCase(source.getName())) {
            File intermediate = createTempFile(source.getParentFile(), source.getName(), ".tmp", false, false);
            return source.renameTo(intermediate) && intermediate.renameTo(target);
        }
        else {
            return source.renameTo(target);
        }
    }

    public static void rename(File source, File target) throws IOException {
        if (source.renameTo(target)) {
            return;
        }
        if (!source.exists()) {
            return;
        }

        copy(source, target);
        delete(source);
    }

    public static boolean filesEqual(@Nullable File file1, @Nullable File file2) {
        // on MacOS java.io.File.equals() is incorrectly case-sensitive
        return pathsEqual(file1 == null ? null : file1.getPath(), file2 == null ? null : file2.getPath());
    }

    public static boolean pathsEqual(@Nullable String path1, @Nullable String path2) {
        if (path1 == path2) {
            return true;
        }
        if (path1 == null || path2 == null) {
            return false;
        }

        path1 = toCanonicalPath(path1);
        path2 = toCanonicalPath(path2);
        return PATH_HASHING_STRATEGY.equals(path1, path2);
    }

    /**
     * optimized version of pathsEqual - it only compares pure names, without file separators
     */
    public static boolean namesEqual(@Nullable String name1, @Nullable String name2) {
        if (name1 == name2) {
            return true;
        }
        return !(name1 == null || name2 == null) && PATH_HASHING_STRATEGY.equals(name1, name2);
    }

    public static int compareFiles(@Nullable File file1, @Nullable File file2) {
        return comparePaths(file1 == null ? null : file1.getPath(), file2 == null ? null : file2.getPath());
    }

    public static int comparePaths(@Nullable String path1, @Nullable String path2) {
        path1 = path1 == null ? null : toSystemIndependentName(path1);
        path2 = path2 == null ? null : toSystemIndependentName(path2);
        return StringUtil.compare(path1, path2, !Platform.current().fs().isCaseSensitive());
    }

    public static int fileHashCode(@Nullable File file) {
        return pathHashCode(file == null ? null : file.getPath());
    }

    public static int pathHashCode(@Nullable String path) {
        return StringUtil.isEmpty(path) ? 0 : PATH_HASHING_STRATEGY.hashCode(toCanonicalPath(path));
    }

    /**
     * This method returns extension converted to lower case, this may not be correct for case-sensitive FS.
     * Use {@link #getCaseSensitiveExtension(String)} instead to get the unchanged extension.
     * If you need to check whether a file has a specified extension use {@link consulo.util.io.FileUtil#extensionEquals(String, String)}
     */
    public static String getExtension(String fileName) {
        return consulo.util.io.FileUtil.getExtension(fileName).toLowerCase(Locale.ROOT);
    }

    
    public static String getCaseSensitiveExtension(String fileName) {
        return consulo.util.io.FileUtil.getExtension(fileName);
    }

    
    public static String resolveShortWindowsName(String path) throws IOException {
        return Platform.current().os().isWindows() && containsWindowsShortName(path) ? new File(path).getCanonicalPath() : path;
    }

    public static boolean containsWindowsShortName(String path) {
        if (StringUtil.containsChar(path, '~')) {
            path = toSystemIndependentName(path);

            int start = 0;
            while (start < path.length()) {
                int end = path.indexOf('/', start);
                if (end < 0) {
                    end = path.length();
                }

                // "How Windows Generates 8.3 File Names from Long File Names", https://support.microsoft.com/en-us/kb/142982
                int dot = path.lastIndexOf('.', end);
                if (dot < start) {
                    dot = end;
                }
                if (dot - start > 2 && dot - start <= 8 && end - dot - 1 <= 3 && path.charAt(dot - 2) == '~' && Character.isDigit(path.charAt(dot - 1))) {
                    return true;
                }

                start = end + 1;
            }
        }

        return false;
    }

    public static void collectMatchedFiles(File root, Pattern pattern, List<File> outFiles) {
        collectMatchedFiles(root, root, pattern, outFiles);
    }

    private static void collectMatchedFiles(File absoluteRoot, File root, Pattern pattern, List<File> files) {
        File[] dirs = root.listFiles();
        if (dirs == null) {
            return;
        }
        for (File dir : dirs) {
            if (dir.isFile()) {
                String relativePath = getRelativePath(absoluteRoot, dir);
                if (relativePath != null) {
                    String path = toSystemIndependentName(relativePath);
                    if (pattern.matcher(path).matches()) {
                        files.add(dir);
                    }
                }
            }
            else {
                collectMatchedFiles(absoluteRoot, dir, pattern, files);
            }
        }
    }

    @RegExp
    
    public static String convertAntToRegexp(String antPattern) {
        return convertAntToRegexp(antPattern, true);
    }

    /**
     * @param antPattern ant-style path pattern
     * @return java regexp pattern.
     * Note that no matter whether forward or backward slashes were used in the antPattern
     * the returned regexp pattern will use forward slashes ('/') as file separators.
     * Paths containing windows-style backslashes must be converted before matching against the resulting regexp
     * @see FileUtil#toSystemIndependentName
     */
    @RegExp
    
    public static String convertAntToRegexp(String antPattern, boolean ignoreStartingSlash) {
        StringBuilder builder = new StringBuilder();
        int asteriskCount = 0;
        boolean recursive = true;
        int start = ignoreStartingSlash && (StringUtil.startsWithChar(antPattern, '/') || StringUtil.startsWithChar(antPattern, '\\')) ? 1 : 0;
        for (int idx = start; idx < antPattern.length(); idx++) {
            char ch = antPattern.charAt(idx);

            if (ch == '*') {
                asteriskCount++;
                continue;
            }

            boolean foundRecursivePattern = recursive && asteriskCount == 2 && (ch == '/' || ch == '\\');
            boolean asterisksFound = asteriskCount > 0;

            asteriskCount = 0;
            recursive = ch == '/' || ch == '\\';

            if (foundRecursivePattern) {
                builder.append("(?:[^/]+/)*?");
                continue;
            }

            if (asterisksFound) {
                builder.append("[^/]*?");
            }

            if (ch == '(' || ch == ')' || ch == '[' || ch == ']' || ch == '^' || ch == '$' || ch == '.' || ch == '{' || ch == '}' || ch == '+' || ch == '|') {
                // quote regexp-specific symbols
                builder.append('\\').append(ch);
                continue;
            }
            if (ch == '?') {
                builder.append("[^/]{1}");
                continue;
            }
            if (ch == '\\') {
                builder.append('/');
                continue;
            }
            builder.append(ch);
        }

        // handle ant shorthand: my_package/test/ is interpreted as if it were my_package/test/**
        boolean isTrailingSlash = builder.length() > 0 && builder.charAt(builder.length() - 1) == '/';
        if (asteriskCount == 0 && isTrailingSlash || recursive && asteriskCount == 2) {
            if (isTrailingSlash) {
                builder.setLength(builder.length() - 1);
            }
            if (builder.length() == 0) {
                builder.append(".*");
            }
            else {
                builder.append("(?:$|/.+)");
            }
        }
        else if (asteriskCount > 0) {
            builder.append("[^/]*?");
        }
        return builder.toString();
    }

    public static boolean moveDirWithContent(File fromDir, File toDir) {
        if (!toDir.exists()) {
            return fromDir.renameTo(toDir);
        }

        File[] files = fromDir.listFiles();
        if (files == null) {
            return false;
        }

        boolean success = true;

        for (File fromFile : files) {
            File toFile = new File(toDir, fromFile.getName());
            success = success && fromFile.renameTo(toFile);
        }
        //noinspection ResultOfMethodCallIgnored
        fromDir.delete();

        return success;
    }

    
    public static String sanitizeFileName(String name) {
        return sanitizeFileName(name, true);
    }

    /**
     * @deprecated use {@link #sanitizeFileName(String, boolean)} (to be removed in IDEA 17)
     */
    @SuppressWarnings("unused")
    public static String sanitizeName(String name) {
        return sanitizeFileName(name, false);
    }

    
    public static String sanitizeFileName(String name, boolean strict) {
        StringBuilder result = null;

        int last = 0;
        int length = name.length();
        for (int i = 0; i < length; i++) {
            char c = name.charAt(i);
            boolean appendReplacement = true;
            if (c > 0 && c < 255) {
                if (strict ? Character.isLetterOrDigit(c) || c == '_' : Character.isJavaIdentifierPart(c) || c == ' ' || c == '@' || c == '-') {
                    continue;
                }
            }
            else {
                appendReplacement = false;
            }

            if (result == null) {
                result = new StringBuilder();
            }
            if (last < i) {
                result.append(name, last, i);
            }
            if (appendReplacement) {
                result.append('_');
            }
            last = i + 1;
        }

        if (result == null) {
            return name;
        }

        if (last < length) {
            result.append(name, last, length);
        }

        return result.toString();
    }

    public static boolean canExecute(File file) {
        return file.canExecute();
    }

    public static void appendToFile(File file, String text) throws IOException {
        writeToFile(file, text.getBytes(StandardCharsets.UTF_8), true);
    }

    public static void writeToFile(File file, byte[] text) throws IOException {
        writeToFile(file, text, false);
    }

    public static void writeToFile(File file, String text) throws IOException {
        writeToFile(file, text, false);
    }

    public static void writeToFile(File file, String text, boolean append) throws IOException {
        writeToFile(file, text.getBytes(StandardCharsets.UTF_8), append);
    }

    public static void writeToFile(File file, byte[] text, int off, int len) throws IOException {
        writeToFile(file, text, off, len, false);
    }

    public static void writeToFile(File file, byte[] text, boolean append) throws IOException {
        writeToFile(file, text, 0, text.length, append);
    }

    private static void writeToFile(File file, byte[] text, int off, int len, boolean append) throws IOException {
        createParentDirs(file);

        OutputStream stream = new FileOutputStream(file, append);
        try {
            stream.write(text, off, len);
        }
        finally {
            stream.close();
        }
    }

    public static boolean processFilesRecursively(File root, Predicate<File> processor) {
        return processFilesRecursively(root, processor, null);
    }

    public static boolean processFilesRecursively(
        File root,
        Predicate<File> processor,
        @Nullable Predicate<File> directoryFilter
    ) {
        LinkedList<File> queue = new LinkedList<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            File file = queue.removeFirst();
            if (!processor.test(file)) {
                return false;
            }
            if (directoryFilter != null && (!file.isDirectory() || !directoryFilter.test(file))) {
                continue;
            }

            File[] children = file.listFiles();
            if (children != null) {
                ContainerUtil.addAll(queue, children);
            }
        }
        return true;
    }

    public static @Nullable File findFirstThatExist(String... paths) {
        for (String path : paths) {
            if (!StringUtil.isEmptyOrSpaces(path)) {
                File file = new File(toSystemDependentName(path));
                if (file.exists()) {
                    return file;
                }
            }
        }

        return null;
    }

    
    public static List<File> findFilesByMask(Pattern pattern, File dir) {
        ArrayList<File> found = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    found.addAll(findFilesByMask(pattern, file));
                }
                else if (pattern.matcher(file.getName()).matches()) {
                    found.add(file);
                }
            }
        }
        return found;
    }

    
    public static List<File> findFilesOrDirsByMask(Pattern pattern, File dir) {
        ArrayList<File> found = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (pattern.matcher(file.getName()).matches()) {
                    found.add(file);
                }
                if (file.isDirectory()) {
                    found.addAll(findFilesOrDirsByMask(pattern, file));
                }
            }
        }
        return found;
    }

    /**
     * Returns empty string for empty path.
     * First checks whether provided path is a path of a file with sought-for name.
     * Unless found, checks if provided file was a directory. In this case checks existence
     * of child files with given names in order "as provided". Finally checks filename among
     * brother-files of provided. Returns null if nothing found.
     *
     * @return path of the first of found files or empty string or null.
     */
    public static @Nullable String findFileInProvidedPath(String providedPath, String... fileNames) {
        return consulo.util.io.FileUtil.findFileInProvidedPath(providedPath, fileNames);
    }

    public static boolean isAbsolutePlatformIndependent(String path) {
        return isUnixAbsolutePath(path) || isWindowsAbsolutePath(path);
    }

    public static boolean isUnixAbsolutePath(String path) {
        return path.startsWith("/");
    }

    public static boolean isWindowsAbsolutePath(String pathString) {
        return pathString.length() >= 2 && Character.isLetter(pathString.charAt(0)) && pathString.charAt(1) == ':';
    }

    
    public static File[] notNullize(@Nullable File[] files) {
        return notNullize(files, ArrayUtil.EMPTY_FILE_ARRAY);
    }

    
    public static File[] notNullize(@Nullable File[] files, File[] defaultFiles) {
        return files == null ? defaultFiles : files;
    }

    public static boolean isHashBangLine(CharSequence firstCharsIfText, String marker) {
        if (firstCharsIfText == null) {
            return false;
        }
        int lineBreak = StringUtil.indexOf(firstCharsIfText, '\n');
        if (lineBreak < 0) {
            return false;
        }
        String firstLine = firstCharsIfText.subSequence(0, lineBreak).toString();
        return firstLine.startsWith("#!") && firstLine.contains(marker);
    }

    
    public static File createTempDirectory(String prefix, @Nullable String suffix) throws IOException {
        return FileUtilRt.createTempDirectory(prefix, suffix);
    }

    
    public static File createTempDirectory(String prefix, @Nullable String suffix, boolean deleteOnExit) throws IOException {
        return FileUtilRt.createTempDirectory(prefix, suffix, deleteOnExit);
    }

    
    public static File createTempDirectory(File dir, String prefix, @Nullable String suffix) throws IOException {
        return FileUtilRt.createTempDirectory(dir, prefix, suffix);
    }

    
    public static File createTempDirectory(File dir, String prefix, @Nullable String suffix, boolean deleteOnExit) throws IOException {
        return FileUtilRt.createTempDirectory(dir, prefix, suffix, deleteOnExit);
    }

    
    public static File createTempFile(String prefix, @Nullable String suffix) throws IOException {
        return consulo.util.io.FileUtil.createTempFile(prefix, suffix);
    }

    
    public static File createTempFile(String prefix, @Nullable String suffix, boolean deleteOnExit) throws IOException {
        return consulo.util.io.FileUtil.createTempFile(prefix, suffix, deleteOnExit);
    }

    
    public static File createTempFile(File dir, String prefix, @Nullable String suffix) throws IOException {
        return consulo.util.io.FileUtil.createTempFile(dir, prefix, suffix);
    }

    
    public static File createTempFile(File dir, String prefix, @Nullable String suffix, boolean create) throws IOException {
        return consulo.util.io.FileUtil.createTempFile(dir, prefix, suffix, create);
    }

    
    public static File createTempFile(File dir, String prefix, @Nullable String suffix, boolean create, boolean deleteOnExit) throws IOException {
        return consulo.util.io.FileUtil.createTempFile(dir, prefix, suffix, create, deleteOnExit);
    }

    
    public static String getTempDirectory() {
        return consulo.util.io.FileUtil.getTempDirectory();
    }

    @TestOnly
    public static void resetCanonicalTempPathCache(String tempPath) {
        FileUtilRt.resetCanonicalTempPathCache(tempPath);
    }

    
    public static File generateRandomTemporaryPath() throws IOException {
        return FileUtilRt.generateRandomTemporaryPath();
    }

    public static void setExecutableAttribute(String path, boolean executableFlag) throws IOException {
        FileUtilRt.setExecutableAttribute(path, executableFlag);
    }

    public static void setLastModified(File file, long timeStamp) throws IOException {
        if (!file.setLastModified(timeStamp)) {
            LOG.warn(file.getPath());
        }
    }

    
    public static String loadFile(File file) throws IOException {
        return FileUtilRt.loadFile(file);
    }

    
    public static String loadFile(File file, boolean convertLineSeparators) throws IOException {
        return FileUtilRt.loadFile(file, convertLineSeparators);
    }

    
    public static String loadFile(File file, @Nullable String encoding) throws IOException {
        return FileUtilRt.loadFile(file, encoding);
    }

    
    public static String loadFile(File file, Charset encoding) throws IOException {
        return String.valueOf(FileUtilRt.loadFileText(file, encoding));
    }

    
    public static String loadFile(File file, @Nullable String encoding, boolean convertLineSeparators) throws IOException {
        return FileUtilRt.loadFile(file, encoding, convertLineSeparators);
    }

    
    public static char[] loadFileText(File file) throws IOException {
        return FileUtilRt.loadFileText(file);
    }

    
    public static char[] loadFileText(File file, @Nullable String encoding) throws IOException {
        return FileUtilRt.loadFileText(file, encoding);
    }

    
    public static char[] loadText(Reader reader, int length) throws IOException {
        return FileUtilRt.loadText(reader, length);
    }

    
    public static List<String> loadLines(File file) throws IOException {
        return consulo.util.io.FileUtil.loadLines(file);
    }

    
    public static List<String> loadLines(File file, @Nullable String encoding) throws IOException {
        return consulo.util.io.FileUtil.loadLines(file, encoding);
    }

    
    public static List<String> loadLines(String path) throws IOException {
        return consulo.util.io.FileUtil.loadLines(path);
    }

    
    public static List<String> loadLines(String path, @Nullable String encoding) throws IOException {
        return consulo.util.io.FileUtil.loadLines(path, encoding);
    }

    
    public static List<String> loadLines(BufferedReader reader) throws IOException {
        List<String> lines = new ArrayList<String>();
        String line;
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }
        return lines;
    }

    
    public static byte[] loadBytes(InputStream stream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        copy(stream, buffer);
        return buffer.toByteArray();
    }

    
    public static byte[] loadBytes(InputStream stream, int length) throws IOException {
        if (length == 0) {
            return ArrayUtil.EMPTY_BYTE_ARRAY;
        }
        byte[] bytes = new byte[length];
        int count = 0;
        while (count < length) {
            int n = stream.read(bytes, count, length - count);
            if (n <= 0) {
                break;
            }
            count += n;
        }
        return bytes;
    }

    
    public static List<String> splitPath(String path) {
        ArrayList<String> list = new ArrayList<>();
        int index = 0;
        int nextSeparator;
        while ((nextSeparator = path.indexOf(File.separatorChar, index)) != -1) {
            list.add(path.substring(index, nextSeparator));
            index = nextSeparator + 1;
        }
        list.add(path.substring(index, path.length()));
        return list;
    }

    public static boolean visitFiles(File root, Predicate<File> processor) {
        if (!processor.test(root)) {
            return false;
        }

        File[] children = root.listFiles();
        if (children != null) {
            for (File child : children) {
                if (!visitFiles(child, processor)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Like {@link Properties#load(Reader)}, but preserves the order of key/value pairs.
     */
    public static Map<String, String> loadProperties(Reader reader) throws IOException {
        final Map<String, String> map = new LinkedHashMap<>();

        new Properties() {
            @Override
            public synchronized Object put(Object key, Object value) {
                map.put(String.valueOf(key), String.valueOf(value));
                //noinspection UseOfPropertiesAsHashtable
                return super.put(key, value);
            }
        }.load(reader);

        return map;
    }

    public static boolean isRootPath(String path) {
        return path.equals("/") || path.matches("[a-zA-Z]:[/\\\\]");
    }

    public static boolean deleteWithRenaming(File file) {
        File tempFileNameForDeletion = findSequentNonexistentFile(file.getParentFile(), file.getName(), "");
        boolean success = file.renameTo(tempFileNameForDeletion);
        return delete(success ? tempFileNameForDeletion : file);
    }
}
