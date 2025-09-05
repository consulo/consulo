/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.change.patch;

import consulo.application.progress.ProgressIndicator;
import consulo.component.ProcessCanceledException;
import consulo.diff.chain.DiffRequestProducer;
import consulo.diff.chain.DiffRequestProducerException;
import consulo.diff.request.DiffRequest;
import consulo.project.Project;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.io.FileUtil;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ContentRevision;
import consulo.versionControlSystem.impl.internal.change.shelf.ShelvedBinaryContentRevision;
import consulo.versionControlSystem.impl.internal.change.shelf.ShelvedBinaryFile;
import consulo.versionControlSystem.impl.internal.change.shelf.ShelvedBinaryFilePatch;
import consulo.versionControlSystem.impl.internal.patch.PatchDiffRequestFactory;
import consulo.versionControlSystem.impl.internal.patch.PatchReader;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.util.Collection;

public class BinaryFilePatchInProgress extends AbstractFilePatchInProgress<ShelvedBinaryFilePatch> {

  protected BinaryFilePatchInProgress(ShelvedBinaryFilePatch patch,
                                      Collection<VirtualFile> autoBases,
                                      VirtualFile baseDir) {
    super(ShelvedBinaryFilePatch.patchCopy(patch), autoBases, baseDir);
  }

  @Override
  public ContentRevision getNewContentRevision() {
    if (FilePatchStatus.DELETED.equals(myStatus)) return null;

    if (myNewContentRevision != null) return myNewContentRevision;
    if (myPatch.getAfterFileName() != null) {
      FilePath newFilePath = FilePatchStatus.ADDED.equals(myStatus) ? VcsUtil.getFilePath(myIoCurrentBase, false)
                                                                          : detectNewFilePathForMovedOrModified();
      myNewContentRevision = new ShelvedBinaryContentRevision(newFilePath, myPatch.getShelvedBinaryFile().SHELVED_PATH);
    }
    return myNewContentRevision;
  }

  @Nonnull
  @Override
  public DiffRequestProducer getDiffRequestProducers(final Project project, PatchReader baseContents) {
    final ShelvedBinaryFile file = getPatch().getShelvedBinaryFile();
    return new DiffRequestProducer() {
      @Nonnull
      @Override
      public DiffRequest process(@Nonnull UserDataHolder context, @Nonnull ProgressIndicator indicator)
              throws DiffRequestProducerException, ProcessCanceledException {
        Change change = file.createChange(project);
        return PatchDiffRequestFactory.createDiffRequest(project, change, getName(), context, indicator);
      }

      @Nonnull
      @Override
      public String getName() {
        File file1 = new File(VirtualFileUtil.virtualToIoFile(getBase()), file.AFTER_PATH == null ? file.BEFORE_PATH : file.AFTER_PATH);
        return FileUtil.toSystemDependentName(file1.getPath());
      }
    };
  }
}
