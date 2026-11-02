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
package consulo.mcpServer.impl.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.mcp.tool.McpToolException;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-11-02
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class McpProjectResolver {
    public static final String PROJECT_PATH_ARGUMENT = "projectPath";
    public static final String PROJECT_PATH_HEADER = "X-Consulo-Mcp-Project-Path";

    private final Application myApplication;
    private final ProjectManager myProjectManager;

    @Inject
    public McpProjectResolver(Application application, ProjectManager projectManager) {
        myApplication = application;
        myProjectManager = projectManager;
    }

    public Project resolve(@Nullable String argumentPath, @Nullable String callHeaderPath, @Nullable String sessionPath) {
        if (argumentPath != null && !argumentPath.isBlank()) {
            Project project = findProject(argumentPath);
            if (project == null) {
                throw new McpToolException("'" + PROJECT_PATH_ARGUMENT + "' = '" + argumentPath + "' does not match any open project."
                    + describeOpenProjects());
            }
            return project;
        }

        for (String path : new String[]{callHeaderPath, sessionPath}) {
            if (path != null && !path.isBlank()) {
                Project project = findProject(path);
                if (project != null) {
                    return project;
                }
            }
        }

        Project[] projects = myProjectManager.getOpenProjects();
        if (projects.length == 1) {
            return projects[0];
        }
        if (projects.length == 0) {
            throw new McpToolException("No project is open in the IDE.");
        }
        throw new McpToolException("Several projects are open. Pass the absolute project path as '" + PROJECT_PATH_ARGUMENT + "'."
            + describeOpenProjects());
    }

    public List<Project> getOpenProjects() {
        return List.of(myProjectManager.getOpenProjects());
    }

    public @Nullable Project findProject(String path) {
        Path target = parse(path);
        if (target == null) {
            return null;
        }

        List<McpProjectMatch> matches = myApplication.runReadAction((Supplier<List<McpProjectMatch>>) () -> collectMatches(target));
        if (matches.isEmpty()) {
            return null;
        }
        matches.sort(Comparator.comparingInt((McpProjectMatch match) -> match.directory().getNameCount()).reversed());

        McpProjectMatch best = matches.get(0);
        if (!best.projectDirectory()) {
            for (McpProjectMatch match : matches) {
                if (match.project() != best.project() && match.directory().equals(best.directory())) {
                    return null;
                }
            }
        }
        return best.project();
    }

    private List<McpProjectMatch> collectMatches(Path target) {
        List<McpProjectMatch> matches = new ArrayList<>();
        for (Project project : myProjectManager.getOpenProjects()) {
            if (project.isDisposed()) {
                continue;
            }

            Path baseDir = toPath(project.getBaseDir());
            if (baseDir != null && target.startsWith(baseDir)) {
                matches.add(new McpProjectMatch(project, baseDir, true));
            }

            for (VirtualFile root : ProjectRootManager.getInstance(project).getContentRoots()) {
                Path rootPath = toPath(root);
                if (rootPath != null && !rootPath.equals(baseDir) && target.startsWith(rootPath)) {
                    matches.add(new McpProjectMatch(project, rootPath, false));
                }
            }
        }
        return matches;
    }

    private String describeOpenProjects() {
        StringBuilder builder = new StringBuilder("\nOpen projects:");
        for (Project project : myProjectManager.getOpenProjects()) {
            String basePath = project.getBasePath();
            builder.append("\n - ").append(project.getName()).append(": ").append(basePath == null ? "(no base directory)" : basePath);
        }
        return builder.toString();
    }

    private static @Nullable Path toPath(@Nullable VirtualFile file) {
        if (file == null || !file.isInLocalFileSystem()) {
            return null;
        }
        return parse(file.getPath());
    }

    private static @Nullable Path parse(String path) {
        String trimmed = path.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            Path parsed = trimmed.startsWith("file:") ? Path.of(URI.create(trimmed)) : Path.of(trimmed);
            return parsed.isAbsolute() ? parsed.normalize() : null;
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }
}
