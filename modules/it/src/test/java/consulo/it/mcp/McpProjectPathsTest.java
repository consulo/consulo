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
package consulo.it.mcp;

import consulo.application.Application;
import consulo.it.HeadlessApplicationExtension;
import consulo.mcp.tool.McpToolException;
import consulo.mcpServer.McpProjectPaths;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ProjectOpenContext;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Paths reaching {@link McpProjectPaths} come from an external agent, so the traversal guard is the
 * part that actually has to hold.
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class McpProjectPathsTest {
    @Test
    public void normalizeUnifiesSeparatorsAndStripsLeadingSlashes() {
        assertThat(McpProjectPaths.normalize("foo/bar")).isEqualTo("foo/bar");
        assertThat(McpProjectPaths.normalize("/foo/bar")).isEqualTo("foo/bar");
        assertThat(McpProjectPaths.normalize("///foo/bar")).isEqualTo("foo/bar");
        assertThat(McpProjectPaths.normalize("\\foo\\bar")).isEqualTo("foo/bar");
        assertThat(McpProjectPaths.normalize("")).isEmpty();
        assertThat(McpProjectPaths.normalize("/")).isEmpty();
    }

    @Test
    public void resolvesFilesInsideTheProject(Application application, ProjectManager projectManager) throws Exception {
        Project project = openProject(application, projectManager, directory -> {
            Files.createDirectories(directory.resolve("src"));
            Files.writeString(directory.resolve("src").resolve("Main.java"), "class Main {}");
        });

        VirtualFile baseDir = McpProjectPaths.baseDir(project);
        VirtualFile resolved = McpProjectPaths.resolve(project, "src/Main.java");

        assertThat(resolved.getName()).isEqualTo("Main.java");
        assertThat(McpProjectPaths.resolve(project, "/src/Main.java")).isEqualTo(resolved);
        assertThat(McpProjectPaths.resolve(project, "src\\Main.java")).isEqualTo(resolved);
        assertThat(McpProjectPaths.resolve(project, "")).isEqualTo(baseDir);

        assertThat(McpProjectPaths.relativePath(project, resolved)).isEqualTo("src/Main.java");
    }

    @Test
    public void rejectsPathsEscapingTheProjectRoot(Application application, ProjectManager projectManager) throws Exception {
        Project project = openProject(application, projectManager);

        assertThatThrownBy(() -> McpProjectPaths.resolve(project, "../.."))
            .isInstanceOf(McpToolException.class);

        assertThatThrownBy(() -> McpProjectPaths.resolve(project, "../../etc/passwd"))
            .isInstanceOf(McpToolException.class);
    }

    @Test
    public void reportsMissingFilesInsteadOfReturningNull(Application application, ProjectManager projectManager) throws Exception {
        Project project = openProject(application, projectManager);

        assertThatThrownBy(() -> McpProjectPaths.resolve(project, "does/not/exist.txt"))
            .isInstanceOf(McpToolException.class)
            .hasMessageContaining("No such file");
    }

    private static Project openProject(Application application, ProjectManager projectManager) throws Exception {
        return openProject(application, projectManager, directory -> {
        });
    }

    /**
     * Content is laid down on disk before the project opens, so the VFS picks it up during the
     * initial refresh. Creating it afterwards would need a write action, which must not be taken
     * from the UI thread the test runs on.
     */
    private static Project openProject(Application application, ProjectManager projectManager, Content content) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-mcp-paths");
        content.fill(directory);
        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(directory.toFile());

        return projectManager
            .openProjectAsync(directory, application.getLastUIAccess(), new ProjectOpenContext())
            .get(30, TimeUnit.SECONDS);
    }

    @FunctionalInterface
    private interface Content {
        void fill(Path directory) throws Exception;
    }
}
