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
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposer;
import consulo.execution.BeforeRunTask;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.impl.internal.configuration.RunManagerImpl;
import consulo.execution.internal.ConfigurationSettingsEditorWrapper;
import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.util.jdom.JDOMUtil;
import consulo.util.xml.serializer.WriteExternalException;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author anna
 * @since 2006-03-27
 */
public class ConfigurationSettingsEditorWrapperImpl extends ConfigurationSettingsEditorWrapper {
    private final RunnerAndConfigurationSettings mySettings;
    private final RunConfigurationFragmentPage myFragmentPage;
    private final BeforeRunTaskRow myBeforeRunRow;

    private @Nullable Element myBaselineElement;

    @RequiredUIAccess
    public ConfigurationSettingsEditorWrapperImpl(RunnerAndConfigurationSettings settings) {
        mySettings = settings;
        myFragmentPage = new RunConfigurationFragmentPage(settings, this::createFragmentSnapshot, this::fireEditorStateChanged);
        Disposer.register(this, myFragmentPage);
        myBeforeRunRow = new BeforeRunTaskRow(settings, this::fireEditorStateChanged);
        myFragmentPage.setBeforeRunActions(myBeforeRunRow::createAddActions);
    }

    @RequiredUIAccess
    public Component createModifyOptionsLink() {
        return myFragmentPage.createModifyOptionsLink();
    }

    @RequiredUIAccess
    @Override
    protected Component createUIComponent() {
        DockLayout layout = DockLayout.create();
        layout.putUserData(UiDataProvider.KEY, sink -> sink.set(CONFIGURATION_EDITOR_KEY, this));

        VerticalLayout content = VerticalLayout.create(Space.MEDIUM);
        content.add(myBeforeRunRow.createComponent());
        content.add(myFragmentPage.getComponent());
        layout.top(content);
        return layout;
    }

    @RequiredUIAccess
    @Override
    public void resetEditorFrom(RunnerAndConfigurationSettings settings) {
        myFragmentPage.reset(settings);
        myBeforeRunRow.reset(settings);
        myBaselineElement = writeSnapshotElement();
    }

    @Override
    public void applyEditorTo(RunnerAndConfigurationSettings settings) throws ConfigurationException {
        myFragmentPage.apply(settings);
        applyBeforeRun(settings);
        myBaselineElement = writeElement(settings);
    }

    @Override
    public RunnerAndConfigurationSettings getSnapshot() throws ConfigurationException {
        RunnerAndConfigurationSettings result = createContentSnapshot();
        applyBeforeRun(result);
        return result;
    }

    private RunnerAndConfigurationSettings createContentSnapshot() throws ConfigurationException {
        RunnerAndConfigurationSettings snapshot = mySettings.createFactory().get();
        snapshot.setName(mySettings.getConfiguration().getName());
        myFragmentPage.apply(snapshot);
        return snapshot;
    }

    private RunnerAndConfigurationSettings createFragmentSnapshot() {
        try {
            return createContentSnapshot();
        }
        catch (ConfigurationException e) {
            return mySettings;
        }
    }

    private void applyBeforeRun(RunnerAndConfigurationSettings settings) {
        RunConfiguration runConfiguration = settings.getConfiguration();
        RunManagerImpl.getInstanceImpl(runConfiguration.getProject()).setBeforeRunTasks(runConfiguration, myBeforeRunRow.getTasks(true), false);
    }

    @RequiredUIAccess
    boolean isModified(RunnerAndConfigurationSettings settings) {
        try {
            RunnerAndConfigurationSettings snapshot = createContentSnapshot();
            Element baselineElement = myBaselineElement != null ? myBaselineElement : writeElement(settings);
            Element snapshotElement = writeElement(snapshot);
            return baselineElement == null || snapshotElement == null || !JDOMUtil.areElementsEqual(baselineElement, snapshotElement);
        }
        catch (ConfigurationException e) {
            return true;
        }
    }

    private @Nullable Element writeSnapshotElement() {
        try {
            return writeElement(createContentSnapshot());
        }
        catch (ConfigurationException e) {
            return null;
        }
    }

    private static @Nullable Element writeElement(RunnerAndConfigurationSettings settings) {
        Element element = writeElement(settings.getConfiguration());
        if (element != null) {
            element.setAttribute("singleton", String.valueOf(settings.isSingleton()));
        }
        return element;
    }

    static @Nullable Element writeElement(RunConfiguration configuration) {
        try {
            Element element = new Element("configuration");
            configuration.writeExternal(element);
            return element;
        }
        catch (WriteExternalException e) {
            return null;
        }
    }

    @RequiredUIAccess
    @Override
    public void addBeforeLaunchStep(BeforeRunTask task) {
        myBeforeRunRow.addTask(task);
    }

    @Override
    public List<BeforeRunTask> getStepsBeforeLaunch() {
        return myBeforeRunRow.getTasks(true);
    }
}
