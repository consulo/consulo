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
import consulo.mcp.tool.McpParameter;
import consulo.mcp.tool.McpToolCallResult;
import consulo.mcp.tool.McpToolException;
import consulo.mcpServer.McpProjectPaths;
import consulo.mcpServer.McpToolCallContext;
import consulo.mcpServer.McpToolActions;
import consulo.mcpServer.McpToolRegistrar;
import consulo.mcpServer.McpToolset;
import consulo.module.content.ProjectFileIndex;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author VISTALL
 * @since 2026-08-03
 */
@ExtensionImpl
@Singleton
public class SearchToolset implements McpToolset {
    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_FILE_LENGTH = 1024 * 1024;

    @Override
    public void registerTools(McpToolRegistrar registrar) {
        registrar.tool("search_file")
            .description("Finds project files whose name contains the given substring, case insensitively. Walks project content through "
                + "the IDE's virtual file system, skipping excluded and ignored directories; libraries are not searched.")
            .readOnly()
            .idempotent()
            .requiresProject()
            .param(McpParameter.string("nameSubstring", "Substring to look for in file names."))
            .param(McpParameter.integer("limit", "Maximum number of results.").defaultValue(DEFAULT_LIMIT))
            .handler(SearchToolset::searchFile);

        registrar.tool("search_text")
            .description("Finds project files containing the given text, reporting each match as path:line: text. Scans the content of "
                + "project files directly rather than using an index, skipping excluded and ignored directories, binary files and files "
                + "over 1 MB. Stops once limit matches are found.")
            .readOnly()
            .idempotent()
            .requiresProject()
            .param(McpParameter.string("text", "Text to look for."))
            .param(McpParameter.bool("caseSensitive", "Whether the match is case sensitive.").defaultValue(false))
            .param(McpParameter.integer("limit", "Maximum number of matching lines.").defaultValue(DEFAULT_LIMIT))
            .handler(context -> searchContent(context, false));

        registrar.tool("search_regex")
            .description("Finds project files whose lines match the given Java regular expression, reporting each match as path:line: "
                + "text. The pattern is matched against one line at a time, so it cannot span lines. Scans the content of project files "
                + "directly rather than using an index, skipping excluded and ignored directories, binary files and files over 1 MB.")
            .readOnly()
            .idempotent()
            .requiresProject()
            .param(McpParameter.string("pattern", "Java regular expression to match against each line."))
            .param(McpParameter.bool("caseSensitive", "Whether the match is case sensitive.").defaultValue(false))
            .param(McpParameter.integer("limit", "Maximum number of matching lines.").defaultValue(DEFAULT_LIMIT))
            .handler(context -> searchContent(context, true));
    }

    private static CompletableFuture<McpToolCallResult> searchFile(McpToolCallContext context) {
        Project project = context.getProject();
        String nameSubstring = context.getString("nameSubstring").toLowerCase(Locale.ROOT);
        int limit = context.getInt("limit");

        return McpToolActions.readAction(project, () -> {
            StringBuilder builder = new StringBuilder();
            int[] found = {0};

            visitContent(project, file -> {
                if (file.getName().toLowerCase(Locale.ROOT).contains(nameSubstring)) {
                    builder.append(McpProjectPaths.relativePath(project, file)).append('\n');
                    return ++found[0] < limit;
                }
                return true;
            });

            return McpToolCallResult.text(builder.toString());
        });
    }

    private static CompletableFuture<McpToolCallResult> searchContent(McpToolCallContext context, boolean regex) {
        Project project = context.getProject();
        boolean caseSensitive = context.getBoolean("caseSensitive");
        int limit = context.getInt("limit");

        String query = regex ? context.getString("pattern") : context.getString("text");
        Pattern pattern = compile(query, regex, caseSensitive);

        return McpToolActions.readAction(project, () -> {
            StringBuilder builder = new StringBuilder();
            int[] found = {0};

            visitContent(project, file -> {
                if (file.getLength() > MAX_FILE_LENGTH || file.getFileType().isBinary()) {
                    return true;
                }

                String text;
                try {
                    text = VirtualFileUtil.loadText(file);
                }
                catch (IOException e) {
                    return true;
                }

                String path = McpProjectPaths.relativePath(project, file);
                String[] lines = text.split("\n", -1);
                for (int i = 0; i < lines.length; i++) {
                    if (pattern.matcher(lines[i]).find()) {
                        builder.append(path).append(':').append(i + 1).append(": ").append(lines[i].strip()).append('\n');
                        if (++found[0] >= limit) {
                            return false;
                        }
                    }
                }
                return true;
            });

            return McpToolCallResult.text(builder.toString());
        });
    }

    private static Pattern compile(String query, boolean regex, boolean caseSensitive) {
        int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        try {
            return Pattern.compile(regex ? query : Pattern.quote(query), flags);
        }
        catch (PatternSyntaxException e) {
            throw new McpToolException("Invalid regular expression: " + e.getMessage());
        }
    }

    /**
     * Walks project content only, so libraries, excluded roots and ignored directories stay out of results.
     * A processor returning false aborts the whole walk, which is what makes the result limits real.
     */
    private static void visitContent(Project project, FileProcessor processor) {
        ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);

        VirtualFileUtil.iterateChildrenRecursively(
            McpProjectPaths.baseDir(project),
            file -> !fileIndex.isExcluded(file) && !fileIndex.isUnderIgnored(file),
            file -> file.isDirectory() || processor.process(file));
    }

    @FunctionalInterface
    private interface FileProcessor {
        /**
         * @return false to stop the walk
         */
        boolean process(VirtualFile file);
    }
}
