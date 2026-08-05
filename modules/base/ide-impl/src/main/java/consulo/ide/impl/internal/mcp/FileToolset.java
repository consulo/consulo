/*
 * Copyright 2000-2026 JetBrains s.r.o.
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
package consulo.ide.impl.internal.mcp;

import consulo.annotation.component.ExtensionImpl;
import consulo.fileEditor.FileEditorManager;
import consulo.mcp.tool.McpParameter;
import consulo.mcp.tool.McpToolCallResult;
import consulo.mcp.tool.McpToolException;
import consulo.mcpServer.McpProjectPaths;
import consulo.mcpServer.McpToolCallContext;
import consulo.mcpServer.McpToolActions;
import consulo.mcpServer.McpToolRegistrar;
import consulo.mcpServer.McpToolset;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-08-03
 */
@ExtensionImpl
@Singleton
public class FileToolset implements McpToolset {
    private static final int MAX_ENTRIES = 1000;

    @Override
    public void registerTools(McpToolRegistrar registrar) {
        registrar.tool("list_directory_tree")
            .description("Lists the file tree under a project-relative directory path, reading the IDE's virtual file system so unsaved "
                + "and freshly created files are visible. Descends maxDepth levels and stops after 1000 entries.")
            .readOnly()
            .idempotent()
            .requiresProject()
            .param(McpParameter.string("path", "Project-relative directory path. Empty means the project root.")
                       .defaultValue(""))
            .param(McpParameter.integer("maxDepth", "How many directory levels to descend.").defaultValue(3))
            .handler(FileToolset::listDirectoryTree);

        registrar.tool("create_new_file")
            .description("Creates a file at a project-relative path, together with any missing parent directories. Runs as an IDE write "
                + "action, so the file appears immediately in the editor and in version control. Fails if the file already exists.")
            .param(McpParameter.string("path", "Project-relative path of the file to create."))
            .param(McpParameter.string("text", "Content to write into the new file.").defaultValue(""))
            .handler(FileToolset::createNewFile);

        registrar.tool("open_file_in_editor")
            .description("Opens a project file in the editor and brings it to the front. Affects the IDE window the user is looking at.")
            .param(McpParameter.string("path", "Project-relative path of the file to open."))
            .handler(FileToolset::openFileInEditor);

        registrar.tool("get_all_open_file_paths")
            .description("Lists the project-relative paths of all files currently open in the editor tabs.")
            .readOnly()
            .idempotent()
            .requiresProject()
            .handler(FileToolset::getAllOpenFilePaths);
    }

    private static CompletableFuture<McpToolCallResult> listDirectoryTree(McpToolCallContext context) {
        Project project = context.getProject();
        String path = context.getString("path");
        int maxDepth = context.getInt("maxDepth");

        return McpToolActions.readAction(project, () -> {
            VirtualFile root = McpProjectPaths.resolve(project, path);
            if (!root.isDirectory()) {
                return McpToolCallResult.error("Not a directory: " + path);
            }

            StringBuilder builder = new StringBuilder();
            appendChildren(root, maxDepth, 0, builder, new int[]{0});
            return McpToolCallResult.text(builder.toString());
        });
    }

    private static void appendChildren(VirtualFile directory, int maxDepth, int depth, StringBuilder builder, int[] count) {
        if (depth >= maxDepth || count[0] >= MAX_ENTRIES) {
            return;
        }

        for (VirtualFile child : directory.getChildren()) {
            if (count[0]++ >= MAX_ENTRIES) {
                builder.append("... truncated at ").append(MAX_ENTRIES).append(" entries\n");
                return;
            }

            builder.append("  ".repeat(depth)).append(child.getName());
            if (child.isDirectory()) {
                builder.append('/');
            }
            builder.append('\n');

            if (child.isDirectory()) {
                appendChildren(child, maxDepth, depth + 1, builder, count);
            }
        }
    }

    private static CompletableFuture<McpToolCallResult> createNewFile(McpToolCallContext context) {
        Project project = context.getProject();
        String path = context.getString("path");
        String text = context.getString("text");

        return McpToolActions.writeAction(project, () -> {
            String normalized = path.replace('\\', '/');
            int slash = normalized.lastIndexOf('/');
            String parentPath = slash < 0 ? "" : normalized.substring(0, slash);
            String name = slash < 0 ? normalized : normalized.substring(slash + 1);

            if (name.isEmpty()) {
                return McpToolCallResult.error("Not a file path: " + path);
            }

            try {
                VirtualFile baseDir = McpProjectPaths.baseDir(project);
                VirtualFile parent = parentPath.isEmpty() ? baseDir : VirtualFileUtil.createDirectoryIfMissing(baseDir, parentPath);
                if (parent.findChild(name) != null) {
                    return McpToolCallResult.error("File already exists: " + path);
                }

                VirtualFile file = parent.createChildData(FileToolset.class, name);
                VirtualFileUtil.saveText(file, text);
                return McpToolCallResult.text(McpProjectPaths.relativePath(project, file));
            }
            catch (IOException e) {
                throw new McpToolException("Cannot create " + path + ": " + e.getMessage());
            }
        });
    }

    private static CompletableFuture<McpToolCallResult> openFileInEditor(McpToolCallContext context) {
        Project project = context.getProject();
        String path = context.getString("path");

        return McpToolActions.uiAction(project, () -> {
            VirtualFile file = McpProjectPaths.resolve(project, path);
            if (file.isDirectory()) {
                return McpToolCallResult.error("Not a file: " + path);
            }

            FileEditorManager.getInstance(project).openFile(file, true);
            return McpToolCallResult.success();
        });
    }

    private static CompletableFuture<McpToolCallResult> getAllOpenFilePaths(McpToolCallContext context) {
        Project project = context.getProject();

        return McpToolActions.uiAction(project, () -> {
            StringBuilder builder = new StringBuilder();
            for (VirtualFile file : FileEditorManager.getInstance(project).getOpenFiles()) {
                builder.append(McpProjectPaths.relativePath(project, file)).append('\n');
            }
            return McpToolCallResult.text(builder.toString());
        });
    }
}
