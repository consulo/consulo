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
package consulo.it.project;

import consulo.application.WriteAction;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.project.Project;
import consulo.project.content.library.ProjectLibraryTable;
import consulo.it.HeadlessProjectExtension;
import consulo.it.HeadlessProjects;
import consulo.virtualFileSystem.VirtualFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static consulo.it.index.ScanningTestSupport.findFile;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code ProjectLibraryTableImpl} lives in its own module now, so a headless application gets the real project library
 * table rather than nothing at all. These exercise it through the public {@link ProjectLibraryTable} surface.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessProjectExtension.class)
public class ProjectLibraryTableTest {
    @Test
    public void theTableIsBoundAndProjectScoped(Project project) throws Exception {
        ProjectLibraryTable table = ProjectLibraryTable.getInstance(project);

        assertThat(table).as("the headless application must resolve the project library table").isNotNull();
        assertThat(table.getProject()).isSameAs(project);
        assertThat(table.getTableLevel()).isEqualTo(ProjectLibraryTable.PROJECT_LEVEL);
        assertThat(table.isEditable()).isTrue();
        assertThat(table.getLibraries()).as("a fresh project owns no libraries").isEmpty();
    }

    @Test
    public void aCommittedLibraryBecomesVisible(Project project) throws Exception {
        ProjectLibraryTable table = ProjectLibraryTable.getInstance(project);

        createLibrary(table, "committed-library");

        assertThat(table.getLibraryByName("committed-library")).isNotNull();
        assertThat(table.getLibraries()).hasSize(1);
        assertThat(table.getLibraries()[0].getName()).isEqualTo("committed-library");
    }

    @Test
    public void anUncommittedModelChangesNothing(Project project) throws Exception {
        ProjectLibraryTable table = ProjectLibraryTable.getInstance(project);

        WriteAction.run(() -> {
            LibraryTable.ModifiableModel model = table.getModifiableModel();
            model.createLibrary("never-committed");
            assertThat(model.isChanged()).isTrue();
        });

        assertThat(table.getLibraryByName("never-committed"))
            .as("a model that was never committed must not reach the table")
            .isNull();
        assertThat(table.getLibraries()).isEmpty();
    }

    @Test
    public void libraryRootsSurviveTheCommit(HeadlessProjects projects) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-libtable-roots");
        Path classes = Files.createDirectory(directory.resolve("classes"));
        Project project = projects.open(directory);
        ProjectLibraryTable table = ProjectLibraryTable.getInstance(project);

        VirtualFile classesRoot = findFile(classes);
        WriteAction.run(() -> {
            LibraryTable.ModifiableModel model = table.getModifiableModel();
            Library library = model.createLibrary("library-with-roots");
            Library.ModifiableModel libraryModel = library.getModifiableModel();
            libraryModel.addRoot(classesRoot, BinariesOrderRootType.ID);
            libraryModel.commit();
            model.commit();
        });

        Library library = table.getLibraryByName("library-with-roots");
        assertThat(library).isNotNull();
        assertThat(library.getFiles(BinariesOrderRootType.ID)).containsExactly(classesRoot);
    }

    @Test
    public void removingALibraryTakesItOutOfTheTable(Project project) throws Exception {
        ProjectLibraryTable table = ProjectLibraryTable.getInstance(project);

        createLibrary(table, "doomed-library");
        Library library = table.getLibraryByName("doomed-library");
        assertThat(library).isNotNull();

        WriteAction.run(() -> table.removeLibrary(library));

        assertThat(table.getLibraryByName("doomed-library")).isNull();
        assertThat(table.getLibraries()).isEmpty();
    }

    @Test
    public void listenersSeeTheLibraryComeAndGo(Project project) throws Exception {
        ProjectLibraryTable table = ProjectLibraryTable.getInstance(project);

        List<String> events = new ArrayList<>();
        LibraryTable.Listener listener = new LibraryTable.Listener() {
            @Override
            public void afterLibraryAdded(Library newLibrary) {
                events.add("added:" + newLibrary.getName());
            }

            @Override
            public void afterLibraryRenamed(Library library) {
                events.add("renamed:" + library.getName());
            }

            @Override
            public void beforeLibraryRemoved(Library library) {
                events.add("beforeRemoved:" + library.getName());
            }

            @Override
            public void afterLibraryRemoved(Library library) {
                events.add("afterRemoved:" + library.getName());
            }
        };
        table.addListener(listener);
        try {
            createLibrary(table, "watched-library");
            Library library = table.getLibraryByName("watched-library");
            assertThat(library).isNotNull();
            WriteAction.run(() -> table.removeLibrary(library));
        }
        finally {
            table.removeListener(listener);
        }

        assertThat(events).containsExactly(
            "added:watched-library",
            "beforeRemoved:watched-library",
            "afterRemoved:watched-library"
        );
    }

    private static void createLibrary(LibraryTable table, String name) throws Exception {
        WriteAction.run(() -> {
            LibraryTable.ModifiableModel model = table.getModifiableModel();
            model.createLibrary(name);
            model.commit();
        });
    }
}
