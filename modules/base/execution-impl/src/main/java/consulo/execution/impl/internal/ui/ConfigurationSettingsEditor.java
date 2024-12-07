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
import consulo.execution.RunnerRegistry;
import consulo.execution.configuration.ConfigurationPerRunnerSettings;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.RunnerSettings;
import consulo.execution.configuration.ui.*;
import consulo.execution.executor.Executor;
import consulo.execution.executor.ExecutorRegistry;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.runner.ProgramRunner;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.awt.ScrollingUtil;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * @author dyoma
 */
class ConfigurationSettingsEditor extends CompositeSettingsEditor<RunnerAndConfigurationSettings> {
    private final ArrayList<SettingsEditor<RunnerAndConfigurationSettings>> myRunnerEditors =
        new ArrayList<SettingsEditor<RunnerAndConfigurationSettings>>();
    private RunnersEditorComponent myRunnersComponent;
    private final RunConfiguration myConfiguration;
    private final SettingsEditor<RunConfiguration> myConfigurationEditor;
    private SettingsEditorGroup<RunnerAndConfigurationSettings> myCompound;

    @Override
    public CompositeSettingsBuilder<RunnerAndConfigurationSettings> getBuilder() {
        init();
        return new GroupSettingsBuilder<RunnerAndConfigurationSettings>(myCompound);
    }

    private void init() {
        if (myCompound == null) {
            myCompound = new SettingsEditorGroup<RunnerAndConfigurationSettings>();
            Disposer.register(this, myCompound);
            if (myConfigurationEditor instanceof SettingsEditorGroup) {
                SettingsEditorGroup<RunConfiguration> group = (SettingsEditorGroup<RunConfiguration>) myConfigurationEditor;
                List<Pair<String, SettingsEditor<RunConfiguration>>> editors = group.getEditors();
                for (Pair<String, SettingsEditor<RunConfiguration>> pair : editors) {
                    myCompound.addEditor(pair.getFirst(), new ConfigToSettingsWrapper(pair.getSecond()));
                }
            }
            else {
                myCompound.addEditor(
                    ExecutionLocalize.runConfigurationConfigurationTabTitle().get(),
                    new ConfigToSettingsWrapper(myConfigurationEditor)
                );
            }


            myRunnersComponent = new RunnersEditorComponent();
            ProgramRunner[] runners = RunnerRegistry.getInstance().getRegisteredRunners();

            final Executor[] executors = ExecutorRegistry.getInstance().getRegisteredExecutors();
            for (final Executor executor : executors) {
                for (ProgramRunner runner : runners) {
                    if (runner.canRun(executor.getId(), myConfiguration)) {
                        JComponent perRunnerSettings = createCompositePerRunnerSettings(executor, runner);
                        if (perRunnerSettings != null) {
                            myRunnersComponent.addExecutorComponent(executor, perRunnerSettings);
                        }
                    }
                }
            }

            if (myRunnerEditors.size() > 0) {
                myCompound.addEditor(
                    ExecutionLocalize.runConfigurationStartupConnectionRabTitle().get(),
                    new CompositeSettingsEditor<>(getFactory()) {
                        @Override
                        public CompositeSettingsBuilder<RunnerAndConfigurationSettings> getBuilder() {
                            return new CompositeSettingsBuilder<>() {
                                @Nonnull
                                @Override
                                public Collection<SettingsEditor<RunnerAndConfigurationSettings>> getEditors() {
                                    return myRunnerEditors;
                                }

                                @Nonnull
                                @Override
                                public JComponent createCompoundEditor(@Nonnull Disposable disposable) {
                                    return myRunnersComponent.getComponent();
                                }
                            };
                        }
                    }
                );
            }
        }
    }

    private JComponent createCompositePerRunnerSettings(final Executor executor, final ProgramRunner runner) {
        final SettingsEditor<ConfigurationPerRunnerSettings> configEditor = myConfiguration.getRunnerSettingsEditor(runner);
        SettingsEditor<RunnerSettings> runnerEditor;

        try {
            runnerEditor = runner.getSettingsEditor(executor, myConfiguration);
        }
        catch (AbstractMethodError error) {
            // this is stub code for plugin compatibility!
            runnerEditor = null;
        }

        if (configEditor == null && runnerEditor == null) {
            return null;
        }
        SettingsEditor<RunnerAndConfigurationSettings> wrappedConfigEditor = null;
        SettingsEditor<RunnerAndConfigurationSettings> wrappedRunEditor = null;
        if (configEditor != null) {
            wrappedConfigEditor = new SettingsEditorWrapper<>(
                configEditor,
                (Function<RunnerAndConfigurationSettings, ConfigurationPerRunnerSettings>) configurationSettings ->
                    configurationSettings.getConfigurationSettings(runner)
            );
            myRunnerEditors.add(wrappedConfigEditor);
            Disposer.register(this, wrappedConfigEditor);
        }

        if (runnerEditor != null) {
            wrappedRunEditor = new SettingsEditorWrapper<>(
                runnerEditor,
                (Function<RunnerAndConfigurationSettings, RunnerSettings>) configurationSettings -> configurationSettings.getRunnerSettings(runner)
            );
            myRunnerEditors.add(wrappedRunEditor);
            Disposer.register(this, wrappedRunEditor);
        }

        if (wrappedRunEditor != null && wrappedConfigEditor != null) {
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(wrappedConfigEditor.getComponent(), BorderLayout.CENTER);
            JComponent wrappedRunEditorComponent = wrappedRunEditor.getComponent();
            wrappedRunEditorComponent.setBorder(IdeBorderFactory.createEmptyBorder(3, 0, 0, 0));
            panel.add(wrappedRunEditorComponent, BorderLayout.SOUTH);
            return panel;
        }

        if (wrappedRunEditor != null) {
            return wrappedRunEditor.getComponent();
        }
        return wrappedConfigEditor.getComponent();
    }

    public ConfigurationSettingsEditor(RunnerAndConfigurationSettings settings) {
        super(settings.createFactory());
        myConfigurationEditor = (SettingsEditor<RunConfiguration>) settings.getConfiguration().getConfigurationEditor();
        Disposer.register(this, myConfigurationEditor);
        myConfiguration = settings.getConfiguration();
    }

    @Override
    public RunnerAndConfigurationSettings getSnapshot() throws ConfigurationException {
        RunnerAndConfigurationSettings settings = getFactory().get();
        settings.setName(myConfiguration.getName());
        if (myConfigurationEditor instanceof CheckableRunConfigurationEditor) {
            ((CheckableRunConfigurationEditor) myConfigurationEditor).checkEditorData(settings.getConfiguration());
        }
        else {
            applyTo(settings);
        }
        return settings;
    }

    private static class RunnersEditorComponent {
        @NonNls
        private static final String NO_RUNNER_COMPONENT = "<NO RUNNER LABEL>";

        private JList myRunnersList;
        private JPanel myRunnerPanel;
        private final CardLayout myLayout = new CardLayout();
        private final DefaultListModel myListModel = new DefaultListModel();
        private final JLabel myNoRunner = new JLabel(ExecutionLocalize.runConfigurationNorunnerSelectedLabel().get());
        private JPanel myRunnersPanel;

        public RunnersEditorComponent() {
            myRunnerPanel.setLayout(myLayout);
            myRunnerPanel.add(myNoRunner, NO_RUNNER_COMPONENT);
            myRunnersList.setModel(myListModel);
            myRunnersList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    updateRunnerComponent();
                }
            });
            updateRunnerComponent();
            myRunnersList.setCellRenderer(new ColoredListCellRenderer() {
                @Override
                protected void customizeCellRenderer(JList list, Object value, int index, boolean selected, boolean hasFocus) {
                    Executor executor = (Executor) value;
                    setIcon(executor.getIcon());
                    append(executor.getId(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                }
            });
        }

        private void updateRunnerComponent() {
            Executor executor = (Executor) myRunnersList.getSelectedValue();
            myLayout.show(myRunnerPanel, executor != null ? executor.getId() : NO_RUNNER_COMPONENT);
            myRunnersPanel.revalidate();
        }

        public void addExecutorComponent(Executor executor, JComponent component) {
            myRunnerPanel.add(component, executor.getId());
            myListModel.addElement(executor);
            ScrollingUtil.ensureSelectionExists(myRunnersList);
        }

        public JComponent getComponent() {
            return myRunnersPanel;
        }
    }

    private class ConfigToSettingsWrapper extends SettingsEditor<RunnerAndConfigurationSettings> {
        private final SettingsEditor<RunConfiguration> myConfigEditor;

        public ConfigToSettingsWrapper(SettingsEditor<RunConfiguration> configEditor) {
            myConfigEditor = configEditor;
            if (configEditor instanceof RunConfigurationSettingsEditor) {
                ((RunConfigurationSettingsEditor) configEditor).setOwner(ConfigurationSettingsEditor.this);
            }
        }

        @Override
        public void resetEditorFrom(RunnerAndConfigurationSettings configurationSettings) {
            myConfigEditor.resetFrom(configurationSettings.getConfiguration());
        }

        @Override
        public void applyEditorTo(RunnerAndConfigurationSettings configurationSettings) throws ConfigurationException {
            myConfigEditor.applyTo(configurationSettings.getConfiguration());
        }

        @Override
        @Nonnull
        public JComponent createEditor() {
            return myConfigEditor.getComponent();
        }

        @Override
        public void disposeEditor() {
            Disposer.dispose(myConfigEditor);
        }
    }
}
