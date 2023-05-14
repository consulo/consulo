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
package consulo.ide.impl.idea.packaging.impl.ui.actions;

import consulo.ide.impl.idea.openapi.deployment.DeploymentUtil;
import consulo.ide.impl.idea.openapi.util.io.FileUtil;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.element.ArtifactRootElement;
import consulo.compiler.artifact.element.CompositePackagingElement;
import consulo.compiler.artifact.ArtifactUtil;
import consulo.compiler.artifact.PackagingElementPath;
import consulo.compiler.artifact.element.ArchivePackagingElement;
import consulo.ide.impl.idea.util.PathUtil;
import consulo.util.io.zip.JBZipEntry;
import consulo.util.io.zip.JBZipFile;
import consulo.application.AccessRule;
import consulo.application.ApplicationManager;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.compiler.CompilerBundle;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.notification.Notifications;
import consulo.util.concurrent.AsyncResult;
import consulo.util.lang.Trinity;
import consulo.virtualFileSystem.RawFileLoader;
import consulo.virtualFileSystem.VirtualFile;

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
  public static final NotificationGroup ARTIFACT_PACKAGING_GROUP = NotificationGroup.balloonGroup("artifactPackaging", LocalizeValue.localizeTODO("Artifact Packaging"));

  private static final Logger LOG = Logger.getInstance(PackageFileWorker.class);
  private final File myFile;
  private final String myRelativeOutputPath;

  private PackageFileWorker(File file, String relativeOutputPath) {
    myFile = file;
    myRelativeOutputPath = relativeOutputPath;
  }

  public static void startPackagingFiles(Project project, List<VirtualFile> files, Artifact[] artifacts, final @Nonnull Runnable onFinishedInAwt) {
    startPackagingFiles(project, files, artifacts).doWhenProcessed(new Runnable() {
      @Override
      public void run() {
        ApplicationManager.getApplication().invokeLater(onFinishedInAwt);
      }
    });
  }

  public static AsyncResult<Void> startPackagingFiles(final Project project, final List<VirtualFile> files, final Artifact[] artifacts) {
    final AsyncResult<Void> callback = new AsyncResult<>();
    ProgressManager.getInstance().run(new Task.Backgroundable(project, "Packaging Files") {
      @Override
      public void run(@Nonnull ProgressIndicator indicator) {
        try {
          for (final VirtualFile file : files) {
            indicator.checkCanceled();
            AccessRule.read(() -> {
              try {
                packageFile(file, project, artifacts);
              }
              catch (IOException e) {
                String message = CompilerBundle.message("message.tect.package.file.io.error", e.toString());
                Notifications.Bus.notify(new Notification(ARTIFACT_PACKAGING_GROUP, "Cannot package file", message, NotificationType.ERROR));
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

  public static void packageFile(@Nonnull VirtualFile file, @Nonnull Project project, final Artifact[] artifacts) throws IOException {
    LOG.debug("Start packaging file: " + file.getPath());
    final Collection<Trinity<Artifact, PackagingElementPath, String>> items = ArtifactUtil.findContainingArtifactsWithOutputPaths(file, project, artifacts);
    File ioFile = VfsUtilCore.virtualToIoFile(file);
    for (Trinity<Artifact, PackagingElementPath, String> item : items) {
      final Artifact artifact = item.getFirst();
      final String outputPath = artifact.getOutputPath();
      if (!StringUtil.isEmpty(outputPath)) {
        PackageFileWorker worker = new PackageFileWorker(ioFile, item.getThird());
        LOG.debug(" package to " + outputPath);
        worker.packageFile(outputPath, item.getSecond().getParents());
      }
    }
  }

  private void packageFile(String outputPath, List<CompositePackagingElement<?>> parents) throws IOException {
    List<CompositePackagingElement<?>> parentsList = new ArrayList<CompositePackagingElement<?>>(parents);
    Collections.reverse(parentsList);
    if (!parentsList.isEmpty() && parentsList.get(0) instanceof ArtifactRootElement) {
      parentsList = parentsList.subList(1, parentsList.size());
    }
    copyFile(outputPath, parentsList);
  }

  private void copyFile(String outputPath, List<CompositePackagingElement<?>> parents) throws IOException {
    if (parents.isEmpty()) {
      final String fullOutputPath = DeploymentUtil.appendToPath(outputPath, myRelativeOutputPath);
      File target = new File(fullOutputPath);
      if (FileUtil.filesEqual(myFile, target)) {
        LOG.debug("  skipping copying file to itself");
      }
      else {
        LOG.debug("  copying to " + fullOutputPath);
        FileUtil.copy(myFile, target);
      }
      return;
    }

    final CompositePackagingElement<?> element = parents.get(0);
    final String nextOutputPath = outputPath + "/" + element.getName();
    final List<CompositePackagingElement<?>> parentsTrail = parents.subList(1, parents.size());
    if (element instanceof ArchivePackagingElement) {
      packFile(nextOutputPath, "", parentsTrail);
    }
    else {
      copyFile(nextOutputPath, parentsTrail);
    }
  }

  private void packFile(String archivePath, String pathInArchive, List<CompositePackagingElement<?>> parents) throws IOException {
    final File archiveFile = new File(archivePath);
    if (parents.isEmpty()) {
      LOG.debug("  adding to archive " + archivePath);
      JBZipFile file = getOrCreateZipFile(archiveFile);
      try {
        final String fullPathInArchive = DeploymentUtil.trimForwardSlashes(DeploymentUtil.appendToPath(pathInArchive, myRelativeOutputPath));
        final JBZipEntry entry = file.getOrCreateEntry(fullPathInArchive);
        entry.setData(RawFileLoader.getInstance().loadFileBytes(myFile));
      }
      finally {
        file.close();
      }
      return;
    }

    final CompositePackagingElement<?> element = parents.get(0);
    final String nextPathInArchive = DeploymentUtil.trimForwardSlashes(DeploymentUtil.appendToPath(pathInArchive, element.getName()));
    final List<CompositePackagingElement<?>> parentsTrail = parents.subList(1, parents.size());
    if (element instanceof ArchivePackagingElement) {
      JBZipFile zipFile = getOrCreateZipFile(archiveFile);
      try {
        final JBZipEntry entry = zipFile.getOrCreateEntry(nextPathInArchive);
        LOG.debug("  extracting to temp file: " + nextPathInArchive + " from " + archivePath);
        final File tempFile = FileUtil.createTempFile("packageFile" + FileUtil.sanitizeFileName(nextPathInArchive),
                                                      FileUtil.getExtension(PathUtil.getFileName(nextPathInArchive)));
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
