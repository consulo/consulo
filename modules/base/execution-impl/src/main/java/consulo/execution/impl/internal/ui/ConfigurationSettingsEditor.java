/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.execution.impl.internal.ui;

import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.ConfigurationPerRunnerSettings;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.RunnerSettings;
import consulo.execution.configuration.ui.CheckableRunConfigurationEditor;
import consulo.execution.configuration.ui.CompositeSettingsBuilder;
import consulo.execution.configuration.ui.CompositeSettingsEditor;
import consulo.execution.configuration.ui.RunConfigurationSettingsEditor;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.execution.configuration.ui.SettingsEditorGroup;
import consulo.execution.configuration.ui.SettingsEditorWrapper;
import consulo.execution.executor.Executor;
import consulo.execution.executor.ExecutorRegistry;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.runner.ProgramRunner;
import consulo.execution.runner.RunnerRegistry;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.ListBox;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.ui.style.ComponentColors;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author dyoma
 */
class ConfigurationSettingsEditor extends CompositeSettingsEditor<RunnerAndConfigurationSettings> {
    private static final Logger LOG = Logger.getInstance(ConfigurationSettingsEditor.class);

    private final List<SettingsEditor<RunnerAndConfigurationSettings>> myRunnerEditors = new ArrayList<>();
    private final RunConfiguration myConfiguration;
    private final SettingsEditor<RunConfiguration> myConfigurationEditor;
    private @Nullable SettingsEditorGroup<RunnerAndConfigurationSettings> myCompound;

    public ConfigurationSettingsEditor(RunnerAndConfigurationSettings settings) {
        super(settings.createFactory());
        myConfiguration = settings.getConfiguration();
        myConfigurationEditor = createConfigurationEditor(myConfiguration);
        Disposer.register(this, myConfigurationEditor);
    }

    @SuppressWarnings("unchecked")
    private static SettingsEditor<RunConfiguration> createConfigurationEditor(RunConfiguration configuration) {
        try {
            return (SettingsEditor<RunConfiguration>) configuration.getConfigurationEditor();
        }
        catch (Throwable e) {
            LOG.error("Run configuration editor can not be created: " + configuration.getClass().getName(), e);
            return new NotSupportedEditor();
        }
    }

    @RequiredUIAccess
    @Override
    public CompositeSettingsBuilder<RunnerAndConfigurationSettings> getBuilder() {
        return new GroupSettingsBuilder<>(init());
    }

    @RequiredUIAccess
    private SettingsEditorGroup<RunnerAndConfigurationSettings> init() {
        SettingsEditorGroup<RunnerAndConfigurationSettings> compound = myCompound;
        if (compound != null) {
            return compound;
        }

        compound = new SettingsEditorGroup<>();
        Disposer.register(this, compound);
        myCompound = compound;

        if (myConfigurationEditor instanceof SettingsEditorGroup<RunConfiguration> group) {
            for (Pair<LocalizeValue, SettingsEditor<RunConfiguration>> pair : group.getEditors()) {
                compound.addEditor(pair.getFirst(), new ConfigToSettingsWrapper(pair.getSecond()));
            }
        }
        else {
            compound.addEditor(ExecutionLocalize.runConfigurationConfigurationTabTitle(), new ConfigToSettingsWrapper(myConfigurationEditor));
        }

        RunnersEditorComponent runnersComponent = new RunnersEditorComponent();

        ProgramRunner[] runners = RunnerRegistry.getInstance().getRegisteredRunners();
        for (Executor executor : ExecutorRegistry.getInstance().getRegisteredExecutors()) {
            for (ProgramRunner runner : runners) {
                if (runner.canRun(executor.getId(), myConfiguration)) {
                    Component perRunnerSettings = createCompositePerRunnerSettings(executor, runner);
                    if (perRunnerSettings != null) {
                        runnersComponent.addExecutorComponent(executor, perRunnerSettings);
                    }
                }
            }
        }

        if (!myRunnerEditors.isEmpty()) {
            compound.addEditor(
                ExecutionLocalize.runConfigurationStartupConnectionRabTitle(),
                new CompositeSettingsEditor<>(getFactory()) {
                    @Override
                    public CompositeSettingsBuilder<RunnerAndConfigurationSettings> getBuilder() {
                        return new CompositeSettingsBuilder<>() {
                            @Override
                            public Collection<SettingsEditor<RunnerAndConfigurationSettings>> getEditors() {
                                return myRunnerEditors;
                            }

                            @RequiredUIAccess
                            @Override
                            public Component createCompoundEditor(Disposable disposable) {
                                return runnersComponent.getComponent();
                            }
                        };
                    }
                }
            );
        }
        return compound;
    }

    @RequiredUIAccess
    private @Nullable Component createCompositePerRunnerSettings(Executor executor, ProgramRunner runner) {
        SettingsEditor<ConfigurationPerRunnerSettings> configEditor = myConfiguration.getRunnerSettingsEditor(runner);
        SettingsEditor<RunnerSettings> runnerEditor = runner.getSettingsEditor(executor, myConfiguration);
        if (configEditor == null && runnerEditor == null) {
            return null;
        }

        Component configComponent = null;
        if (configEditor != null) {
            SettingsEditor<RunnerAndConfigurationSettings> wrappedConfigEditor = new SettingsEditorWrapper<>(
                configEditor,
                configurationSettings -> configurationSettings.getConfigurationSettings(runner)
            );
            myRunnerEditors.add(wrappedConfigEditor);
            Disposer.register(this, wrappedConfigEditor);
            configComponent = wrappedConfigEditor.getUIComponent();
        }

        Component runComponent = null;
        if (runnerEditor != null) {
            SettingsEditor<RunnerAndConfigurationSettings> wrappedRunEditor = new SettingsEditorWrapper<>(
                runnerEditor,
                configurationSettings -> configurationSettings.getRunnerSettings(runner)
            );
            myRunnerEditors.add(wrappedRunEditor);
            Disposer.register(this, wrappedRunEditor);
            runComponent = wrappedRunEditor.getUIComponent();
        }

        if (configComponent != null && runComponent != null) {
            DockLayout panel = DockLayout.create(Space.SMALL);
            panel.center(configComponent);
            panel.bottom(runComponent);
            return panel;
        }
        return runComponent != null ? runComponent : configComponent;
    }

    @Override
    public RunnerAndConfigurationSettings getSnapshot() throws ConfigurationException {
        RunnerAndConfigurationSettings settings = getFactory().get();
        settings.setName(myConfiguration.getName());
        if (myConfigurationEditor instanceof CheckableRunConfigurationEditor checkableEditor) {
            @SuppressWarnings("unchecked")
            CheckableRunConfigurationEditor<RunConfiguration> editor = checkableEditor;
            editor.checkEditorData(settings.getConfiguration());
        }
        else {
            applyTo(settings);
        }
        return settings;
    }

    private static class RunnersEditorComponent {
        private final MutableFlatDataModel<Executor> myModel = FlatDataModel.of(List.of());
        private final Map<Executor, Component> myComponents = new HashMap<>();
        private final ListBox<Executor> myRunnersList;
        private final DockLayout myRunnerPanel;
        private final Label myNoRunner;
        private final DockLayout myComponent;

        @RequiredUIAccess
        RunnersEditorComponent() {
            ListBox<Executor> runnersList = ListBox.create(myModel);
            myRunnersList = runnersList;
            runnersList.setRender((presentation, item) -> {
                Executor executor = item.getValue();
                if (executor != null) {
                    presentation.withIcon(executor.getIcon());
                    presentation.append(executor.getId());
                }
            });
            runnersList.addValueListener(event -> showRunnerComponent(event.getValue()));

            myNoRunner = Label.create(ExecutionLocalize.runConfigurationNorunnerSelectedLabel());
            myNoRunner.setForegroundColor(ComponentColors.DISABLED_TEXT);

            myRunnerPanel = DockLayout.create();
            myRunnerPanel.paddingBuilder().leftSet(Space.MEDIUM).apply();
            myRunnerPanel.center(myNoRunner);

            myComponent = DockLayout.create();
            myComponent.left(ScrollableLayout.create(runnersList));
            myComponent.center(myRunnerPanel);
        }

        @RequiredUIAccess
        private void showRunnerComponent(@Nullable Executor executor) {
            Component component = executor == null ? null : myComponents.get(executor);
            myRunnerPanel.center(component == null ? myNoRunner : component);
        }

        @RequiredUIAccess
        void addExecutorComponent(Executor executor, Component component) {
            myComponents.put(executor, component);
            myModel.add(executor);
            if (myRunnersList.getValue() == null) {
                myRunnersList.setValueByIndex(0);
            }
        }

        Component getComponent() {
            return myComponent;
        }
    }

    private class ConfigToSettingsWrapper extends SettingsEditor<RunnerAndConfigurationSettings> {
        private final SettingsEditor<RunConfiguration> myConfigEditor;

        public ConfigToSettingsWrapper(SettingsEditor<RunConfiguration> configEditor) {
            myConfigEditor = configEditor;
            if (configEditor instanceof RunConfigurationSettingsEditor runConfigurationSettingsEditor) {
                runConfigurationSettingsEditor.setOwner(ConfigurationSettingsEditor.this);
            }
            configEditor.addSettingsEditorListener(editor -> fireEditorStateChanged());
        }

        @Override
        public void resetEditorFrom(RunnerAndConfigurationSettings configurationSettings) {
            myConfigEditor.resetFrom(configurationSettings.getConfiguration());
        }

        @Override
        public void applyEditorTo(RunnerAndConfigurationSettings configurationSettings) throws ConfigurationException {
            myConfigEditor.applyTo(configurationSettings.getConfiguration());
        }

        @RequiredUIAccess
        @Override
        protected Component createUIComponent() {
            return myConfigEditor.getUIComponent();
        }

        @Override
        public void disposeEditor() {
            Disposer.dispose(myConfigEditor);
        }
    }

    private static class NotSupportedEditor extends SettingsEditor<RunConfiguration> {
        @Override
        protected void resetEditorFrom(RunConfiguration configuration) {
        }

        @Override
        protected void applyEditorTo(RunConfiguration configuration) {
        }

        @RequiredUIAccess
        @Override
        protected Component createUIComponent() {
            Label label = Label.create(ExecutionLocalize.runConfigurationEditorNotSupported());
            label.setForegroundColor(ComponentColors.DISABLED_TEXT);
            return label;
        }
    }
}
