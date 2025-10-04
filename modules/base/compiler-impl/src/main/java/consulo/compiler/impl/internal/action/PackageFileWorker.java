/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.compiler.impl.internal.action;

import consulo.application.ReadAction;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ArtifactUtil;
import consulo.compiler.artifact.PackagingElementPath;
import consulo.compiler.artifact.element.ArchivePackagingElement;
import consulo.compiler.artifact.element.ArtifactRootElement;
import consulo.compiler.artifact.element.CompositePackagingElement;
import consulo.compiler.localize.CompilerLocalize;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationService;
import consulo.util.concurrent.AsyncResult;
import consulo.util.io.FilePermissionCopier;
import consulo.util.io.FileUtil;
import consulo.util.io.PathUtil;
import consulo.util.io.zip.JBZipEntry;
import consulo.util.io.zip.JBZipFile;
import consulo.util.lang.StringUtil;
import consulo.util.lang.Trinity;
import consulo.virtualFileSystem.RawFileLoader;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public class PackageFileWorker {
    public static final NotificationGroup ARTIFACT_PACKAGING_GROUP =
        NotificationGroup.balloonGroup("artifactPackaging", LocalizeValue.localizeTODO("Artifact Packaging"));

    private static final Logger LOG = Logger.getInstance(PackageFileWorker.class);
    private final File myFile;
    private final String myRelativeOutputPath;

    private PackageFileWorker(File file, String relativeOutputPath) {
        myFile = file;
        myRelativeOutputPath = relativeOutputPath;
    }

    public static void startPackagingFiles(
        Project project,
        List<VirtualFile> files,
        Artifact[] artifacts,
        @Nonnull Runnable onFinishedInAwt
    ) {
        startPackagingFiles(project, files, artifacts).doWhenProcessed(() -> project.getApplication().invokeLater(onFinishedInAwt));
    }

    public static AsyncResult<Void> startPackagingFiles(final Project project, final List<VirtualFile> files, final Artifact[] artifacts) {
        final AsyncResult<Void> callback = new AsyncResult<>();
        ProgressManager.getInstance().run(new Task.Backgroundable(project, CompilerLocalize.taskPackagingFilesTitle()) {
            @Override
            public void run(@Nonnull ProgressIndicator indicator) {
                try {
                    for (VirtualFile file : files) {
                        indicator.checkCanceled();
                        ReadAction.run(() -> {
                            try {
                                packageFile(file, project, artifacts);
                            }
                            catch (IOException e) {
                                NotificationService.getInstance()
                                    .newError(ARTIFACT_PACKAGING_GROUP)
                                    .title(CompilerLocalize.messageTitleCannotPackageFile())
                                    .content(CompilerLocalize.messageTextPackageFileIoError(e))
                                    .notify(null);
                            }
                        });
                        callback.setDone();
                    }
                }
                finally {
                    if (!callback.isDone()) {
                        callback.setRejected();
                    }
                }
            }
        });
        return callback;
    }

    public static void packageFile(@Nonnull VirtualFile file, @Nonnull Project project, Artifact[] artifacts) throws IOException {
        LOG.debug("Start packaging file: " + file.getPath());
        Collection<Trinity<Artifact, PackagingElementPath, String>> items =
            ArtifactUtil.findContainingArtifactsWithOutputPaths(file, project, artifacts);
        File ioFile = VirtualFileUtil.virtualToIoFile(file);
        for (Trinity<Artifact, PackagingElementPath, String> item : items) {
            Artifact artifact = item.getFirst();
            String outputPath = artifact.getOutputPath();
            if (!StringUtil.isEmpty(outputPath)) {
                PackageFileWorker worker = new PackageFileWorker(ioFile, item.getThird());
                LOG.debug(" package to " + outputPath);
                worker.packageFile(outputPath, item.getSecond().getParents());
            }
        }
    }

    private void packageFile(String outputPath, List<CompositePackagingElement<?>> parents) throws IOException {
        List<CompositePackagingElement<?>> parentsList = new ArrayList<>(parents);
        Collections.reverse(parentsList);
        if (!parentsList.isEmpty() && parentsList.get(0) instanceof ArtifactRootElement) {
            parentsList = parentsList.subList(1, parentsList.size());
        }
        copyFile(outputPath, parentsList);
    }

    private void copyFile(String outputPath, List<CompositePackagingElement<?>> parents) throws IOException {
        if (parents.isEmpty()) {
            String fullOutputPath = ArtifactUtil.appendToPath(outputPath, myRelativeOutputPath);
            File target = new File(fullOutputPath);
            if (FileUtil.filesEqual(myFile, target)) {
                LOG.debug("  skipping copying file to itself");
            }
            else {
                LOG.debug("  copying to " + fullOutputPath);
                FileUtil.copy(myFile, target, FilePermissionCopier.BY_NIO2);
            }
            return;
        }

        CompositePackagingElement<?> element = parents.get(0);
        String nextOutputPath = outputPath + "/" + element.getName();
        List<CompositePackagingElement<?>> parentsTrail = parents.subList(1, parents.size());
        if (element instanceof ArchivePackagingElement) {
            packFile(nextOutputPath, "", parentsTrail);
        }
        else {
            copyFile(nextOutputPath, parentsTrail);
        }
    }

    private void packFile(String archivePath, String pathInArchive, List<CompositePackagingElement<?>> parents) throws IOException {
        File archiveFile = new File(archivePath);
        if (parents.isEmpty()) {
            LOG.debug("  adding to archive " + archivePath);
            JBZipFile file = getOrCreateZipFile(archiveFile);
            try {
                String fullPathInArchive =
                    ArtifactUtil.trimForwardSlashes(ArtifactUtil.appendToPath(pathInArchive, myRelativeOutputPath));
                JBZipEntry entry = file.getOrCreateEntry(fullPathInArchive);
                entry.setData(RawFileLoader.getInstance().loadFileBytes(myFile));
            }
            finally {
                file.close();
            }
            return;
        }

        CompositePackagingElement<?> element = parents.get(0);
        String nextPathInArchive = ArtifactUtil.trimForwardSlashes(ArtifactUtil.appendToPath(pathInArchive, element.getName()));
        List<CompositePackagingElement<?>> parentsTrail = parents.subList(1, parents.size());
        if (element instanceof ArchivePackagingElement) {
            JBZipFile zipFile = getOrCreateZipFile(archiveFile);
            try {
                JBZipEntry entry = zipFile.getOrCreateEntry(nextPathInArchive);
                LOG.debug("  extracting to temp file: " + nextPathInArchive + " from " + archivePath);
                File tempFile = FileUtil.createTempFile(
                    "packageFile" + FileUtil.sanitizeFileName(nextPathInArchive),
                    FileUtil.getExtension(PathUtil.getFileName(nextPathInArchive))
                );
                if (entry.getSize() != -1) {
                    FileUtil.writeToFile(tempFile, entry.getData());
                }
                packFile(FileUtil.toSystemIndependentName(tempFile.getAbsolutePath()), "", parentsTrail);
                entry.setData(RawFileLoader.getInstance().loadFileBytes(tempFile));
                FileUtil.delete(tempFile);
            }
            finally {
                zipFile.close();
            }
        }
        else {
            packFile(archivePath, nextPathInArchive, parentsTrail);
        }
    }

    private static JBZipFile getOrCreateZipFile(File archiveFile) throws IOException {
        FileUtil.createIfDoesntExist(archiveFile);
        return new JBZipFile(archiveFile);
    }
}
