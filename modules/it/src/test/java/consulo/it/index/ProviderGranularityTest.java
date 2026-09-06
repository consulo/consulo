/*
 * Copyright 2013-2026 consulo.io
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
package consulo.it.index;

import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.WriteAction;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.library.Library;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.it.AllowLogError;
import consulo.it.HeadlessApplicationExtension;
import consulo.it.index.ScanningTestSupport.RecordedScans;
import consulo.language.index.impl.internal.FileBasedIndexImpl;
import consulo.language.index.impl.internal.roots.IndexableFilesIterator;
import consulo.language.index.impl.internal.roots.kind.IndexableSetOrigin;
import consulo.language.index.impl.internal.roots.kind.LibraryOrigin;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.virtualFileSystem.VirtualFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static consulo.it.index.ScanningTestSupport.addContentRoot;
import static consulo.it.index.ScanningTestSupport.awaitIdle;
import static consulo.it.index.ScanningTestSupport.awaitSmart;
import static consulo.it.index.ScanningTestSupport.createSandFiles;
import static consulo.it.index.ScanningTestSupport.findFile;
import static consulo.it.index.ScanningTestSupport.openProject;
import static consulo.it.index.ScanningTestSupport.recordScans;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The indexable files providers are now one per root instead of one per entity:
 * {@code LibraryIndexableFilesIteratorImpl.createIterators} emits an iterator per library root, each snapshotting that
 * single root, and the origins are keyed on the entity together with its roots, so that a library reached more than
 * once still contributes each of its roots exactly once.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessApplicationExtension.class)
@AllowLogError({
    "consulo.virtualFileSystem.internal.BaseVirtualFileManager",
    "consulo.application.impl.internal.BaseApplication",
    "consulo.ui.ex.impl.internal.action.ActionManagerImpl"
})
public class ProviderGranularityTest {
    private static final int FILES = 3;

    @Test
    public void moduleLibraryWithTwoRootsIsScannedByOneIteratorPerRoot(Application application, ProjectManager projectManager)
        throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-granularity-module");
        Path firstRoot = createSandFiles(Files.createTempDirectory("consulo-it-granularity-first"), FILES, "GranFirst");
        Path secondRoot = createSandFiles(Files.createTempDirectory("consulo-it-granularity-second"), FILES, "GranSecond");

        Project project = openProject(application, projectManager, directory);
        awaitSmart(DumbService.getInstance(project));
        awaitIdle(project);
        Module module = addContentRoot(project, directory);
        awaitIdle(project);

        VirtualFile firstRootFile = findFile(firstRoot);
        VirtualFile secondRootFile = findFile(secondRoot);

        Disposable disposable = Disposable.newDisposable();
        try {
            RecordedScans scans = recordScans(project, disposable);
            WriteAction.run(() -> {
                ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
                Library library = rootModel.getModuleLibraryTable().createLibrary("granularity-library");
                Library.ModifiableModel libraryModel = library.getModifiableModel();
                libraryModel.addRoot(firstRootFile, BinariesOrderRootType.ID);
                libraryModel.addRoot(secondRootFile, BinariesOrderRootType.ID);
                libraryModel.commit();
                rootModel.commit();
            });
            awaitIdle(project);

            List<LibraryOrigin> providers = libraryOrigins(project, "granularity-library");
            assertThat(providers)
                .as("a library with two roots must be scanned by one iterator per root")
                .hasSize(2);
            assertThat(providers.get(0))
                .as("the two per-root origins must not be equal")
                .isNotEqualTo(providers.get(1));
            assertThat(allClassRoots(providers))
                .as("each origin must carry exactly one root, and the two must cover the whole library")
                .containsExactlyInAnyOrder(firstRootFile, secondRootFile);
            for (LibraryOrigin origin : providers) {
                assertThat(origin.getClassRoots()).as("%s must snapshot a single root", origin).hasSize(1);
                assertThat(origin.getSourceRoots()).as("%s must carry no source root", origin).isEmpty();
            }

            for (IndexableSetOrigin origin : scans.allOrigins()) {
                if (origin instanceof LibraryOrigin libraryOrigin && "granularity-library".equals(libraryName(libraryOrigin))) {
                    assertThat(libraryOrigin.getClassRoots().size() + libraryOrigin.getSourceRoots().size())
                        .as("the scan of %s must be limited to a single root", libraryOrigin)
                        .isEqualTo(1);
                }
            }
        }
        finally {
            Disposer.dispose(disposable);
        }
    }

    /**
     * A library reachable from two places is deduplicated by its origins, so the origins of one library must be equal
     * across two independent collections and collapse into one entry per root when put in a set.
     * <p>
     * The two places cannot be two modules here: sharing one library between modules needs a project level library
     * table, whose implementation lives in {@code ide-impl} and is not bound in the headless application.
     */
    @Test
    public void perRootOriginsAreEqualAcrossCollections(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-granularity-shared");
        Path firstRoot = createSandFiles(Files.createTempDirectory("consulo-it-granularity-shared-first"), FILES, "SharedFirst");
        Path secondRoot = createSandFiles(Files.createTempDirectory("consulo-it-granularity-shared-second"), FILES, "SharedSecond");

        Project project = openProject(application, projectManager, directory);
        awaitSmart(DumbService.getInstance(project));
        awaitIdle(project);
        Module module = addContentRoot(project, directory);
        awaitIdle(project);

        VirtualFile firstRootFile = findFile(firstRoot);
        VirtualFile secondRootFile = findFile(secondRoot);

        WriteAction.run(() -> {
            ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
            Library library = rootModel.getModuleLibraryTable().createLibrary("shared-library");
            Library.ModifiableModel libraryModel = library.getModifiableModel();
            libraryModel.addRoot(firstRootFile, BinariesOrderRootType.ID);
            libraryModel.addRoot(secondRootFile, BinariesOrderRootType.ID);
            libraryModel.commit();
            rootModel.commit();
        });
        awaitIdle(project);

        List<LibraryOrigin> origins = libraryOrigins(project, "shared-library");
        assertThat(origins).as("the library must be scanned by one iterator per root").hasSize(2);
        assertThat(allClassRoots(origins)).containsExactlyInAnyOrder(firstRootFile, secondRootFile);
        assertThat(origins).doesNotHaveDuplicates();

        List<LibraryOrigin> collectedAgain = libraryOrigins(project, "shared-library");
        assertThat(collectedAgain)
            .as("origins keyed on the entity and its roots must be equal across two collections")
            .isEqualTo(origins);

        Set<IndexableSetOrigin> deduplicated = new LinkedHashSet<>(origins);
        deduplicated.addAll(collectedAgain);
        assertThat(deduplicated)
            .as("the same library seen twice must deduplicate into one origin per root")
            .hasSize(2);
    }

    private static List<LibraryOrigin> libraryOrigins(Project project, String libraryName) {
        FileBasedIndexImpl index = (FileBasedIndexImpl) FileBasedIndex.getInstance();
        List<LibraryOrigin> origins = new ArrayList<>();
        for (IndexableFilesIterator iterator : index.getOrderedIndexableFilesProviders(project)) {
            if (iterator.getOrigin() instanceof LibraryOrigin libraryOrigin && libraryName.equals(libraryName(libraryOrigin))) {
                origins.add(libraryOrigin);
            }
        }
        return origins;
    }

    private static String libraryName(LibraryOrigin origin) {
        return ReadAction.compute(() -> String.valueOf(origin.getLibrary().getName()));
    }

    private static List<VirtualFile> allClassRoots(List<LibraryOrigin> origins) {
        List<VirtualFile> roots = new ArrayList<>();
        for (LibraryOrigin origin : origins) {
            roots.addAll(origin.getClassRoots());
        }
        return roots;
    }
}
