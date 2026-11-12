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
package consulo.it.vcs;

import consulo.application.Application;
import consulo.component.messagebus.MessageBusConnection;
import consulo.it.HeadlessProjectExtension;
import consulo.it.HeadlessProjects;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsDirectoryMapping;
import consulo.versionControlSystem.root.VcsRoot;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.BulkFileListener;
import consulo.virtualFileSystem.event.VFileCreateEvent;
import consulo.virtualFileSystem.event.VFileEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static consulo.it.index.ScanningTestSupport.waitFor;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Resolving the VCS root of a {@link FilePath} is a pure path lookup: a file which exists on disk but is not in the VFS yet
 * must not be pulled into the VFS by the lookup, because every such lookup would fire a create event and feed the VFS
 * listeners that asked for the root in the first place.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessProjectExtension.class)
public class VcsRootForUncachedFilePathTest {
    private static final String VCS_NAME = "ItTestVcs";

    @Test
    public void filePathLookupDoesNotRefreshVfs(HeadlessProjects projects, Application application) throws Exception {
        Path directory = projects.newDirectory();
        Path repository = directory.resolve("repository");
        Files.createDirectories(repository);
        Path outside = directory.resolve("outside");
        Files.createDirectories(outside);

        Project project = projects.open(directory);
        ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);
        waitFor("the VCS mappings must be activated", vcsManager::areVcsesActivated);

        LocalFileSystem fileSystem = LocalFileSystem.getInstance();
        VirtualFile repositoryFile = fileSystem.refreshAndFindFileByNioFile(repository);
        assertThat(repositoryFile).isNotNull();
        repositoryFile.getChildren();

        String repositoryPath = FileUtil.toSystemIndependentName(repository.toString());
        vcsManager.setDirectoryMappings(List.of(new VcsDirectoryMapping(repositoryPath, VCS_NAME)));

        Path newDirectory = repository.resolve("generated");
        Path newFile = newDirectory.resolve("Created.txt");
        Files.createDirectories(newDirectory);
        Files.writeString(newFile, "created by an external tool");
        Path outsideFile = outside.resolve("Outside.txt");
        Files.writeString(outsideFile, "not under a mapping");

        String newFilePath = FileUtil.toSystemIndependentName(newFile.toString());
        String newDirectoryPath = FileUtil.toSystemIndependentName(newDirectory.toString());
        assertThat(fileSystem.findFileByPathIfCached(newFilePath)).isNull();
        assertThat(fileSystem.findFileByPathIfCached(newDirectoryPath)).isNull();

        List<String> createdPaths = Collections.synchronizedList(new ArrayList<>());
        MessageBusConnection connection = application.getMessageBus().connect();
        try {
            connection.subscribe(BulkFileListener.class, new BulkFileListener() {
                @Override
                public void after(List<? extends VFileEvent> events) {
                    for (VFileEvent event : events) {
                        if (event instanceof VFileCreateEvent) {
                            createdPaths.add(event.getPath());
                        }
                    }
                }
            });

            FilePath filePath = VcsUtil.getFilePath(newFilePath, false);
            FilePath directoryPath = VcsUtil.getFilePath(newDirectoryPath, true);
            FilePath outsidePath = VcsUtil.getFilePath(FileUtil.toSystemIndependentName(outsideFile.toString()), false);

            assertThat(vcsManager.getVcsRootFor(filePath)).isEqualTo(repositoryFile);
            assertThat(vcsManager.getVcsRootFor(directoryPath)).isEqualTo(repositoryFile);

            VcsRoot vcsRoot = vcsManager.getVcsRootObjectFor(filePath);
            assertThat(vcsRoot).isNotNull();
            assertThat(vcsRoot.getPath()).isEqualTo(repositoryFile);

            VcsDirectoryMapping mapping = vcsManager.getDirectoryMappingFor(filePath);
            assertThat(mapping).isNotNull();
            assertThat(mapping.getDirectory()).isEqualTo(repositoryPath);
            assertThat(mapping.getVcs()).isEqualTo(VCS_NAME);

            assertThat(vcsManager.getVcsFor(filePath)).isNull();
            assertThat(vcsManager.getVcsRootFor(outsidePath)).isNull();
        }
        finally {
            connection.disconnect();
        }

        assertThat(fileSystem.findFileByPathIfCached(newFilePath))
            .as("resolving the VCS root of a path must not load the file into the VFS")
            .isNull();
        assertThat(fileSystem.findFileByPathIfCached(newDirectoryPath))
            .as("resolving the VCS root of a path must not load its parent directory into the VFS")
            .isNull();
        assertThat(createdPaths)
            .as("resolving the VCS root of a path must not fire VFS create events")
            .isEmpty();
    }
}
