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
package consulo.ai.impl.internal.instruction;

import consulo.ai.AIAgentInstructionProvider;
import consulo.ai.AIAgentRuleDirectory;
import consulo.ai.localize.AILocalize;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.configurable.ProjectConfigurable;
import consulo.configurable.StandardConfigurableIds;
import consulo.disposer.Disposable;
import consulo.fileEditor.FileEditorManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.InputBoxBuilder;
import consulo.ui.Label;
import consulo.ui.LabelStyle;
import consulo.ui.ListBox;
import consulo.ui.MessageBoxes;
import consulo.ui.TextAttribute;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionToolbarPosition;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.toolbar.AddAction;
import consulo.ui.ex.toolbar.DownMoveAction;
import consulo.ui.ex.toolbar.EditAction;
import consulo.ui.ex.toolbar.RemoveAction;
import consulo.ui.ex.toolbar.ToolbarDecoratorBuilderFactory;
import consulo.ui.ex.toolbar.UpMoveAction;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.TabbedLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.util.lang.function.ThrowableSupplier;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * The instructions the agents read in a project - instruction files, rules and skills - listed from the disk, each
 * opened in an editor, added from a template or removed. Every change goes to the files at once - nothing to apply.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
@ExtensionImpl
public class AIAgentInstructionsConfigurable implements ProjectConfigurable {
    private static final Logger LOG = Logger.getInstance(AIAgentInstructionsConfigurable.class);

    private final Project myProject;
    private final Application myApplication;
    private final ApplicationConcurrency myConcurrency;

    private final MutableFlatDataModel<AIAgentInstruction> myFiles = FlatDataModel.of(List.of());
    private final MutableFlatDataModel<AIAgentInstruction> myRules = FlatDataModel.of(List.of());
    private final MutableFlatDataModel<AIAgentInstruction> mySkills = FlatDataModel.of(List.of());

    @Inject
    public AIAgentInstructionsConfigurable(Project project, Application application, ApplicationConcurrency concurrency) {
        myProject = project;
        myApplication = application;
        myConcurrency = concurrency;
    }

    @Override
    public String getId() {
        return "ai.agent.instructions";
    }

    @Override
    public @Nullable String getParentId() {
        return StandardConfigurableIds.AI_GROUP;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return AILocalize.configurableAgentInstructions();
    }

    @RequiredUIAccess
    @Override
    public @Nullable Component createUIComponent(Disposable parentDisposable) {
        DockLayout root = DockLayout.create();
        Label hint = Label.create(AILocalize.labelAgentInstructionsHint());
        hint.addStyle(LabelStyle.FONT_SMALL);
        root.top(hint);

        Path projectDirectory = projectDirectory();
        if (projectDirectory == null) {
            return root;
        }
        TabbedLayout tabs = TabbedLayout.create();
        tabs.addTab(AILocalize.labelAgentInstructionFiles(), list(myFiles, () -> addInstructionFile(projectDirectory)));
        tabs.addTab(AILocalize.labelAgentRules(), list(myRules, () -> addRule(projectDirectory)));
        tabs.addTab(AILocalize.labelAgentSkills(), list(mySkills, () -> addSkill(projectDirectory)));
        root.center(tabs);
        refresh();
        return root;
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        return false;
    }

    @RequiredUIAccess
    @Override
    public void apply() {
    }

    @RequiredUIAccess
    private Component list(MutableFlatDataModel<AIAgentInstruction> model, Runnable add) {
        ListBox<AIAgentInstruction> listBox = ListBox.create(model);
        listBox.setRenderer((presentation, item) -> {
            AIAgentInstruction instruction = item.getValue();
            if (instruction == null) {
                presentation.append(" ");
                return;
            }
            presentation.append(instruction.name());
            LocalizeValue where = instruction.global()
                ? LocalizeValue.join(instruction.agent(), LocalizeValue.of(", "), AILocalize.labelAgentInstructionGlobal())
                : instruction.agent();
            presentation.append(LocalizeValue.join(LocalizeValue.of("  ("), where, LocalizeValue.of(")")), TextAttribute.GRAYED);
            if (!instruction.description().isEmpty()) {
                presentation.append("  " + instruction.description(), TextAttribute.GRAY);
            }
        });
        listBox.addDoubleClickListener(event -> {
            AIAgentInstruction instruction = event.getValue();
            if (instruction != null) {
                open(instruction.file());
            }
        });
        return myApplication.getInstance(ToolbarDecoratorBuilderFactory.class)
            .create(listBox)
            .addOrReplaceAction(new AddAction<AIAgentInstruction>() {
                @Override
                @RequiredUIAccess
                protected void doAdd(AnActionEvent e) {
                    add.run();
                }
            })
            .addOrReplaceAction(new EditAction<AIAgentInstruction>() {
                @Override
                @RequiredUIAccess
                protected void doEdit(AIAgentInstruction value, AnActionEvent e) {
                    open(value.file());
                }
            })
            .addOrReplaceAction(new RemoveAction<AIAgentInstruction>() {
                @Override
                @RequiredUIAccess
                protected void doRemove(AIAgentInstruction value, AnActionEvent e) {
                    remove(value, listBox);
                }
            })
            .disableAction(UpMoveAction.class)
            .disableAction(DownMoveAction.class)
            .withToolbarPosition(ActionToolbarPosition.TOP)
            .build();
    }

    @RequiredUIAccess
    private void refresh() {
        Path projectDirectory = projectDirectory();
        if (projectDirectory == null) {
            return;
        }
        List<AIAgentInstructionProvider> providers = providers();
        Path home = home();
        CompletableFuture.supplyAsync(() -> AIAgentInstructionScanner.scan(projectDirectory, home, providers), myConcurrency.executor())
            .whenCompleteAsync((instructions, error) -> {
                if (error != null) {
                    LOG.error(error);
                    return;
                }
                fill(myFiles, instructions, AIAgentInstructionKind.FILE);
                fill(myRules, instructions, AIAgentInstructionKind.RULE);
                fill(mySkills, instructions, AIAgentInstructionKind.SKILL);
            }, UIAccess.current());
    }

    private static void fill(MutableFlatDataModel<AIAgentInstruction> model, List<AIAgentInstruction> instructions, AIAgentInstructionKind kind) {
        model.removeAll();
        for (AIAgentInstruction instruction : instructions) {
            if (instruction.kind() == kind) {
                model.add(instruction);
            }
        }
    }

    @RequiredUIAccess
    private void addInstructionFile(Path projectDirectory) {
        List<String> missing = AIAgentInstructionScanner.missingInstructionFiles(projectDirectory, providers());
        if (missing.isEmpty()) {
            MessageBoxes.okInfo(AILocalize.messageAgentInstructionAllFilesExist())
                .title(AILocalize.titleAgentInstructionNewFile())
                .showAsync();
            return;
        }
        UIAccess uiAccess = UIAccess.current();
        InputBoxBuilder.items(missing)
            .title(AILocalize.titleAgentInstructionNewFile())
            .text(AILocalize.labelAgentInstructionLocation())
            .value(missing.getFirst())
            .showAsync()
            .thenAccept(name -> create(() -> AIAgentInstructionScanner.createInstructionFile(projectDirectory, name), uiAccess));
    }

    @RequiredUIAccess
    private void addRule(Path projectDirectory) {
        Map<String, AIAgentRuleDirectory> directories = new LinkedHashMap<>();
        for (AIAgentInstructionProvider provider : providers()) {
            for (AIAgentRuleDirectory directory : provider.getRuleDirectories()) {
                directories.putIfAbsent(directory.path(), directory);
            }
        }
        if (directories.isEmpty()) {
            return;
        }
        UIAccess uiAccess = UIAccess.current();
        InputBoxBuilder.text()
            .title(AILocalize.titleAgentRuleNew())
            .text(AILocalize.labelAgentRuleName())
            .showAsync()
            .thenComposeAsync(name -> location(new ArrayList<>(directories.keySet()), AILocalize.titleAgentRuleNew())
                .thenAccept(path -> create(() -> AIAgentInstructionScanner.createRule(projectDirectory, Objects.requireNonNull(directories.get(path)), name), uiAccess)), uiAccess);
    }

    @RequiredUIAccess
    private void addSkill(Path projectDirectory) {
        List<String> directories = new ArrayList<>();
        for (AIAgentInstructionProvider provider : providers()) {
            for (String directory : provider.getSkillDirectories()) {
                if (!directories.contains(directory)) {
                    directories.add(directory);
                }
            }
        }
        if (directories.isEmpty()) {
            return;
        }
        UIAccess uiAccess = UIAccess.current();
        InputBoxBuilder.text()
            .title(AILocalize.titleAgentSkillNew())
            .text(AILocalize.labelAgentSkillName())
            .showAsync()
            .thenComposeAsync(name -> location(directories, AILocalize.titleAgentSkillNew())
                .thenAccept(path -> create(() -> AIAgentInstructionScanner.createSkill(projectDirectory.resolve(path), name), uiAccess)), uiAccess);
    }

    /**
     * Asks where a new instruction goes - unless there is one place only.
     */
    @RequiredUIAccess
    private static CompletableFuture<String> location(List<String> paths, LocalizeValue title) {
        if (paths.size() == 1) {
            return CompletableFuture.completedFuture(paths.getFirst());
        }
        return InputBoxBuilder.items(paths)
            .title(title)
            .text(AILocalize.labelAgentInstructionLocation())
            .value(paths.getFirst())
            .showAsync();
    }

    private void create(ThrowableSupplier<Path, IOException> creation, UIAccess uiAccess) {
        CompletableFuture.supplyAsync(() -> {
            try {
                Path file = creation.get();
                return LocalFileSystem.getInstance().refreshAndFindFileByNioFile(file);
            }
            catch (IOException e) {
                throw new CompletionException(e);
            }
        }, myConcurrency.executor()).whenCompleteAsync((file, error) -> {
            if (error != null) {
                Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
                LocalizeValue text = cause instanceof FileAlreadyExistsException exists
                    ? AILocalize.messageAgentInstructionExists(exists.getFile())
                    : LocalizeValue.of(String.valueOf(cause.getMessage()));
                MessageBoxes.okError(text).showAsync();
                return;
            }
            refresh();
            if (file != null) {
                FileEditorManager.getInstance(myProject).openFile(file, true);
            }
        }, uiAccess);
    }

    @RequiredUIAccess
    private void remove(AIAgentInstruction instruction, Component owner) {
        if (instruction.global()) {
            MessageBoxes.okWarning(AILocalize.messageAgentInstructionGlobalRemove(shown(instruction.removed())))
                .title(AILocalize.titleAgentInstructionRemove())
                .showAsync(owner);
            return;
        }
        UIAccess uiAccess = UIAccess.current();
        MessageBoxes.yesNo()
            .title(AILocalize.titleAgentInstructionRemove())
            .text(AILocalize.messageAgentInstructionRemove(shown(instruction.removed())))
            .asWarning()
            .showAsync(owner)
            .thenAcceptAsync(confirmed -> {
                if (!confirmed) {
                    return;
                }
                CompletableFuture.runAsync(() -> {
                    try {
                        AIAgentInstructionScanner.remove(instruction);
                    }
                    catch (IOException e) {
                        throw new CompletionException(e);
                    }
                    Path parent = instruction.removed().getParent();
                    VirtualFile directory = parent == null ? null : LocalFileSystem.getInstance().refreshAndFindFileByNioFile(parent);
                    if (directory != null) {
                        VirtualFileUtil.markDirtyAndRefresh(true, true, true, directory);
                    }
                }, myConcurrency.executor()).whenCompleteAsync((ignored, error) -> {
                    if (error != null) {
                        MessageBoxes.okError(error.getCause() != null ? error.getCause() : error).showAsync(owner);
                    }
                    refresh();
                }, uiAccess);
            }, uiAccess);
    }

    @RequiredUIAccess
    private void open(Path file) {
        UIAccess uiAccess = UIAccess.current();
        CompletableFuture.supplyAsync(() -> LocalFileSystem.getInstance().refreshAndFindFileByNioFile(file), myConcurrency.executor())
            .whenCompleteAsync((virtualFile, error) -> {
                if (virtualFile != null && !myProject.isDisposed()) {
                    FileEditorManager.getInstance(myProject).openFile(virtualFile, true);
                }
            }, uiAccess);
    }

    /**
     * A path as short as it can be told - relative to the project, or to the home directory.
     */
    private String shown(Path path) {
        Path projectDirectory = projectDirectory();
        if (projectDirectory != null && path.startsWith(projectDirectory)) {
            return projectDirectory.relativize(path).toString();
        }
        Path home = home();
        if (home != null && path.startsWith(home)) {
            return "~/" + home.relativize(path);
        }
        return path.toString();
    }

    private List<AIAgentInstructionProvider> providers() {
        return myApplication.getExtensionList(AIAgentInstructionProvider.class);
    }

    private @Nullable Path projectDirectory() {
        String basePath = myProject.getBasePath();
        return basePath == null ? null : Path.of(basePath);
    }

    private static @Nullable Path home() {
        String home = System.getProperty("user.home");
        return home == null ? null : Path.of(home);
    }
}
