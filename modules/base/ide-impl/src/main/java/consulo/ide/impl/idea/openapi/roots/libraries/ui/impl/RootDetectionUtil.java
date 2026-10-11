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
package consulo.ide.impl.idea.openapi.roots.libraries.ui.impl;

import consulo.application.Application;
import consulo.application.progress.ProgressBuilderFactory;
import consulo.application.progress.ProgressIndicator;
import consulo.component.ProcessCanceledException;
import consulo.content.OrderRootType;
import consulo.content.library.LibraryRootType;
import consulo.content.library.OrderRoot;
import consulo.content.library.ui.DetectedLibraryRoot;
import consulo.content.library.ui.LibraryRootsComponentDescriptor;
import consulo.content.library.ui.LibraryRootsDetector;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.ChooseItemsDialogDescriptor;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author nik
 */
public class RootDetectionUtil {
    private static final Logger LOG = Logger.getInstance(RootDetectionUtil.class);

    private RootDetectionUtil() {
    }

    @RequiredUIAccess
    public static CompletableFuture<List<OrderRoot>> detectRootsAsync(
        Collection<VirtualFile> rootCandidates,
        consulo.ui.@Nullable Component parentComponent,
        @Nullable Project project,
        LibraryRootsComponentDescriptor rootsComponentDescriptor
    ) {
        return detectRootsAsync(
            rootCandidates,
            parentComponent,
            project,
            rootsComponentDescriptor.getRootsDetector(),
            rootsComponentDescriptor.getRootTypes()
        );
    }

    @RequiredUIAccess
    public static CompletableFuture<List<OrderRoot>> detectRootsAsync(
        Collection<VirtualFile> rootCandidates,
        consulo.ui.@Nullable Component parentComponent,
        @Nullable Project project,
        LibraryRootsDetector detector,
        List<OrderRootType> rootTypesAllowedToBeSelectedByUserIfNothingIsDetected
    ) {
        List<OrderRoot> detected = new ArrayList<>();
        List<SuggestedChildRootInfo> suggestedRoots = new ArrayList<>();

        UIAccess uiAccess = UIAccess.current();
        CompletableFuture<?> scanned = Application.get().getInstance(ProgressBuilderFactory.class)
            .newProgressBuilder(project, ProjectLocalize.libraryScanningRootsProgress())
            .cancelable()
            .modal()
            .execute(uiAccess, () -> Coroutine.first(CodeExecution.run(continuation -> {
                scan(rootCandidates, detector, ProgressIndicator.from(continuation), detected, suggestedRoots);
            })));

        return scanned
            .thenComposeAsync(ignored -> chooseSuggestedRoots(parentComponent, suggestedRoots), uiAccess)
            .thenComposeAsync(chosen -> {
                List<OrderRoot> result = new ArrayList<>(detected);
                result.addAll(chosen);
                if (!result.isEmpty() || rootTypesAllowedToBeSelectedByUserIfNothingIsDetected.isEmpty()) {
                    return CompletableFuture.completedFuture(result);
                }
                return chooseRootTypes(parentComponent, rootCandidates, detector, rootTypesAllowedToBeSelectedByUserIfNothingIsDetected);
            }, uiAccess);
    }

    @RequiredUIAccess
    private static CompletableFuture<List<OrderRoot>> chooseSuggestedRoots(
        consulo.ui.@Nullable Component parentComponent,
        List<SuggestedChildRootInfo> suggestedRoots
    ) {
        if (suggestedRoots.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        return DetectedRootsDialogDescriptor.showAsync(parentComponent, suggestedRoots).thenApply(chosen -> {
            List<OrderRoot> roots = new ArrayList<>();
            for (SuggestedChildRootInfo rootInfo : chosen) {
                LibraryRootType type = rootInfo.getSelectedRootType();
                roots.add(new OrderRoot(rootInfo.getDetectedRoot().getFile(), type.getType(), type.isJarDirectory()));
            }
            return roots;
        });
    }

    @RequiredUIAccess
    private static CompletableFuture<List<OrderRoot>> chooseRootTypes(
        consulo.ui.@Nullable Component parentComponent,
        Collection<VirtualFile> rootCandidates,
        LibraryRootsDetector detector,
        List<OrderRootType> rootTypes
    ) {
        Map<String, Pair<OrderRootType, Boolean>> types = collectRootTypeNames(detector, rootTypes);
        if (types.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        List<String> names = new ArrayList<>(types.keySet());
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return ChooseItemsDialogDescriptor.showAsync(
            parentComponent,
            ProjectLocalize.libraryRootCategoriesTitle(),
            ProjectLocalize.libraryRootCategoriesDescription(Application.get().getName()),
            names,
            (name, presentation) -> presentation.append(name),
            true,
            List.of()
        ).thenApply(chosen -> {
            List<OrderRoot> roots = new ArrayList<>();
            for (String rootType : chosen) {
                Pair<OrderRootType, Boolean> pair = types.get(rootType);
                for (VirtualFile candidate : rootCandidates) {
                    roots.add(new OrderRoot(candidate, pair.getFirst().getId(), pair.getSecond()));
                }
            }
            return roots;
        });
    }

    private static Map<String, Pair<OrderRootType, Boolean>> collectRootTypeNames(LibraryRootsDetector detector, List<OrderRootType> rootTypes) {
        Map<String, Pair<OrderRootType, Boolean>> types = new HashMap<>();
        for (OrderRootType type : rootTypes) {
            for (boolean isJarDirectory : new boolean[]{false, true}) {
                String typeName = detector.getRootTypeName(new LibraryRootType(type.getId(), isJarDirectory));
                if (typeName != null) {
                    types.put(typeName, Pair.create(type, isJarDirectory));
                }
            }
        }
        return types;
    }

    private static void scan(
        Collection<VirtualFile> rootCandidates,
        LibraryRootsDetector detector,
        ProgressIndicator indicator,
        List<OrderRoot> result,
        List<SuggestedChildRootInfo> suggestedRoots
    ) {
        try {
            for (VirtualFile rootCandidate : rootCandidates) {
                Collection<DetectedLibraryRoot> roots = detector.detectRoots(rootCandidate, indicator);
                if (!roots.isEmpty() && allRootsHaveOneTypeAndEqualTo(roots, rootCandidate)) {
                    for (DetectedLibraryRoot root : roots) {
                        LibraryRootType libraryRootType = root.getTypes().get(0);
                        result.add(new OrderRoot(root.getFile(), libraryRootType.getType(), libraryRootType.isJarDirectory()));
                    }
                }
                else {
                    for (DetectedLibraryRoot root : roots) {
                        Map<LibraryRootType, String> names = new HashMap<>();
                        for (LibraryRootType type : root.getTypes()) {
                            String typeName = detector.getRootTypeName(type);
                            LOG.assertTrue(
                                typeName != null,
                                "Unexpected root type " + type.getType() +
                                    (type.isJarDirectory() ? " (jar directory)" : "") + ", " +
                                    "detectors: " + detector
                            );
                            names.put(type, typeName);
                        }
                        suggestedRoots.add(new SuggestedChildRootInfo(rootCandidate, root, names));
                    }
                }
            }
        }
        catch (ProcessCanceledException ignored) {
        }
    }

    private static boolean allRootsHaveOneTypeAndEqualTo(Collection<DetectedLibraryRoot> roots, VirtualFile candidate) {
        for (DetectedLibraryRoot root : roots) {
            if (root.getTypes().size() > 1 || !root.getFile().equals(candidate)) {
                return false;
            }
        }
        return true;
    }
}
