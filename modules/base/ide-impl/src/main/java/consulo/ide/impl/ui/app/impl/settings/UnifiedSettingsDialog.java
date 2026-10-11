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
package consulo.ide.impl.ui.app.impl.settings;

import consulo.configurable.Configurable;
import consulo.configurable.Settings;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.configurable.ConfigurablePreselectStrategy;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.WidthAndHeight;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.dialog.DialogValue;
import consulo.ui.ex.dialog.WholeLeftDialogDescriptor;
import consulo.ui.ex.dialog.action.DialogCancelAction;
import consulo.ui.ex.dialog.action.DialogEscapeAction;
import consulo.ui.ex.dialog.action.DialogOkAction;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public class UnifiedSettingsDialog extends WholeLeftDialogDescriptor {
    private static final Logger LOG = Logger.getInstance(UnifiedSettingsDialog.class);

    private final Project myProject;
    private final Function<Project, Configurable[]> myConfigurablesBuilder;
    private final ConfigurablePreselectStrategy myPreselectStrategy;
    private final String myFilter;
    private final Consumer<UnifiedSettingsDialog> myAfterLoad;
    private final CompletableFuture<Void> myResult = new CompletableFuture<>();

    private @Nullable UnifiedOptionsEditor myEditor;

    public UnifiedSettingsDialog(
        Project project,
        Function<Project, Configurable[]> configurablesBuilder,
        ConfigurablePreselectStrategy preselectStrategy,
        String filter,
        Consumer<UnifiedSettingsDialog> afterLoad
    ) {
        super(Platform.current().os().isMac() ? CommonLocalize.titleSettingsMac() : CommonLocalize.titleSettings());
        myProject = project;
        myConfigurablesBuilder = configurablesBuilder;
        myPreselectStrategy = preselectStrategy;
        myFilter = filter;
        myAfterLoad = afterLoad;
    }

    public Project getProject() {
        return myProject;
    }

    public CompletableFuture<Void> getResult() {
        return myResult;
    }

    public Settings getSettings() {
        return Objects.requireNonNull(myEditor, "Dialog is not shown").getSettings();
    }

    @RequiredUIAccess
    public void reselect(ConfigurablePreselectStrategy preselectStrategy, String filter, Consumer<UnifiedSettingsDialog> afterLoad) {
        UnifiedOptionsEditor editor = myEditor;
        if (editor != null) {
            editor.doSelect(preselectStrategy, filter, () -> afterLoad.accept(this));
        }
    }

    @Override
    public @Nullable WidthAndHeight getInitialSize() {
        return WidthAndHeight.ofFont(80, 40);
    }

    @Override
    public @Nullable String getDimensionServiceKey() {
        return ShowSettingsUtil.DIMENSION_KEY;
    }

    @Override
    public boolean hasDefaultContentBorder() {
        return false;
    }

    @Override
    public String getLeftSplitterKey() {
        return OptionsEditorCore.MAIN_SPLITTER_PROPORTION;
    }

    @Override
    @RequiredUIAccess
    public Component createLeftComponent(Disposable uiDisposable) {
        return getOrCreateEditor(uiDisposable).getLeftComponent();
    }

    @Override
    @RequiredUIAccess
    public Component createCenterComponent(Disposable uiDisposable) {
        UnifiedOptionsEditor editor = getOrCreateEditor(uiDisposable);

        editor.load(myPreselectStrategy, myFilter, () -> myAfterLoad.accept(this));

        return editor.getCenterComponent();
    }

    @RequiredUIAccess
    private UnifiedOptionsEditor getOrCreateEditor(Disposable uiDisposable) {
        UnifiedOptionsEditor existing = myEditor;
        if (existing != null) {
            return existing;
        }

        UnifiedOptionsEditor editor = new UnifiedOptionsEditor(myProject, myConfigurablesBuilder);
        Disposer.register(uiDisposable, editor);
        myEditor = editor;

        editor.getCore().getContext().addColleague(new OptionsEditorColleague() {
            @Override
            @RequiredUIAccess
            public CompletableFuture<Void> onModifiedAdded(Configurable configurable) {
                updateOkButtonState();
                return CompletableFuture.completedFuture(null);
            }

            @Override
            @RequiredUIAccess
            public CompletableFuture<Void> onModifiedRemoved(Configurable configurable) {
                updateOkButtonState();
                return CompletableFuture.completedFuture(null);
            }

            @Override
            @RequiredUIAccess
            public CompletableFuture<Void> onErrorsChanged() {
                updateOkButtonState();
                return CompletableFuture.completedFuture(null);
            }
        });

        return editor;
    }

    @Override
    @RequiredUIAccess
    public @Nullable Component getPreferredFocusedComponent() {
        UnifiedOptionsEditor editor = myEditor;
        return editor == null ? null : editor.getPreferredFocusedComponent();
    }

    @Override
    public AnAction[] createActions(boolean inverseOrder) {
        AnAction okAction = createOkAction();
        AnAction cancelAction = new DialogCancelAction();
        AnAction applyAction = new ApplyAction();
        if (inverseOrder) {
            return new AnAction[]{applyAction, cancelAction, okAction};
        }
        return new AnAction[]{okAction, cancelAction, applyAction};
    }

    @Override
    public boolean hasHelp() {
        return true;
    }

    @Override
    public @Nullable String getHelpId() {
        UnifiedOptionsEditor editor = myEditor;
        return editor == null ? null : editor.getCore().getHelpTopic();
    }

    @Override
    @RequiredUIAccess
    public boolean canHandle(AnAction action, @Nullable DialogValue value) {
        UnifiedOptionsEditor editor = myEditor;
        if (editor == null) {
            return true;
        }

        OptionsEditorCore core = editor.getCore();

        if (action instanceof DialogCancelAction) {
            if (action instanceof DialogEscapeAction && core.getContext().isHoldingFilter()) {
                editor.clearFilter();
                return false;
            }
            return true;
        }

        if (action instanceof DialogOkAction) {
            core.flushModifications();

            if (core.canApply()) {
                core.apply();

                if (editor.hasErrors()) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public void onHandleValue(AnAction action, @Nullable DialogValue value) {
        saveCurrentConfigurable();

        if (action instanceof DialogOkAction) {
            myProject.getApplication().saveAllWithProgress(UIAccess.current());
        }
    }

    private void saveCurrentConfigurable() {
        UnifiedOptionsEditor editor = myEditor;
        if (editor == null || !editor.getCore().isConfigurablesLoaded()) {
            return;
        }

        Configurable current = editor.getCore().getContext().getCurrentConfigurable();
        if (current == null) {
            return;
        }

        try {
            myPreselectStrategy.save(current);
        }
        catch (Exception e) {
            LOG.warn(e);
        }
    }

    private class ApplyAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private ApplyAction() {
            super(CommonLocalize.buttonApply());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            UnifiedOptionsEditor editor = myEditor;
            if (editor != null) {
                editor.getCore().apply();
                updateOkButtonState();
            }
        }

        @Override
        public void update(AnActionEvent e) {
            UnifiedOptionsEditor editor = myEditor;
            e.getPresentation().setEnabled(editor != null && editor.getCore().canApply());
        }
    }
}
