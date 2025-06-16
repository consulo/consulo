/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.execution.test.sm.action;

import consulo.application.ApplicationPropertiesComponent;
import consulo.execution.DefaultExecutionTarget;
import consulo.execution.ExecutionTarget;
import consulo.execution.ExecutionTargetProvider;
import consulo.execution.runner.RunnerRegistry;
import consulo.execution.configuration.*;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.execution.executor.Executor;
import consulo.execution.executor.ExecutorRegistry;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.ExecutionEnvironmentBuilder;
import consulo.execution.runner.ProgramRunner;
import consulo.execution.test.TestStateStorage;
import consulo.execution.test.sm.runner.SMRunnerConsolePropertiesProvider;
import consulo.execution.test.sm.runner.SMTRunnerConsoleProperties;
import consulo.execution.test.sm.runner.history.ImportedTestRunnableState;
import consulo.logging.Logger;
import consulo.process.ExecutionException;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.Messages;
import consulo.ui.image.Image;
import consulo.util.io.FileUtil;
import consulo.util.jdom.JDOMUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jdom.Document;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.util.Arrays;

/**
 * 1. chooses file where test results were saved
 * 2. finds the configuration element saved during export
 * 3. creates corresponding configuration with {@link SMTRunnerConsoleProperties} if configuration implements {@link SMRunnerConsolePropertiesProvider}
 * <p>
 * Without console properties no navigation, no rerun failed is possible.
 */
public abstract class AbstractImportTestsAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(AbstractImportTestsAction.class);
    public static final String TEST_HISTORY_SIZE = "test_history_size";
    private SMTRunnerConsoleProperties myProperties;

    public AbstractImportTestsAction(@Nullable String text, @Nullable String description, @Nullable Image icon) {
        super(text, description, icon);
    }

    public AbstractImportTestsAction(
        SMTRunnerConsoleProperties properties,
        @Nullable String text,
        @Nullable String description,
        @Nullable Image icon
    ) {
        this(text, description, icon);
        myProperties = properties;
    }

    public static int getHistorySize() {
        int historySize;
        try {
            historySize = Math.max(0, Integer.parseInt(ApplicationPropertiesComponent.getInstance().getValue(TEST_HISTORY_SIZE, "10")));
        }
        catch (NumberFormatException e) {
            historySize = 10;
        }
        return historySize;
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(e.getData(Project.KEY) != null);
    }

    @Nullable
    public abstract VirtualFile getFile(@Nonnull Project project);

    @Override
    public void actionPerformed(AnActionEvent e) {
        final Project project = e.getData(Project.KEY);
        LOG.assertTrue(project != null);
        final VirtualFile file = getFile(project);
        if (file != null) {
            try {
                final ImportRunProfile profile = new ImportRunProfile(file, project);
                SMTRunnerConsoleProperties properties = profile.getProperties();
                if (properties == null) {
                    properties = myProperties;
                    LOG.info("Failed to detect test framework in " + file.getPath() + "; use " + (properties != null ? properties.getTestFrameworkName() + " from toolbar" : "no properties"));
                }
                final Executor executor = properties != null ? properties.getExecutor() : ExecutorRegistry.getInstance()
                    .getExecutorById(DefaultRunExecutor.EXECUTOR_ID);
                ExecutionEnvironmentBuilder builder = ExecutionEnvironmentBuilder.create(project, executor, profile);
                ExecutionTarget target = profile.getTarget();
                if (target != null) {
                    builder = builder.target(target);
                }
                final RunConfiguration initialConfiguration = profile.getInitialConfiguration();
                final ProgramRunner runner =
                    initialConfiguration != null ? RunnerRegistry.getInstance().getRunner(executor.getId(), initialConfiguration) : null;
                if (runner != null) {
                    builder = builder.runner(runner);
                }
                builder.buildAndExecute();
            }
            catch (ExecutionException e1) {
                Messages.showErrorDialog(project, e1.getMessage(), "Import Failed");
            }
        }
    }

    public static void adjustHistory(Project project) {
        int historySize = getHistorySize();

        final File[] files = TestStateStorage.getTestHistoryRoot(project).listFiles((dir, name) -> name.endsWith(".xml"));
        if (files != null && files.length >= historySize + 1) {
            Arrays.sort(files, (o1, o2) -> {
                final long l1 = o1.lastModified();
                final long l2 = o2.lastModified();
                if (l1 == l2) {
                    return FileUtil.compareFiles(o1, o2);
                }
                return l1 < l2 ? -1 : 1;
            });
            FileUtil.delete(files[0]);
        }
    }

    public static class ImportRunProfile implements RunProfile {
        private final VirtualFile myFile;
        private final Project myProject;
        private RunConfiguration myConfiguration;
        private boolean myImported;
        private SMTRunnerConsoleProperties myProperties;
        private String myTargetId;

        public ImportRunProfile(VirtualFile file, Project project) {
            myFile = file;
            myProject = project;
            try {
                final Document document = JDOMUtil.loadDocument(VirtualFileUtil.virtualToIoFile(myFile));
                final Element config = document.getRootElement().getChild("config");
                if (config != null) {
                    String configTypeId = config.getAttributeValue("configId");
                    if (configTypeId != null) {
                        final ConfigurationType configurationType = ConfigurationTypeUtil.findConfigurationType(configTypeId);
                        if (configurationType != null) {
                            myConfiguration = configurationType.getConfigurationFactories()[0].createTemplateConfiguration(project);
                            myConfiguration.setName(config.getAttributeValue("name"));
                            myConfiguration.readExternal(config);

                            final Executor executor = ExecutorRegistry.getInstance().getExecutorById(DefaultRunExecutor.EXECUTOR_ID);
                            if (executor != null) {
                                if (myConfiguration instanceof SMRunnerConsolePropertiesProvider provider) {
                                    myProperties = provider.createTestConsoleProperties(executor);
                                }
                            }
                        }
                    }
                    myTargetId = config.getAttributeValue("target");
                }
            }
            catch (Exception ignore) {
            }
        }

        public ExecutionTarget getTarget() {
            if (myTargetId != null) {
                if (DefaultExecutionTarget.INSTANCE.getId().equals(myTargetId)) {
                    return DefaultExecutionTarget.INSTANCE;
                }
                for (ExecutionTargetProvider provider : ExecutionTargetProvider.EXTENSION_NAME.getExtensionList()) {
                    for (ExecutionTarget target : provider.getTargets(myProject, myConfiguration)) {
                        if (myTargetId.equals(target.getId())) {
                            return target;
                        }
                    }
                }
                return null;
            }
            return DefaultExecutionTarget.INSTANCE;
        }

        @Nullable
        @Override
        public RunProfileState getState(@Nonnull Executor executor, @Nonnull ExecutionEnvironment environment) throws ExecutionException {
            if (!myImported) {
                myImported = true;
                return new ImportedTestRunnableState(this, VirtualFileUtil.virtualToIoFile(myFile));
            }
            if (myConfiguration != null) {
                try {
                    return myConfiguration.getState(executor, environment);
                }
                catch (Throwable e) {
                    if (myTargetId != null && getTarget() == null) {
                        throw new ExecutionException("The target " + myTargetId + " does not exist");
                    }

                    LOG.info(e);
                    throw new ExecutionException("Unable to run the configuration: settings are corrupted");
                }
            }
            throw new ExecutionException("Unable to run the configuration: failed to detect test framework");
        }

        @Override
        public String getName() {
            return myImported && myConfiguration != null ? myConfiguration.getName() : myFile.getNameWithoutExtension();
        }

        @Nullable
        @Override
        public Image getIcon() {
            return myProperties != null ? myProperties.getConfiguration().getIcon() : null;
        }

        public SMTRunnerConsoleProperties getProperties() {
            return myProperties;
        }

        public RunConfiguration getInitialConfiguration() {
            return myConfiguration;
        }

        public Project getProject() {
            return myProject;
        }
    }
}
