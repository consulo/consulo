/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.compiler.impl.internal.artifact;

import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.compiler.CompileContext;
import consulo.compiler.CompilerMessageCategory;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ArtifactManager;
import consulo.compiler.artifact.ArtifactProperties;
import consulo.compiler.artifact.ArtifactPropertiesProvider;
import consulo.compiler.artifact.element.*;
import consulo.compiler.artifact.internal.ArtifactSortingUtil;
import consulo.compiler.generic.GenericCompilerCacheState;
import consulo.compiler.generic.GenericCompilerInstance;
import consulo.compiler.generic.GenericCompilerProcessingItem;
import consulo.compiler.generic.VirtualFilePersistentState;
import consulo.compiler.impl.internal.ArtifactCompilerUtil;
import consulo.compiler.localize.CompilerLocalize;
import consulo.compiler.util.CompilerUtil;
import consulo.component.ProcessCanceledException;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.DumbService;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Sets;
import consulo.util.io.FileUtil;
import consulo.util.io.URLUtil;
import consulo.util.lang.ExceptionUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;

import java.io.*;
import java.util.*;

/**
 * @author nik
 */
public class ArtifactsCompilerInstance extends GenericCompilerInstance<ArtifactBuildTarget, ArtifactCompilerCompileItem, String, VirtualFilePersistentState, ArtifactPackagingItemOutputState> {
    private static final Logger LOG = Logger.getInstance(ArtifactsCompilerInstance.class);
    public static final Logger FULL_LOG = Logger.getInstance("#com.intellij.full-artifacts-compiler-log");
    private ArtifactsProcessingItemsBuilderContext myBuilderContext;

    public ArtifactsCompilerInstance(CompileContext context) {
        super(context);
    }

    @Nonnull
    @Override
    public List<ArtifactBuildTarget> getAllTargets() {
        return getArtifactTargets(false);
    }

    @Nonnull
    @Override
    public List<ArtifactBuildTarget> getSelectedTargets() {
        return getArtifactTargets(true);
    }

    private List<ArtifactBuildTarget> getArtifactTargets(boolean selectedOnly) {
        List<ArtifactBuildTarget> targets = new ArrayList<>();
        ReadAction.run(() -> {
            Set<Artifact> artifacts;
            if (selectedOnly) {
                artifacts = ArtifactCompileScope.getArtifactsToBuild(getProject(), myContext.getCompileScope(), true);
            }
            else {
                artifacts = new HashSet<>(Arrays.asList(ArtifactManager.getInstance(getProject()).getArtifacts()));
            }

            Map<String, Artifact> artifactsMap = new HashMap<>();
            for (Artifact artifact : artifacts) {
                artifactsMap.put(artifact.getName(), artifact);
            }
            for (String name : ArtifactSortingUtil.getInstance(getProject()).getArtifactsSortedByInclusion()) {
                Artifact artifact = artifactsMap.get(name);
                if (artifact != null) {
                    targets.add(new ArtifactBuildTarget(artifact));
                }
            }
        });
        return targets;
    }

    @Override
    public void processObsoleteTarget(
        @Nonnull String targetId,
        @Nonnull List<GenericCompilerCacheState<String, VirtualFilePersistentState, ArtifactPackagingItemOutputState>> obsoleteItems
    ) {
        deleteFiles(
            obsoleteItems,
            Collections.<GenericCompilerProcessingItem<ArtifactCompilerCompileItem, VirtualFilePersistentState, ArtifactPackagingItemOutputState>>emptyList()
        );
    }

    @Nonnull
    @Override
    public List<ArtifactCompilerCompileItem> getItems(@Nonnull ArtifactBuildTarget target) {
        myBuilderContext = new ArtifactsProcessingItemsBuilderContext(myContext);
        Artifact artifact = target.getArtifact();

        Map<String, String> selfIncludingArtifacts =
            ReadAction.compute(() -> ArtifactSortingUtil.getInstance(getProject()).getArtifactToSelfIncludingNameMap());
        String selfIncludingName = selfIncludingArtifacts.get(artifact.getName());
        if (selfIncludingName != null) {
            String name = selfIncludingName.equals(artifact.getName()) ? "it" : "'" + selfIncludingName + "' artifact";
            myContext.addMessage(
                CompilerMessageCategory.ERROR,
                "Cannot build '" + artifact.getName() + "' artifact: " + name + " includes itself in the output layout",
                null,
                -1,
                -1
            );
            return Collections.emptyList();
        }

        String outputPath = artifact.getOutputPath();
        if (outputPath == null || outputPath.length() == 0) {
            myContext.addMessage(
                CompilerMessageCategory.ERROR,
                "Cannot build '" + artifact.getName() + "' artifact: output path is not specified",
                null,
                -1,
                -1
            );
            return Collections.emptyList();
        }

        DumbService.getInstance(getProject()).waitForSmartMode();
        ReadAction.run(() -> collectItems(artifact, outputPath));
        return new ArrayList<>(myBuilderContext.getProcessingItems());
    }

    private void collectItems(@Nonnull Artifact artifact, @Nonnull String outputPath) {
        CompositePackagingElement<?> rootElement = artifact.getRootElement();
        VirtualFile outputFile = LocalFileSystem.getInstance().findFileByPath(outputPath);
        CopyToDirectoryInstructionCreator instructionCreator =
            new CopyToDirectoryInstructionCreator(myBuilderContext, outputPath, outputFile);
        PackagingElementResolvingContext resolvingContext = ArtifactManager.getInstance(getProject()).getResolvingContext();
        FULL_LOG.debug("Collecting items for " + artifact.getName());
        rootElement.computeIncrementalCompilerInstructions(
            instructionCreator,
            resolvingContext,
            myBuilderContext,
            artifact.getArtifactType()
        );
    }

    private boolean doBuild(
        @Nonnull Artifact artifact,
        List<GenericCompilerProcessingItem<ArtifactCompilerCompileItem, VirtualFilePersistentState, ArtifactPackagingItemOutputState>> changedItems,
        Set<ArtifactCompilerCompileItem> processedItems,
        @Nonnull Set<String> writtenPaths,
        Set<String> deletedJars
    ) {
        FULL_LOG.debug("Building " + artifact.getName());
        boolean testMode = Application.get().isUnitTestMode();

        FileFilter fileFilter = new IgnoredFileFilter();
        Set<ArchivePackageInfo> changedJars = new HashSet<>();
        for (String deletedJar : deletedJars) {
            ContainerUtil.addIfNotNull(changedJars, myBuilderContext.getJarInfo(deletedJar));
        }

        try {
            onBuildStartedOrFinished(artifact, false);
            if (myContext.getMessageCount(CompilerMessageCategory.ERROR) > 0) {
                return false;
            }

            int i = 0;
            for (GenericCompilerProcessingItem<ArtifactCompilerCompileItem, VirtualFilePersistentState, ArtifactPackagingItemOutputState> item : changedItems) {
                ArtifactCompilerCompileItem sourceItem = item.getItem();
                myContext.getProgressIndicator().checkCanceled();

                ReadAction.run(() -> {
                    VirtualFile sourceFile = sourceItem.getFile();
                    for (DestinationInfo destination : sourceItem.getDestinations()) {
                        if (destination instanceof ExplodedDestinationInfo) {
                            ExplodedDestinationInfo explodedDestination = (ExplodedDestinationInfo) destination;
                            File toFile = new File(FileUtil.toSystemDependentName(explodedDestination.getOutputPath()));
                            if (sourceFile.isInLocalFileSystem()) {
                                File ioFromFile = VirtualFileUtil.virtualToIoFile(sourceFile);
                                if (ioFromFile.exists()) {
                                    DeploymentUtilImpl.copyFile(ioFromFile, toFile, myContext, writtenPaths, fileFilter);
                                }
                                else {
                                    LOG.debug("Cannot copy " + ioFromFile.getAbsolutePath() + ": file doesn't exist");
                                }
                            }
                            else {
                                extractFile(sourceFile, toFile, writtenPaths, fileFilter);
                            }
                        }
                        else {
                            changedJars.add(((ArchiveDestinationInfo) destination).getArchivePackageInfo());
                        }
                    }
                });

                myContext.getProgressIndicator().setFraction(++i * 1.0 / changedItems.size());
                processedItems.add(sourceItem);
                if (testMode) {
                    //FIXME [VISTALL] CompilerManagerImpl.addRecompiledPath(FileUtil.toSystemDependentName(sourceItem.getFile().getPath()));
                }
            }

            ArchivesBuilder builder = new ArchivesBuilder(changedJars, fileFilter, myContext);
            boolean processed = builder.buildArchives(writtenPaths);
            if (!processed) {
                return false;
            }

            Set<VirtualFile> recompiledSources = new HashSet<>();
            for (ArchivePackageInfo info : builder.getArchivesToBuild()) {
                for (Pair<String, VirtualFile> pair : info.getPackedFiles()) {
                    recompiledSources.add(pair.getSecond());
                }
            }
            for (VirtualFile source : recompiledSources) {
                ArtifactCompilerCompileItem item = myBuilderContext.getItemBySource(source);
                LOG.assertTrue(item != null, source);
                processedItems.add(item);
                if (testMode) {
                    //FIXME [VISTALL] CompilerManagerImpl.addRecompiledPath(FileUtil.toSystemDependentName(item.getFile().getPath()));
                }
            }

            onBuildStartedOrFinished(artifact, true);
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (Exception e) {
            LOG.info(e);
            myContext.addMessage(CompilerMessageCategory.ERROR, ExceptionUtil.getThrowableText(e), null, -1, -1);
            return false;
        }
        return true;
    }

    private void extractFile(VirtualFile sourceFile, File toFile, Set<String> writtenPaths, FileFilter fileFilter) throws IOException {
        if (!writtenPaths.add(toFile.getPath())) {
            return;
        }

        if (!FileUtil.createParentDirs(toFile)) {
            myContext.addMessage(
                CompilerMessageCategory.ERROR,
                "Cannot create directory for '" + toFile.getAbsolutePath() + "' file",
                null,
                -1,
                -1
            );
            return;
        }

        InputStream input = ArtifactCompilerUtil.getArchiveEntryInputStream(sourceFile, myContext).getFirst();
        if (input == null) {
            return;
        }
        try (BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(toFile))) {
            FileUtil.copy(input, output);
        }
        finally {
            input.close();
        }
    }

    private void onBuildStartedOrFinished(@Nonnull Artifact artifact, boolean finished) throws Exception {
        for (ArtifactPropertiesProvider provider : artifact.getPropertiesProviders()) {
            ArtifactProperties<?> properties = artifact.getProperties(provider);
            if (finished) {
                properties.onBuildFinished(artifact, myContext);
            }
            else {
                properties.onBuildStarted(artifact, myContext);
            }
        }
    }

    private static Set<String> createPathsHashSet() {
        return Sets.newHashSet(FileUtil.PATH_HASHING_STRATEGY);
    }

    @Override
    public void processItems(
        @Nonnull ArtifactBuildTarget target,
        @Nonnull List<GenericCompilerProcessingItem<ArtifactCompilerCompileItem, VirtualFilePersistentState, ArtifactPackagingItemOutputState>> changedItems,
        @Nonnull List<GenericCompilerCacheState<String, VirtualFilePersistentState, ArtifactPackagingItemOutputState>> obsoleteItems,
        @Nonnull OutputConsumer<ArtifactCompilerCompileItem> consumer
    ) {
        Set<String> deletedJars = deleteFiles(obsoleteItems, changedItems);

        Set<String> writtenPaths = createPathsHashSet();
        SimpleReference<Boolean> built = SimpleReference.create(false);
        Set<ArtifactCompilerCompileItem> processedItems = new HashSet<>();
        CompilerUtil.runInContext(
            myContext,
            "Copying files",
            () -> built.set(doBuild(target.getArtifact(), changedItems, processedItems, writtenPaths, deletedJars))
        );
        if (!built.get()) {
            return;
        }

        myContext.getProgressIndicator().setTextValue(CompilerLocalize.packagingCompilerMessageUpdatingCaches());
        myContext.getProgressIndicator().setText2Value(LocalizeValue.empty());
        for (String path : writtenPaths) {
            consumer.addFileToRefresh(new File(path));
        }
        for (ArtifactCompilerCompileItem item : processedItems) {
            consumer.addProcessedItem(item);
        }
        ArtifactsCompiler.addWrittenPaths(myContext, writtenPaths);
        ArtifactsCompiler.addChangedArtifact(myContext, target.getArtifact());
    }

    private Set<String> deleteFiles(
        List<GenericCompilerCacheState<String, VirtualFilePersistentState, ArtifactPackagingItemOutputState>> obsoleteItems,
        List<GenericCompilerProcessingItem<ArtifactCompilerCompileItem, VirtualFilePersistentState, ArtifactPackagingItemOutputState>> changedItems
    ) {
        myContext.getProgressIndicator().setTextValue(CompilerLocalize.packagingCompilerMessageDeletingOutdatedFiles());

        boolean testMode = Application.get().isUnitTestMode();
        Set<String> deletedJars = new HashSet<>();
        Set<String> notDeletedJars = new HashSet<>();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Deleting outdated files...");
        }

        Set<String> pathToDelete = new HashSet<>();
        for (GenericCompilerProcessingItem<ArtifactCompilerCompileItem, VirtualFilePersistentState, ArtifactPackagingItemOutputState> item : changedItems) {
            ArtifactPackagingItemOutputState cached = item.getCachedOutputState();
            if (cached != null) {
                for (Pair<String, Long> destination : cached.myDestinations) {
                    pathToDelete.add(destination.getFirst());
                }
            }
        }
        for (GenericCompilerProcessingItem<ArtifactCompilerCompileItem, VirtualFilePersistentState, ArtifactPackagingItemOutputState> item : changedItems) {
            for (DestinationInfo destination : item.getItem().getDestinations()) {
                pathToDelete.remove(destination.getOutputPath());
            }
        }
        for (GenericCompilerCacheState<String, VirtualFilePersistentState, ArtifactPackagingItemOutputState> item : obsoleteItems) {
            for (Pair<String, Long> destination : item.getOutputState().myDestinations) {
                pathToDelete.add(destination.getFirst());
            }
        }

        int notDeletedFilesCount = 0;
        List<File> filesToRefresh = new ArrayList<>();

        for (String fullPath : pathToDelete) {
            int end = fullPath.indexOf(URLUtil.ARCHIVE_SEPARATOR);
            boolean isJar = end != -1;
            String filePath = isJar ? fullPath.substring(0, end) : fullPath;
            boolean deleted = false;
            if (isJar) {
                if (notDeletedJars.contains(filePath)) {
                    continue;
                }
                deleted = deletedJars.contains(filePath);
            }

            File file = new File(FileUtil.toSystemDependentName(filePath));
            if (!deleted) {
                filesToRefresh.add(file);
                deleted = FileUtil.delete(file);
            }

            if (deleted) {
                if (isJar) {
                    deletedJars.add(filePath);
                }
                if (testMode) {
                    //FIXME [VISTALL] CompilerManagerImpl.addDeletedPath(file.getAbsolutePath());
                }
            }
            else {
                if (isJar) {
                    notDeletedJars.add(filePath);
                }
                if (notDeletedFilesCount++ > 50) {
                    myContext.addMessage(
                        CompilerMessageCategory.WARNING,
                        "Deletion of outdated files stopped because too many files cannot be deleted",
                        null,
                        -1,
                        -1
                    );
                    break;
                }
                myContext.addMessage(CompilerMessageCategory.WARNING, "Cannot delete file '" + filePath + "'", null, -1, -1);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Cannot delete file " + file);
                }
            }
        }

        CompilerUtil.refreshIOFiles(filesToRefresh);
        return deletedJars;
    }

}
