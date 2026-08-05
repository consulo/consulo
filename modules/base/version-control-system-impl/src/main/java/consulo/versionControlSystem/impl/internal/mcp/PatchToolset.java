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
package consulo.versionControlSystem.impl.internal.mcp;

import consulo.annotation.component.ExtensionImpl;
import consulo.mcp.tool.McpParameter;
import consulo.mcp.tool.McpToolCallResult;
import consulo.mcp.tool.McpToolException;
import consulo.mcpServer.McpProjectPaths;
import consulo.mcpServer.McpToolActions;
import consulo.mcpServer.McpToolCallContext;
import consulo.mcpServer.McpToolRegistrar;
import consulo.mcpServer.McpToolset;
import consulo.project.Project;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.versionControlSystem.change.patch.FilePatch;
import consulo.versionControlSystem.change.patch.TextFilePatch;
import consulo.versionControlSystem.impl.internal.patch.PatchReader;
import consulo.versionControlSystem.impl.internal.patch.PatchSyntaxException;
import consulo.versionControlSystem.impl.internal.patch.apply.PatchApplier;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-08-04
 */
@ExtensionImpl
@Singleton
public class PatchToolset implements McpToolset {
    @Override
    public void registerTools(McpToolRegistrar registrar) {
        registrar.tool("apply_patch")
            .description("Applies a unified diff to the project. Paths inside the patch are resolved against the project "
                + "root. Runs through the IDE's patch applier, so partially applicable hunks raise the usual merge "
                + "conflict UI instead of being forced, and the result is undoable by the user.")
            .destructive()
            .requiresProject()
            .param(McpParameter.string("patch", "The unified diff text to apply."))
            .handler(PatchToolset::applyPatch);
    }

    private static CompletableFuture<McpToolCallResult> applyPatch(McpToolCallContext context) {
        Project project = context.getProject();
        String patch = context.getString("patch");

        return McpToolActions.uiAction(project, () -> {
            List<TextFilePatch> textPatches;
            try {
                textPatches = new PatchReader(patch).readAllPatches();
            }
            catch (PatchSyntaxException e) {
                throw new McpToolException("Cannot parse the patch: " + e.getMessage());
            }

            if (textPatches.isEmpty()) {
                return McpToolCallResult.error("The patch contains no file entries.");
            }

            List<FilePatch> patches = new ArrayList<>(textPatches);
            // null target change list means the applied changes land in the active one
            new PatchApplier<>(project, McpProjectPaths.baseDir(project), patches, (LocalChangeList) null, null, null).execute();

            StringBuilder builder = new StringBuilder("Applied ").append(textPatches.size()).append(" file patch(es):\n");
            for (TextFilePatch textPatch : textPatches) {
                builder.append("  ").append(textPatch.getAfterName() == null ? textPatch.getBeforeName() : textPatch.getAfterName())
                    .append('\n');
            }
            return McpToolCallResult.text(builder.toString());
        });
    }
}
