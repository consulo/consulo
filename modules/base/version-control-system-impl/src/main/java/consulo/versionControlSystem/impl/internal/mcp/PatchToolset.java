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
import consulo.mcpServer.McpToolGroup;
import consulo.mcpServer.McpToolRegistrar;
import consulo.mcpServer.McpToolset;
import consulo.project.Project;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.versionControlSystem.change.patch.ApplyPatchStatus;
import consulo.versionControlSystem.change.patch.FilePatch;
import consulo.versionControlSystem.change.patch.TextFilePatch;
import consulo.versionControlSystem.change.patch.PatchReader;
import consulo.versionControlSystem.change.patch.PatchSyntaxException;
import consulo.versionControlSystem.impl.internal.patch.apply.PatchApplier;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.Collection;
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
    public McpToolGroup getGroup() {
        return McpToolGroup.EDITING;
    }

    @Override
    public void registerTools(McpToolRegistrar registrar) {
        registrar.tool("apply_patch")
            .description("Applies a unified diff to the project. Paths inside the patch are resolved against the project root; git "
                + "'a/' and 'b/' prefixes are accepted. Every path must stay inside the project. Hunks that do not apply cleanly are "
                + "not merged: the whole patch is rolled back and the call fails, listing the files that could not be patched. "
                + "The applied change is undoable by the user.")
            .destructive()
            .requiresProject()
            .param(McpParameter.string("patch", "The unified diff text to apply."))
            .handler(PatchToolset::applyPatch);
    }

    private static CompletableFuture<McpToolCallResult> applyPatch(McpToolCallContext context) {
        Project project = context.getProject();
        String patch = context.getString("patch");

        return McpToolActions.uiActionAsync(project, () -> {
            List<TextFilePatch> textPatches;
            try {
                textPatches = new PatchReader(patch).readAllPatches();
            }
            catch (PatchSyntaxException e) {
                throw new McpToolException("Cannot parse the patch: " + e.getMessage());
            }

            if (textPatches.isEmpty()) {
                return CompletableFuture.completedFuture(McpToolCallResult.error("The patch contains no file entries."));
            }

            for (TextFilePatch textPatch : textPatches) {
                normalizeNames(textPatch);
            }

            List<FilePatch> patches = new ArrayList<>(textPatches);
            PatchApplier<?> applier = new PatchApplier<>(project, McpProjectPaths.baseDir(project), patches, (LocalChangeList) null, null, null);
            applier.setRequireVcsRoot(false);
            applier.setUnattended(true);
            return applier.execute(false, true).thenApply(status -> describe(status, textPatches, applier.getFailedPatches()));
        });
    }

    private static McpToolCallResult describe(ApplyPatchStatus status, List<TextFilePatch> patches, Collection<FilePatch> failed) {
        StringBuilder builder = new StringBuilder();
        switch (status) {
            case SUCCESS -> builder.append("Applied ").append(patches.size()).append(" file patch(es):\n");
            case ALREADY_APPLIED -> builder.append("The patch is already applied, nothing changed:\n");
            case PARTIAL -> builder.append("Applied partially; some files were already up to date:\n");
            case SKIP -> builder.append("Nothing was applied:\n");
            case FAILURE, ABORT -> builder.append("The patch was not applied and every change was rolled back.\n");
        }
        for (TextFilePatch patch : patches) {
            builder.append("  ").append(failed.contains(patch) ? "FAILED " : "").append(nameOf(patch)).append('\n');
        }
        String text = builder.toString();
        return status == ApplyPatchStatus.FAILURE || status == ApplyPatchStatus.ABORT ? McpToolCallResult.error(text) : McpToolCallResult.text(text);
    }

    private static void normalizeNames(TextFilePatch patch) {
        String before = patch.getBeforeName();
        String after = patch.getAfterName();
        boolean gitPrefixes = (before == null || before.startsWith("a/")) && (after == null || after.startsWith("b/"));
        if (before != null) {
            patch.setBeforeName(checkInsideProject(gitPrefixes ? before.substring(2) : before));
        }
        if (after != null) {
            patch.setAfterName(checkInsideProject(gitPrefixes ? after.substring(2) : after));
        }
    }

    private static String checkInsideProject(String name) {
        String normalized = name.replace('\\', '/');
        if (normalized.startsWith("/") || normalized.matches("^[A-Za-z]:.*")) {
            throw new McpToolException("Patch paths must be project-relative: " + name);
        }
        int depth = 0;
        for (String segment : normalized.split("/")) {
            if (segment.equals("..")) {
                depth--;
                if (depth < 0) {
                    throw new McpToolException("Patch path escapes the project root: " + name);
                }
            }
            else if (!segment.isEmpty() && !segment.equals(".")) {
                depth++;
            }
        }
        return normalized;
    }

    private static String nameOf(TextFilePatch patch) {
        String after = patch.getAfterName();
        String before = patch.getBeforeName();
        return after != null ? after : before != null ? before : "?";
    }
}
