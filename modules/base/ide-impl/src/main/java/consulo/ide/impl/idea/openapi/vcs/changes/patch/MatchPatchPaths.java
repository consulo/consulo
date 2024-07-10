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
package consulo.ide.impl.idea.openapi.vcs.changes.patch;

import consulo.application.util.function.Computable;
import consulo.ide.impl.idea.openapi.diff.impl.patch.FilePatch;
import consulo.ide.impl.idea.openapi.diff.impl.patch.TextFilePatch;
import consulo.ide.impl.idea.openapi.diff.impl.patch.apply.GenericPatchApplier;
import consulo.ide.impl.idea.openapi.vcs.changes.shelf.ShelveChangesManager;
import consulo.ide.impl.idea.openapi.vcs.changes.shelf.ShelvedBinaryFilePatch;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.project.Project;
import consulo.util.collection.MultiMap;
import consulo.util.io.FileUtil;
import consulo.util.lang.Pair;
import consulo.versionControlSystem.util.ObjectsConvertor;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static consulo.ide.impl.idea.openapi.vcs.changes.patch.AutoMatchStrategy.processStipUp;

public class MatchPatchPaths {
  private static final int BIG_FILE_BOUND = 100000;
  private final Project myProject;
  private final VirtualFile myBaseDir;
  private boolean myUseProjectRootAsPredefinedBase;

  public MatchPatchPaths(Project project) {
    myProject = project;
    myBaseDir = myProject.getBaseDir();
  }

  public List<AbstractFilePatchInProgress> execute(@Nonnull final List<? extends FilePatch> list) {
    return execute(list, false);
  }

  /**
   * Find the best matched bases for file patches; e.g. Unshelve has to use project dir as best base by default,
   * while Apply patch should process through context, because it may have been created outside IDE for a certain vcs root
   *
   * @param list
   * @param useProjectRootAsPredefinedBase if true then we use project dir as default base despite context matching
   * @return
   */
  public List<AbstractFilePatchInProgress> execute(@Nonnull final List<? extends FilePatch> list, boolean useProjectRootAsPredefinedBase) {
    final PatchBaseDirectoryDetector directoryDetector = PatchBaseDirectoryDetector.getInstance(myProject);

    myUseProjectRootAsPredefinedBase = useProjectRootAsPredefinedBase;
    final List<PatchAndVariants> candidates = new ArrayList<>(list.size());
    final List<FilePatch> newOrWithoutMatches = new ArrayList<>();
    findCandidates(list, directoryDetector, candidates, newOrWithoutMatches);

    final MultiMap<VirtualFile, AbstractFilePatchInProgress> result = new MultiMap<>();
    // process exact matches: if one, leave and extract. if several - leave only them
    filterExactMatches(candidates, result);

    // partially check by context
    selectByContextOrByStrip(candidates, result); // for text only
    // created or no variants
    workWithNotExisting(directoryDetector, newOrWithoutMatches, result);
    return new ArrayList<>(result.values());
  }

  private void workWithNotExisting(@Nonnull PatchBaseDirectoryDetector directoryDetector,
                                   @Nonnull List<FilePatch> newOrWithoutMatches,
                                   @Nonnull MultiMap<VirtualFile, AbstractFilePatchInProgress> result) {
    for (FilePatch patch : newOrWithoutMatches) {
      String afterName = patch.getAfterName();
      final String[] strings = afterName != null ? afterName.replace('\\', '/').split("/") : ArrayUtil.EMPTY_STRING_ARRAY;
      Pair<VirtualFile, Integer> best = null;
      for (int i = strings.length - 2; i >= 0; --i) {
        final String name = strings[i];
        final Collection<VirtualFile> files = findFilesFromIndex(directoryDetector, name);
        if (!files.isEmpty()) {
          // check all candidates
          for (VirtualFile file : files) {
            Pair<VirtualFile, Integer> pair = compareNamesImpl(strings, file, i);
            if (pair != null && pair.getSecond() < i) {
              if (best == null || pair.getSecond() < best.getSecond() || isGoodAndProjectBased(best, pair)) {
                best = pair;
              }
            }
          }
        }
      }
      if (best != null) {
        final AbstractFilePatchInProgress patchInProgress = createPatchInProgress(patch, best.getFirst());
        if (patchInProgress == null) break;
        processStipUp(patchInProgress, best.getSecond());
        result.putValue(best.getFirst(), patchInProgress);
      }
      else {
        final AbstractFilePatchInProgress patchInProgress = createPatchInProgress(patch, myBaseDir);
        if (patchInProgress == null) break;
        result.putValue(myBaseDir, patchInProgress);
      }
    }
  }

  private boolean isGoodAndProjectBased(@Nonnull Pair<VirtualFile, Integer> bestVariant,
                                        @Nonnull Pair<VirtualFile, Integer> currentVariant) {
    return currentVariant.getSecond().equals(bestVariant.getSecond()) && myBaseDir.equals(currentVariant.getFirst());
  }

  private static void selectByContextOrByStrip(@Nonnull List<PatchAndVariants> candidates,
                                               @Nonnull MultiMap<VirtualFile, AbstractFilePatchInProgress> result) {
    for (final PatchAndVariants candidate : candidates) {
      candidate.findAndAddBestVariant(result);
    }
  }

  private static void filterExactMatches(@Nonnull List<PatchAndVariants> candidates,
                                         @Nonnull MultiMap<VirtualFile, AbstractFilePatchInProgress> result) {
    for (Iterator<PatchAndVariants> iterator = candidates.iterator(); iterator.hasNext(); ) {
      final PatchAndVariants candidate = iterator.next();
      if (candidate.getVariants().size() == 1) {
        final AbstractFilePatchInProgress oneCandidate = candidate.getVariants().get(0);
        result.putValue(oneCandidate.getBase(), oneCandidate);
        iterator.remove();
      }
      else {
        final List<AbstractFilePatchInProgress> exact = new ArrayList<>(candidate.getVariants().size());
        for (AbstractFilePatchInProgress patch : candidate.getVariants()) {
          if (patch.getCurrentStrip() == 0) {
            exact.add(patch);
          }
        }
        if (exact.size() == 1) {
          final AbstractFilePatchInProgress patchInProgress = exact.get(0);
          putSelected(result, candidate.getVariants(), patchInProgress);
          iterator.remove();
        }
        else if (!exact.isEmpty()) {
          candidate.getVariants().retainAll(exact);
        }
      }
    }
  }

  private void findCandidates(@Nonnull List<? extends FilePatch> list,
                              @Nonnull final PatchBaseDirectoryDetector directoryDetector,
                              @Nonnull List<PatchAndVariants> candidates, @Nonnull List<FilePatch> newOrWithoutMatches) {
    for (final FilePatch patch : list) {
      final String fileName = patch.getBeforeFileName();
      if (patch.isNewFile() || (patch.getBeforeName() == null)) {
        newOrWithoutMatches.add(patch);
        continue;
      }
      final Collection<VirtualFile> files = new ArrayList<>(findFilesFromIndex(directoryDetector, fileName));
      // for directories outside the project scope but under version control
      if (patch.getBeforeName() != null && patch.getBeforeName().startsWith("..")) {
        final VirtualFile relativeFile = VfsUtil.findRelativeFile(myBaseDir, patch.getBeforeName().replace('\\', '/').split("/"));
        if (relativeFile != null) {
          files.add(relativeFile);
        }
      }
      if (files.isEmpty()) {
        newOrWithoutMatches.add(patch);
      }
      else {
        //files order is not defined, so get the best variant depends on it, too
        final List<AbstractFilePatchInProgress> variants =
          ObjectsConvertor.convert(files, o -> processMatch(patch, o), ObjectsConvertor.NOT_NULL);
        if (variants.isEmpty()) {
          newOrWithoutMatches.add(patch); // just to be sure
        }
        else {
          candidates.add(new PatchAndVariants(variants));
        }
      }
    }
  }

  private Collection<VirtualFile> findFilesFromIndex(
    @Nonnull final PatchBaseDirectoryDetector directoryDetector,
    @Nonnull final String fileName
  ) {
    Collection<VirtualFile> files = myProject.getApplication()
      .runReadAction((Computable<Collection<VirtualFile>>)() -> directoryDetector.findFiles(fileName));
    final File shelfResourcesDirectory = ShelveChangesManager.getInstance(myProject).getShelfResourcesDirectory();
    return ContainerUtil.filter(files, file -> !FileUtil.isAncestor(shelfResourcesDirectory, VfsUtilCore.virtualToIoFile(file), false));
  }

  private static void putSelected(
    @Nonnull MultiMap<VirtualFile, AbstractFilePatchInProgress> result,
    @Nonnull final List<AbstractFilePatchInProgress> variants,
    @Nonnull AbstractFilePatchInProgress patchInProgress
  ) {
    patchInProgress.setAutoBases(ObjectsConvertor.convert(variants, AbstractFilePatchInProgress::getBase, ObjectsConvertor.NOT_NULL));
    result.putValue(patchInProgress.getBase(), patchInProgress);
  }

  private static int getMatchingLines(final AbstractFilePatchInProgress<TextFilePatch> patch) {
    final VirtualFile base = patch.getCurrentBase();
    if (base == null) return -1;
    String text;
    try {
      if (base.getLength() > BIG_FILE_BOUND) {
        // partially
        text = VfsUtilCore.loadText(base, BIG_FILE_BOUND);
      }
      else {
        text = VfsUtilCore.loadText(base);
      }
    }
    catch (IOException e) {
      return 0;
    }
    return new GenericPatchApplier(text, patch.getPatch().getHunks()).weightContextMatch(100, 5);
  }

  private class PatchAndVariants {
    @Nonnull
    private final List<AbstractFilePatchInProgress> myVariants;

    private PatchAndVariants(@Nonnull List<AbstractFilePatchInProgress> variants) {
      myVariants = variants;
    }

    @Nonnull
    public List<AbstractFilePatchInProgress> getVariants() {
      return myVariants;
    }

    public void findAndAddBestVariant(@Nonnull MultiMap<VirtualFile, AbstractFilePatchInProgress> result) {
      AbstractFilePatchInProgress best = ContainerUtil.getFirstItem(myVariants);
      if (best == null) return;
      if (best instanceof TextFilePatchInProgress) {
        //only for text patches
        int maxLines = -100;
        for (AbstractFilePatchInProgress variant : myVariants) {
          TextFilePatchInProgress textFilePAch = (TextFilePatchInProgress)variant;
          if (myUseProjectRootAsPredefinedBase && variantMatchedToProjectDir(textFilePAch)) {
            best = textFilePAch;
            break;
          }
          final int lines = getMatchingLines(textFilePAch);
          if (lines > maxLines) {
            maxLines = lines;
            best = textFilePAch;
          }
        }
        putSelected(result, myVariants, best);
      }
      else {
        int stripCounter = Integer.MAX_VALUE;
        for (AbstractFilePatchInProgress variant : myVariants) {
          int currentStrip = variant.getCurrentStrip();
          //the best variant if several match should be project based variant
          if (variantMatchedToProjectDir(variant)) {
            best = variant;
            break;
          }
          else if (currentStrip < stripCounter) {
            best = variant;
            stripCounter = currentStrip;
          }
        }
        putSelected(result, myVariants, best);
      }
    }
  }

  private boolean variantMatchedToProjectDir(@Nonnull AbstractFilePatchInProgress variant) {
    return variant.getCurrentStrip() == 0 && myProject.getBaseDir().equals(variant.getBase());
  }

  private static Pair<VirtualFile, Integer> compareNames(final String beforeName, final VirtualFile file) {
    if (beforeName == null) return null;
    final String[] parts = beforeName.replace('\\', '/').split("/");
    return compareNamesImpl(parts, file.getParent(), parts.length - 2);
  }

  private static Pair<VirtualFile, Integer> compareNamesImpl(String[] parts, VirtualFile parent, int idx) {
    while ((parent != null) && (idx >= 0)) {
      if (!parent.getName().equals(parts[idx])) {
        return new Pair<>(parent, idx + 1);
      }
      parent = parent.getParent();
      --idx;
    }
    return new Pair<>(parent, idx + 1);
  }

  @Nullable
  private static AbstractFilePatchInProgress processMatch(final FilePatch patch, final VirtualFile file) {
    final String beforeName = patch.getBeforeName();
    final Pair<VirtualFile, Integer> pair = compareNames(beforeName, file);
    if (pair == null) return null;
    final VirtualFile parent = pair.getFirst();
    if (parent == null) return null;
    final AbstractFilePatchInProgress result = createPatchInProgress(patch, parent);
    if (result != null) {
      processStipUp(result, pair.getSecond());
    }
    return result;
  }

  @Nullable
  private static AbstractFilePatchInProgress createPatchInProgress(@Nonnull FilePatch patch, @Nonnull VirtualFile dir) {
    return patch instanceof TextFilePatch textFilePatch ? new TextFilePatchInProgress(textFilePatch, null, dir)
      : patch instanceof ShelvedBinaryFilePatch binaryFilePatch ? new BinaryFilePatchInProgress(binaryFilePatch, null, dir)
      : null;
  }
}
