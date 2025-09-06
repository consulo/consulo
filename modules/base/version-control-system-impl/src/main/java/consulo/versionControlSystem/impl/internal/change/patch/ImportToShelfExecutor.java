/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import consulo.application.progress.ProgressManager;
import consulo.application.util.function.ThrowableComputable;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationType;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.MultiMap;
import consulo.util.io.FileUtil;
import consulo.util.io.PathUtil;
import consulo.versionControlSystem.AbstractVcsHelper;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.change.CommitContext;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.versionControlSystem.change.patch.FilePatch;
import consulo.versionControlSystem.change.patch.PatchEP;
import consulo.versionControlSystem.change.patch.TextFilePatch;
import consulo.versionControlSystem.change.shelf.ShelvedChangesViewManager;
import consulo.versionControlSystem.impl.internal.change.shelf.ShelveChangesManagerImpl;
import consulo.versionControlSystem.impl.internal.change.shelf.ShelvedChangeListImpl;
import consulo.versionControlSystem.impl.internal.patch.PatchSyntaxException;
import consulo.versionControlSystem.impl.internal.util.VcsCatchingRunnable;
import consulo.versionControlSystem.ui.VcsBalloonProblemNotifier;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ImportToShelfExecutor implements ApplyPatchExecutor<TextFilePatchInProgress> {
  private static final Logger LOG = Logger.getInstance(ImportToShelfExecutor.class);

  private static final String IMPORT_TO_SHELF = "Import to Shelf";
  private final Project myProject;

  public ImportToShelfExecutor(Project project) {
    myProject = project;
  }

  @Override
  public String getName() {
    return IMPORT_TO_SHELF;
  }

  @Override
  public void apply(
    @Nonnull List<FilePatch> remaining,
    @Nonnull final MultiMap<VirtualFile, TextFilePatchInProgress> patchGroupsToApply,
    @Nullable LocalChangeList localList,
    @Nullable final String fileName,
    @Nullable ThrowableComputable<Map<String, Map<String, CharSequence>>, PatchSyntaxException> additionalInfo
  ) {
    if (fileName == null) {
      LOG.error("Patch file name shouldn't be null");
      return;
    }
    VcsCatchingRunnable vcsCatchingRunnable = new VcsCatchingRunnable() {
      @Override
      public void runImpl() throws VcsException {
        VirtualFile baseDir = myProject.getBaseDir();
        File ioBase = new File(baseDir.getPath());
        List<FilePatch> allPatches = new ArrayList<>();
        for (VirtualFile virtualFile : patchGroupsToApply.keySet()) {
          File ioCurrentBase = new File(virtualFile.getPath());
          allPatches.addAll(ContainerUtil.map(patchGroupsToApply.get(virtualFile), patchInProgress -> {
            TextFilePatch was = patchInProgress.getPatch();
            was.setBeforeName(PathUtil.toSystemIndependentName(FileUtil.getRelativePath(ioBase, new File(ioCurrentBase, was.getBeforeName()))));
            was.setAfterName(PathUtil.toSystemIndependentName(FileUtil.getRelativePath(ioBase, new File(ioCurrentBase, was.getAfterName()))));
            return was;
          }));
        }
        if (!allPatches.isEmpty()) {
          PatchEP[] patchTransitExtensions = null;
          if (additionalInfo != null) {
            try {
              Map<String, PatchEP> extensions = new HashMap<>();
              for (Map.Entry<String, Map<String, CharSequence>> entry : additionalInfo.compute().entrySet()) {
                String filePath = entry.getKey();
                Map<String, CharSequence> extToValue = entry.getValue();
                for (Map.Entry<String, CharSequence> innerEntry : extToValue.entrySet()) {
                  TransitExtension patchEP = (TransitExtension)extensions.get(innerEntry.getKey());
                  if (patchEP == null) {
                    patchEP = new TransitExtension(innerEntry.getKey());
                    extensions.put(innerEntry.getKey(), patchEP);
                  }
                  patchEP.put(filePath, innerEntry.getValue());
                }
              }
              Collection<PatchEP> values = extensions.values();
              patchTransitExtensions = values.toArray(new PatchEP[values.size()]);
            }
            catch (PatchSyntaxException e) {
              VcsBalloonProblemNotifier.showOverChangesView(myProject, "Can not import additional patch info: " + e.getMessage(), NotificationType.ERROR);
            }
          }
          try {
            ShelvedChangeListImpl shelvedChangeList = ShelveChangesManagerImpl.getInstance(myProject).
                    importFilePatches(fileName, allPatches, patchTransitExtensions);
            ShelvedChangesViewManager.getInstance(myProject).activateView(shelvedChangeList);
          }
          catch (IOException e) {
            throw new VcsException(e);
          }
        }
      }
    };
    ProgressManager.getInstance().runProcessWithProgressSynchronously(vcsCatchingRunnable, "Import Patch to Shelf", true, myProject);
    if (!vcsCatchingRunnable.get().isEmpty()) {
      AbstractVcsHelper.getInstance(myProject).showErrors(vcsCatchingRunnable.get(), IMPORT_TO_SHELF);
    }
  }

  private static class TransitExtension implements PatchEP {
    private final String myName;
    private final Map<String, CharSequence> myMap;

    private TransitExtension(String name) {
      myName = name;
      myMap = new HashMap<>();
    }

    @Nonnull
    @Override
    public String getName() {
      return myName;
    }

    @Override
    public CharSequence provideContent(@Nonnull String path, CommitContext commitContext) {
      return myMap.get(path);
    }

    @Override
    public void consumeContent(@Nonnull String path, @Nonnull CharSequence content, CommitContext commitContext) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void consumeContentBeforePatchApplied(@Nonnull String path, @Nonnull CharSequence content, CommitContext commitContext) {
      throw new UnsupportedOperationException();
    }

    public void put(String fileName, CharSequence value) {
      myMap.put(fileName, value);
    }
  }
}
