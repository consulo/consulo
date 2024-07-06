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
package consulo.ide.impl.idea.openapi.vcs.changes.patch;

import consulo.application.ApplicationManager;
import consulo.application.progress.ProgressIndicator;
import consulo.component.ProcessCanceledException;
import consulo.diff.chain.DiffRequestProducer;
import consulo.diff.chain.DiffRequestProducerException;
import consulo.diff.request.DiffRequest;
import consulo.ide.impl.idea.diff.requests.UnknownFileTypeDiffRequest;
import consulo.ide.impl.idea.openapi.diff.impl.patch.FilePatch;
import consulo.ide.impl.idea.openapi.diff.impl.patch.PatchReader;
import consulo.ide.impl.idea.openapi.diff.impl.patch.TextFilePatch;
import consulo.ide.impl.idea.openapi.util.Getter;
import consulo.ide.impl.idea.openapi.vcs.changes.SimpleContentRevision;
import consulo.project.Project;
import consulo.util.dataholder.UserDataHolder;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.ContentRevision;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.util.Collection;

public class TextFilePatchInProgress extends AbstractFilePatchInProgress<TextFilePatch> {

  protected TextFilePatchInProgress(TextFilePatch patch,
                                    Collection<VirtualFile> autoBases,
                                    VirtualFile baseDir) {
    super(patch.pathsOnlyCopy(), autoBases, baseDir);
  }

  public ContentRevision getNewContentRevision() {
    if (FilePatchStatus.DELETED.equals(myStatus)) return null;

    if (myNewContentRevision == null) {
      myConflicts = null;
      if (FilePatchStatus.ADDED.equals(myStatus)) {
        final FilePath newFilePath = VcsUtil.getFilePath(myIoCurrentBase, false);
        final String content = myPatch.getNewFileText();
        myNewContentRevision = new SimpleContentRevision(content, newFilePath, myPatch.getAfterVersionId());
      }
      else {
        final FilePath newFilePath = detectNewFilePathForMovedOrModified();
        myNewContentRevision = new LazyPatchContentRevision(myCurrentBase, newFilePath, myPatch.getAfterVersionId(), myPatch);
        if (myCurrentBase != null) {
          ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            public void run() {
              ((LazyPatchContentRevision)myNewContentRevision).getContent();
            }
          });
        }
      }
    }
    return myNewContentRevision;
  }

  @Nonnull
  @Override
  public DiffRequestProducer getDiffRequestProducers(final Project project, final PatchReader patchReader) {
    final PatchChange change = getChange();
    final FilePatch patch = getPatch();
    final String path = patch.getBeforeName() == null ? patch.getAfterName() : patch.getBeforeName();
    final Getter<CharSequence> baseContentGetter = new Getter<CharSequence>() {
      @Override
      public CharSequence get() {
        return patchReader.getBaseRevision(project, path);
      }
    };
    return new DiffRequestProducer() {
      @Nonnull
      @Override
      public DiffRequest process(@Nonnull UserDataHolder context, @Nonnull ProgressIndicator indicator)
              throws DiffRequestProducerException, ProcessCanceledException {
        if (myCurrentBase != null && myCurrentBase.getFileType() == UnknownFileType.INSTANCE) {
          return new UnknownFileTypeDiffRequest(myCurrentBase, getName());
        }

        if (isConflictingChange()) {
          final VirtualFile file = getCurrentBase();

          Getter<ApplyPatchForBaseRevisionTexts> getter = new Getter<ApplyPatchForBaseRevisionTexts>() {
            @Override
            public ApplyPatchForBaseRevisionTexts get() {
              return ApplyPatchForBaseRevisionTexts.create(project, file, VcsUtil.getFilePath(file), getPatch(), baseContentGetter);
            }
          };

          String afterTitle = getPatch().getAfterVersionId();
          if (afterTitle == null) afterTitle = "Patched Version";
          return PatchDiffRequestFactory.createConflictDiffRequest(project, file, getPatch(), afterTitle, getter, getName(), context, indicator);
        }
        else {
          return PatchDiffRequestFactory.createDiffRequest(project, change, getName(), context, indicator);
        }
      }

      @Nonnull
      @Override
      public String getName() {
        final File ioCurrentBase = getIoCurrentBase();
        return ioCurrentBase == null ? getCurrentPath() : ioCurrentBase.getPath();
      }
    };
  }
}