/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ide.impl.configurable;

import consulo.annotation.component.ServiceImpl;
import consulo.configurable.Configurable;
import consulo.configurable.UnnamedConfigurable;
import consulo.configurable.internal.ConfigurableWrapper;
import consulo.content.bundle.SdkTable;
import consulo.ide.impl.base.BaseShowSettingsUtil;
import consulo.ide.impl.ui.app.impl.settings.UnifiedSettingsDialog;
import consulo.ide.impl.ui.app.impl.settings.UnifiedSingleConfigurableEditor;
import consulo.ide.setting.ProjectStructureSelector;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.internal.DefaultProjectFactory;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogService;
import consulo.util.dataholder.Key;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2019-02-02
 */
@Singleton
@ServiceImpl
public class UnifiedShowSettingsUtil extends BaseProjectStructureShowSettingsUtil {
    private static final Key<Map<Project, UnifiedSettingsDialog>> SHOWN_DIALOGS = Key.create("UnifiedShowSettingsUtil.SHOWN_DIALOGS");

    private final DefaultProjectFactory myDefaultProjectFactory;
    private final Provider<DialogService> myDialogService;

    @Inject
    public UnifiedShowSettingsUtil(
        DefaultProjectFactory defaultProjectFactory,
        Provider<SdkTable> sdkTableProvider,
        Provider<DialogService> dialogService
    ) {
        super(sdkTableProvider);
        myDefaultProjectFactory = defaultProjectFactory;
        myDialogService = dialogService;
    }

    @Override
    @RequiredUIAccess
    public CompletableFuture<Void> showSettingsDialog(@Nullable Project project) {
        Project actualProject = resolveProject(project);
        return showSettingsImpl(actualProject, ConfigurablePreselectStrategy.lastStored(actualProject), "", dialog -> {
        });
    }

    @Override
    @RequiredUIAccess
    public <T extends UnnamedConfigurable> CompletableFuture<Void> showAndSelect(
        @Nullable Project project,
        Class<T> toSelect,
        Consumer<T> afterSelect
    ) {
        Project actualProject = resolveProject(project);
        ConfigurablePreselectStrategy lastStored = ConfigurablePreselectStrategy.lastStored(actualProject);
        ConfigurablePreselectStrategy strategy = configurables -> containsConfigurable(configurables, toSelect)
            ? SKIP_SELECTION_CONFIGURATION
            : lastStored.get(configurables);

        return showSettingsImpl(actualProject, strategy, "", dialog -> {
            dialog.getSettings().select(toSelect).whenComplete((configurable, error) -> {
                if (error != null || configurable == null) {
                    return;
                }

                afterSelect.accept(configurable);
            });
        });
    }

    @Override
    @RequiredUIAccess
    public CompletableFuture<Void> showSettingsDialog(@Nullable Project project, String nameToSelect) {
        Project actualProject = resolveProject(project);
        return showSettingsImpl(
            actualProject,
            configurables -> DefaultConfigurablePreselectStrategy.getPreselectedByDisplayName(configurables, nameToSelect, actualProject),
            "",
            dialog -> {
            }
        );
    }

    @Override
    @RequiredUIAccess
    public CompletableFuture<Void> showSettingsDialog(@Nullable Project project, String id2Select, String filter) {
        Project actualProject = resolveProject(project);
        return showSettingsImpl(
            actualProject,
            configurables -> DefaultConfigurablePreselectStrategy.findById(id2Select, configurables),
            filter,
            dialog -> {
            }
        );
    }

    @Override
    @RequiredUIAccess
    public CompletableFuture<Void> showSettingsDialog(@Nullable Project project, @Nullable Configurable toSelect) {
        return showSettingsImpl(resolveProject(project), ConfigurablePreselectStrategy.preOrNotSelected(toSelect), "", dialog -> {
        });
    }

    @Override
    @RequiredUIAccess
    public CompletableFuture<Void> showProjectStructureDialog(Project project, Consumer<ProjectStructureSelector> consumer) {
        return showSettingsImpl(
            project,
            ConfigurablePreselectStrategy.notSelected(),
            "",
            dialog -> consumer.accept(new ProjectStructureSelectorOverSettings(dialog.getSettings()))
        );
    }

    @Override
    @RequiredUIAccess
    public CompletableFuture<Void> editConfigurable(@Nullable String title, Project project, Configurable configurable) {
        return editConfigurableImpl(null, configurable, title, createDimensionKey(configurable), null);
    }

    @Override
    @RequiredUIAccess
    public CompletableFuture<Void> editConfigurable(
        @Nullable String title,
        Project project,
        Configurable configurable,
        Runnable advancedInitialization
    ) {
        return editConfigurableImpl(null, configurable, title, createDimensionKey(configurable), advancedInitialization);
    }

    @Override
    @RequiredUIAccess
    public CompletableFuture<Void> editConfigurable(Component parent, Configurable configurable) {
        return editConfigurableImpl(parent, configurable, null, createDimensionKey(configurable), null);
    }

    @Override
    @RequiredUIAccess
    public CompletableFuture<Void> editConfigurable(Component parent, Configurable configurable, Runnable advancedInitialization) {
        return editConfigurableImpl(parent, configurable, null, createDimensionKey(configurable), advancedInitialization);
    }

    @Override
    @RequiredUIAccess
    public CompletableFuture<Void> editConfigurable(
        @Nullable String title,
        Project project,
        String dimensionServiceKey,
        Configurable configurable
    ) {
        return editConfigurableImpl(null, configurable, title, dimensionServiceKey, null);
    }

    @Override
    @RequiredUIAccess
    public CompletableFuture<Void> editConfigurable(Component parent, String dimensionServiceKey, Configurable configurable) {
        return editConfigurableImpl(parent, configurable, null, dimensionServiceKey, null);
    }

    @Override
    public boolean isAlreadyShown(Project project) {
        if (!UIAccess.isUIThread()) {
            return false;
        }

        Map<Project, UnifiedSettingsDialog> dialogs = UIAccess.current().getUserData(SHOWN_DIALOGS);
        return dialogs != null && dialogs.containsKey(project);
    }

    @RequiredUIAccess
    private CompletableFuture<Void> showSettingsImpl(
        Project project,
        ConfigurablePreselectStrategy preselectStrategy,
        String filter,
        Consumer<UnifiedSettingsDialog> afterLoad
    ) {
        UIAccess uiAccess = UIAccess.current();

        Map<Project, UnifiedSettingsDialog> dialogs = uiAccess.getUserData(SHOWN_DIALOGS);
        if (dialogs == null) {
            dialogs = new HashMap<>();
            uiAccess.putUserData(SHOWN_DIALOGS, dialogs);
        }

        UnifiedSettingsDialog shownDialog = dialogs.get(project);
        if (shownDialog != null) {
            shownDialog.reselect(preselectStrategy, filter, afterLoad);
            return shownDialog.getResult();
        }

        UnifiedSettingsDialog settingsDialog = new UnifiedSettingsDialog(
            project,
            BaseShowSettingsUtil::buildConfigurables,
            preselectStrategy,
            filter,
            afterLoad
        );

        Window activeWindow = Window.getActiveWindow();
        Dialog dialog = activeWindow == null
            ? myDialogService.get().build(settingsDialog)
            : myDialogService.get().build(activeWindow, settingsDialog);

        Map<Project, UnifiedSettingsDialog> shownDialogs = dialogs;
        shownDialogs.put(project, settingsDialog);

        CompletableFuture<Void> result = settingsDialog.getResult();
        dialog.showAsync().whenCompleteAsync((value, error) -> {
            shownDialogs.remove(project, settingsDialog);

            clearCaches();

            if (error == null) {
                result.complete(null);
            }
            else {
                result.completeExceptionally(error);
            }
        }, uiAccess);
        return result;
    }

    @RequiredUIAccess
    private CompletableFuture<Void> editConfigurableImpl(
        @Nullable Component parent,
        Configurable configurable,
        @Nullable String title,
        String dimensionKey,
        @Nullable Runnable advancedInitialization
    ) {
        UIAccess uiAccess = UIAccess.current();

        UnifiedSingleConfigurableEditor editor =
            new UnifiedSingleConfigurableEditor(configurable, LocalizeValue.ofNullable(title), dimensionKey);

        DialogService dialogService = myDialogService.get();
        Component owner = parent == null ? Window.getActiveWindow() : parent;
        Dialog dialog = owner == null ? dialogService.build(editor) : dialogService.build(owner, editor);

        CompletableFuture<Void> result = new CompletableFuture<>();
        dialog.showAsync().whenCompleteAsync((value, error) -> {
            if (error == null) {
                result.complete(null);
            }
            else {
                result.completeExceptionally(new CancellationException());
            }
        }, uiAccess);

        if (advancedInitialization != null) {
            uiAccess.execute(() -> editor.runAdvancedInitialization(advancedInitialization));
        }
        return result;
    }

    private Project resolveProject(@Nullable Project project) {
        return project == null ? myDefaultProjectFactory.getDefaultProject() : project;
    }

    private static boolean containsConfigurable(Configurable[] configurables, Class<?> configurableClass) {
        for (Configurable configurable : configurables) {
            if (ConfigurableWrapper.cast(configurable, configurableClass) != null) {
                return true;
            }

            if (configurable instanceof Configurable.Composite composite && containsConfigurable(composite.getConfigurables(), configurableClass)) {
                return true;
            }
        }
        return false;
    }
}
