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
package consulo.mcpServer;

import consulo.mcp.tool.McpToolException;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;

/**
 * Translates between the project-relative paths agents speak and {@link VirtualFile}s.
 * <p>
 * Every path here comes from outside the IDE, so resolution is deliberately strict - anything that
 * points outside the project root is rejected instead of followed.
 *
 * @author VISTALL
 * @since 2026-08-03
 */
public final class McpProjectPaths {
    /**
     * @param path project-relative path, empty for the project root itself
     * @throws McpToolException when the file does not exist or lies outside the project
     */
    public static VirtualFile resolve(Project project, String path) {
        VirtualFile baseDir = baseDir(project);
        String normalized = normalize(path);

        VirtualFile file = normalized.isEmpty() ? baseDir : baseDir.findFileByRelativePath(normalized);
        if (file == null) {
            throw new McpToolException("No such file in project: " + path);
        }
        if (!VirtualFileUtil.isAncestor(baseDir, file, false)) {
            throw new McpToolException("Path escapes the project root: " + path);
        }
        return file;
    }

    /**
     * Strips leading separators and unifies them, so that {@code \foo\bar}, {@code /foo/bar} and
     * {@code foo/bar} all mean the same project-relative path.
     */
    public static String normalize(String path) {
        String normalized = path.replace('\\', '/');
        int start = 0;
        while (start < normalized.length() && normalized.charAt(start) == '/') {
            start++;
        }
        return normalized.substring(start);
    }

    public static VirtualFile baseDir(Project project) {
        VirtualFile baseDir = project.getBaseDir();
        if (baseDir == null) {
            throw new McpToolException("Project '" + project.getName() + "' has no base directory.");
        }
        return baseDir;
    }

    /**
     * Falls back to the absolute path for files outside the project, so results stay readable
     * instead of empty.
     */
    public static String relativePath(Project project, VirtualFile file) {
        String relativePath = VirtualFileUtil.getRelativePath(file, baseDir(project));
        return relativePath == null ? file.getPath() : relativePath;
    }

    private McpProjectPaths() {
    }
}
