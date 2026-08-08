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
import consulo.mcpServer.McpProjectPaths;
import consulo.mcpServer.McpToolActions;
import consulo.mcpServer.McpToolCallContext;
import consulo.mcpServer.McpToolRegistrar;
import consulo.mcpServer.McpToolset;
import consulo.project.Project;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.ContentRevision;
import consulo.versionControlSystem.internal.ChangeListManagerEx;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-08-03
 */
@ExtensionImpl
@Singleton
public class ChangeListToolset implements McpToolset {
    private static final int DEFAULT_LIMIT = 1000;

    @Override
    public void registerTools(McpToolRegistrar registrar) {
        registrar.tool("get_changelists")
            .description("Lists the local change lists of the project together with the files each one holds, as shown in the Local "
                + "Changes view. Change lists are an IDE concept and have no VCS command line equivalent.")
            .readOnly()
            .idempotent()
            .requiresProject()
            .handler(ChangeListToolset::getChangeLists);

        registrar.tool("vcs_status")
            .description("Lists the pending changes the IDE knows about, as shown in the Local Changes view. This is the IDE's own model "
                + "and covers any configured VCS; it does not run the VCS command line.")
            .readOnly()
            .idempotent()
            .requiresProject()
            .param(McpParameter.bool("includeUnversioned", "Whether to include files not under version control.")
                       .defaultValue(true))
            .param(McpParameter.bool("includeIgnored", "Whether to include ignored files.").defaultValue(false))
            .param(McpParameter.integer("limit", "Maximum number of entries returned.").defaultValue(DEFAULT_LIMIT))
            .handler(ChangeListToolset::vcsStatus);

        registrar.tool("create_changelist")
            .description("Creates a local change list. Change lists group pending changes inside the IDE and are not visible to the VCS.")
            .requiresProject()
            .param(McpParameter.string("name", "Name of the change list to create."))
            .param(McpParameter.string("comment", "Optional description of the change list.").defaultValue(""))
            .handler(ChangeListToolset::createChangeList);

        registrar.tool("set_active_changelist")
            .description("Makes a local change list the default one, so subsequently modified files are placed in it automatically.")
            .requiresProject()
            .param(McpParameter.string("name", "Name of the change list to activate."))
            .handler(ChangeListToolset::setActiveChangeList);

        registrar.tool("move_changes_to_changelist")
            .description("Moves the pending changes of the given project files into a local change list. Every listed file must already "
                + "have a pending change, otherwise nothing is moved and the call reports which files were not changed.")
            .requiresProject()
            .param(McpParameter.string("name", "Name of the target change list."))
            .param(McpParameter.stringArray("paths", "Project-relative paths of the files to move."))
            .handler(ChangeListToolset::moveChangesToChangeList);

        registrar.tool("delete_changelist")
            .description("Deletes a local change list. The changes it held move to the default change list; no file content is lost and "
                + "nothing is reverted. The active change list cannot be deleted.")
            .destructive()
            .requiresProject()
            .param(McpParameter.string("name", "Name of the change list to delete."))
            .handler(ChangeListToolset::deleteChangeList);
    }

    private static CompletableFuture<McpToolCallResult> getChangeLists(McpToolCallContext context) {
        Project project = context.getProject();

        return McpToolActions.readAction(project, () -> {
            StringBuilder builder = new StringBuilder();

            for (LocalChangeList changeList : ChangeListManager.getInstance(project).getChangeLists()) {
                builder.append(changeList.getName());
                if (changeList.isDefault()) {
                    builder.append(" (active)");
                }
                builder.append('\n');

                String comment = changeList.getComment();
                if (comment != null && !comment.isEmpty()) {
                    builder.append("  comment: ").append(comment).append('\n');
                }

                for (Change change : changeList.getChanges()) {
                    builder.append("  ").append(describe(project, change)).append('\n');
                }
            }

            return McpToolCallResult.text(builder.toString());
        });
    }

    private static CompletableFuture<McpToolCallResult> vcsStatus(McpToolCallContext context) {
        Project project = context.getProject();
        boolean includeUnversioned = context.getBoolean("includeUnversioned");
        boolean includeIgnored = context.getBoolean("includeIgnored");
        int limit = context.getInt("limit");

        return McpToolActions.readAction(project, () -> {
            ChangeListManager changeListManager = ChangeListManager.getInstance(project);

            StringBuilder builder = new StringBuilder();
            Counter counter = new Counter(limit);

            for (Change change : changeListManager.getAllChanges()) {
                if (!counter.hasRoom()) {
                    break;
                }
                builder.append(describe(project, change)).append('\n');
            }

            if (changeListManager instanceof ChangeListManagerEx changeListManagerEx) {
                if (includeUnversioned) {
                    append(project, builder, counter, "UNVERSIONED", changeListManagerEx.getUnversionedFiles());
                }
                if (includeIgnored) {
                    append(project, builder, counter, "IGNORED", changeListManagerEx.getIgnoredFiles());
                }
            }

            if (counter.truncated) {
                builder.append("... truncated at ").append(limit).append(" entries\n");
            }
            return McpToolCallResult.text(builder.toString());
        });
    }

    private static void append(Project project, StringBuilder builder, Counter counter, String kind, List<VirtualFile> files) {
        for (VirtualFile file : files) {
            if (!counter.hasRoom()) {
                return;
            }
            builder.append(kind).append(' ').append(McpProjectPaths.relativePath(project, file)).append('\n');
        }
    }

    private static final class Counter {
        private final int myLimit;

        private int myCount;
        private boolean truncated;

        private Counter(int limit) {
            myLimit = limit;
        }

        private boolean hasRoom() {
            if (myCount >= myLimit) {
                truncated = true;
                return false;
            }
            myCount++;
            return true;
        }
    }

    private static CompletableFuture<McpToolCallResult> createChangeList(McpToolCallContext context) {
        Project project = context.getProject();
        String name = context.getString("name");
        String comment = context.getString("comment");

        return McpToolActions.uiAction(project, () -> {
            ChangeListManager changeListManager = ChangeListManager.getInstance(project);
            if (find(changeListManager, name) != null) {
                return McpToolCallResult.error("Change list already exists: " + name);
            }

            changeListManager.addChangeList(name, comment.isEmpty() ? null : comment);
            return McpToolCallResult.success();
        });
    }

    private static CompletableFuture<McpToolCallResult> setActiveChangeList(McpToolCallContext context) {
        Project project = context.getProject();
        String name = context.getString("name");

        return McpToolActions.uiAction(project, () -> {
            ChangeListManager changeListManager = ChangeListManager.getInstance(project);
            LocalChangeList changeList = find(changeListManager, name);
            if (changeList == null) {
                return McpToolCallResult.error("No such change list: " + name);
            }

            changeListManager.setDefaultChangeList(changeList);
            return McpToolCallResult.success();
        });
    }

    private static CompletableFuture<McpToolCallResult> moveChangesToChangeList(McpToolCallContext context) {
        Project project = context.getProject();
        String name = context.getString("name");
        List<String> paths = context.getStringList("paths");

        return McpToolActions.uiAction(project, () -> {
            ChangeListManager changeListManager = ChangeListManager.getInstance(project);
            LocalChangeList changeList = find(changeListManager, name);
            if (changeList == null) {
                return McpToolCallResult.error("No such change list: " + name);
            }

            List<Change> changes = new ArrayList<>();
            List<String> unchanged = new ArrayList<>();
            for (String path : paths) {
                Change change = changeListManager.getChange(McpProjectPaths.resolve(project, path));
                if (change == null) {
                    unchanged.add(path);
                }
                else {
                    changes.add(change);
                }
            }

            if (!unchanged.isEmpty()) {
                return McpToolCallResult.error("These files have no pending change: " + String.join(", ", unchanged));
            }

            changeListManager.moveChangesTo(changeList, changes.toArray(new Change[0]));
            return McpToolCallResult.success();
        });
    }

    private static CompletableFuture<McpToolCallResult> deleteChangeList(McpToolCallContext context) {
        Project project = context.getProject();
        String name = context.getString("name");

        return McpToolActions.uiAction(project, () -> {
            ChangeListManager changeListManager = ChangeListManager.getInstance(project);
            LocalChangeList changeList = find(changeListManager, name);
            if (changeList == null) {
                return McpToolCallResult.error("No such change list: " + name);
            }
            if (changeList.isDefault()) {
                return McpToolCallResult.error("The active change list cannot be deleted: " + name);
            }

            changeListManager.removeChangeList(changeList);
            return McpToolCallResult.success();
        });
    }

    private static @Nullable LocalChangeList find(ChangeListManager changeListManager, String name) {
        for (LocalChangeList changeList : changeListManager.getChangeLists()) {
            if (changeList.getName().equals(name)) {
                return changeList;
            }
        }
        return null;
    }

    /**
     * A deleted file only has a before revision, a new one only an after revision.
     */
    private static String describe(Project project, Change change) {
        ContentRevision revision = change.getAfterRevision() != null ? change.getAfterRevision() : change.getBeforeRevision();
        String path = revision == null ? "?" : revision.getFile().getPath();
        return change.getType() + " " + path;
    }
}
