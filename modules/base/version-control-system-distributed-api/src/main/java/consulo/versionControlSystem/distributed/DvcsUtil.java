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
package consulo.versionControlSystem.distributed;

import consulo.application.AccessToken;
import consulo.application.internal.BackgroundTaskUtil;
import consulo.application.util.DateFormatUtil;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.TextEditor;
import consulo.fileEditor.statusBar.StatusBarUtil;
import consulo.logging.Logger;
import consulo.module.content.ProjectRootManager;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.ModuleExtensionWithSdkOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.WindowManager;
import consulo.util.collection.ContainerUtil;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.distributed.push.PushSupport;
import consulo.versionControlSystem.distributed.repository.AbstractRepositoryManager;
import consulo.versionControlSystem.distributed.repository.RepoStateException;
import consulo.versionControlSystem.distributed.repository.Repository;
import consulo.versionControlSystem.distributed.repository.RepositoryManager;
import consulo.versionControlSystem.log.TimedVcsCommit;
import consulo.versionControlSystem.log.VcsFullCommitDetails;
import consulo.versionControlSystem.update.RefreshVFsSynchronously;
import consulo.versionControlSystem.util.VcsImplUtil;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.RawFileLoader;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveVfsUtil;
import consulo.virtualFileSystem.event.BatchFileChangeListener;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Contract;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class DvcsUtil {

    private static final Logger LOG = Logger.getInstance(DvcsUtil.class);

    private static final Logger LOGGER = Logger.getInstance(DvcsUtil.class);
    private static final int IO_RETRIES = 3; // number of retries before fail if an IOException happens during file read.
    private static final int SHORT_HASH_LENGTH = 8;
    private static final int LONG_HASH_LENGTH = 40;

    /**
     * Comparator for virtual files by name
     */
    public static final Comparator<VirtualFile> VIRTUAL_FILE_PRESENTATION_COMPARATOR = (o1, o2) -> {
        if (o1 == null && o2 == null) {
            return 0;
        }
        if (o1 == null) {
            return -1;
        }
        if (o2 == null) {
            return 1;
        }
        return o1.getPresentableUrl().compareTo(o2.getPresentableUrl());
    };

    @Nonnull
    public static List<VirtualFile> sortVirtualFilesByPresentation(@Nonnull Collection<VirtualFile> virtualFiles) {
        return ContainerUtil.sorted(virtualFiles, VIRTUAL_FILE_PRESENTATION_COMPARATOR);
    }

    @Nonnull
    public static List<VirtualFile> findVirtualFilesWithRefresh(@Nonnull List<File> files) {
        RefreshVFsSynchronously.refreshFiles(files);
        return ContainerUtil.mapNotNull(files, file -> VirtualFileUtil.findFileByIoFile(file, false));
    }

    /**
     * @deprecated use {@link VcsImplUtil#getShortVcsRootName}
     */
    @Nonnull
    @Deprecated
    public static String getShortRepositoryName(@Nonnull Project project, @Nonnull VirtualFile root) {
        return VcsImplUtil.getShortVcsRootName(project, root);
    }

    @Nonnull
    public static String getShortRepositoryName(@Nonnull Repository repository) {
        return getShortRepositoryName(repository.getProject(), repository.getRoot());
    }

    /**
     * Find the VCS root on which a repository-wide AnAction is to be invoked in a given context.
     */
    @Nullable
    public static <T extends Repository> T guessRepositoryForOperation(@Nonnull Project project,
                                                                       @Nonnull AbstractRepositoryManager<T> manager,
                                                                       @Nonnull DataContext dataContext) {
        VirtualFile file = dataContext.getData(VirtualFile.KEY);
        T repository = manager.getRepositoryForRootQuick(findVcsRootFor(project, file));
        if (repository != null) {
            return repository;
        }

        file = getSelectedFile(dataContext); // last active FileEditor
        repository = manager.getRepositoryForRootQuick(findVcsRootFor(project, file));
        if (repository != null) {
            return repository;
        }

        repository = manager.getRepositoryForRootQuick(guessRootForVcs(project, manager.getVcs(), null));
        if (repository != null) {
            return repository;
        }

        return null;
    }

    /**
     * Find relevant VCS root for a given file, if any. Note that this root might not track the file itself.
     */
    @Nullable
    public static VirtualFile findVcsRootFor(@Nonnull Project project, @Nullable VirtualFile file) {
        VirtualFile root = ProjectLevelVcsManager.getInstance(project).getVcsRootFor(file);
        if (root != null) {
            return root;
        }

        if (file != null) {
            root = getVcsRootForLibraryFile(project, file);
            if (root != null) {
                return root;
            }
        }

        return null;
    }

    @Nonnull
    public static String getShortNames(@Nonnull Collection<? extends Repository> repositories) {
        return StringUtil.join(repositories, DvcsUtil::getShortRepositoryName, ", ");
    }

    @Nonnull
    public static String fileOrFolder(@Nonnull VirtualFile file) {
        if (file.isDirectory()) {
            return "folder";
        }
        else {
            return "file";
        }
    }

    public static boolean anyRepositoryIsFresh(Collection<? extends Repository> repositories) {
        for (Repository repository : repositories) {
            if (repository.isFresh()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static String joinMessagesOrNull(@Nonnull Collection<String> messages) {
        String joined = StringUtil.join(messages, "\n");
        return StringUtil.isEmptyOrSpaces(joined) ? null : joined;
    }

    public static @Nullable VirtualFile getSelectedFile(@Nonnull DataContext dataProvider) {
        FileEditor fileEditor = dataProvider.getData(FileEditor.KEY);
        return fileEditor == null ? null : fileEditor.getFile();
    }

    /**
     * Returns the currently selected file, based on which VcsBranch or StatusBar components will identify the current repository root.
     */
    @Nullable
    public static VirtualFile getSelectedFile(@Nonnull Project project) {
        StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
        FileEditor fileEditor = StatusBarUtil.getCurrentFileEditor(statusBar);
        VirtualFile result = null;
        if (fileEditor != null) {
            if (fileEditor instanceof TextEditor) {
                Document document = ((TextEditor) fileEditor).getEditor().getDocument();
                result = FileDocumentManager.getInstance().getFile(document);
            }
            else {
                result = fileEditor.getFile();
            }
        }

        if (result == null) {
            FileEditorManager manager = FileEditorManager.getInstance(project);
            if (manager != null) {
                Editor editor = manager.getSelectedTextEditor();
                if (editor != null) {
                    result = FileDocumentManager.getInstance().getFile(editor.getDocument());
                }
            }
        }
        return result;
    }

    @Nonnull
    public static String getShortHash(@Nonnull String hash) {
        if (hash.length() < SHORT_HASH_LENGTH) {
            LOG.debug("Unexpectedly short hash: [" + hash + "]");
        }
        if (hash.length() > LONG_HASH_LENGTH) {
            LOG.debug("Unexpectedly long hash: [" + hash + "]");
        }
        return hash.substring(0, Math.min(SHORT_HASH_LENGTH, hash.length()));
    }

    @Nonnull
    public static String getDateString(@Nonnull TimedVcsCommit commit) {
        return DateFormatUtil.formatPrettyDateTime(commit.getTimestamp()) + " ";
    }

    @Nonnull
    public static AccessToken workingTreeChangeStarted(@Nonnull Project project) {
        return workingTreeChangeStarted(project, null);
    }

    @Nonnull
    public static AccessToken workingTreeChangeStarted(@Nonnull Project project, @Nullable String activityName) {
        BackgroundTaskUtil.syncPublisher(BatchFileChangeListener.class).batchChangeStarted(project, activityName);
        return new AccessToken() {
            @Override
            public void finish() {
                BackgroundTaskUtil.syncPublisher(BatchFileChangeListener.class).batchChangeCompleted(project);
            }
        };
    }

    /**
     * @deprecated Call {@link AccessToken#finish()} directly from the AccessToken received by {@link #workingTreeChangeStarted(Project)}
     */
    @Deprecated
    public static void workingTreeChangeFinished(@Nonnull Project project, @Nonnull AccessToken token) {
        token.finish();
    }

    public static final Comparator<Repository> REPOSITORY_COMPARATOR = Comparator.comparing(Repository::getPresentableUrl);

    public static void assertFileExists(File file, String message) throws IllegalStateException {
        if (!file.exists()) {
            throw new IllegalStateException(message);
        }
    }

    /**
     * Loads the file content.
     * Tries 3 times, then a {@link RepoStateException} is thrown.
     * Content is then trimmed and line separators get converted.
     *
     * @param file File to read.
     * @return file content.
     */
    @Nonnull
    public static String tryLoadFile(@Nonnull File file) throws RepoStateException {
        return tryLoadFile(file, null);
    }

    @Nonnull
    public static String tryLoadFile(@Nonnull File file, @Nullable Charset encoding) throws RepoStateException {
        return tryOrThrow(() -> StringUtil.convertLineSeparators(RawFileLoader.getInstance()
            .loadFileText(file, encoding == null ? StandardCharsets.UTF_8 : encoding)).trim(), file);
    }

    @Nullable
    @Contract("_ , !null -> !null")
    public static String tryLoadFileOrReturn(@Nonnull File file, @Nullable String defaultValue) {
        return tryLoadFileOrReturn(file, defaultValue, null);
    }

    @Nullable
    @Contract("_ , !null, _ -> !null")
    public static String tryLoadFileOrReturn(@Nonnull File file, @Nullable String defaultValue, @Nullable Charset encoding) {
        try {
            return tryLoadFile(file, encoding);
        }
        catch (RepoStateException e) {
            LOG.error(e);
            return defaultValue;
        }
    }

    /**
     * Tries to execute the given action.
     * If an IOException happens, tries again up to 3 times, and then throws a {@link RepoStateException}.
     * If an other exception happens, rethrows it as a {@link RepoStateException}.
     * In the case of success returns the result of the task execution.
     */
    private static <T> T tryOrThrow(Callable<T> actionToTry, File fileToLoad) throws RepoStateException {
        IOException cause = null;
        for (int i = 0; i < IO_RETRIES; i++) {
            try {
                return actionToTry.call();
            }
            catch (IOException e) {
                LOG.info("IOException while loading " + fileToLoad, e);
                cause = e;
            }
            catch (Exception e) {    // this shouldn't happen since only IOExceptions are thrown in clients.
                throw new RepoStateException("Couldn't load file " + fileToLoad, e);
            }
        }
        throw new RepoStateException("Couldn't load file " + fileToLoad, cause);
    }

    public static void visitVcsDirVfs(@Nonnull VirtualFile vcsDir, @Nonnull Collection<String> subDirs) {
        vcsDir.getChildren();
        for (String subdir : subDirs) {
            VirtualFile dir = vcsDir.findFileByRelativePath(subdir);
            // process recursively, because we need to visit all branches under refs/heads and refs/remotes
            ensureAllChildrenInVfs(dir);
        }
    }

    public static void ensureAllChildrenInVfs(@Nullable VirtualFile dir) {
        if (dir != null) {
            //noinspection unchecked
            VirtualFileUtil.processFilesRecursively(dir, file -> true);
        }
    }

    public static void addMappingIfSubRoot(@Nonnull Project project, @Nonnull String newRepositoryPath, @Nonnull String vcsName) {
        if (project.getBasePath() != null && FileUtil.isAncestor(project.getBasePath(), newRepositoryPath, true)) {
            ProjectLevelVcsManager manager = ProjectLevelVcsManager.getInstance(project);
            manager.setDirectoryMappings(VcsUtil.addMapping(manager.getDirectoryMappings(), newRepositoryPath, vcsName));
        }
    }

    @Nullable
    public static <T extends Repository> T guessRepositoryForFile(
        @Nonnull Project project,
        @Nonnull RepositoryManager<T> manager,
        @Nullable VirtualFile file,
        @Nullable String defaultRootPathValue
    ) {
        T repository = manager.getRepositoryForRoot(guessVcsRoot(project, file));
        return repository != null ? repository : manager.getRepositoryForRoot(guessRootForVcs(
            project,
            manager.getVcs(),
            defaultRootPathValue
        ));
    }

    @Nullable
    public static <T extends Repository> T guessCurrentRepositoryQuick(
        @Nonnull Project project,
        @Nonnull AbstractRepositoryManager<T> manager,
        @Nullable String defaultRootPathValue
    ) {
        T repository = manager.getRepositoryForRootQuick(guessVcsRoot(project, getSelectedFile(project)));
        return repository != null
            ? repository
            : manager.getRepositoryForRootQuick(guessRootForVcs(project, manager.getVcs(), defaultRootPathValue));
    }

    @Nullable
    private static VirtualFile guessRootForVcs(@Nonnull Project project, @Nullable AbstractVcs vcs, @Nullable String defaultRootPathValue) {
        if (project.isDisposed()) {
            return null;
        }
        LOG.debug("Guessing vcs root...");
        ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);
        if (vcs == null) {
            LOG.debug("Vcs not found.");
            return null;
        }
        String vcsName = vcs.getDisplayName();
        VirtualFile[] vcsRoots = vcsManager.getRootsUnderVcs(vcs);
        if (vcsRoots.length == 0) {
            LOG.debug("No " + vcsName + " roots in the project.");
            return null;
        }

        if (vcsRoots.length == 1) {
            VirtualFile onlyRoot = vcsRoots[0];
            LOG.debug("Only one " + vcsName + " root in the project, returning: " + onlyRoot);
            return onlyRoot;
        }

        // get remembered last visited repository root
        if (defaultRootPathValue != null) {
            VirtualFile recentRoot = VcsUtil.getVirtualFile(defaultRootPathValue);
            if (recentRoot != null) {
                LOG.debug("Returning the recent root: " + recentRoot);
                return recentRoot;
            }
        }

        // otherwise return the root of the project dir or the root containing the project dir, if there is such
        VirtualFile projectBaseDir = project.getBaseDir();
        if (projectBaseDir == null) {
            VirtualFile firstRoot = vcsRoots[0];
            LOG.debug("Project base dir is null, returning the first root: " + firstRoot);
            return firstRoot;
        }
        VirtualFile rootCandidate;
        for (VirtualFile root : vcsRoots) {
            if (root.equals(projectBaseDir) || VirtualFileUtil.isAncestor(root, projectBaseDir, true)) {
                LOG.debug("The best candidate: " + root);
                return root;
            }
        }
        rootCandidate = vcsRoots[0];
        LOG.debug("Returning the best candidate: " + rootCandidate);
        return rootCandidate;
    }

    public static class Updater implements Consumer<Object> {
        private final Repository myRepository;

        public Updater(Repository repository) {
            myRepository = repository;
        }

        @Override
        public void accept(Object dummy) {
            if (!Disposer.isDisposed(myRepository)) {
                myRepository.update();
            }
        }
    }

    public static <T extends Repository> List<T> sortRepositories(@Nonnull Collection<T> repositories) {
        return repositories.stream().filter(t -> t.getRoot().isValid()).sorted(REPOSITORY_COMPARATOR).toList();
    }

    @Nullable
    private static VirtualFile getVcsRootForLibraryFile(@Nonnull Project project, @Nonnull VirtualFile file) {
        ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);
        // for a file inside .jar/.zip consider the .jar/.zip file itself
        VirtualFile root = vcsManager.getVcsRootFor(ArchiveVfsUtil.getVirtualFileForJar(file));
        if (root != null) {
            LOGGER.debug("Found root for zip/jar file: " + root);
            return root;
        }

        // for other libs which don't have jars inside the project dir (such as JDK) take the owner module of the lib
        List<OrderEntry> entries = ProjectRootManager.getInstance(project).getFileIndex().getOrderEntriesForFile(file);
        Set<VirtualFile> libraryRoots = new HashSet<>();
        for (OrderEntry entry : entries) {
            if (entry instanceof LibraryOrderEntry || entry instanceof ModuleExtensionWithSdkOrderEntry) {
                VirtualFile moduleRoot = vcsManager.getVcsRootFor(entry.getOwnerModule().getModuleDir());
                if (moduleRoot != null) {
                    libraryRoots.add(moduleRoot);
                }
            }
        }

        if (libraryRoots.size() == 0) {
            LOGGER.debug("No library roots");
            return null;
        }

        // if the lib is used in several modules, take the top module
        // (for modules of the same level we can't guess anything => take the first one)
        Iterator<VirtualFile> libIterator = libraryRoots.iterator();
        VirtualFile topLibraryRoot = libIterator.next();
        while (libIterator.hasNext()) {
            VirtualFile libRoot = libIterator.next();
            if (VirtualFileUtil.isAncestor(libRoot, topLibraryRoot, true)) {
                topLibraryRoot = libRoot;
            }
        }
        LOGGER.debug("Several library roots, returning " + topLibraryRoot);
        return topLibraryRoot;
    }

    @Nullable
    public static VirtualFile guessVcsRoot(@Nonnull Project project, @Nullable VirtualFile file) {
        VirtualFile root = null;
        if (file != null) {
            root = ProjectLevelVcsManager.getInstance(project).getVcsRootFor(file);
            if (root == null) {
                LOGGER.debug("Cannot get root by file. Trying with get by library: " + file);
                root = getVcsRootForLibraryFile(project, file);
            }
        }
        return root;
    }

    @Nonnull
    public static <R extends Repository> Map<R, List<VcsFullCommitDetails>> groupCommitsByRoots(
        @Nonnull RepositoryManager<R> repoManager,
        @Nonnull List<? extends VcsFullCommitDetails> commits
    ) {
        Map<R, List<VcsFullCommitDetails>> groupedCommits = new HashMap<>();
        for (VcsFullCommitDetails commit : commits) {
            R repository = repoManager.getRepositoryForRoot(commit.getRoot());
            if (repository == null) {
                LOGGER.info("No repository found for commit " + commit);
                continue;
            }
            List<VcsFullCommitDetails> commitsInRoot = groupedCommits.get(repository);
            if (commitsInRoot == null) {
                commitsInRoot = new ArrayList<>();
                groupedCommits.put(repository, commitsInRoot);
            }
            commitsInRoot.add(commit);
        }
        return groupedCommits;
    }

    @Nullable
    public static PushSupport getPushSupport(@Nonnull AbstractVcs vcs) {
        return ContainerUtil.find(
            vcs.getProject().getExtensionList(PushSupport.class),
            support -> support.getVcs().equals(vcs)
        );
    }

    @Nonnull
    public static String joinShortNames(@Nonnull Collection<? extends Repository> repositories) {
        return joinShortNames(repositories, -1);
    }

    @Nonnull
    public static String joinShortNames(@Nonnull Collection<? extends Repository> repositories, int limit) {
        return joinWithAnd(ContainerUtil.map(repositories, repository -> getShortRepositoryName(repository)), limit);
    }

    @Nonnull
    public static String joinWithAnd(@Nonnull List<String> strings, int limit) {
        int size = strings.size();
        if (size == 0) {
            return "";
        }
        if (size == 1) {
            return strings.get(0);
        }
        if (size == 2) {
            return strings.get(0) + " and " + strings.get(1);
        }

        boolean isLimited = limit >= 2 && limit < size;
        int listCount = isLimited ? limit - 1 : size - 1;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < listCount; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(strings.get(i));
        }

        if (isLimited) {
            sb.append(" and ").append(size - limit + 1).append(" others");
        }
        else {
            sb.append(" and ").append(strings.get(size - 1));
        }
        return sb.toString();
    }
}
