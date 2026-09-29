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

import consulo.application.concurrent.coroutine.ReadLock;
import consulo.configurable.BaseConfigurable;
import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.BeforeRunTask;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.RuntimeConfigurationException;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.ConfigurationPerRunnerSettings;
import consulo.execution.configuration.LocatableConfiguration;
import consulo.execution.configuration.LocatableConfigurationBase;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.RunnerSettings;
import consulo.execution.executor.Executor;
import consulo.execution.executor.ExecutorRegistry;
import consulo.execution.impl.internal.configuration.RunManagerImpl;
import consulo.execution.impl.internal.configuration.UnknownRunConfiguration;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.runner.ProgramRunner;
import consulo.execution.runner.RunnerRegistry;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.TextBox;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.util.LabeledBuilder;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.jdom.JDOMUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SingleConfigurationConfigurable<Config extends RunConfiguration> extends BaseConfigurable {
    private final RunnerAndConfigurationSettings mySettings;
    private @Nullable ConfigurationSettingsEditorWrapperImpl myEditor;
    private final @Nullable Executor myExecutor;

    private final String myDisplayName;
    private final String myHelpTopic;
    private final boolean myBrokenConfiguration;

    private boolean myStoreProjectConfiguration;
    private boolean mySingleton;
    private @Nullable String myFolderName;

    private final List<Runnable> myPresentationListeners = new CopyOnWriteArrayList<>();

    private String myNameText = "";
    private boolean myChangingNameFromCode;
    private boolean myNameChangedByUser;

    private @Nullable Component myComponent;
    private @Nullable TextBox myNameBox;
    private @Nullable CheckBox myShareBox;
    private @Nullable CheckBox mySingletonBox;
    private @Nullable Label myValidationLabel;
    private @Nullable Hyperlink myFixLink;
    private @Nullable HorizontalLayout myValidationPanel;

    private @Nullable ValidationResult myLastValidationResult;
    private @Nullable Runnable myQuickFix;
    private @Nullable String myValidatedState;
    private int myValidationStamp;
    private boolean myDisposed;

    @RequiredUIAccess
    private SingleConfigurationConfigurable(RunnerAndConfigurationSettings settings, @Nullable Executor executor) {
        mySettings = settings;
        myExecutor = executor;

        ConfigurationSettingsEditorWrapperImpl editor = new ConfigurationSettingsEditorWrapperImpl(settings);
        myEditor = editor;
        editor.addSettingsEditorListener(settingsEditor -> onEditorStateChanged());
        editor.getUIComponent();

        Config configuration = getConfiguration();
        myDisplayName = settings.getName();
        myHelpTopic = "reference.dialogs.rundebug." + configuration.getType().getId();
        myBrokenConfiguration = configuration instanceof UnknownRunConfiguration;
        myFolderName = settings.getFolderName();

        setNameText(configuration.getName());
    }

    @RequiredUIAccess
    public static <Config extends RunConfiguration> SingleConfigurationConfigurable<Config> editSettings(
        RunnerAndConfigurationSettings settings,
        @Nullable Executor executor
    ) {
        SingleConfigurationConfigurable<Config> configurable = new SingleConfigurationConfigurable<>(settings, executor);
        configurable.reset();
        return configurable;
    }

    public void addPresentationListener(Runnable listener) {
        myPresentationListeners.add(listener);
    }

    private void firePresentationChanged() {
        for (Runnable listener : myPresentationListeners) {
            listener.run();
        }
    }

    public String getNameText() {
        return myNameText;
    }

    public void setNameText(String name) {
        myChangingNameFromCode = true;
        try {
            myNameText = name;

            TextBox nameBox = myNameBox;
            if (nameBox != null && !name.equals(nameBox.getValue())) {
                nameBox.setValue(name, false);
            }
        }
        finally {
            myChangingNameFromCode = false;
        }
    }

    @RequiredUIAccess
    public void selectNameText() {
        TextBox nameBox = myNameBox;
        if (nameBox != null) {
            nameBox.selectAll();
            nameBox.focus();
        }
    }

    public List<BeforeRunTask> getStepsBeforeLaunch() {
        ConfigurationSettingsEditorWrapperImpl editor = myEditor;
        if (editor != null) {
            return editor.getStepsBeforeLaunch();
        }
        RunConfiguration configuration = getConfiguration();
        return RunManagerImpl.getInstanceImpl(configuration.getProject()).getBeforeRunTasks(configuration);
    }

    public boolean hasValidationError() {
        return myLastValidationResult != null;
    }

    @RequiredUIAccess
    @Override
    public void apply() throws ConfigurationException {
        RunnerAndConfigurationSettings settings = getSettings();
        RunManagerImpl runManager = RunManagerImpl.getInstanceImpl(settings.getConfiguration().getProject());
        runManager.shareConfiguration(settings, myStoreProjectConfiguration);
        settings.setName(getNameText());
        settings.setSingleton(mySingleton);
        settings.setFolderName(myFolderName);
        ConfigurationSettingsEditorWrapperImpl editor = myEditor;
        if (editor != null) {
            editor.applyTo(settings);
        }
        setModified(false);
        runManager.fireRunConfigurationChanged(settings);
    }

    @RequiredUIAccess
    @Override
    public void reset() {
        RunnerAndConfigurationSettings settings = getSettings();
        setNameText(settings.getName());
        ConfigurationSettingsEditorWrapperImpl editor = myEditor;
        if (editor != null) {
            editor.resetFrom(settings);
        }
        setModified(false);

        RunManagerImpl runManager = RunManagerImpl.getInstanceImpl(settings.getConfiguration().getProject());
        myStoreProjectConfiguration = runManager.isConfigurationShared(settings);
        mySingleton = settings.isSingleton();

        CheckBox shareBox = myShareBox;
        if (shareBox != null) {
            shareBox.setValue(myStoreProjectConfiguration, false);
        }

        CheckBox singletonBox = mySingletonBox;
        if (singletonBox != null) {
            singletonBox.setValue(mySingleton, false);
        }
    }

    @RequiredUIAccess
    @Override
    public Component createUIComponent(Disposable parentDisposable) {
        Component component = myComponent;
        if (component != null) {
            return component;
        }

        RunnerAndConfigurationSettings settings = getSettings();

        TextBox nameBox = TextBox.create(myNameText);
        nameBox.setEnabled(!isBrokenConfiguration());
        nameBox.addValueListener(event -> {
            if (myChangingNameFromCode) {
                return;
            }

            String value = event.getValue();
            myNameText = value == null ? "" : value;
            myNameChangedByUser = true;
            setModified(true);

            if (getSettings().getConfiguration() instanceof LocatableConfigurationBase locatableConfiguration) {
                locatableConfiguration.setNameChangedByUser(true);
            }

            scheduleValidation();
            firePresentationChanged();
        });
        myNameBox = nameBox;

        CheckBox shareBox = CheckBox.create(ExecutionLocalize.runConfigurationStorePlaceOption());
        shareBox.setValue(myStoreProjectConfiguration, false);
        shareBox.setEnabled(!isBrokenConfiguration());
        shareBox.setVisible(!settings.isTemplate());
        shareBox.addValueListener(event -> {
            myStoreProjectConfiguration = Boolean.TRUE.equals(event.getValue());
            myNameChangedByUser = true;
            setModified(true);
            firePresentationChanged();
        });
        myShareBox = shareBox;

        CheckBox singletonBox = CheckBox.create(ExecutionLocalize.runConfigurationSingleton());
        singletonBox.setValue(mySingleton, false);
        singletonBox.setEnabled(!isBrokenConfiguration());
        ConfigurationFactory factory = settings.getFactory();
        singletonBox.setVisible(factory != null && factory.canConfigurationBeSingleton());
        singletonBox.addValueListener(event -> {
            mySingleton = Boolean.TRUE.equals(event.getValue());
            setModified(true);
        });
        mySingletonBox = singletonBox;

        HorizontalLayout options = HorizontalLayout.create();
        options.add(shareBox);
        options.add(singletonBox);

        DockLayout header = DockLayout.create();
        header.center(LabeledBuilder.filled(ExecutionLocalize.editRunConfigurationRunConfigurationNameLabel(), nameBox));
        header.right(options);
        header.paddingBuilder().bottomSet(Space.MEDIUM).apply();

        Label validationLabel = Label.create();
        validationLabel.setImage(PlatformIconGroup.generalError());
        myValidationLabel = validationLabel;

        Hyperlink fixLink = Hyperlink.create(ExecutionLocalize.fixRunConfigurationProblemButton(), event -> {
            Runnable quickFix = myQuickFix;
            if (quickFix == null) {
                return;
            }

            quickFix.run();
            scheduleValidation();
        });
        fixLink.setIcon(PlatformIconGroup.actionsQuickfixbulb());
        myFixLink = fixLink;

        HorizontalLayout validationPanel = HorizontalLayout.create();
        validationPanel.add(validationLabel);
        validationPanel.add(fixLink);
        validationPanel.paddingBuilder().topSet(Space.MEDIUM).apply();
        validationPanel.setVisible(false);
        myValidationPanel = validationPanel;

        DockLayout layout = DockLayout.create();
        layout.top(header);
        ConfigurationSettingsEditorWrapperImpl editor = myEditor;
        if (editor != null) {
            layout.center(ScrollableLayout.create(editor.getUIComponent()));
        }
        layout.bottom(validationPanel);

        myComponent = layout;

        scheduleValidation();

        return layout;
    }

    @RequiredUIAccess
    private void scheduleValidation() {
        if (myDisposed || myValidationPanel == null) {
            return;
        }

        int stamp = ++myValidationStamp;

        RunnerAndConfigurationSettings snapshot;
        try {
            snapshot = createValidationSnapshot();
        }
        catch (ConfigurationException e) {
            myValidatedState = e.getLocalizedMessage();
            showValidationResult(toValidationResult(e));
            return;
        }

        if (snapshot == null) {
            myValidatedState = null;
            showValidationResult(null);
            return;
        }

        myValidatedState = writeState(snapshot.getConfiguration());

        Executor executor = myExecutor;
        Project project = getConfiguration().getProject();

        CoroutineScope.launchAsync(
            project.coroutineContext(),
            () -> Coroutine
                .first(ReadLock.<Void, @Nullable ValidationResult>apply(ignored -> {
                    try {
                        checkSnapshot(snapshot, executor);
                        return null;
                    }
                    catch (ConfigurationException e) {
                        return toValidationResult(e);
                    }
                }))
                .then(UIAction.<@Nullable ValidationResult, Void>apply(result -> {
                    if (stamp == myValidationStamp && !myDisposed) {
                        showValidationResult(result);
                    }
                    return null;
                }))
        );
    }

    @RequiredUIAccess
    public void revalidateIfEdited() {
        if (myDisposed || myValidationPanel == null) {
            return;
        }

        String state;
        try {
            RunnerAndConfigurationSettings snapshot = createValidationSnapshot();
            state = snapshot == null ? null : writeState(snapshot.getConfiguration());
        }
        catch (ConfigurationException e) {
            state = e.getLocalizedMessage();
        }

        if (!Objects.equals(state, myValidatedState)) {
            updateGeneratedName();
            scheduleValidation();
        }
    }

    @RequiredUIAccess
    private void onEditorStateChanged() {
        setModified(true);
        updateGeneratedName();
        scheduleValidation();
    }

    @RequiredUIAccess
    private void updateGeneratedName() {
        if (myNameChangedByUser || !(getConfiguration() instanceof LocatableConfiguration configuration) || !configuration.isGeneratedName()) {
            return;
        }

        try {
            RunnerAndConfigurationSettings snapshot = getSnapshot();
            if (snapshot != null && snapshot.getConfiguration() instanceof LocatableConfiguration snapshotConfiguration) {
                String generatedName = snapshotConfiguration.suggestedName();
                if (!StringUtil.isEmpty(generatedName) && !generatedName.equals(myNameText)) {
                    setNameText(generatedName);
                    setModified(true);
                    firePresentationChanged();
                }
            }
        }
        catch (ConfigurationException ignored) {
        }
    }

    private static @Nullable String writeState(RunConfiguration configuration) {
        Element element = ConfigurationSettingsEditorWrapperImpl.writeElement(configuration);
        return element == null ? null : JDOMUtil.writeElement(element);
    }

    @RequiredUIAccess
    private void showValidationResult(@Nullable ValidationResult result) {
        boolean hadError = myLastValidationResult != null;

        myLastValidationResult = result;
        myQuickFix = result == null ? null : result.getQuickFix();

        Label validationLabel = myValidationLabel;
        if (validationLabel != null && result != null) {
            validationLabel.setText(LocalizeValue.of(result.getTitle() + ": " + result.getMessage()));
        }

        Hyperlink fixLink = myFixLink;
        if (fixLink != null) {
            fixLink.setVisible(myQuickFix != null);
        }

        HorizontalLayout validationPanel = myValidationPanel;
        if (validationPanel != null) {
            validationPanel.setVisible(result != null);
        }

        if (hadError != (result != null)) {
            firePresentationChanged();
        }
    }

    private @Nullable RunnerAndConfigurationSettings createValidationSnapshot() throws ConfigurationException {
        RunnerAndConfigurationSettings snapshot = getSnapshot();
        if (snapshot != null) {
            snapshot.setName(getNameText());
        }
        return snapshot;
    }

    private static void checkSnapshot(RunnerAndConfigurationSettings snapshot, @Nullable Executor executor)
        throws RuntimeConfigurationException {
        snapshot.checkSettings(executor);
        for (ProgramRunner runner : RunnerRegistry.getInstance().getRegisteredRunners()) {
            for (Executor registeredExecutor : ExecutorRegistry.getInstance().getRegisteredExecutors()) {
                if (runner.canRun(registeredExecutor.getId(), snapshot.getConfiguration())) {
                    checkConfiguration(runner, snapshot);
                    break;
                }
            }
        }
    }

    private static void checkConfiguration(ProgramRunner runner, RunnerAndConfigurationSettings snapshot)
        throws RuntimeConfigurationException {
        RunnerSettings runnerSettings = snapshot.getRunnerSettings(runner);
        ConfigurationPerRunnerSettings configurationSettings = snapshot.getConfigurationSettings(runner);
        runner.checkConfiguration(runnerSettings, configurationSettings);
    }

    private static ValidationResult toValidationResult(ConfigurationException exception) {
        if (exception instanceof RuntimeConfigurationException runtimeException) {
            return new ValidationResult(
                runtimeException.getLocalizedMessage(),
                runtimeException.getTitle().get(),
                runtimeException.getQuickFix()
            );
        }
        return new ValidationResult(exception.getLocalizedMessage(), ExecutionLocalize.invalidDataDialogTitle().get(), null);
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        ConfigurationSettingsEditorWrapperImpl editor = myEditor;
        return super.isModified() || editor != null && editor.isModified(getSettings());
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        myDisposed = true;
        myPresentationListeners.clear();

        ConfigurationSettingsEditorWrapperImpl editor = myEditor;
        if (editor != null) {
            Disposer.dispose(editor);
        }
        myEditor = null;

        myComponent = null;
        myNameBox = null;
        myShareBox = null;
        mySingletonBox = null;
        myValidationLabel = null;
        myFixLink = null;
        myValidationPanel = null;
    }

    public boolean isBrokenConfiguration() {
        return myBrokenConfiguration;
    }

    public boolean isStoreProjectConfiguration() {
        return myStoreProjectConfiguration;
    }

    public boolean isSingleton() {
        return mySingleton;
    }

    public void setFolderName(@Nullable String folderName) {
        if (!Comparing.equal(myFolderName, folderName)) {
            myFolderName = folderName;
            setModified(true);
        }
    }

    public @Nullable String getFolderName() {
        return myFolderName;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.ofNullable(myDisplayName);
    }

    @Override
    public String getHelpTopic() {
        return myHelpTopic;
    }

    public RunnerAndConfigurationSettings getSettings() {
        return mySettings;
    }

    @SuppressWarnings("unchecked")
    public Config getConfiguration() {
        return (Config) mySettings.getConfiguration();
    }

    public @Nullable RunnerAndConfigurationSettings getSnapshot() throws ConfigurationException {
        ConfigurationSettingsEditorWrapperImpl editor = myEditor;
        return editor == null ? null : editor.getSnapshot();
    }

    @Override
    public String toString() {
        return myDisplayName;
    }
}
