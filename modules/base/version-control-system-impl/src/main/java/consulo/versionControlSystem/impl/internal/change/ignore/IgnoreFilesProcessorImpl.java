// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.impl.internal.change.ignore;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.ProjectCoreUtil;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsApplicationSettings;
import consulo.versionControlSystem.change.ChangeListListener;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.IgnoredFileContentProvider;
import consulo.versionControlSystem.change.IgnoredFileDescriptor;
import consulo.versionControlSystem.change.IgnoredFileProvider;
import consulo.versionControlSystem.change.VcsIgnoreManager;
import consulo.versionControlSystem.change.ignore.IgnoreFileUtil;
import consulo.versionControlSystem.impl.internal.FilesProcessorWithNotificationImpl;
import consulo.versionControlSystem.impl.internal.util.RecursiveFilePathSet;
import consulo.versionControlSystem.internal.ChangeListManagerEx;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.util.VcsImplUtil;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.AsyncVfsEventsListener;
import consulo.virtualFileSystem.event.AsyncVfsEventsPostProcessor;
import consulo.virtualFileSystem.event.VFileCopyEvent;
import consulo.virtualFileSystem.event.VFileCreateEvent;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.virtualFileSystem.event.VFileMoveEvent;
import consulo.virtualFileSystem.event.VFilePropertyChangeEvent;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import static consulo.versionControlSystem.impl.internal.change.ignore.IgnoreConfigurationProperty.ASKED_MANAGE_IGNORE_FILES_PROPERTY;
import static consulo.versionControlSystem.impl.internal.change.ignore.IgnoreConfigurationProperty.MANAGE_IGNORE_FILES_PROPERTY;

/**
 * Automatically generate or update .ignore files basing on {@link IgnoredFileProvider} extension point.
 */
public class IgnoreFilesProcessorImpl extends FilesProcessorWithNotificationImpl implements AsyncVfsEventsListener, ChangeListListener {
    private static final Logger LOG = Logger.getInstance(IgnoreFilesProcessorImpl.class);

    private final ReentrantReadWriteLock UNPROCESSED_FILES_LOCK = new ReentrantReadWriteLock();

    private RecursiveFilePathSet myUnprocessedPathsInConfigDir = newPathSet();

    private RecursiveFilePathSet myUnprocessedPathsOutsideConfigDir = newPathSet();

    private final AbstractVcs myVcs;

    private final VcsIgnoreManager myVcsIgnoreManager;

    public IgnoreFilesProcessorImpl(Project project, Disposable parentDisposable, AbstractVcs vcs) {
        super(project, parentDisposable);
        myVcs = vcs;
        myVcsIgnoreManager = VcsIgnoreManager.getInstance(project);
    }

    public void install() {
        myProject.getMessageBus().connect(this).subscribe(ChangeListListener.class, this);
        AsyncVfsEventsPostProcessor.getInstance().addListener(this, this);
    }

    @Override
    public void unchangedFileStatusChanged() {
        if (myProject.getApplication().isUnitTestMode()) {
            return;
        }
        processCollectedPaths(!ChangeListManager.getInstance(myProject).isInUpdate());
    }

    /**
     * Processes every path that {@link #collectPotentiallyIgnoredPaths} collected before.
     */
    public void processCollectedPaths(boolean upToDate) {
        if (!upToDate) {
            return;
        }

        RecursiveFilePathSet pathsInConfigDir;
        RecursiveFilePathSet pathsOutsideConfigDir;
        UNPROCESSED_FILES_LOCK.writeLock().lock();
        try {
            pathsInConfigDir = myUnprocessedPathsInConfigDir;
            pathsOutsideConfigDir = myUnprocessedPathsOutsideConfigDir;
            myUnprocessedPathsInConfigDir = newPathSet();
            myUnprocessedPathsOutsideConfigDir = newPathSet();
        }
        finally {
            UNPROCESSED_FILES_LOCK.writeLock().unlock();
        }

        List<FilePath> unversionedPaths = null;

        if (!pathsInConfigDir.isEmpty()) {
            unversionedPaths = getUnversionedFilesPaths();
            List<VirtualFile> unversionedPathsInConfigDir = getFilesUnder(unversionedPaths, pathsInConfigDir);
            myProject.getUIAccess().give(() -> writeIgnores(myProject, unversionedPathsInConfigDir));
        }

        if (needProcessIgnoredFiles() && !pathsOutsideConfigDir.isEmpty()) {
            if (unversionedPaths == null) {
                unversionedPaths = getUnversionedFilesPaths();
            }
            processFiles(getFilesUnder(unversionedPaths, pathsOutsideConfigDir));
        }
    }

    private List<FilePath> getUnversionedFilesPaths() {
        List<FilePath> result = new ArrayList<>();
        for (VirtualFile file : ChangeListManagerEx.getInstanceEx(myProject).getUnversionedFiles()) {
            result.add(VcsUtil.getFilePath(file));
        }
        return result;
    }

    @Override
    public void filesChanged(List<? extends VFileEvent> events) {
        if (myProject.getApplication().isUnitTestMode()) {
            return;
        }
        collectPotentiallyIgnoredPaths(events);
    }

    /**
     * Keeps the path of every potentially ignored file that {@code events} report.
     * {@link #processCollectedPaths} processes the kept paths later.
     */
    public void collectPotentiallyIgnoredPaths(List<? extends VFileEvent> events) {
        List<FilePath> potentiallyIgnoredPaths = new ArrayList<>();
        for (VFileEvent event : events) {
            FilePath path = getAffectedFilePath(event);
            if (path != null && myVcsIgnoreManager.isPotentiallyIgnoredFile(path)) {
                potentiallyIgnoredPaths.add(path);
            }
        }

        if (potentiallyIgnoredPaths.isEmpty()) {
            return;
        }

        LOG.debug("Got potentially ignored files from VFS events " + potentiallyIgnoredPaths);

        VirtualFile storeDir = findStoreDir(myProject);
        FilePath configDirPath = storeDir != null ? VcsUtil.getFilePath(storeDir) : null;

        UNPROCESSED_FILES_LOCK.writeLock().lock();
        try {
            for (FilePath path : potentiallyIgnoredPaths) {
                if (configDirPath != null && path.isUnder(configDirPath, true)) {
                    myUnprocessedPathsInConfigDir.add(path);
                }
                else {
                    myUnprocessedPathsOutsideConfigDir.add(path);
                }
            }
        }
        finally {
            UNPROCESSED_FILES_LOCK.writeLock().unlock();
        }
    }

    @Override
    public void doActionOnChosenFiles(Collection<VirtualFile> files) {
        myProject.getUIAccess().give(() -> writeIgnores(myProject, files));
    }

    @Override
    public void dispose() {
        super.dispose();
        UNPROCESSED_FILES_LOCK.writeLock().lock();
        try {
            myUnprocessedPathsInConfigDir.clear();
            myUnprocessedPathsOutsideConfigDir.clear();
        }
        finally {
            UNPROCESSED_FILES_LOCK.writeLock().unlock();
        }
    }

    private void writeIgnores(Project project, Collection<VirtualFile> potentiallyIgnoredFiles) {
        if (project.isDisposed()) {
            return;
        }
        if (potentiallyIgnoredFiles.isEmpty()) {
            return;
        }

        LOG.debug("Try to write potential ignored files " + potentiallyIgnoredFiles);

        Map<VirtualFile, List<IgnoreGroupContent>> ignoreFileToContent = new HashMap<>();

        Map<IgnoredFileProvider, Set<IgnoredFileDescriptor>> providerToDescriptorMap = new LinkedHashMap<>();
        for (IgnoredFileProvider provider : project.getExtensionPoint(IgnoredFileProvider.class).getExtensionList()) {
            providerToDescriptorMap.put(provider, provider.getIgnoredFiles());
        }

        for (VirtualFile potentiallyIgnoredFile : potentiallyIgnoredFiles) {
            AbstractVcs vcs = VcsUtil.getVcsFor(project, potentiallyIgnoredFile);
            if (vcs == null) {
                continue;
            }
            IgnoredFileContentProvider ignoredContentProvider = VcsImplUtil.findIgnoredFileContentProvider(vcs);
            if (ignoredContentProvider == null) {
                continue;
            }
            VirtualFile ignoreFile = findOrCreateIgnoreFileByFile(project, ignoredContentProvider, potentiallyIgnoredFile);
            if (ignoreFile == null) {
                continue;
            }
            for (Map.Entry<IgnoredFileProvider, Set<IgnoredFileDescriptor>> entry : providerToDescriptorMap.entrySet()) {
                for (IgnoredFileDescriptor ignoredFileDescriptor : entry.getValue()) {
                    if (!ignoredFileDescriptor.matchesFile(VcsUtil.getFilePath(potentiallyIgnoredFile))) {
                        continue;
                    }
                    List<IgnoreGroupContent> ignoreFileContent = ignoreFileToContent.computeIfAbsent(ignoreFile, it -> new ArrayList<>());
                    String groupDescription = ignoredContentProvider.buildIgnoreGroupDescription(entry.getKey());
                    getOrInitialize(ignoreFileContent, groupDescription).ignoredDescriptors().add(ignoredFileDescriptor);
                }
            }
        }

        for (Map.Entry<VirtualFile, List<IgnoreGroupContent>> entry : ignoreFileToContent.entrySet()) {
            VirtualFile ignoreFile = entry.getKey();
            for (IgnoreGroupContent groupContent : entry.getValue()) {
                Set<IgnoredFileDescriptor> ignoredDescriptors = groupContent.ignoredDescriptors();
                LOG.debug("Write to ignore file " + ignoreFile + " ignores: " + ignoredDescriptors);
                IgnoreFileUtil.addNewElementsToIgnoreBlock(
                    project,
                    ignoreFile,
                    groupContent.group(),
                    ignoredDescriptors.toArray(new IgnoredFileDescriptor[0])
                );
            }
        }
    }

    private static IgnoreGroupContent getOrInitialize(List<IgnoreGroupContent> list, String group) {
        for (IgnoreGroupContent content : list) {
            if (content.group().equals(group)) {
                return content;
            }
        }
        IgnoreGroupContent content = new IgnoreGroupContent(group, new LinkedHashSet<>());
        list.add(content);
        return content;
    }

    private record IgnoreGroupContent(String group, Set<IgnoredFileDescriptor> ignoredDescriptors) {
    }

    private @Nullable VirtualFile findOrCreateIgnoreFileByFile(
        Project project,
        IgnoredFileContentProvider ignoredContentProvider,
        VirtualFile file
    ) {
        VirtualFile storeDir = findStoreDir(project);
        VirtualFile ignoreFileRoot;
        if (storeDir != null
            && ignoredContentProvider.supportIgnoreFileNotInVcsRoot()
            && VirtualFileUtil.isAncestor(storeDir, file, true)) {
            if (ignoredContentProvider.canCreateIgnoreFileInStateStoreDir()) {
                ignoreFileRoot = storeDir;
            }
            else {
                return null;
            }
        }
        else {
            ignoreFileRoot = VcsUtil.getVcsRootFor(project, file);
            if (ignoreFileRoot == null) {
                return null;
            }
        }

        VirtualFile ignoreFile = ignoreFileRoot.findChild(ignoredContentProvider.getFileName());
        if (ignoreFile != null) {
            return ignoreFile;
        }

        VirtualFile root = ignoreFileRoot;
        Supplier<@Nullable VirtualFile> create = () -> {
            try {
                return root.createChildData(this, ignoredContentProvider.getFileName());
            }
            catch (IOException e) {
                LOG.warn(e);
                return null;
            }
        };
        return project.getApplication().runWriteAction(create);
    }

    private static @Nullable VirtualFile findStoreDir(Project project) {
        return ProjectCoreUtil.getDirectoryStoreFile(project);
    }

    private static List<VirtualFile> getFilesUnder(Collection<FilePath> paths, RecursiveFilePathSet parents) {
        List<VirtualFile> result = new ArrayList<>();
        for (FilePath path : paths) {
            if (parents.hasAncestor(path)) {
                VirtualFile file = path.getVirtualFile();
                if (file != null) {
                    result.add(file);
                }
            }
        }
        return result;
    }

    @Override
    protected void rememberForAllProjects() {
        VcsApplicationSettings applicationSettings = VcsApplicationSettings.getInstance();
        applicationSettings.MANAGE_IGNORE_FILES = true;
    }

    @Override
    protected String getAskedBeforeProperty() {
        return ASKED_MANAGE_IGNORE_FILES_PROPERTY;
    }

    @Override
    protected String getDoForCurrentProjectProperty() {
        return MANAGE_IGNORE_FILES_PROPERTY;
    }

    @Override
    protected LocalizeValue getShowActionText() {
        return VcsLocalize.ignoredFileManageView();
    }

    @Override
    protected LocalizeValue getForCurrentProjectActionText() {
        return VcsLocalize.ignoredFileManageThisProject();
    }

    @Override
    protected LocalizeValue getForAllProjectsActionText() {
        return VcsLocalize.ignoredFileManageAllProject();
    }

    @Override
    protected LocalizeValue getMuteActionText() {
        return VcsLocalize.ignoredFileManageNotmanage();
    }

    @Override
    protected LocalizeValue getViewFilesDialogTitle() {
        return VcsLocalize.ignoredFileManageViewDialogTitle();
    }

    @Override
    protected LocalizeValue getViewFilesDialogOkActionName() {
        return VcsLocalize.ignoredFileManageViewDialogIgnoreAction();
    }

    @Override
    protected LocalizeValue notificationTitle() {
        return LocalizeValue.empty();
    }

    @Override
    protected LocalizeValue notificationMessage() {
        IgnoredFileContentProvider provider = VcsImplUtil.findIgnoredFileContentProvider(myVcs);
        return VcsLocalize.ignoredFileManageWithFilesMessage(
            myProject.getApplication().getName(),
            provider != null ? LocalizeValue.of(provider.getFileName()) : VcsLocalize.changesIgnoreFile()
        );
    }

    @Override
    protected boolean needDoForCurrentProject() {
        VcsApplicationSettings appSettings = VcsApplicationSettings.getInstance();
        return !appSettings.DISABLE_MANAGE_IGNORE_FILES && (appSettings.MANAGE_IGNORE_FILES || super.needDoForCurrentProject());
    }

    private static @Nullable FilePath getAffectedFilePath(VFileEvent event) {
        if (!(event.getFileSystem() instanceof LocalFileSystem)) {
            return null;
        }

        if (event instanceof VFileCreateEvent createEvent) {
            return VcsUtil.getFilePath(event.getPath(), createEvent.isDirectory());
        }
        if (event instanceof VFileMoveEvent
            || event instanceof VFileCopyEvent
            || event instanceof VFilePropertyChangeEvent propertyChangeEvent && propertyChangeEvent.isRename()) {
            VirtualFile file = event.getFile();
            return file != null ? VcsUtil.getFilePath(event.getPath(), file.isDirectory()) : null;
        }
        return null;
    }

    private static boolean needProcessIgnoredFiles() {
        return VcsApplicationSettings.getInstance().GENERATE_IGNORE_FILES_IN_VCS_ROOTS;
    }

    private static RecursiveFilePathSet newPathSet() {
        return new RecursiveFilePathSet(Platform.current().fs().isCaseSensitive());
    }
}
