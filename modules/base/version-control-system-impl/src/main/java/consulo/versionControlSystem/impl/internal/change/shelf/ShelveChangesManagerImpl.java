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

package consulo.versionControlSystem.impl.internal.change.shelf;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.component.messagebus.MessageBus;
import consulo.component.persist.RoamingType;
import consulo.component.persist.scheme.BaseSchemeProcessor;
import consulo.component.persist.scheme.SchemeManager;
import consulo.component.persist.scheme.SchemeManagerFactory;
import consulo.component.util.text.UniqueNameGenerator;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.macro.ProjectPathMacroManager;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.SmartList;
import consulo.util.io.FilePermissionCopier;
import consulo.util.io.FileUtil;
import consulo.util.io.PathUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.Ref;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.JDOMExternalizable;
import consulo.util.xml.serializer.JDOMExternalizerUtil;
import consulo.util.xml.serializer.WriteExternalException;
import consulo.versionControlSystem.*;
import consulo.versionControlSystem.change.*;
import consulo.versionControlSystem.change.patch.*;
import consulo.versionControlSystem.change.shelf.*;
import consulo.versionControlSystem.impl.internal.change.ChangeListUtil;
import consulo.versionControlSystem.impl.internal.change.patch.ApplyPatchDefaultExecutor;
import consulo.versionControlSystem.impl.internal.change.ui.RollbackWorker;
import consulo.versionControlSystem.impl.internal.change.ui.awt.RollbackChangesDialog;
import consulo.versionControlSystem.impl.internal.patch.*;
import consulo.versionControlSystem.impl.internal.patch.apply.ApplyFilePatchBase;
import consulo.versionControlSystem.impl.internal.patch.apply.ApplyPatchStatus;
import consulo.versionControlSystem.impl.internal.patch.apply.CustomBinaryPatchApplier;
import consulo.versionControlSystem.impl.internal.patch.apply.PatchApplier;
import consulo.versionControlSystem.impl.internal.ui.awt.PatchNameChecker;
import consulo.versionControlSystem.impl.internal.util.FilesProgress;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.RawFileLoader;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;
import org.jdom.Parent;
import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Singleton
@ServiceImpl
public class ShelveChangesManagerImpl implements ShelveChangesManager, JDOMExternalizable {
    private static final Logger LOG = Logger.getInstance(ShelveChangesManagerImpl.class);

    private static final String ELEMENT_CHANGELIST = "changelist";
    private static final String ELEMENT_RECYCLED_CHANGELIST = "recycled_changelist";
    private static final String DEFAULT_PATCH_NAME = "shelved";
    private static final String REMOVE_FILES_FROM_SHELF_STRATEGY = "remove_strategy";

    @Nonnull
    private final SchemeManager<ShelvedChangeList, ShelvedChangeListImpl> mySchemeManager;
    private ScheduledFuture<?> myCleaningFuture;
    private boolean myRemoveFilesFromShelf;

    public static ShelveChangesManagerImpl getInstance(@Nonnull Project project) {
        return (ShelveChangesManagerImpl) ShelveChangesManager.getInstance(project);
    }

    private static final String SHELVE_MANAGER_DIR_PATH = "shelf";
    private final MessageBus myBus;

    @NonNls
    private static final String ATTRIBUTE_SHOW_RECYCLED = "show_recycled";
    @Nonnull
    private final CompoundShelfFileProcessor myFileProcessor;

    private boolean myShowRecycled;
    private final Project myProject;

    @Inject
    public ShelveChangesManagerImpl(Project project,
                                    ProjectPathMacroManager projectPathMacroManager,
                                    SchemeManagerFactory schemeManagerFactory,
                                    ChangeListManager changeListManager,
                                    ApplicationConcurrency applicationConcurrency) {
        myProject = project;
        myBus = project.getMessageBus();
        mySchemeManager = schemeManagerFactory.createSchemeManager(
            SHELVE_MANAGER_DIR_PATH,
            new BaseSchemeProcessor<ShelvedChangeList, ShelvedChangeListImpl>() {
                @Nullable
                @Override
                public ShelvedChangeListImpl readScheme(@Nonnull Element element, boolean duringLoad) throws InvalidDataException {
                    return readOneShelvedChangeList(element);
                }

                @Nonnull
                @Override
                public Parent writeScheme(@Nonnull ShelvedChangeListImpl scheme) throws WriteExternalException {
                    Element child = new Element(ELEMENT_CHANGELIST);
                    scheme.writeExternal(child);
                    projectPathMacroManager.collapsePaths(child);
                    return child;
                }

                @Nonnull
                @Override
                public String getName(@Nonnull ShelvedChangeList immutableElement) {
                    return immutableElement.getName();
                }
            },
            RoamingType.DEFAULT
        );

        myCleaningFuture = applicationConcurrency.getScheduledExecutorService().scheduleWithFixedDelay(this::cleanSystemUnshelvedOlderOneWeek, 1, 1, TimeUnit.DAYS);
        Disposer.register(project, this::stopCleanScheduler);

        File shelfDirectory = mySchemeManager.getRootDirectory();
        myFileProcessor = new CompoundShelfFileProcessor(shelfDirectory);
        // do not try to ignore when new project created,
        // because it may lead to predefined ignore creation conflict; see ConvertExcludedToIgnoredTest etc
        if (shelfDirectory.exists()) {
            changeListManager.addDirectoryToIgnoreImplicitly(shelfDirectory.getAbsolutePath());
        }
    }

    private void stopCleanScheduler() {
        if (myCleaningFuture != null) {
            myCleaningFuture.cancel(false);
            myCleaningFuture = null;
        }
    }

    public void projectOpened() {
        try {
            mySchemeManager.loadSchemes();
            //workaround for ignoring not valid patches, because readScheme doesn't support nullable value as it should be
            filterNonValidShelvedChangeLists();
            cleanSystemUnshelvedOlderOneWeek();
        }
        catch (Exception e) {
            LOG.error("Couldn't read shelf information", e);
        }
    }

    private void filterNonValidShelvedChangeLists() {
        List<ShelvedChangeList> allSchemes = new ArrayList<>(mySchemeManager.getAllSchemes());
        ContainerUtil.process(allSchemes, shelvedChangeList -> {
            if (!shelvedChangeList.isValid()) {
                mySchemeManager.removeScheme(shelvedChangeList);
            }
            return true;
        });
    }

    @Nonnull
    public File getShelfResourcesDirectory() {
        return myFileProcessor.getBaseDir();
    }

    @Nonnull
    private ShelvedChangeListImpl readOneShelvedChangeList(@Nonnull Element element) throws InvalidDataException {
        ShelvedChangeListImpl data = new ShelvedChangeListImpl();
        ProjectPathMacroManager.getInstance(myProject).expandPaths(element);
        data.readExternal(element);
        return data;
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        String showRecycled = element.getAttributeValue(ATTRIBUTE_SHOW_RECYCLED);
        myShowRecycled = showRecycled == null || Boolean.parseBoolean(showRecycled);
        String removeFilesStrategy = JDOMExternalizerUtil.readField(element, REMOVE_FILES_FROM_SHELF_STRATEGY);
        myRemoveFilesFromShelf = removeFilesStrategy != null && Boolean.parseBoolean(removeFilesStrategy);
        migrateOldShelfInfo(element, true);
        migrateOldShelfInfo(element, false);
    }

    //load old shelf information from workspace.xml without moving .patch and binary files into new directory
    private void migrateOldShelfInfo(@Nonnull Element element, boolean recycled) throws InvalidDataException {
        for (Element changeSetElement : element.getChildren(recycled ? ELEMENT_RECYCLED_CHANGELIST : ELEMENT_CHANGELIST)) {
            ShelvedChangeListImpl list = readOneShelvedChangeList(changeSetElement);
            if (!list.isValid()) {
                break;
            }
            File uniqueDir = generateUniqueSchemePatchDir(list.DESCRIPTION, false);
            list.setName(uniqueDir.getName());
            list.setRecycled(recycled);
            mySchemeManager.addNewScheme(list, false);
        }
    }

    /**
     * Should be called only once: when Settings Repository plugin runs first time
     *
     * @return collection of non-migrated or not deleted files to show a error somewhere outside
     */
    @Nonnull
    public Collection<String> checkAndMigrateOldPatchResourcesToNewSchemeStorage() {
        Collection<String> nonMigratedPaths = new ArrayList<>();
        for (ShelvedChangeList list : mySchemeManager.getAllSchemes()) {
            File patchDir = new File(myFileProcessor.getBaseDir(), list.getName());
            nonMigratedPaths.addAll(migrateIfNeededToSchemeDir((ShelvedChangeListImpl) list, patchDir));
        }
        return nonMigratedPaths;
    }

    @Nonnull
    private static Collection<String> migrateIfNeededToSchemeDir(@Nonnull ShelvedChangeListImpl list, @Nonnull File targetDirectory) {
        // it should be enough for migration to check if resource directory exists. If any bugs appeared add isAncestor checks for each path
        if (targetDirectory.exists() || !targetDirectory.mkdirs()) {
            return List.of();
        }
        Collection<String> nonMigratedPaths = new ArrayList<>();
        //try to move .patch file
        File patchFile = new File(list.PATH);
        if (patchFile.exists()) {
            File newPatchFile = getPatchFileInConfigDir(targetDirectory);
            try {
                FileUtil.copy(patchFile, newPatchFile, FilePermissionCopier.BY_NIO2);
                list.PATH = FileUtil.toSystemIndependentName(newPatchFile.getPath());
                FileUtil.delete(patchFile);
            }
            catch (IOException e) {
                nonMigratedPaths.add(list.PATH);
            }
        }

        for (ShelvedBinaryFile file : list.getBinaryFiles()) {
            ShelvedBinaryFileImpl fileImpl = (ShelvedBinaryFileImpl) file;

            if (fileImpl.SHELVED_PATH != null) {
                File shelvedFile = new File(fileImpl.SHELVED_PATH);
                if (!StringUtil.isEmptyOrSpaces(fileImpl.AFTER_PATH) && shelvedFile.exists()) {
                    File newShelvedFile = new File(targetDirectory, PathUtil.getFileName(fileImpl.AFTER_PATH));
                    try {
                        FileUtil.copy(shelvedFile, newShelvedFile, FilePermissionCopier.BY_NIO2);
                        fileImpl.SHELVED_PATH = FileUtil.toSystemIndependentName(newShelvedFile.getPath());
                        FileUtil.delete(shelvedFile);
                    }
                    catch (IOException e) {
                        nonMigratedPaths.add(shelvedFile.getPath());
                    }
                }
            }
        }
        return nonMigratedPaths;
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        element.setAttribute(ATTRIBUTE_SHOW_RECYCLED, Boolean.toString(myShowRecycled));
        JDOMExternalizerUtil.writeField(element, REMOVE_FILES_FROM_SHELF_STRATEGY, Boolean.toString(isRemoveFilesFromShelf()));
    }

    @Override
    @Nonnull
    public List<ShelvedChangeList> getShelvedChangeLists() {
        return getRecycled(false);
    }

    @Nonnull
    private List<ShelvedChangeList> getRecycled(boolean recycled) {
        return ContainerUtil.newArrayList(ContainerUtil.filter(mySchemeManager.getAllSchemes(), list -> recycled == list.isRecycled()));
    }

    @Override
    public ShelvedChangeListImpl shelveChanges(Collection<Change> changes, String commitMessage, boolean rollback, boolean markToBeDeleted) throws IOException, VcsException {
        ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
        if (progressIndicator != null) {
            progressIndicator.setText(VcsBundle.message("shelve.changes.progress.title"));
        }
        File schemePatchDir = generateUniqueSchemePatchDir(commitMessage, true);
        List<Change> textChanges = new ArrayList<>();
        List<ShelvedBinaryFile> binaryFiles = new ArrayList<>();
        for (Change change : changes) {
            if (ChangesUtil.getFilePath(change).isDirectory()) {
                continue;
            }
            if (change.getBeforeRevision() instanceof BinaryContentRevision || change.getAfterRevision() instanceof BinaryContentRevision) {
                binaryFiles.add(shelveBinaryFile(schemePatchDir, change));
            }
            else {
                textChanges.add(change);
            }
        }

        ShelvedChangeListImpl changeList;
        try {
            File patchPath = getPatchFileInConfigDir(schemePatchDir);
            ProgressManager.checkCanceled();
            List<FilePatch> patches =
                IdeaTextPatchBuilder.buildPatch(myProject, textChanges, myProject.getBaseDir().getPresentableUrl(), false);
            ProgressManager.checkCanceled();

            CommitContext commitContext = new CommitContext();
            baseRevisionsOfDvcsIntoContext(textChanges, commitContext);
            myFileProcessor.savePathFile(
                (writer, commitContext1) ->
                    UnifiedDiffWriter.write(myProject, patches, writer, "\n", commitContext1), patchPath, commitContext
            );

            changeList = new ShelvedChangeListImpl(patchPath.toString(), commitMessage.replace('\n', ' '), binaryFiles);
            changeList.markToDelete(markToBeDeleted);
            changeList.setName(schemePatchDir.getName());
            ProgressManager.checkCanceled();
            mySchemeManager.addNewScheme(changeList, false);

            if (rollback) {
                String operationName = UIUtil.removeMnemonic(RollbackChangesDialog.operationNameByChanges(myProject, changes));
                boolean modalContext = UIAccess.isUIThread() && UIAccess.current().isInModalContext();
                if (progressIndicator != null) {
                    progressIndicator.startNonCancelableSection();
                }
                new RollbackWorker(myProject, operationName, modalContext)
                    .doRollback(changes, true, null, VcsLocalize.shelveChangesAction().get());
            }
        }
        finally {
            notifyStateChanged();
        }

        return changeList;
    }

    public void unshelveSilentlyAsynchronously(
        @Nonnull final Project project,
        @Nonnull final List<ShelvedChangeListImpl> selectedChangeLists,
        @Nonnull final List<ShelvedChangeImpl> selectedChanges,
        @Nonnull final List<ShelvedBinaryFileImpl> selectedBinaryChanges,
        @Nullable final LocalChangeList forcePredefinedOneChangelist
    ) {
        ProgressManager.getInstance().run(new Task.Backgroundable(
            project,
            VcsBundle.message("unshelve.changes.progress.title"),
            true
        ) {
            @Override
            public void run(@Nonnull ProgressIndicator indicator) {
                for (ShelvedChangeListImpl changeList : selectedChangeLists) {
                    List<ShelvedChangeImpl> changesForChangelist = ContainerUtil.newArrayList(
                        ContainerUtil.intersection(changeList.getChanges((Project) myProject), selectedChanges)
                    );
                    List<ShelvedBinaryFile> binariesForChangelist =
                        ContainerUtil.newArrayList(ContainerUtil.intersection(changeList.getBinaryFiles(), selectedBinaryChanges));
                    boolean shouldUnshelveAllList = changesForChangelist.isEmpty() && binariesForChangelist.isEmpty();
                    unshelveChangeList(
                        changeList,
                        shouldUnshelveAllList ? null : changesForChangelist,
                        shouldUnshelveAllList ? null : binariesForChangelist,
                        forcePredefinedOneChangelist != null ? forcePredefinedOneChangelist : getChangeListUnshelveTo(changeList),
                        true
                    );
                }
            }
        });
    }

    @Nonnull
    private LocalChangeList getChangeListUnshelveTo(@Nonnull ShelvedChangeListImpl list) {
        String changeListName = list.DESCRIPTION;
        ChangeListManager manager = ChangeListManager.getInstance(myProject);
        LocalChangeList localChangeList = manager.findChangeList(changeListName);
        if (localChangeList != null) {
            return localChangeList;
        }
        if (list.isMarkedToDelete()) {
            localChangeList = ChangeListUtil.getPredefinedChangeList(changeListName, manager);
        }
        return localChangeList != null ? localChangeList : manager.addChangeList(changeListName, "");
    }

    @Nonnull
    private static File getPatchFileInConfigDir(@Nonnull File schemePatchDir) {
        return new File(schemePatchDir, DEFAULT_PATCH_NAME + "." + VcsConfiguration.PATCH);
    }

    private void baseRevisionsOfDvcsIntoContext(List<Change> textChanges, CommitContext commitContext) {
        ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(myProject);
        if (dvcsUsedInProject() && VcsConfiguration.getInstance(myProject).INCLUDE_TEXT_INTO_SHELF) {
            Set<Change> big = SelectFilesToAddTextsToPatchPanel.getBig(textChanges);
            ArrayList<FilePath> toKeep = new ArrayList<>();
            for (Change change : textChanges) {
                if (change.getBeforeRevision() == null || change.getAfterRevision() == null) {
                    continue;
                }
                if (big.contains(change)) {
                    continue;
                }
                FilePath filePath = ChangesUtil.getFilePath(change);
                AbstractVcs vcs = vcsManager.getVcsFor(filePath);
                if (vcs != null && VcsType.distributed.equals(vcs.getType())) {
                    toKeep.add(filePath);
                }
            }
            commitContext.putUserData(BaseRevisionTextPatchEP.ourPutBaseRevisionTextKey, true);
            commitContext.putUserData(BaseRevisionTextPatchEP.ourBaseRevisionPaths, toKeep);
        }
    }

    private boolean dvcsUsedInProject() {
        return Arrays.stream(ProjectLevelVcsManager.getInstance(myProject).getAllActiveVcss())
            .anyMatch(vcs -> VcsType.distributed.equals(vcs.getType()));
    }

    public ShelvedChangeListImpl importFilePatches(
        String fileName,
        List<FilePatch> patches,
        PatchEP[] patchTransitExtensions
    ) throws IOException {
        try {
            File schemePatchDir = generateUniqueSchemePatchDir(fileName, true);
            File patchPath = getPatchFileInConfigDir(schemePatchDir);
            myFileProcessor.savePathFile(
                (writer, commitContext) ->
                    UnifiedDiffWriter.write(myProject, patches, writer, "\n", patchTransitExtensions, commitContext),
                patchPath,
                new CommitContext()
            );

            ShelvedChangeListImpl changeList =
                new ShelvedChangeListImpl(patchPath.toString(), fileName.replace('\n', ' '), new SmartList<>());
            changeList.setName(schemePatchDir.getName());
            mySchemeManager.addNewScheme(changeList, false);
            return changeList;
        }
        finally {
            notifyStateChanged();
        }
    }

    public List<VirtualFile> gatherPatchFiles(Collection<VirtualFile> files) {
        List<VirtualFile> result = new ArrayList<>();

        LinkedList<VirtualFile> filesQueue = new LinkedList<>(files);
        while (!filesQueue.isEmpty()) {
            ProgressManager.checkCanceled();
            VirtualFile file = filesQueue.removeFirst();
            if (file.isDirectory()) {
                filesQueue.addAll(Arrays.asList(file.getChildren()));
                continue;
            }
            if (PatchFileType.isPatchFile(file)) {
                result.add(file);
            }
        }

        return result;
    }

    public List<ShelvedChangeListImpl> importChangeLists(Collection<VirtualFile> files, Consumer<VcsException> exceptionConsumer) {
        List<ShelvedChangeListImpl> result = new ArrayList<>(files.size());
        try {
            FilesProgress filesProgress = new FilesProgress(files.size(), "Processing ");
            for (VirtualFile file : files) {
                filesProgress.updateIndicator(file);
                String description = file.getNameWithoutExtension().replace('_', ' ');
                File schemeNameDir = generateUniqueSchemePatchDir(description, true);
                File patchPath = getPatchFileInConfigDir(schemeNameDir);
                ShelvedChangeListImpl list = new ShelvedChangeListImpl(patchPath.getPath(), description, new SmartList<>(), file.getTimeStamp());
                list.setName(schemeNameDir.getName());
                try {
                    List<TextFilePatch> patchesList = loadPatches(myProject, file.getPath(), new CommitContext());
                    if (!patchesList.isEmpty()) {
                        FileUtil.copy(new File(file.getPath()), patchPath, FilePermissionCopier.BY_NIO2);
                        // add only if ok to read patch
                        mySchemeManager.addNewScheme(list, false);
                        result.add(list);
                    }
                }
                catch (IOException | PatchSyntaxException e) {
                    exceptionConsumer.accept(new VcsException(e));
                }
            }
        }
        finally {
            notifyStateChanged();
        }
        return result;
    }

    private ShelvedBinaryFileImpl shelveBinaryFile(@Nonnull File schemePatchDir, Change change) throws IOException {
        ContentRevision beforeRevision = change.getBeforeRevision();
        ContentRevision afterRevision = change.getAfterRevision();
        File beforeFile = beforeRevision == null ? null : beforeRevision.getFile().getIOFile();
        File afterFile = afterRevision == null ? null : afterRevision.getFile().getIOFile();
        String shelvedPath = null;
        if (afterFile != null) {
            File shelvedFile = new File(schemePatchDir, afterFile.getName());
            FileUtil.copy(afterRevision.getFile().getIOFile(), shelvedFile, FilePermissionCopier.BY_NIO2);
            shelvedPath = shelvedFile.getPath();
        }
        String beforePath = ChangesUtil.getProjectRelativePath(myProject, beforeFile);
        String afterPath = ChangesUtil.getProjectRelativePath(myProject, afterFile);
        return new ShelvedBinaryFileImpl(beforePath, afterPath, shelvedPath);
    }

    private void notifyStateChanged() {
        if (!myProject.isDisposed()) {
            myBus.syncPublisher(ShelveChangesListener.class).changeChanged(this);
        }
    }

    @Nonnull
    private File generateUniqueSchemePatchDir(@Nonnull String defaultName, boolean createResourceDirectory) {
        ignoreShelfDirectoryIfFirstShelf();
        String uniqueName = UniqueNameGenerator.generateUniqueName(shortenAndSanitize(defaultName), mySchemeManager.getAllSchemeNames());
        File dir = new File(myFileProcessor.getBaseDir(), uniqueName);
        if (createResourceDirectory && !dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        return dir;
    }

    private void ignoreShelfDirectoryIfFirstShelf() {
        File shelfDir = getShelfResourcesDirectory();
        //check that shelf directory wasn't exist before that to ignore it only once
        if (!shelfDir.exists()) {
            ChangeListManager.getInstance(myProject).addDirectoryToIgnoreImplicitly(shelfDir.getAbsolutePath());
        }
    }

    @Nonnull
    // for create patch only
    public static File suggestPatchName(
        Project project,
        @Nonnull String commitMessage,
        File file,
        String extension
    ) {
        @NonNls String defaultPath = shortenAndSanitize(commitMessage);
        while (true) {
            File nonexistentFile = FileUtil.findSequentNonexistentFile(
                file,
                defaultPath,
                extension == null ? VcsConfiguration.getInstance(project).getPatchFileExtension() : extension
            );
            if (nonexistentFile.getName().length() >= PatchNameChecker.MAX) {
                defaultPath = defaultPath.substring(0, defaultPath.length() - 1);
                continue;
            }
            return nonexistentFile;
        }
    }

    @Nonnull
    private static String shortenAndSanitize(@Nonnull String commitMessage) {
        @NonNls String defaultPath = FileUtil.sanitizeFileName(commitMessage);
        if (defaultPath.isEmpty()) {
            defaultPath = "unnamed";
        }
        if (defaultPath.length() > PatchNameChecker.MAX - 10) {
            defaultPath = defaultPath.substring(0, PatchNameChecker.MAX - 10);
        }
        return defaultPath;
    }

    @Override
    public void unshelveChangeList(
        ShelvedChangeList changeList,
        @Nullable List<? extends ShelvedChange> changes,
        @Nullable List<? extends ShelvedBinaryFile> binaryFiles,
        @Nullable LocalChangeList targetChangeList,
        boolean showSuccessNotification,
        boolean systemOperation,
        boolean reverse,
        String leftConflictTitle,
        String rightConflictTitle
    ) {
        List<FilePatch> remainingPatches = new ArrayList<>();

        CommitContext commitContext = new CommitContext();
        List<TextFilePatch> textFilePatches;
        try {
            textFilePatches = loadTextPatches(myProject, changeList, changes, remainingPatches, commitContext);
        }
        catch (IOException e) {
            LOG.info(e);
            PatchApplier.showError(myProject, "Cannot load patch(es): " + e.getMessage(), true);
            return;
        }
        catch (PatchSyntaxException e) {
            PatchApplier.showError(myProject, "Cannot load patch(es): " + e.getMessage(), true);
            LOG.info(e);
            return;
        }

        List<FilePatch> patches = new ArrayList<>(textFilePatches);

        List<ShelvedBinaryFile> remainingBinaries = new ArrayList<>();
        List<ShelvedBinaryFile> binaryFilesToUnshelve = getBinaryFilesToUnshelve(changeList, binaryFiles, remainingBinaries);

        for (ShelvedBinaryFile shelvedBinaryFile : binaryFilesToUnshelve) {
            patches.add(new ShelvedBinaryFilePatch((ShelvedBinaryFileImpl) shelvedBinaryFile));
        }

        myProject.getApplication().invokeAndWait(
            () -> {
                BinaryPatchApplier binaryPatchApplier = new BinaryPatchApplier();
                PatchApplier<ShelvedBinaryFilePatch> patchApplier = new PatchApplier<>(
                    myProject,
                    myProject.getBaseDir(),
                    patches,
                    targetChangeList,
                    binaryPatchApplier,
                    commitContext,
                    reverse,
                    leftConflictTitle,
                    rightConflictTitle
                );
                patchApplier.setIsSystemOperation(systemOperation);
                patchApplier.execute(showSuccessNotification, systemOperation);
                if (isRemoveFilesFromShelf() || systemOperation) {
                    remainingPatches.addAll(patchApplier.getRemainingPatches());
                    if (remainingPatches.isEmpty() && remainingBinaries.isEmpty()) {
                        recycleChangeList(changeList);
                    }
                    else {
                        saveRemainingPatches((ShelvedChangeListImpl) changeList, remainingPatches, remainingBinaries, commitContext);
                    }
                }
            },
            Application.get().getDefaultModalityState()
        );
    }

    private static List<TextFilePatch> loadTextPatches(
        Project project,
        ShelvedChangeList changeList,
        List<? extends ShelvedChange> changes,
        List<FilePatch> remainingPatches,
        CommitContext commitContext
    ) throws IOException, PatchSyntaxException {
        List<TextFilePatch> textFilePatches = loadPatches(project, changeList.getPath(), commitContext);

        if (changes != null) {
            Iterator<TextFilePatch> iterator = textFilePatches.iterator();
            while (iterator.hasNext()) {
                TextFilePatch patch = iterator.next();
                if (!needUnshelve(patch, changes)) {
                    remainingPatches.add(patch);
                    iterator.remove();
                }
            }
        }
        return textFilePatches;
    }

    public void setRemoveFilesFromShelf(boolean removeFilesFromShelf) {
        myRemoveFilesFromShelf = removeFilesFromShelf;
    }

    public boolean isRemoveFilesFromShelf() {
        return myRemoveFilesFromShelf;
    }

    private void cleanSystemUnshelvedOlderOneWeek() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -7);
        cleanUnshelved(true, cal.getTimeInMillis());
    }

    public void cleanUnshelved(boolean onlyMarkedToDelete, long timeBefore) {
        Date limitDate = new Date(timeBefore);
        List<ShelvedChangeList> toDelete = ContainerUtil.filter(
            mySchemeManager.getAllSchemes(),
            list -> (list.isRecycled()) && list.getDate().before(limitDate) && (!onlyMarkedToDelete || list.isMarkedToDelete())
        );
        clearShelvedLists(toDelete);
    }

    private class BinaryPatchApplier implements CustomBinaryPatchApplier<ShelvedBinaryFilePatch> {
        private final List<FilePatch> myAppliedPatches;

        private BinaryPatchApplier() {
            myAppliedPatches = new ArrayList<>();
        }

        @Override
        @Nonnull
        public ApplyPatchStatus apply(
            List<Pair<VirtualFile, ApplyFilePatchBase<ShelvedBinaryFilePatch>>> patches
        ) throws IOException {
            for (Pair<VirtualFile, ApplyFilePatchBase<ShelvedBinaryFilePatch>> patch : patches) {
                ShelvedBinaryFilePatch shelvedPatch = patch.getSecond().getPatch();
                unshelveBinaryFile(shelvedPatch.getShelvedBinaryFile(), patch.getFirst());
                myAppliedPatches.add(shelvedPatch);
            }
            return ApplyPatchStatus.SUCCESS;
        }

        @Override
        @Nonnull
        public List<FilePatch> getAppliedPatches() {
            return myAppliedPatches;
        }
    }

    private static List<ShelvedBinaryFile> getBinaryFilesToUnshelve(
        ShelvedChangeList changeList,
        List<? extends ShelvedBinaryFile> binaryFiles,
        List<? super ShelvedBinaryFile> remainingBinaries
    ) {
        if (binaryFiles == null) {
            return new ArrayList<>(changeList.getBinaryFiles());
        }
        ArrayList<ShelvedBinaryFile> result = new ArrayList<>();
        for (ShelvedBinaryFile file : changeList.getBinaryFiles()) {
            if (binaryFiles.contains(file)) {
                result.add(file);
            }
            else {
                remainingBinaries.add(file);
            }
        }
        return result;
    }

    @RequiredUIAccess
    private void unshelveBinaryFile(ShelvedBinaryFileImpl file, @Nonnull final VirtualFile patchTarget) throws IOException {
        final Ref<IOException> ex = new Ref<>();
        final Ref<VirtualFile> patchedFileRef = new Ref<>();
        final File shelvedFile = file.SHELVED_PATH == null ? null : new File(file.SHELVED_PATH);

        myProject.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                try {
                    if (shelvedFile == null) {
                        patchTarget.delete(this);
                    }
                    else {
                        patchTarget.setBinaryContent(RawFileLoader.getInstance().loadFileBytes(shelvedFile));
                        patchedFileRef.set(patchTarget);
                    }
                }
                catch (IOException e) {
                    ex.set(e);
                }
            }
        });
        if (!ex.isNull()) {
            throw ex.get();
        }
    }

    private static boolean needUnshelve(FilePatch patch, List<? extends ShelvedChange> changes) {
        for (ShelvedChange change : changes) {
            if (Comparing.equal(patch.getBeforeName(), change.getBeforePath())) {
                return true;
            }
        }
        return false;
    }

    private static void writePatchesToFile(
        Project project,
        String path,
        List<FilePatch> remainingPatches,
        CommitContext commitContext
    ) {
        try {
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8)) {
                UnifiedDiffWriter.write(project, remainingPatches, writer, "\n", commitContext);
            }
        }
        catch (IOException e) {
            LOG.error(e);
        }
    }

    public void saveRemainingPatches(
        ShelvedChangeListImpl changeList,
        List<FilePatch> remainingPatches,
        List<? extends ShelvedBinaryFile> remainingBinaries,
        CommitContext commitContext
    ) {
        ShelvedChangeList listCopy;
        try {
            listCopy = !changeList.isRecycled() ? createRecycledChangelist(changeList) : null;
        }
        catch (IOException e) {
            // do not delete if cannot recycle
            return;
        }
        writePatchesToFile(myProject, changeList.getPath(), remainingPatches, commitContext);

        changeList.getBinaryFiles().retainAll(remainingBinaries);
        changeList.clearLoadedChanges();
        if (listCopy != null) {
            recycleChangeList((ShelvedChangeListImpl) listCopy, changeList);
            // all newly create ShelvedChangeList have to be added to SchemesManger as new scheme
            mySchemeManager.addNewScheme(listCopy, false);
        }
        notifyStateChanged();
    }

    @Nullable
    private ShelvedChangeList createRecycledChangelist(ShelvedChangeList changeList) throws IOException {
        File newPatchDir = generateUniqueSchemePatchDir(changeList.getDescription(), true);
        File newPath = getPatchFileInConfigDir(newPatchDir);
        FileUtil.copy(new File(changeList.getPath()), newPath, FilePermissionCopier.BY_NIO2);
        ShelvedChangeListImpl listCopy = new ShelvedChangeListImpl(
            newPath.getAbsolutePath(),
            changeList.getDescription(),
            new ArrayList<>(changeList.getBinaryFiles())
        );
        listCopy.markToDelete(changeList.isMarkedToDelete());
        listCopy.setName(newPatchDir.getName());
        return listCopy;
    }

    public void restoreList(@Nonnull ShelvedChangeListImpl changeList) {
        ShelvedChangeListImpl list = (ShelvedChangeListImpl) mySchemeManager.findSchemeByName(changeList.getName());

        if (list != null) {
            list.setRecycled(false);
            list.updateDate();
        }

        notifyStateChanged();
    }

    @Nonnull
    public List<ShelvedChangeList> getRecycledShelvedChangeLists() {
        return getRecycled(true);
    }

    public void clearRecycled() {
        clearShelvedLists(getRecycledShelvedChangeLists());
    }

    private void clearShelvedLists(@Nonnull List<? extends ShelvedChangeList> shelvedLists) {
        if (shelvedLists.isEmpty()) {
            return;
        }
        for (ShelvedChangeList list : shelvedLists) {
            deleteListImpl(list);
            mySchemeManager.removeScheme(list);
        }
        notifyStateChanged();
    }

    private void recycleChangeList(@Nonnull ShelvedChangeListImpl listCopy, @Nullable ShelvedChangeListImpl newList) {
        if (newList != null) {
            for (Iterator<? extends ShelvedBinaryFile> shelvedChangeListIterator = listCopy.getBinaryFiles().iterator();
                 shelvedChangeListIterator.hasNext(); ) {
                ShelvedBinaryFileImpl binaryFile = (ShelvedBinaryFileImpl) shelvedChangeListIterator.next();

                for (ShelvedBinaryFile newBinary : newList.getBinaryFiles()) {
                    if (Comparing.equal(newBinary.getBeforePath(), binaryFile.getBeforePath())
                        && Comparing.equal(newBinary.getAfterPath(), binaryFile.getAfterPath())) {
                        shelvedChangeListIterator.remove();
                    }
                }
            }
            for (Iterator<? extends ShelvedChange> iterator = listCopy.getChanges(myProject).iterator(); iterator.hasNext(); ) {
                ShelvedChange change = iterator.next();
                for (ShelvedChangeImpl newChange : newList.getChanges(myProject)) {
                    if (Comparing.equal(change.getBeforePath(), newChange.getBeforePath())
                        && Comparing.equal(change.getAfterPath(), newChange.getAfterPath())) {
                        iterator.remove();
                    }
                }
            }

            // needed only if partial unshelve
            try {
                CommitContext commitContext = new CommitContext();
                List<FilePatch> patches = new ArrayList<>();
                for (ShelvedChangeImpl change : listCopy.getChanges(myProject)) {
                    patches.add(change.loadFilePatch(myProject, commitContext));
                }
                writePatchesToFile(myProject, listCopy.getPath(), patches, commitContext);
            }
            catch (IOException | PatchSyntaxException e) {
                LOG.info(e);
                // left file as is
            }
        }

        if (!listCopy.getBinaryFiles().isEmpty() || !listCopy.getChanges(myProject).isEmpty()) {
            listCopy.setRecycled(true);
            listCopy.updateDate();
            notifyStateChanged();
        }
    }

    public void recycleChangeList(@Nonnull ShelvedChangeList changeList) {
        recycleChangeList((ShelvedChangeListImpl) changeList, null);
        notifyStateChanged();
    }

    public void deleteChangeList(@Nonnull ShelvedChangeList changeList) {
        deleteListImpl(changeList);
        mySchemeManager.removeScheme(changeList);
        notifyStateChanged();
    }

    private void deleteListImpl(@Nonnull ShelvedChangeList changeList) {
        FileUtil.delete(new File(myFileProcessor.getBaseDir(), changeList.getName()));
        //backward compatibility deletion: if we didn't preform resource migration
        FileUtil.delete(new File(changeList.getPath()));
        for (ShelvedBinaryFile binaryFile : changeList.getBinaryFiles()) {
            String path = binaryFile.getShelvedPath();
            if (path != null) {
                FileUtil.delete(new File(path));
            }
        }
    }

    public void renameChangeList(ShelvedChangeListImpl changeList, String newName) {
        changeList.DESCRIPTION = newName;
        notifyStateChanged();
    }

    @Nonnull
    public static List<TextFilePatch> loadPatches(
        Project project,
        String patchPath,
        CommitContext commitContext
    ) throws IOException, PatchSyntaxException {
        return loadPatches(project, patchPath, commitContext, true);
    }

    @Nonnull
    static List<? extends FilePatch> loadPatchesWithoutContent(
        Project project,
        String patchPath,
        CommitContext commitContext
    ) throws IOException, PatchSyntaxException {
        return loadPatches(project, patchPath, commitContext, false);
    }

    private static List<TextFilePatch> loadPatches(
        Project project,
        String patchPath,
        CommitContext commitContext,
        boolean loadContent
    ) throws IOException, PatchSyntaxException {
        String patchText = Files.readString(Path.of(patchPath), StandardCharsets.UTF_8);
        PatchReader reader = new PatchReader(patchText, loadContent);
        List<TextFilePatch> textFilePatches = reader.readTextPatches();
        ApplyPatchDefaultExecutor.applyAdditionalInfoBefore(project, reader.getAdditionalInfo(null), commitContext);
        return textFilePatches;
    }

    public boolean isShowRecycled() {
        return myShowRecycled;
    }

    public void setShowRecycled(boolean showRecycled) {
        myShowRecycled = showRecycled;
        notifyStateChanged();
    }
}
