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
package consulo.versionControlSystem.util;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.ApplicationPropertiesComponent;
import consulo.application.progress.ProgressManager;
import consulo.document.FileDocumentManager;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.io.FileUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.Ref;
import consulo.versionControlSystem.*;
import consulo.versionControlSystem.action.VcsContextFactory;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ContentRevision;
import consulo.versionControlSystem.change.VcsDirtyScopeManager;
import consulo.versionControlSystem.root.VcsRoot;
import consulo.versionControlSystem.root.VcsRootDetector;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.RawFileLoader;
import consulo.virtualFileSystem.RefreshQueue;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.util.VirtualFileVisitor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

@SuppressWarnings({"UtilityClassWithoutPrivateConstructor"})
public class VcsUtil {
    protected static final char[] ourCharsToBeChopped = new char[]{'/', '\\'};
    private static final Logger LOG = Logger.getInstance(VcsUtil.class);

    public final static String MAX_VCS_LOADED_SIZE_KB = "idea.max.vcs.loaded.size.kb";
    private static final int ourMaxLoadedFileSize = computeLoadedFileSize();

    private static final int MAX_COMMIT_MESSAGE_LENGTH = 50000;
    private static final int MAX_COMMIT_MESSAGE_LINES = 3000;

    public static int getMaxVcsLoadedFileSize() {
        return ourMaxLoadedFileSize;
    }

    private static int computeLoadedFileSize() {
        int result = RawFileLoader.getInstance().getFileLengthToCacheThreshold();
        String userLimitKb = System.getProperty(MAX_VCS_LOADED_SIZE_KB);
        try {
            return userLimitKb != null ? Math.min(Integer.parseInt(userLimitKb) * 1024, result) : result;
        }
        catch (NumberFormatException ignored) {
            return result;
        }
    }

    public static void markFileAsDirty(Project project, VirtualFile file) {
        VcsDirtyScopeManager.getInstance(project).fileDirty(file);
    }

    public static void markFileAsDirty(Project project, FilePath path) {
        VcsDirtyScopeManager.getInstance(project).fileDirty(path);
    }

    public static void markFileAsDirty(Project project, String path) {
        FilePath filePath = VcsContextFactory.getInstance().createFilePathOn(new File(path));
        markFileAsDirty(project, filePath);
    }

    public static void refreshFiles(Project project, HashSet<FilePath> paths) {
        for (FilePath path : paths) {
            VirtualFile vFile = path.getVirtualFile();
            if (vFile != null) {
                if (vFile.isDirectory()) {
                    markFileAsDirty(project, vFile);
                }
                else {
                    vFile.refresh(true, vFile.isDirectory());
                }
            }
        }
    }

    /**
     * @param project Project component
     * @param file    File to check
     * @return true if the given file resides under the root associated with any
     */
    public static boolean isFileUnderVcs(Project project, String file) {
        return getVcsFor(project, getFilePath(file)) != null;
    }

    public static boolean isFileUnderVcs(Project project, FilePath file) {
        return getVcsFor(project, file) != null;
    }

    /**
     * File is considered to be a valid vcs file if it resides under the content
     * root controlled by the given vcs.
     */
    public static boolean isFileForVcs(@Nonnull VirtualFile file, Project project, AbstractVcs host) {
        return getVcsFor(project, file) == host;
    }

    //  NB: do not reduce this method to the method above since PLVcsMgr uses
    //      different methods for computing its predicate (since FilePath can
    //      refer to the deleted files).
    public static boolean isFileForVcs(FilePath path, Project project, AbstractVcs host) {
        return getVcsFor(project, path) == host;
    }

    public static boolean isFileForVcs(String path, Project project, AbstractVcs host) {
        return getVcsFor(project, getFilePath(path)) == host;
    }

    @Nullable
    public static AbstractVcs getVcsFor(@Nonnull final Project project, final FilePath file) {
        final AbstractVcs[] vcss = new AbstractVcs[1];
        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                //  IDEADEV-17916, when e.g. ContentRevision.getContent is called in
                //  a future task after the component has been disposed.
                if (!project.isDisposed()) {
                    ProjectLevelVcsManager mgr = ProjectLevelVcsManager.getInstance(project);
                    vcss[0] = (mgr != null) ? mgr.getVcsFor(file) : null;
                }
            }
        });
        return vcss[0];
    }

    @Nullable
    public static AbstractVcs getVcsFor(final Project project, @Nonnull final VirtualFile file) {
        final AbstractVcs[] vcss = new AbstractVcs[1];

        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                //  IDEADEV-17916, when e.g. ContentRevision.getContent is called in
                //  a future task after the component has been disposed.
                if (!project.isDisposed()) {
                    ProjectLevelVcsManager mgr = ProjectLevelVcsManager.getInstance(project);
                    vcss[0] = (mgr != null) ? mgr.getVcsFor(file) : null;
                }
            }
        });
        return vcss[0];
    }

    @Nullable
    public static VirtualFile getVcsRootFor(final Project project, final FilePath file) {
        final VirtualFile[] roots = new VirtualFile[1];

        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                //  IDEADEV-17916, when e.g. ContentRevision.getContent is called in
                //  a future task after the component has been disposed.
                if (!project.isDisposed()) {
                    ProjectLevelVcsManager mgr = ProjectLevelVcsManager.getInstance(project);
                    roots[0] = (mgr != null) ? mgr.getVcsRootFor(file) : null;
                }
            }
        });
        return roots[0];
    }

    @Nullable
    public static VirtualFile getVcsRootFor(final Project project, final VirtualFile file) {
        final VirtualFile[] roots = new VirtualFile[1];

        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                //  IDEADEV-17916, when e.g. ContentRevision.getContent is called in
                //  a future task after the component has been disposed.
                if (!project.isDisposed()) {
                    ProjectLevelVcsManager mgr = ProjectLevelVcsManager.getInstance(project);
                    roots[0] = (mgr != null) ? mgr.getVcsRootFor(file) : null;
                }
            }
        });
        return roots[0];
    }

    @RequiredUIAccess
    public static void refreshFiles(FilePath[] roots, Runnable runnable) {
        UIAccess.assertIsUIThread();
        refreshFiles(collectFilesToRefresh(roots), runnable);
    }

    @RequiredUIAccess
    public static void refreshFiles(File[] roots, Runnable runnable) {
        UIAccess.assertIsUIThread();
        refreshFiles(collectFilesToRefresh(roots), runnable);
    }

    private static File[] collectFilesToRefresh(FilePath[] roots) {
        File[] result = new File[roots.length];
        for (int i = 0; i < roots.length; i++) {
            result[i] = roots[i].getIOFile();
        }
        return result;
    }

    private static void refreshFiles(List<VirtualFile> filesToRefresh, Runnable runnable) {
        RefreshQueue.getInstance().refresh(true, true, runnable, filesToRefresh);
    }

    private static List<VirtualFile> collectFilesToRefresh(File[] roots) {
        ArrayList<VirtualFile> result = new ArrayList<>();
        for (File root : roots) {
            VirtualFile vFile = findFileFor(root);
            if (vFile != null) {
                result.add(vFile);
            }
            else {
                LOG.info("Failed to find VirtualFile for one of refresh roots: " + root.getAbsolutePath());
            }
        }
        return result;
    }

    @Nullable
    private static VirtualFile findFileFor(File root) {
        File current = root;
        while (current != null) {
            VirtualFile vFile = LocalFileSystem.getInstance().findFileByIoFile(root);
            if (vFile != null) {
                return vFile;
            }
            current = current.getParentFile();
        }

        return null;
    }

    @Nullable
    public static VirtualFile getVirtualFile(final String path) {
        return ApplicationManager.getApplication().runReadAction(new Supplier<VirtualFile>() {
            @Override
            @Nullable
            public VirtualFile get() {
                return LocalFileSystem.getInstance().findFileByPath(path.replace(File.separatorChar, '/'));
            }
        });
    }

    @Nullable
    public static VirtualFile getVirtualFile(final File file) {
        return ApplicationManager.getApplication().runReadAction(new Supplier<VirtualFile>() {
            @Override
            @Nullable
            public VirtualFile get() {
                return LocalFileSystem.getInstance().findFileByIoFile(file);
            }
        });
    }

    @Nullable
    public static VirtualFile getVirtualFileWithRefresh(File file) {
        if (file == null) {
            return null;
        }
        LocalFileSystem lfs = LocalFileSystem.getInstance();
        VirtualFile result = lfs.findFileByIoFile(file);
        if (result == null) {
            result = lfs.refreshAndFindFileByIoFile(file);
        }
        return result;
    }

    public static String getFileContent(final String path) {
        return ApplicationManager.getApplication().runReadAction(new Supplier<String>() {
            @Override
            public String get() {
                VirtualFile vFile = getVirtualFile(path);
                assert vFile != null;
                return FileDocumentManager.getInstance().getDocument(vFile).getText();
            }
        });
    }

    @Nullable
    public static byte[] getFileByteContent(@Nonnull File file) {
        try {
            return RawFileLoader.getInstance().loadFileBytes(file);
        }
        catch (IOException e) {
            LOG.info(e);
            return null;
        }
    }

    public static FilePath getFilePath(String path) {
        return getFilePath(new File(path));
    }

    public static FilePath getFilePath(@Nonnull VirtualFile file) {
        return VcsContextFactory.getInstance().createFilePathOn(file);
    }

    public static FilePath getFilePath(@Nonnull File file) {
        return VcsContextFactory.getInstance().createFilePathOn(file);
    }

    public static FilePath getFilePath(@Nonnull String path, boolean isDirectory) {
        return VcsContextFactory.getInstance().createFilePath(path, isDirectory);
    }

    public static FilePath getFilePathOnNonLocal(String path, boolean isDirectory) {
        return VcsContextFactory.getInstance().createFilePathOnNonLocal(path, isDirectory);
    }

    public static FilePath getFilePath(@Nonnull File file, boolean isDirectory) {
        return VcsContextFactory.getInstance().createFilePathOn(file, isDirectory);
    }

    public static FilePath getFilePathForDeletedFile(@Nonnull String path, boolean isDirectory) {
        return VcsContextFactory.getInstance().createFilePathOnDeleted(new File(path), isDirectory);
    }

    @Nonnull
    public static FilePath getFilePath(@Nonnull VirtualFile parent, @Nonnull String name) {
        return VcsContextFactory.getInstance().createFilePathOn(parent, name);
    }

    @Nonnull
    public static FilePath getFilePath(@Nonnull VirtualFile parent, @Nonnull String fileName, boolean isDirectory) {
        return VcsContextFactory.getInstance().createFilePath(parent, fileName, isDirectory);
    }

    /**
     * @param change "Change" description.
     * @return Return true if the "Change" object is created for "Rename" operation:
     * in this case name of files for "before" and "after" revisions must not
     * coniside.
     */
    public static boolean isRenameChange(Change change) {
        boolean isRenamed = false;
        ContentRevision before = change.getBeforeRevision();
        ContentRevision after = change.getAfterRevision();
        if (before != null && after != null) {
            String prevFile = getCanonicalLocalPath(before.getFile().getPath());
            String newFile = getCanonicalLocalPath(after.getFile().getPath());
            isRenamed = !prevFile.equals(newFile);
        }
        return isRenamed;
    }

    /**
     * @param change "Change" description.
     * @return Return true if the "Change" object is created for "New" operation:
     * "before" revision is obviously NULL, while "after" revision is not.
     */
    public static boolean isChangeForNew(Change change) {
        return (change.getBeforeRevision() == null) && (change.getAfterRevision() != null);
    }

    /**
     * @param change "Change" description.
     * @return Return true if the "Change" object is created for "Delete" operation:
     * "before" revision is NOT NULL, while "after" revision is NULL.
     */
    public static boolean isChangeForDeleted(Change change) {
        return (change.getBeforeRevision() != null) && (change.getAfterRevision() == null);
    }

    public static boolean isChangeForFolder(Change change) {
        ContentRevision revB = change.getBeforeRevision();
        ContentRevision revA = change.getAfterRevision();
        return (revA != null && revA.getFile().isDirectory()) || (revB != null && revB.getFile().isDirectory());
    }

    /**
     * Sort file paths so that paths under the same root are placed from the
     * innermost to the outermost (closest to the root).
     *
     * @param files An array of file paths to be sorted. Sorting is done over the parameter.
     * @return Sorted array of the file paths.
     */
    public static FilePath[] sortPathsFromInnermost(FilePath[] files) {
        return sortPaths(files, -1);
    }

    /**
     * Sort file paths so that paths under the same root are placed from the
     * outermost to the innermost (farest from the root).
     *
     * @param files An array of file paths to be sorted. Sorting is done over the parameter.
     * @return Sorted array of the file paths.
     */
    public static FilePath[] sortPathsFromOutermost(FilePath[] files) {
        return sortPaths(files, 1);
    }

    private static FilePath[] sortPaths(FilePath[] files, final int sign) {
        Arrays.sort(files, new Comparator<FilePath>() {
            @Override
            public int compare(@Nonnull FilePath o1, @Nonnull FilePath o2) {
                return sign * o1.getPath().compareTo(o2.getPath());
            }
        });
        return files;
    }

    /**
     * @param e ActionEvent object
     * @return <code>VirtualFile</code> available in the current context.
     * Returns not <code>null</code> if and only if exectly one file is available.
     */
    @Nullable
    public static VirtualFile getOneVirtualFile(AnActionEvent e) {
        VirtualFile[] files = getVirtualFiles(e);
        return (files.length != 1) ? null : files[0];
    }

    /**
     * @param e ActionEvent object
     * @return <code>VirtualFile</code>s available in the current context.
     * Returns empty array if there are no available files.
     */
    public static VirtualFile[] getVirtualFiles(AnActionEvent e) {
        VirtualFile[] files = e.getData(VirtualFile.KEY_OF_ARRAY);
        return (files == null) ? VirtualFile.EMPTY_ARRAY : files;
    }

    /**
     * Collects all files which are located in the passed directory.
     *
     * @throws IllegalArgumentException if <code>dir</code> isn't a directory.
     */
    public static void collectFiles(final VirtualFile dir, final List<VirtualFile> files, final boolean recursive, final boolean addDirectories) {
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException(VcsBundle.message("exception.text.file.should.be.directory", dir.getPresentableUrl()));
        }

        VirtualFileUtil.visitChildrenRecursively(dir, new VirtualFileVisitor() {
            @Override
            public boolean visitFile(@Nonnull VirtualFile file) {
                if (file.isDirectory()) {
                    if (addDirectories) {
                        files.add(file);
                    }
                    if (!recursive && !Comparing.equal(file, dir)) {
                        return false;
                    }
                }
                else if (file.getFileType() != UnknownFileType.INSTANCE) {
                    files.add(file);
                }
                return true;
            }
        });
    }

    public static boolean runVcsProcessWithProgress(final VcsRunnable runnable, String progressTitle, boolean canBeCanceled, Project project) throws VcsException {
        final Ref<VcsException> ex = new Ref<>();
        boolean result = ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                }
                catch (VcsException e) {
                    ex.set(e);
                }
            }
        }, progressTitle, canBeCanceled, project);
        if (!ex.isNull()) {
            throw ex.get();
        }
        return result;
    }

    public static VirtualFile waitForTheFile(final String path) {
        final VirtualFile[] file = new VirtualFile[1];
        final Application app = Application.get();
        Runnable action = new Runnable() {
            @Override
            public void run() {
                app.runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        file[0] = LocalFileSystem.getInstance().refreshAndFindFileByPath(path);
                    }
                });
            }
        };

        app.invokeAndWait(action, app.getDefaultModalityState());

        return file[0];
    }

    public static String getCanonicalLocalPath(String localPath) {
        localPath = chopTrailingChars(localPath.trim().replace('\\', '/'), ourCharsToBeChopped);
        if (localPath.length() == 2 && localPath.charAt(1) == ':') {
            localPath += '/';
        }
        return localPath;
    }

    public static String getCanonicalPath(String path) {
        String canonPath;
        try {
            canonPath = new File(path).getCanonicalPath();
        }
        catch (IOException e) {
            canonPath = path;
        }
        return canonPath;
    }

    public static String getCanonicalPath(File file) {
        String canonPath;
        try {
            canonPath = file.getCanonicalPath();
        }
        catch (IOException e) {
            canonPath = file.getAbsolutePath();
        }
        return canonPath;
    }

    /**
     * @param source Source string
     * @param chars  Symbols to be trimmed
     * @return string without all specified chars at the end. For example,
     * <code>chopTrailingChars("c:\\my_directory\\//\\",new char[]{'\\'}) is <code>"c:\\my_directory\\//"</code>,
     * <code>chopTrailingChars("c:\\my_directory\\//\\",new char[]{'\\','/'}) is <code>"c:\my_directory"</code>.
     * Actually this method can be used to normalize file names to chop trailing separator chars.
     */
    public static String chopTrailingChars(String source, char[] chars) {
        StringBuilder sb = new StringBuilder(source);
        while (true) {
            boolean atLeastOneCharWasChopped = false;
            for (int i = 0; i < chars.length && sb.length() > 0; i++) {
                if (sb.charAt(sb.length() - 1) == chars[i]) {
                    sb.deleteCharAt(sb.length() - 1);
                    atLeastOneCharWasChopped = true;
                }
            }
            if (!atLeastOneCharWasChopped) {
                break;
            }
        }
        return sb.toString();
    }

    public static VirtualFile[] paths2VFiles(String[] paths) {
        VirtualFile[] files = new VirtualFile[paths.length];
        for (int i = 0; i < paths.length; i++) {
            files[i] = getVirtualFile(paths[i]);
        }

        return files;
    }

    private static final String ANNO_ASPECT = "show.vcs.annotation.aspect.";
    //public static boolean isAspectAvailableByDefault(LineAnnotationAspect aspect) {
    //  if (aspect.getId() == null) return aspect.isShowByDefault();
    //  return PropertiesComponent.getInstance().getBoolean(ANNO_ASPECT + aspect.getId(), aspect.isShowByDefault());
    //}

    public static boolean isAspectAvailableByDefault(String id) {
        return isAspectAvailableByDefault(id, true);
    }

    public static boolean isAspectAvailableByDefault(@Nullable String id, boolean defaultValue) {
        if (id == null) {
            return false;
        }
        return ApplicationPropertiesComponent.getInstance().getBoolean(ANNO_ASPECT + id, defaultValue);
    }

    public static void setAspectAvailability(String aspectID, boolean showByDefault) {
        ApplicationPropertiesComponent.getInstance().setValue(ANNO_ASPECT + aspectID, String.valueOf(showByDefault));
    }

    public static boolean isPathRemote(String path) {
        int idx = path.indexOf("://");
        if (idx == -1) {
            int idx2 = path.indexOf(":\\\\");
            if (idx2 == -1) {
                return false;
            }
            return idx2 > 0;
        }
        return idx > 0;
    }

    public static String getPathForProgressPresentation(@Nonnull File file) {
        return file.getName() + " (" + file.getParent() + ")";
    }

    @Nonnull
    public static Collection<VcsDirectoryMapping> findRoots(@Nonnull VirtualFile rootDir, @Nonnull Project project) throws IllegalArgumentException {
        if (!rootDir.isDirectory()) {
            throw new IllegalArgumentException("Can't find VCS at the target file system path. Reason: expected to find a directory there but it's not. The path: " + rootDir.getParent());
        }
        Collection<VcsRoot> roots = project.getInstance(VcsRootDetector.class).detect(rootDir);
        Collection<VcsDirectoryMapping> result = new ArrayList<>();
        for (VcsRoot vcsRoot : roots) {
            VirtualFile vFile = vcsRoot.getPath();
            AbstractVcs rootVcs = vcsRoot.getVcs();
            if (rootVcs != null && vFile != null) {
                result.add(new VcsDirectoryMapping(vFile.getPath(), rootVcs.getName()));
            }
        }
        return result;
    }

    @Nonnull
    public static List<VcsDirectoryMapping> addMapping(@Nonnull List<? extends VcsDirectoryMapping> existingMappings, @Nonnull @NonNls String path, @Nonnull @NonNls String vcs) {
        return addMapping(existingMappings, new VcsDirectoryMapping(path, vcs));
    }

    @Nonnull
    public static List<VcsDirectoryMapping> addMapping(@Nonnull List<? extends VcsDirectoryMapping> existingMappings, @Nonnull VcsDirectoryMapping newMapping) {
        List<VcsDirectoryMapping> mappings = new ArrayList<>(existingMappings);
        for (Iterator<VcsDirectoryMapping> iterator = mappings.iterator(); iterator.hasNext(); ) {
            VcsDirectoryMapping mapping = iterator.next();
            if (mapping.isDefaultMapping() && mapping.isNoneMapping()) {
                LOG.debug("Removing <Project> -> <None> mapping");
                iterator.remove();
            }
            else if (FileUtil.pathsEqual(mapping.getDirectory(), newMapping.getDirectory())) {
                if (!StringUtil.isEmptyOrSpaces(mapping.getVcs())) {
                    LOG.warn("Substituting existing mapping [" + mapping.getDirectory() + "] -> [" + mapping.getVcs() + "] with [" + mapping.getVcs() + "]");
                }
                else {
                    LOG.debug("Removing [" + mapping.getDirectory() + "] -> <None> mapping");
                }
                iterator.remove();
            }
        }
        mappings.add(newMapping);
        return mappings;
    }

    @Nullable
    public static <T> T getIfSingle(@Nullable Stream<T> items) {
        return items == null ? null : items.limit(2).map(Optional::ofNullable).reduce(Optional.empty(), (a, b) -> a.isPresent() ^ b.isPresent() ? b : Optional.empty()).orElse(null);
    }

    public static <T> boolean isEmpty(@Nullable Stream<T> items) {
        return items == null || !items.findAny().isPresent();
    }

    @Nonnull
    public static <T> Stream<T> notNullize(@Nullable Stream<T> items) {
        return ObjectUtil.notNull(items, Stream.empty());
    }

    @Nonnull
    public static <T> Stream<T> toStream(@Nullable T... items) {
        return items == null ? Stream.empty() : Stream.of(items);
    }

    /**
     * There probably could be some performance issues if there is lots of streams to concat. See
     * http://mail.openjdk.java.net/pipermail/lambda-dev/2013-July/010659.html for some details.
     * <p>
     * Also see {@link Stream#concat(Stream, Stream)} documentation for other possible issues of concatenating large number of streams.
     */
    @Nonnull
    public static <T> Stream<T> concat(@Nonnull Stream<T>... streams) {
        return toStream(streams).reduce(Stream.empty(), Stream::concat);
    }

    @Nonnull
    public static String trimCommitMessageToSaneSize(@Nonnull String message) {
        int nthLine = nthIndexOf(message, '\n', MAX_COMMIT_MESSAGE_LINES);
        if (nthLine != -1 && nthLine < MAX_COMMIT_MESSAGE_LENGTH) {
            return trimCommitMessageAt(message, nthLine);
        }
        if (message.length() > MAX_COMMIT_MESSAGE_LENGTH + 50) {
            return trimCommitMessageAt(message, MAX_COMMIT_MESSAGE_LENGTH);
        }
        return message;
    }

    @Nonnull
    public static VirtualFile resolveSymlinkIfNeeded(@Nonnull Project project, @Nonnull VirtualFile file) {
        VirtualFile symlink = resolveSymlink(project, file);
        return symlink != null ? symlink : file;
    }

    @Nullable
    public static VirtualFile resolveSymlink(@Nonnull Project project, @Nullable VirtualFile file) {
        if (file == null) {
            return null;
        }
        for (VcsSymlinkResolver resolver : project.getExtensionList(VcsSymlinkResolver.class)) {
            if (resolver.isEnabled()) {
                VirtualFile symlink = resolver.resolveSymlink(file);
                if (symlink != null) {
                    return symlink;
                }
            }
        }
        return null;
    }

    private static String trimCommitMessageAt(@Nonnull String message, int index) {
        return String.format("%s\n\n... Commit message is too long and was truncated by %s ...", message.substring(0, index), Application.get().getName().get());
    }

    private static int nthIndexOf(@Nonnull String text, char c, int n) {
        assert n > 0;
        int length = text.length();
        int count = 0;
        for (int i = 0; i < length; i++) {
            if (text.charAt(i) == c) {
                count++;
                if (count == n) {
                    return i;
                }
            }
        }
        return -1;
    }
}
