/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.desktop.awt.settings;

import consulo.annotation.component.ServiceImpl;
import consulo.configurable.Configurable;
import consulo.configurable.SearchableConfigurable;
import consulo.configurable.UnnamedConfigurable;
import consulo.content.bundle.SdkTable;
import consulo.disposer.Disposer;
import consulo.ide.impl.base.BaseShowSettingsUtil;
import consulo.ide.impl.configurable.BaseProjectStructureShowSettingsUtil;
import consulo.ide.impl.configurable.ConfigurablePreselectStrategy;
import consulo.ide.impl.configurable.DefaultConfigurablePreselectStrategy;
import consulo.ide.impl.idea.openapi.options.ex.SingleConfigurableEditor;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.DefaultSdksModel;
import consulo.ide.setting.ProjectStructureSelector;
import consulo.configurable.Settings;
import consulo.ide.setting.bundle.SettingsSdksModel;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.internal.DefaultProjectFactory;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.internal.ModalityPerProjectEAPDescriptor;
import consulo.ui.ex.awt.update.UiNotifyConnector;
import consulo.ui.ex.update.Activatable;
import consulo.util.concurrent.AsyncResult;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author max
 */
@Singleton
@ServiceImpl
public class DesktopShowSettingsUtilImpl extends BaseProjectStructureShowSettingsUtil {
    private static final Logger LOG = Logger.getInstance(DesktopShowSettingsUtilImpl.class);

    private final AtomicBoolean myShown = new AtomicBoolean(false);

    private final DefaultProjectFactory myDefaultProjectFactory;

    private final DefaultSdksModel mySdksModel;

    @Inject
    DesktopShowSettingsUtilImpl(DefaultProjectFactory defaultProjectFactory, Provider<SdkTable> sdkTableProvider) {
        myDefaultProjectFactory = defaultProjectFactory;
        mySdksModel = new DefaultSdksModel(sdkTableProvider);
    }

    @Nonnull
    @Override
    public SettingsSdksModel getSdksModel() {
        mySdksModel.initializeIfNeed();
        return mySdksModel;
    }

    @RequiredUIAccess
    private AsyncResult<Void> showSettingsImpl(
        @Nullable Project tempProject,
        @Nonnull Function<Project, Configurable[]> configurableBuilder,
        @Nonnull ConfigurablePreselectStrategy strategy,
        @Nonnull Consumer<DesktopSettingsDialog> onShow
    ) {
        Project actualProject = tempProject != null ? tempProject : myDefaultProjectFactory.getDefaultProject();

        AsyncResult<Void> result = AsyncResult.undefined();

        UIAccess uiAccess = UIAccess.current();
        uiAccess.give(() -> {
            myShown.set(true);

            DesktopSettingsDialog dialog;
            if (ModalityPerProjectEAPDescriptor.is()) {
                dialog = new DesktopSettingsDialog(actualProject, configurableBuilder, strategy, true, onShow);
            }
            else {
                dialog = new DesktopSettingsDialog(actualProject, configurableBuilder, strategy, onShow);
            }

            Disposer.register(dialog.getDisposable(), this::clearCaches);

            dialog.showAsync().doWhenProcessed(() -> myShown.set(false)).notify(result);
        });

        return result;
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    @RequiredUIAccess
    @Override
    public <T extends UnnamedConfigurable> AsyncResult<Void> showAndSelect(
        @Nullable Project project,
        @Nonnull Class<T> configurableClass,
        @Nonnull Consumer<T> afterSelect
    ) {
        assert Configurable.class.isAssignableFrom(configurableClass) : "Not a configurable: " + configurableClass.getName();

        return showSettingsImpl(project, BaseShowSettingsUtil::buildConfigurables, ConfigurablePreselectStrategy.notSelected(), dialog -> {
            final Settings editor = dialog.getDataUnchecked(Settings.KEY);
            assert editor != null;
            editor.select(configurableClass).doWhenDone(afterSelect);
        });
    }

    @Nonnull
    @RequiredUIAccess
    @Override
    public AsyncResult<Void> showSettingsDialog(@Nullable Project project) {
        return showSettingsImpl(
            project,
            BaseShowSettingsUtil::buildConfigurables,
            ConfigurablePreselectStrategy.lastStored(project == null ? myDefaultProjectFactory.getDefaultProject() : project),
            dialog -> {
            }
        );
    }

    @Nonnull
    @RequiredUIAccess
    @Override
    public AsyncResult<Void> showSettingsDialog(@Nullable final Project project, @Nonnull final String nameToSelect) {
        return showSettingsImpl(
            project,
            BaseShowSettingsUtil::buildConfigurables,
            configurables -> DefaultConfigurablePreselectStrategy.getPreselectedByDisplayName(configurables, nameToSelect, project),
            dialog -> {
            }
        );
    }

    @Nonnull
    @Override
    @RequiredUIAccess
    public AsyncResult<Void> showSettingsDialog(@Nullable Project project, final String id2Select, final String filter) {
        return showSettingsImpl(
            project,
            BaseShowSettingsUtil::buildConfigurables,
            configurables -> findConfigurable2Select(id2Select, configurables),
            dialog -> {
            }
        );
    }

    @Nullable
    private static Configurable findConfigurable2Select(String id2Select, Configurable[] configurables) {
        for (Configurable configurable : configurables) {
            final Configurable conf = containsId(id2Select, configurable);
            if (conf != null) {
                return conf;
            }
        }
        return null;
    }

    @Nullable
    private static Configurable containsId(String id2Select, Configurable configurable) {
        if (id2Select.equals(configurable.getId())) {
            return configurable;
        }
        if (configurable instanceof SearchableConfigurable.Parent) {
            for (Configurable subConfigurable : ((SearchableConfigurable.Parent) configurable).getConfigurables()) {
                final Configurable config = containsId(id2Select, subConfigurable);
                if (config != null) {
                    return config;
                }
            }
        }
        return null;
    }

    @Nonnull
    @RequiredUIAccess
    @Override
    public AsyncResult<Void> showSettingsDialog(@Nullable Project project, @Nullable Configurable toSelect) {
        return showSettingsImpl(
            project,
            BaseShowSettingsUtil::buildConfigurables,
            ConfigurablePreselectStrategy.preOrNotSelected(toSelect),
            dialog -> {
            }
        );
    }

    @RequiredUIAccess
    @Override
    public AsyncResult<Void> showProjectStructureDialog(@Nonnull Project project, @Nonnull Consumer<ProjectStructureSelector> consumer) {
        return showSettingsImpl(project, BaseShowSettingsUtil::buildConfigurables, ConfigurablePreselectStrategy.notSelected(), dialog -> {
            final ProjectStructureSelector editor = dialog.getDataUnchecked(ProjectStructureSelector.KEY);
            assert editor != null;
            consumer.accept(editor);
        });
    }

    @RequiredUIAccess
    @Override
    public AsyncResult<Void> editConfigurable(@Nullable String title, Project project, Configurable configurable) {
        return editConfigurable(title, project, createDimensionKey(configurable), configurable);
    }

    @RequiredUIAccess
    @Override
    public AsyncResult<Void> editConfigurable(
        @Nullable String title,
        Project project,
        String dimensionServiceKey,
        @Nonnull Configurable configurable
    ) {
        return editConfigurable(null, project, configurable, title, dimensionServiceKey, null);
    }

    @Override
    @RequiredUIAccess
    public AsyncResult<Void> editConfigurable(String title, Project project, Configurable configurable, Runnable advancedInitialization) {
        return editConfigurable(null, project, configurable, title, createDimensionKey(configurable), advancedInitialization);
    }

    @RequiredUIAccess
    @Override
    public AsyncResult<Void> editConfigurable(Component parent, Configurable configurable) {
        return editConfigurable(parent, configurable, null);
    }

    @RequiredUIAccess
    @Override
    public AsyncResult<Void> editConfigurable(
        final Component parent,
        final Configurable configurable,
        @Nullable final Runnable advancedInitialization
    ) {
        return editConfigurable(parent, null, configurable, null, createDimensionKey(configurable), advancedInitialization);
    }

    @RequiredUIAccess
    private static AsyncResult<Void> editConfigurable(
        @Nullable Component parent,
        @Nullable Project project,
        Configurable configurable,
        String title,
        String dimensionKey,
        @Nullable final Runnable advancedInitialization
    ) {
        SingleConfigurableEditor editor;
        if (parent != null) {
            editor = new SingleConfigurableEditor(parent, configurable, title, dimensionKey, true, DialogWrapper.IdeModalityType.IDE);
        }
        else {
            editor = new SingleConfigurableEditor(project, configurable, title, dimensionKey, true, DialogWrapper.IdeModalityType.IDE);
        }
        if (advancedInitialization != null) {
            new UiNotifyConnector.Once(editor.getContentPane(), new Activatable() {
                @Override
                public void showNotify() {
                    advancedInitialization.run();
                }
            });
        }
        return editor.showAsync();
    }

    @RequiredUIAccess
    @Override
    public AsyncResult<Void> editConfigurable(Component parent, String dimensionServiceKey, Configurable configurable) {
        return editConfigurable(parent, null, configurable, null, dimensionServiceKey, null);
    }

    @Override
    public boolean isAlreadyShown(Project project) {
        return myShown.get();
    }
}