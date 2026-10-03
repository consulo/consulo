/*
 * Copyright 2013-2016 consulo.io
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
package consulo.sandboxPlugin.ide.run;

import consulo.application.Application;
import consulo.configurable.ConfigurationException;
import consulo.execution.CommonProgramRunConfigurationParameters;
import consulo.execution.RuntimeConfigurationException;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.LocatableConfigurationBase;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.configuration.log.ui.LogConfigurationPanel;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.execution.configuration.ui.SettingsEditorGroup;
import consulo.execution.executor.Executor;
import consulo.execution.executor.ExecutorGroup;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.ui.CommonProgramParametersLayout;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.editor.ui.EditorBox;
import consulo.language.editor.ui.EditorBoxBuilderFactory;
import consulo.language.editor.ui.awt.DefaultTextCompletionValueDescriptor;
import consulo.language.editor.ui.awt.TextFieldCompletionProvider;
import consulo.language.editor.ui.awt.TextFieldWithAutoCompletion;
import consulo.language.editor.ui.awt.ValuesCompletionProvider;
import consulo.localize.LocalizeValue;
import consulo.process.ExecutionException;
import consulo.project.Project;
import consulo.sandboxPlugin.ide.profiler.SandProfilerConfigurationState;
import consulo.sandboxPlugin.ide.profiler.SandProfilerRunState;
import consulo.sandboxPlugin.lang.SandLanguage;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.util.FormBuilder;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 04.06.14
 */
public class SandConfiguration extends LocatableConfigurationBase implements CommonProgramRunConfigurationParameters {
    private @Nullable String myProgramParameters;
    private @Nullable String myWorkingDirectory;
    private @Nullable String myEntryPoint;
    private @Nullable String myExpression;
    private @Nullable String myProfile;
    private @Nullable String myTags;
    private final Map<String, String> myEnvs = new LinkedHashMap<>();
    private boolean myPassParentEnvs = true;

    public SandConfiguration(Project project, ConfigurationFactory factory, String name) {
        super(project, factory, name);
    }

    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        SettingsEditorGroup<SandConfiguration> group = new SettingsEditorGroup<>();
        group.addEditor(ExecutionLocalize.runConfigurationConfigurationTabTitle(), new SandConfigurationEditor(getProject()));
        group.addEditor(ExecutionLocalize.logsTabTitle(), new LogConfigurationPanel<>());
        return group;
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
    }

    @Override
    public @Nullable RunProfileState getState(Executor executor, ExecutionEnvironment env) throws ExecutionException {
        if (ExecutorGroup.getGroupIfProxy(executor) instanceof SandExecutorGroup executorGroup) {
            SandExecutorSettings settings = executorGroup.getRegisteredSettings(executor.getId());
            if (settings != null) {
                return new SandProfileState(env, settings);
            }
        }

        SandProfilerConfigurationState profilerConfiguration = SandProfilerRunState.findConfiguration(executor);
        if (profilerConfiguration != null) {
            return new SandProfilerRunState(env, profilerConfiguration);
        }
        return null;
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        super.readExternal(element);

        myProgramParameters = element.getAttributeValue("programParameters");
        myWorkingDirectory = element.getAttributeValue("workingDirectory");
        myEntryPoint = element.getAttributeValue("entryPoint");
        myExpression = element.getAttributeValue("expression");
        myProfile = element.getAttributeValue("profile");
        myTags = element.getAttributeValue("tags");
        myPassParentEnvs = !"false".equals(element.getAttributeValue("passParentEnvs"));

        myEnvs.clear();
        for (Element env : element.getChildren("env")) {
            String name = env.getAttributeValue("name");
            if (name != null) {
                myEnvs.put(name, env.getAttributeValue("value", ""));
            }
        }
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        super.writeExternal(element);

        if (myProgramParameters != null) {
            element.setAttribute("programParameters", myProgramParameters);
        }
        if (myWorkingDirectory != null) {
            element.setAttribute("workingDirectory", myWorkingDirectory);
        }
        if (myEntryPoint != null) {
            element.setAttribute("entryPoint", myEntryPoint);
        }
        if (myExpression != null) {
            element.setAttribute("expression", myExpression);
        }
        if (myProfile != null) {
            element.setAttribute("profile", myProfile);
        }
        if (myTags != null) {
            element.setAttribute("tags", myTags);
        }
        element.setAttribute("passParentEnvs", String.valueOf(myPassParentEnvs));

        for (Map.Entry<String, String> entry : myEnvs.entrySet()) {
            Element env = new Element("env");
            env.setAttribute("name", entry.getKey());
            env.setAttribute("value", entry.getValue());
            element.addContent(env);
        }
    }

    @Override
    public void setProgramParameters(@Nullable String value) {
        myProgramParameters = StringUtil.nullize(value);
    }

    @Override
    public @Nullable String getProgramParameters() {
        return myProgramParameters;
    }

    @Override
    public void setWorkingDirectory(@Nullable String value) {
        myWorkingDirectory = StringUtil.nullize(value);
    }

    @Override
    public @Nullable String getWorkingDirectory() {
        return myWorkingDirectory;
    }

    public void setEntryPoint(@Nullable String entryPoint) {
        myEntryPoint = StringUtil.nullize(entryPoint);
    }

    public @Nullable String getEntryPoint() {
        return myEntryPoint;
    }

    public void setExpression(@Nullable String expression) {
        myExpression = StringUtil.nullize(expression);
    }

    public @Nullable String getExpression() {
        return myExpression;
    }

    public void setProfile(@Nullable String profile) {
        myProfile = StringUtil.nullize(profile);
    }

    public @Nullable String getProfile() {
        return myProfile;
    }

    public void setTags(@Nullable String tags) {
        myTags = StringUtil.nullize(tags);
    }

    public @Nullable String getTags() {
        return myTags;
    }

    @Override
    public void setEnvs(Map<String, String> envs) {
        myEnvs.clear();
        myEnvs.putAll(envs);
    }

    @Override
    public Map<String, String> getEnvs() {
        return myEnvs;
    }

    @Override
    public void setPassParentEnvs(boolean passParentEnvs) {
        myPassParentEnvs = passParentEnvs;
    }

    @Override
    public boolean isPassParentEnvs() {
        return myPassParentEnvs;
    }

    private static class EntryPointCompletionProvider extends TextFieldCompletionProvider {
        @Override
        public void addCompletionVariants(String text, int offset, String prefix, CompletionResultSet result) {
            for (String variant : List.of("main", "mainTest", "benchmark")) {
                result.addElement(LookupElementBuilder.create(variant));
            }
        }
    }

    private static class SandConfigurationEditor extends SettingsEditor<SandConfiguration> {
        private final Project myProject;

        private @Nullable CommonProgramParametersLayout<SandConfiguration> myLayout;
        private @Nullable EditorBox myEntryPointBox;
        private @Nullable EditorBox myExpressionBox;
        private @Nullable EditorBox myProfileBox;
        private @Nullable EditorBox myTagsBox;

        private SandConfigurationEditor(Project project) {
            myProject = project;
        }

        @RequiredUIAccess
        @Override
        protected void resetEditorFrom(SandConfiguration configuration) {
            CommonProgramParametersLayout<SandConfiguration> layout = myLayout;
            if (layout != null) {
                layout.reset(configuration);
            }

            EditorBox entryPointBox = myEntryPointBox;
            if (entryPointBox != null) {
                entryPointBox.setValue(StringUtil.notNullize(configuration.getEntryPoint()));
            }

            EditorBox expressionBox = myExpressionBox;
            if (expressionBox != null) {
                expressionBox.setValue(StringUtil.notNullize(configuration.getExpression()));
            }

            EditorBox profileBox = myProfileBox;
            if (profileBox != null) {
                profileBox.setValue(StringUtil.notNullize(configuration.getProfile()));
            }

            EditorBox tagsBox = myTagsBox;
            if (tagsBox != null) {
                tagsBox.setValue(StringUtil.notNullize(configuration.getTags()));
            }
        }

        @RequiredUIAccess
        @Override
        protected void applyEditorTo(SandConfiguration configuration) throws ConfigurationException {
            CommonProgramParametersLayout<SandConfiguration> layout = myLayout;
            if (layout != null) {
                layout.apply(configuration);
            }

            EditorBox entryPointBox = myEntryPointBox;
            if (entryPointBox != null) {
                configuration.setEntryPoint(entryPointBox.getValue());
            }

            EditorBox expressionBox = myExpressionBox;
            if (expressionBox != null) {
                configuration.setExpression(expressionBox.getValue());
            }

            EditorBox profileBox = myProfileBox;
            if (profileBox != null) {
                configuration.setProfile(profileBox.getValue());
            }

            EditorBox tagsBox = myTagsBox;
            if (tagsBox != null) {
                configuration.setTags(tagsBox.getValue());
            }
        }

        @RequiredUIAccess
        @Override
        protected Component createUIComponent() {
            CommonProgramParametersLayout<SandConfiguration> layout =
                new CommonProgramParametersLayout<>(Application.get().getInstance(DialogService.class)) {
                    @RequiredUIAccess
                    @Override
                    protected void addBefore(FormBuilder builder) {
                        EditorBoxBuilderFactory editorBoxBuilderFactory = myProject.getApplication().getInstance(EditorBoxBuilderFactory.class);

                        EditorBox entryPointBox = editorBoxBuilderFactory.create(myProject)
                            .completion(new EntryPointCompletionProvider())
                            .placeholder(LocalizeValue.localizeTODO("main"))
                            .build();
                        builder.addLabeled(LocalizeValue.localizeTODO("Entry point:"), entryPointBox);
                        myEntryPointBox = entryPointBox;

                        EditorBox expressionBox = editorBoxBuilderFactory.create(myProject)
                            .language(SandLanguage.INSTANCE)
                            .placeholder(LocalizeValue.localizeTODO("Sand expression"))
                            .build();
                        builder.addLabeled(LocalizeValue.localizeTODO("Expression:"), expressionBox);
                        myExpressionBox = expressionBox;

                        EditorBox profileBox = editorBoxBuilderFactory.create(myProject)
                            .completion(new ValuesCompletionProvider<>(
                                new DefaultTextCompletionValueDescriptor.StringValueDescriptor(),
                                List.of("debug", "release", "profile")
                            ))
                            .build();
                        builder.addLabeled(LocalizeValue.localizeTODO("Profile:"), profileBox);
                        myProfileBox = profileBox;

                        EditorBox tagsBox = editorBoxBuilderFactory.create(myProject)
                            .completion(new TextFieldWithAutoCompletion.StringsCompletionProvider(List.of("fast", "slow", "flaky"), null))
                            .build();
                        builder.addLabeled(LocalizeValue.localizeTODO("Tags:"), tagsBox);
                        myTagsBox = tagsBox;
                    }
                };
            layout.build();
            myLayout = layout;
            return layout.getComponent();
        }
    }
}
