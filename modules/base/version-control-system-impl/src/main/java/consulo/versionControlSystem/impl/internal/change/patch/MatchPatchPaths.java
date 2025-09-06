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
package consulo.versionControlSystem.impl.internal.change.patch;

import consulo.application.util.function.Computable;
import consulo.project.Project;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.MultiMap;
import consulo.util.io.FileUtil;
import consulo.util.lang.Pair;
import consulo.versionControlSystem.change.patch.FilePatch;
import consulo.versionControlSystem.change.patch.TextFilePatch;
import consulo.versionControlSystem.impl.internal.change.shelf.ShelveChangesManagerImpl;
import consulo.versionControlSystem.impl.internal.change.shelf.ShelvedBinaryFilePatch;
import consulo.versionControlSystem.impl.internal.patch.apply.GenericPatchApplier;
import consulo.versionControlSystem.util.ObjectsConvertor;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.internal.LoadTextUtil;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static consulo.versionControlSystem.impl.internal.change.patch.AutoMatchStrategy.processStipUp;

public class MatchPatchPaths {
    private static final int BIG_FILE_BOUND = 100000;
    private final Project myProject;
    private final VirtualFile myBaseDir;
    private boolean myUseProjectRootAsPredefinedBase;

    public MatchPatchPaths(Project project) {
        myProject = project;
        myBaseDir = myProject.getBaseDir();
    }

    public List<AbstractFilePatchInProgress> execute(@Nonnull List<? extends FilePatch> list) {
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
    public List<AbstractFilePatchInProgress> execute(@Nonnull List<? extends FilePatch> list, boolean useProjectRootAsPredefinedBase) {
        PatchBaseDirectoryDetector directoryDetector = PatchBaseDirectoryDetector.getInstance(myProject);

        myUseProjectRootAsPredefinedBase = useProjectRootAsPredefinedBase;
        List<PatchAndVariants> candidates = new ArrayList<>(list.size());
        List<FilePatch> newOrWithoutMatches = new ArrayList<>();
        findCandidates(list, directoryDetector, candidates, newOrWithoutMatches);

        MultiMap<VirtualFile, AbstractFilePatchInProgress> result = new MultiMap<>();
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
            String[] strings = afterName != null ? afterName.replace('\\', '/').split("/") : ArrayUtil.EMPTY_STRING_ARRAY;
            Pair<VirtualFile, Integer> best = null;
            for (int i = strings.length - 2; i >= 0; --i) {
                String name = strings[i];
                Collection<VirtualFile> files = findFilesFromIndex(directoryDetector, name);
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
                AbstractFilePatchInProgress patchInProgress = createPatchInProgress(patch, best.getFirst());
                if (patchInProgress == null) {
                    break;
                }
                processStipUp(patchInProgress, best.getSecond());
                result.putValue(best.getFirst(), patchInProgress);
            }
            else {
                AbstractFilePatchInProgress patchInProgress = createPatchInProgress(patch, myBaseDir);
                if (patchInProgress == null) {
                    break;
                }
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
        for (PatchAndVariants candidate : candidates) {
            candidate.findAndAddBestVariant(result);
        }
    }

    private static void filterExactMatches(@Nonnull List<PatchAndVariants> candidates,
                                           @Nonnull MultiMap<VirtualFile, AbstractFilePatchInProgress> result) {
        for (Iterator<PatchAndVariants> iterator = candidates.iterator(); iterator.hasNext(); ) {
            PatchAndVariants candidate = iterator.next();
            if (candidate.getVariants().size() == 1) {
                AbstractFilePatchInProgress oneCandidate = candidate.getVariants().get(0);
                result.putValue(oneCandidate.getBase(), oneCandidate);
                iterator.remove();
            }
            else {
                List<AbstractFilePatchInProgress> exact = new ArrayList<>(candidate.getVariants().size());
                for (AbstractFilePatchInProgress patch : candidate.getVariants()) {
                    if (patch.getCurrentStrip() == 0) {
                        exact.add(patch);
                    }
                }
                if (exact.size() == 1) {
                    AbstractFilePatchInProgress patchInProgress = exact.get(0);
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
                                @Nonnull PatchBaseDirectoryDetector directoryDetector,
                                @Nonnull List<PatchAndVariants> candidates, @Nonnull List<FilePatch> newOrWithoutMatches) {
        for (FilePatch patch : list) {
            String fileName = patch.getBeforeFileName();
            if (patch.isNewFile() || (patch.getBeforeName() == null)) {
                newOrWithoutMatches.add(patch);
                continue;
            }
            Collection<VirtualFile> files = new ArrayList<>(findFilesFromIndex(directoryDetector, fileName));
            // for directories outside the project scope but under version control
            if (patch.getBeforeName() != null && patch.getBeforeName().startsWith("..")) {
                VirtualFile relativeFile = VirtualFileUtil.findRelativeFile(myBaseDir, patch.getBeforeName().replace('\\', '/').split("/"));
                if (relativeFile != null) {
                    files.add(relativeFile);
                }
            }
            if (files.isEmpty()) {
                newOrWithoutMatches.add(patch);
            }
            else {
                //files order is not defined, so get the best variant depends on it, too
                List<AbstractFilePatchInProgress> variants =
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
        @Nonnull PatchBaseDirectoryDetector directoryDetector,
        @Nonnull String fileName
    ) {
        Collection<VirtualFile> files = myProject.getApplication()
            .runReadAction((Computable<Collection<VirtualFile>>) () -> directoryDetector.findFiles(fileName));
        File shelfResourcesDirectory = ShelveChangesManagerImpl.getInstance(myProject).getShelfResourcesDirectory();
        return ContainerUtil.filter(files, file -> !FileUtil.isAncestor(shelfResourcesDirectory, VirtualFileUtil.virtualToIoFile(file), false));
    }

    private static void putSelected(
        @Nonnull MultiMap<VirtualFile, AbstractFilePatchInProgress> result,
        @Nonnull List<AbstractFilePatchInProgress> variants,
        @Nonnull AbstractFilePatchInProgress patchInProgress
    ) {
        patchInProgress.setAutoBases(ObjectsConvertor.convert(variants, AbstractFilePatchInProgress::getBase, ObjectsConvertor.NOT_NULL));
        result.putValue(patchInProgress.getBase(), patchInProgress);
    }

    private static int getMatchingLines(AbstractFilePatchInProgress<TextFilePatch> patch) {
        VirtualFile base = patch.getCurrentBase();
        if (base == null) {
            return -1;
        }
        String text;
        if (base.getLength() > BIG_FILE_BOUND) {
            // partially
            text = LoadTextUtil.loadText(base, BIG_FILE_BOUND).toString();
        }
        else {
            text = LoadTextUtil.loadText(base).toString();
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
            if (best == null) {
                return;
            }
            if (best instanceof TextFilePatchInProgress) {
                //only for text patches
                int maxLines = -100;
                for (AbstractFilePatchInProgress variant : myVariants) {
                    TextFilePatchInProgress textFilePAch = (TextFilePatchInProgress) variant;
                    if (myUseProjectRootAsPredefinedBase && variantMatchedToProjectDir(textFilePAch)) {
                        best = textFilePAch;
                        break;
                    }
                    int lines = getMatchingLines(textFilePAch);
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

    private static Pair<VirtualFile, Integer> compareNames(String beforeName, VirtualFile file) {
        if (beforeName == null) {
            return null;
        }
        String[] parts = beforeName.replace('\\', '/').split("/");
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
    private static AbstractFilePatchInProgress processMatch(FilePatch patch, VirtualFile file) {
        String beforeName = patch.getBeforeName();
        Pair<VirtualFile, Integer> pair = compareNames(beforeName, file);
        if (pair == null) {
            return null;
        }
        VirtualFile parent = pair.getFirst();
        if (parent == null) {
            return null;
        }
        AbstractFilePatchInProgress result = createPatchInProgress(patch, parent);
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
