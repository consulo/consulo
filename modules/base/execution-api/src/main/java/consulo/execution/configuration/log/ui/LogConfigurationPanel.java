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

package consulo.execution.configuration.log.ui;

import consulo.application.Application;
import consulo.configurable.ConfigurationException;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.configuration.log.LogFileOptions;
import consulo.execution.configuration.log.PredefinedLogFile;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.execution.localize.ExecutionLocalize;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.ComponentItemRender;
import consulo.ui.Table;
import consulo.ui.TableItemEditor;
import consulo.ui.ValueComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionToolbarPosition;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.ex.toolbar.AddAction;
import consulo.ui.ex.toolbar.DownMoveAction;
import consulo.ui.ex.toolbar.EditAction;
import consulo.ui.ex.toolbar.RemoveAction;
import consulo.ui.ex.toolbar.ToolbarDecoratorBuilderFactory;
import consulo.ui.ex.toolbar.UpMoveAction;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.util.io.FileUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LogConfigurationPanel<T extends RunConfigurationBase> extends SettingsEditor<T> {
    private final MutableFlatDataModel<LogFileOptions> myModel = FlatDataModel.of(List.of());
    private final Map<LogFileOptions, PredefinedLogFile> myLog2Predefined = new HashMap<>();
    private final List<PredefinedLogFile> myUnresolvedPredefined = new ArrayList<>();

    private boolean myRedirectOutput;
    private String myOutputFilePath = "";
    private boolean myShowConsoleOnStdOut;
    private boolean myShowConsoleOnStdErr;

    private @Nullable Table<LogFileOptions> myFilesTable;
    private @Nullable CheckBox myRedirectOutputCb;
    private FileChooserTextBoxBuilder.@Nullable Controller myOutputFile;
    private @Nullable CheckBox myShowConsoleOnStdOutCb;
    private @Nullable CheckBox myShowConsoleOnStdErrCb;

    @RequiredUIAccess
    @Override
    protected Component createUIComponent() {
        Table<LogFileOptions> table = Table.create(myModel);
        table.addColumn(ExecutionLocalize.logMonitorIsActiveColumn(), LogFileOptions::isEnabled)
            .setWidth(90)
            .setRender(ComponentItemRender.reusable(
                () -> CheckBox.create(LocalizeValue.empty()),
                (checkBox, item) -> checkBox.setValue(Boolean.TRUE.equals(item.getValue()))
            ))
            .setEditor(new TableItemEditor<>() {
                @RequiredUIAccess
                @Override
                public ValueComponent<Boolean> createComponent(LogFileOptions options) {
                    return CheckBox.create(LocalizeValue.empty(), options.isEnabled());
                }

                @RequiredUIAccess
                @Override
                public void commit(LogFileOptions options, @Nullable Boolean value) {
                    boolean checked = Boolean.TRUE.equals(value);
                    PredefinedLogFile predefinedLogFile = myLog2Predefined.get(options);
                    if (predefinedLogFile != null) {
                        predefinedLogFile.setEnabled(checked);
                    }
                    options.setEnable(checked);
                    fireEditorStateChanged();
                }
            });
        table.addColumn(ExecutionLocalize.logMonitorLogFileColumn(), LogFileOptions::getName);
        table.addColumn(ExecutionLocalize.logMonitorIsSkippedColumn(), LogFileOptions::isSkipContent)
            .setWidth(90)
            .setRender(ComponentItemRender.reusable(
                () -> CheckBox.create(LocalizeValue.empty()),
                (checkBox, item) -> checkBox.setValue(Boolean.TRUE.equals(item.getValue()))
            ))
            .setEditor(new TableItemEditor<>() {
                @RequiredUIAccess
                @Override
                public ValueComponent<Boolean> createComponent(LogFileOptions options) {
                    return CheckBox.create(LocalizeValue.empty(), options.isSkipContent());
                }

                @RequiredUIAccess
                @Override
                public void commit(LogFileOptions options, @Nullable Boolean value) {
                    options.setSkipContent(Boolean.TRUE.equals(value));
                    fireEditorStateChanged();
                }

                @Override
                public boolean isEditable(LogFileOptions options) {
                    return !myLog2Predefined.containsKey(options);
                }
            });
        table.addDoubleClickListener(event -> {
            LogFileOptions options = table.getSelectedItem();
            if (options != null) {
                editOptions(options);
            }
        });
        myFilesTable = table;

        Component filesPanel = ToolbarDecoratorBuilderFactory.getInstance()
            .create(table)
            .addOrReplaceAction(new AddLogFileAction())
            .addOrReplaceAction(new RemoveLogFileAction())
            .addOrReplaceAction(new EditLogFileAction())
            .disableAction(UpMoveAction.class)
            .disableAction(DownMoveAction.class)
            .withToolbarPosition(ActionToolbarPosition.RIGHT)
            .build();

        CheckBox redirectOutputCb = CheckBox.create(ExecutionLocalize.logsSaveConsoleOutputToFile());
        redirectOutputCb.setValue(myRedirectOutput, false);
        redirectOutputCb.addValueListener(event -> {
            myRedirectOutput = Boolean.TRUE.equals(event.getValue());
            updateOutputFileState();
        });
        myRedirectOutputCb = redirectOutputCb;

        FileChooserTextBoxBuilder outputFileBuilder = FileChooserTextBoxBuilder.create(null);
        outputFileBuilder.dialogTitle(ExecutionLocalize.logsSaveConsoleOutputChooserTitle());
        outputFileBuilder.dialogDescription(ExecutionLocalize.logsSaveConsoleOutputChooserDescription());
        outputFileBuilder.fileChooserDescriptor(FileChooserDescriptorFactory.createSingleFileOrFolderDescriptor());
        FileChooserTextBoxBuilder.Controller outputFile = outputFileBuilder.build();
        outputFile.setValue(myOutputFilePath, false);
        outputFile.getComponent().addValueListener(event -> myOutputFilePath = StringUtil.notNullize(event.getValue()));
        myOutputFile = outputFile;

        DockLayout outputPanel = DockLayout.create();
        outputPanel.left(redirectOutputCb);
        outputPanel.center(outputFile.getComponent());

        CheckBox showConsoleOnStdOutCb = CheckBox.create(ExecutionLocalize.logsShowConsoleOnStdout());
        showConsoleOnStdOutCb.setValue(myShowConsoleOnStdOut, false);
        showConsoleOnStdOutCb.addValueListener(event -> myShowConsoleOnStdOut = Boolean.TRUE.equals(event.getValue()));
        myShowConsoleOnStdOutCb = showConsoleOnStdOutCb;

        CheckBox showConsoleOnStdErrCb = CheckBox.create(ExecutionLocalize.logsShowConsoleOnStderr());
        showConsoleOnStdErrCb.setValue(myShowConsoleOnStdErr, false);
        showConsoleOnStdErrCb.addValueListener(event -> myShowConsoleOnStdErr = Boolean.TRUE.equals(event.getValue()));
        myShowConsoleOnStdErrCb = showConsoleOnStdErrCb;

        VerticalLayout consolePanel = VerticalLayout.create();
        consolePanel.add(showConsoleOnStdOutCb);
        consolePanel.add(showConsoleOnStdErrCb);

        DockLayout bottomPanel = DockLayout.create();
        bottomPanel.top(outputPanel);
        bottomPanel.center(consolePanel);

        DockLayout panel = DockLayout.create();
        panel.center(LabeledLayout.create(ExecutionLocalize.logMonitorGroup(), filesPanel));
        panel.bottom(bottomPanel);

        updateOutputFileState();
        return panel;
    }

    @RequiredUIAccess
    private void updateOutputFileState() {
        FileChooserTextBoxBuilder.Controller outputFile = myOutputFile;
        if (outputFile != null) {
            outputFile.getComponent().setEnabled(myRedirectOutput);
        }
    }

    public void refreshPredefinedLogFiles(RunConfigurationBase configurationBase) {
        List<LogFileOptions> newItems = new ArrayList<>();
        boolean changed = false;
        for (LogFileOptions item : getItems()) {
            PredefinedLogFile predefined = myLog2Predefined.get(item);
            if (predefined != null) {
                LogFileOptions options = configurationBase.getOptionsForPredefinedLogFile(predefined);
                if (LogFileOptions.areEqual(item, options)) {
                    newItems.add(item);
                }
                else {
                    changed = true;
                    myLog2Predefined.remove(item);
                    if (options == null) {
                        myUnresolvedPredefined.add(predefined);
                    }
                    else {
                        newItems.add(options);
                        myLog2Predefined.put(options, predefined);
                    }
                }
            }
            else {
                newItems.add(item);
            }
        }

        for (PredefinedLogFile logFile : new ArrayList<>(myUnresolvedPredefined)) {
            LogFileOptions options = configurationBase.getOptionsForPredefinedLogFile(logFile);
            if (options != null) {
                changed = true;
                myUnresolvedPredefined.remove(logFile);
                myLog2Predefined.put(options, logFile);
                newItems.add(options);
            }
        }

        if (changed) {
            myModel.replaceAll(newItems);
        }
    }

    @Override
    protected void resetEditorFrom(RunConfigurationBase configuration) {
        List<LogFileOptions> list = new ArrayList<>();
        for (LogFileOptions setting : configuration.getLogFiles()) {
            list.add(new LogFileOptions(
                setting.getName(),
                setting.getPathPattern(),
                setting.isEnabled(),
                setting.isSkipContent(),
                setting.isShowAll()
            ));
        }
        myLog2Predefined.clear();
        myUnresolvedPredefined.clear();
        for (PredefinedLogFile predefinedLogFile : configuration.getPredefinedLogFiles()) {
            PredefinedLogFile logFile = new PredefinedLogFile(predefinedLogFile);
            LogFileOptions options = configuration.getOptionsForPredefinedLogFile(logFile);
            if (options != null) {
                myLog2Predefined.put(options, logFile);
                list.add(options);
            }
            else {
                myUnresolvedPredefined.add(logFile);
            }
        }
        myModel.replaceAll(list);

        myRedirectOutput = configuration.isSaveOutputToFile();
        String fileOutputPath = configuration.getOutputFilePath();
        myOutputFilePath = fileOutputPath != null ? FileUtil.toSystemDependentName(fileOutputPath) : "";
        myShowConsoleOnStdOut = configuration.isShowConsoleOnStdOut();
        myShowConsoleOnStdErr = configuration.isShowConsoleOnStdErr();

        CheckBox redirectOutputCb = myRedirectOutputCb;
        if (redirectOutputCb != null) {
            redirectOutputCb.setValue(myRedirectOutput, false);
        }
        FileChooserTextBoxBuilder.Controller outputFile = myOutputFile;
        if (outputFile != null) {
            outputFile.setValue(myOutputFilePath, false);
        }
        CheckBox showConsoleOnStdOutCb = myShowConsoleOnStdOutCb;
        if (showConsoleOnStdOutCb != null) {
            showConsoleOnStdOutCb.setValue(myShowConsoleOnStdOut, false);
        }
        CheckBox showConsoleOnStdErrCb = myShowConsoleOnStdErrCb;
        if (showConsoleOnStdErrCb != null) {
            showConsoleOnStdErrCb.setValue(myShowConsoleOnStdErr, false);
        }
        if (myFilesTable != null) {
            updateOutputFileState();
        }
    }

    @Override
    protected void applyEditorTo(RunConfigurationBase configuration) throws ConfigurationException {
        configuration.removeAllLogFiles();
        configuration.removeAllPredefinedLogFiles();

        for (LogFileOptions options : getItems()) {
            if (Comparing.equal(options.getPathPattern(), "")) {
                continue;
            }
            PredefinedLogFile predefined = myLog2Predefined.get(options);
            if (predefined != null) {
                configuration.addPredefinedLogFile(new PredefinedLogFile(predefined.getId(), options.isEnabled()));
            }
            else {
                configuration.addLogFile(
                    options.getPathPattern(),
                    options.getName(),
                    options.isEnabled(),
                    options.isSkipContent(),
                    options.isShowAll()
                );
            }
        }
        for (PredefinedLogFile logFile : myUnresolvedPredefined) {
            configuration.addPredefinedLogFile(logFile);
        }
        configuration.setFileOutputPath(StringUtil.isEmpty(myOutputFilePath) ? null : FileUtil.toSystemIndependentName(myOutputFilePath));
        configuration.setSaveOutputToFile(myRedirectOutput);
        configuration.setShowConsoleOnStdOut(myShowConsoleOnStdOut);
        configuration.setShowConsoleOnStdErr(myShowConsoleOnStdErr);
    }

    private List<LogFileOptions> getItems() {
        List<LogFileOptions> items = new ArrayList<>(myModel.getSize());
        for (int i = 0; i < myModel.getSize(); i++) {
            items.add(myModel.get(i));
        }
        return items;
    }

    @RequiredUIAccess
    private void editOptions(LogFileOptions options) {
        if (myLog2Predefined.containsKey(options)) {
            return;
        }

        showEditorDialog(options, () -> {
            myModel.update(options);
            fireEditorStateChanged();
        });
    }

    @RequiredUIAccess
    private void showEditorDialog(LogFileOptions options, @RequiredUIAccess Runnable onOk) {
        EditLogPatternDialogDescriptor descriptor =
            new EditLogPatternDialogDescriptor(options.getName(), options.getPathPattern(), options.isShowAll());

        DialogService dialogService = Application.get().getInstance(DialogService.class);
        Table<LogFileOptions> table = myFilesTable;
        Dialog dialog = table == null ? dialogService.build(descriptor) : dialogService.build(table, descriptor);
        dialog.showAsync().whenComplete((value, error) -> {
            if (error != null || value == null) {
                return;
            }

            options.setName(descriptor.getName());
            options.setPathPattern(descriptor.getLogPattern());
            options.setShowAll(descriptor.isShowAllFiles());
            onOk.run();
        });
    }

    private class AddLogFileAction extends AddAction<LogFileOptions> {
        @Override
        @RequiredUIAccess
        protected void doAdd(AnActionEvent e) {
            LogFileOptions options = new LogFileOptions("", "", true, true, false);
            showEditorDialog(options, () -> {
                myModel.add(options);
                Table<LogFileOptions> table = myFilesTable;
                if (table != null) {
                    table.select(options);
                }
                fireEditorStateChanged();
            });
        }
    }

    private class RemoveLogFileAction extends RemoveAction<LogFileOptions> {
        @Override
        @RequiredUIAccess
        protected void doRemove(LogFileOptions options, AnActionEvent e) {
            if (myLog2Predefined.containsKey(options)) {
                return;
            }

            myModel.remove(options);
            fireEditorStateChanged();
        }
    }

    private class EditLogFileAction extends EditAction<LogFileOptions> {
        @Override
        @RequiredUIAccess
        protected void doEdit(LogFileOptions options, AnActionEvent e) {
            editOptions(options);
        }
    }
}
