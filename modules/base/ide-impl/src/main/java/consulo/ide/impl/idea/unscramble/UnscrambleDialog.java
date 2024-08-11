/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.unscramble;

import consulo.application.Application;
import consulo.application.ApplicationPropertiesComponent;
import consulo.application.HelpManager;
import consulo.execution.ui.RunContentDescriptor;
import consulo.execution.unscramble.*;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.IdeaFileChooser;
import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import consulo.ide.impl.idea.openapi.vcs.configurable.VcsContentAnnotationConfigurable;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.internal.GuiUtils;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.io.FileUtil;
import consulo.util.lang.Comparing;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author cdr
 */
public class UnscrambleDialog extends DialogWrapper {
    private static final String PROPERTY_LOG_FILE_HISTORY_URLS = "UNSCRAMBLE_LOG_FILE_URL";
    private static final String PROPERTY_LOG_FILE_LAST_URL = "UNSCRAMBLE_LOG_FILE_LAST_URL";
    private static final String PROPERTY_UNSCRAMBLER_ID_USED = "UNSCRAMBLER_ID_USED";

    private static final Predicate<ThreadState> DEADLOCK_CONDITION = ThreadState::isDeadlocked;

    private final Project myProject;
    private ComboBox<StacktraceAnalyzer> myAnalyzerBox;
    private JPanel myEditorPanel;
    private JPanel myLogFileChooserPanel;
    private ComboBox<UnscrambleSupport> myUnscrambleChooser;
    private BorderLayoutPanel myRootPanel;
    private TextFieldWithHistory myLogFile;
    private CheckBox myUseUnscrambler;
    private CheckBox myOnTheFly;
    protected AnalyzeStacktraceUtil.StacktraceEditorPanel myStacktraceEditorPanel;
    private VcsContentAnnotationConfigurable myConfigurable;

    private Label myUnscramberLabel;
    private Label myLogFileLabel;

    @RequiredUIAccess
    public UnscrambleDialog(Project project, @Nullable StacktraceAnalyzer stacktraceAnalyzer) {
        super(false);
        myProject = project;

        List<StacktraceAnalyzer> analyzers = Application.get().getExtensionList(StacktraceAnalyzer.class);
        myAnalyzerBox = new ComboBox<>(new CollectionComboBoxModel<>(analyzers));
        myAnalyzerBox.setRenderer(new ColoredListCellRenderer<StacktraceAnalyzer>() {
            @Override
            protected void customizeCellRenderer(@Nonnull JList list, StacktraceAnalyzer value, int index, boolean selected, boolean hasFocus) {
                append(value == null ? "" : value.getName().get());
            }
        });

        if (stacktraceAnalyzer != null) {
            myAnalyzerBox.setSelectedItem(stacktraceAnalyzer);
        }
        else {
            for (StacktraceAnalyzer analyzer : analyzers) {
                if (analyzer.isPreferredForProject(project)) {
                    myAnalyzerBox.setSelectedItem(analyzer);
                    break;
                }
            }
        }
        myAnalyzerBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateUnscrambles();
            }
        });

        myRootPanel = new BorderLayoutPanel();

        FormBuilder topPanel = FormBuilder.createFormBuilder();
        topPanel.addLabeledComponent("Analyzer:", myAnalyzerBox);

        topPanel.addComponent(TargetAWT.to(myUseUnscrambler = CheckBox.create(IdeLocalize.unscrambleUseUnscramblerCheckbox())));

        myUnscrambleChooser = new ComboBox<>();
        topPanel.addLabeledComponent(TargetAWT.to(myUnscramberLabel = Label.create(IdeLocalize.unscrambleUnscramblerCombobox())), myUnscrambleChooser);

        myLogFileChooserPanel = new JPanel(new BorderLayout());
        topPanel.addLabeledComponent(TargetAWT.to(myLogFileLabel = Label.create(IdeLocalize.unscrambleLogPathLabel())), myLogFileChooserPanel);
        createLogFileChooser();

        myRootPanel.addToTop(topPanel.getPanel());

        myEditorPanel = new JPanel(new BorderLayout());
        myEditorPanel.add(TargetAWT.to(Label.create(IdeLocalize.unscrambleStacktraceCaption())), BorderLayout.NORTH);
        myRootPanel.addToCenter(myEditorPanel);

        createEditor();

        JPanel bottomPanel = new JPanel(new VerticalFlowLayout());
        myRootPanel.addToBottom(bottomPanel);

        bottomPanel.add(TargetAWT.to(myOnTheFly = CheckBox.create(LocalizeValue.localizeTODO("Automatically detect and analyze thread dumps copied to the clipboard outside of IDE"))));

        myUnscrambleChooser.addActionListener(e -> {
            UnscrambleSupport unscrambleSupport = getSelectedUnscrambler();
            GuiUtils.enableChildren(myLogFileChooserPanel, unscrambleSupport != null);
        });
        myUseUnscrambler.addValueListener(e -> useUnscramblerChanged());
        myOnTheFly.setValue(UnscrambleManager.getInstance().isEnabled());
        myOnTheFly.addValueListener(e -> UnscrambleManager.getInstance().setEnabled(myOnTheFly.getValueOrError()));

        if (ProjectLevelVcsManager.getInstance(myProject).hasActiveVcss()) {
            myConfigurable = new VcsContentAnnotationConfigurable(myProject);
            bottomPanel.add(myConfigurable.createComponent(getDisposable()), BorderLayout.CENTER);
            myConfigurable.reset();
        }

        updateUnscrambles();

        reset();

        setTitle(IdeLocalize.unscrambleDialogTitle());
        init();
    }

    @RequiredUIAccess
    private void useUnscramblerChanged() {
        boolean selected = myUseUnscrambler.getValueOrError();
        myUnscramberLabel.setEnabled(selected);
        myUnscrambleChooser.setEnabled(selected);
        myLogFileChooserPanel.setEnabled(selected);
        myLogFileLabel.setEnabled(selected);

        GuiUtils.enableChildren(myLogFileChooserPanel, selected);
    }

    @RequiredUIAccess
    private void reset() {
        final List<String> savedUrls = getSavedLogFileUrls();
        myLogFile.setHistorySize(10);
        myLogFile.setHistory(savedUrls);

        String lastUrl = getLastUsedLogUrl();
        if (lastUrl == null && !savedUrls.isEmpty()) {
            lastUrl = savedUrls.get(savedUrls.size() - 1);
        }
        if (lastUrl != null) {
            myLogFile.setText(lastUrl);
            myLogFile.setSelectedItem(lastUrl);
        }
        final UnscrambleSupport selectedUnscrambler = getSavedUnscrambler();

        final int count = getRegisteredUnscramblers().size();
        int index = 0;
        if (selectedUnscrambler != null) {
            for (int i = 0; i < count; i++) {
                final UnscrambleSupport unscrambleSupport = myUnscrambleChooser.getItemAt(i);
                if (unscrambleSupport != null && Comparing.strEqual(unscrambleSupport.getId(), selectedUnscrambler.getId())) {
                    index = i;
                    break;
                }
            }
        }
        if (count > 0) {
            myUseUnscrambler.setEnabled(true);
            myUnscrambleChooser.setSelectedIndex(index);
            myUseUnscrambler.setValue(selectedUnscrambler != null);
        }
        else {
            myUseUnscrambler.setEnabled(false);
        }

        useUnscramblerChanged();
        myStacktraceEditorPanel.pasteTextFromClipboard();
    }

    public static String getLastUsedLogUrl() {
        return PropertiesComponent.getInstance().getValue(PROPERTY_LOG_FILE_LAST_URL);
    }

    @Nullable
    public UnscrambleSupport getSavedUnscrambler() {
        final List<UnscrambleSupport> registeredUnscramblers = getRegisteredUnscramblers();
        final String savedUnscramblerId = PropertiesComponent.getInstance().getValue(PROPERTY_UNSCRAMBLER_ID_USED);
        UnscrambleSupport selectedUnscrambler = null;
        for (final UnscrambleSupport unscrambleSupport : registeredUnscramblers) {
            if (Comparing.strEqual(unscrambleSupport.getId(), savedUnscramblerId)) {
                selectedUnscrambler = unscrambleSupport;
            }
        }
        return selectedUnscrambler;
    }

    public static List<String> getSavedLogFileUrls() {
        final List<String> res = new ArrayList<>();
        final String savedUrl = PropertiesComponent.getInstance().getValue(PROPERTY_LOG_FILE_HISTORY_URLS);
        final String[] strings = savedUrl == null ? ArrayUtil.EMPTY_STRING_ARRAY : savedUrl.split(":::");
        for (int i = 0; i != strings.length; ++i) {
            res.add(strings[i]);
        }
        return res;
    }

    @Nullable
    private UnscrambleSupport getSelectedUnscrambler() {
        if (!myUseUnscrambler.getValueOrError()) {
            return null;
        }
        return (UnscrambleSupport) myUnscrambleChooser.getSelectedItem();
    }

    private void createEditor() {
        myStacktraceEditorPanel = AnalyzeStacktraceUtil.createEditorPanel(myProject, myDisposable);
        myEditorPanel.add(myStacktraceEditorPanel, BorderLayout.CENTER);
    }

    @Override
    @Nonnull
    protected Action[] createActions() {
        return new Action[]{createNormalizeTextAction(), getOKAction(), getCancelAction(), getHelpAction()};
    }

    private void createLogFileChooser() {
        myLogFile = new TextFieldWithHistory();
        JPanel panel = GuiUtils.constructFieldWithBrowseButton(myLogFile, e -> {
            FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor();
            IdeaFileChooser.chooseFiles(
                descriptor,
                myProject,
                null,
                files -> myLogFile.setText(FileUtil.toSystemDependentName(files.get(files.size() - 1).getPath()))
            );
        });
        myLogFileChooserPanel.add(panel, BorderLayout.CENTER);
    }

    private void updateUnscrambles() {
        myUnscrambleChooser.removeAllItems();

        // not selected
        myUnscrambleChooser.addItem(null);

        List<UnscrambleSupport> unscrambleComponents = getRegisteredUnscramblers();
        for (final UnscrambleSupport unscrambleSupport : unscrambleComponents) {
            myUnscrambleChooser.addItem(unscrambleSupport);
        }
        myUnscrambleChooser.setRenderer(new ColoredListCellRenderer<UnscrambleSupport>() {
            @Override
            protected void customizeCellRenderer(@Nonnull JList<? extends UnscrambleSupport> list, UnscrambleSupport value, int index, boolean selected, boolean hasFocus) {
                append(value == null ? IdeLocalize.unscrambleNoUnscramblerItem().get() : value.getName().get());
            }
        });
    }

    @Nonnull
    private List<UnscrambleSupport> getRegisteredUnscramblers() {
        StacktraceAnalyzer analyzer = getAnalyzer();
        List<UnscrambleSupport> unscrambleSupports = new ArrayList<>();
        for (UnscrambleSupport unscrambleSupport : Application.get().getExtensionList(UnscrambleSupport.class)) {
            if (unscrambleSupport.isAvailable(analyzer)) {
                unscrambleSupports.add(unscrambleSupport);
            }
        }
        return unscrambleSupports;
    }

    @Override
    protected JComponent createCenterPanel() {
        return myRootPanel;
    }

    @Override
    public void dispose() {
        if (isOK()) {
            final List list = myLogFile.getHistory();
            String res = null;
            for (Object aList : list) {
                final String s = (String) aList;
                if (res == null) {
                    res = s;
                }
                else {
                    res = res + ":::" + s;
                }
            }

            ApplicationPropertiesComponent propertiesComponent = ApplicationPropertiesComponent.getInstance();

            propertiesComponent.setValue(PROPERTY_LOG_FILE_HISTORY_URLS, res);
            UnscrambleSupport selectedUnscrambler = getSelectedUnscrambler();
            propertiesComponent.setValue(PROPERTY_UNSCRAMBLER_ID_USED, selectedUnscrambler == null ? null : selectedUnscrambler.getId());
            propertiesComponent.setValue(PROPERTY_LOG_FILE_LAST_URL, myLogFile.getText());
        }
        super.dispose();
    }

    public void setText(String trace) {
        myStacktraceEditorPanel.setText(trace);
    }

    public Action createNormalizeTextAction() {
        return new NormalizeTextAction();
    }

    private StacktraceAnalyzer getAnalyzer() {
        return (StacktraceAnalyzer) myAnalyzerBox.getSelectedItem();
    }

    private final class NormalizeTextAction extends AbstractAction {
        public NormalizeTextAction() {
            putValue(NAME, IdeLocalize.unscrambleNormalizeButton().get());
            putValue(DEFAULT_ACTION, Boolean.FALSE);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String text = myStacktraceEditorPanel.getText();
            myStacktraceEditorPanel.setText(normalizeText(text));
        }
    }

    public String normalizeText(String text) {
        return getAnalyzer().normalizeStacktrace(text);
    }

    @Override
    protected void doOKAction() {
        if (myConfigurable != null && myConfigurable.isModified()) {
            myConfigurable.apply();
        }

        if (performUnscramble()) {
            myLogFile.addCurrentTextToHistory();
            close(OK_EXIT_CODE);
        }
    }

    @Override
    public void doHelpAction() {
        HelpManager.getInstance().invokeHelp("find.analyzeStackTrace");
    }

    private boolean performUnscramble() {
        UnscrambleSupport selectedUnscrambler = getSelectedUnscrambler();
        return showUnscrambledText(selectedUnscrambler, myLogFile.getText(), myProject, myStacktraceEditorPanel.getText()) != null;
    }

    @Nullable
    private RunContentDescriptor showUnscrambledText(@Nullable UnscrambleSupport unscrambleSupport, String logName, Project project, String textToUnscramble) {
        String unscrambledTrace = unscrambleSupport == null ? textToUnscramble : unscrambleSupport.unscramble(project, textToUnscramble, logName);
        if (unscrambledTrace == null) {
            return null;
        }
        List<ThreadState> threadStates = getAnalyzer().parseAsThreadDump(unscrambledTrace);
        return addConsole(project, threadStates, unscrambledTrace);
    }

    private RunContentDescriptor addConsole(final Project project, final List<ThreadState> threadDump, String unscrambledTrace) {
        Image icon = null;
        String message = IdeLocalize.unscrambleUnscrambledStacktraceTab().get();
        if (!threadDump.isEmpty()) {
            message = IdeLocalize.unscrambleUnscrambledThreaddumpTab().get();
            icon = PlatformIconGroup.actionsDump();
        }
        else {
            String name = getAnalyzer().parseAsException(unscrambledTrace);
            if (name != null) {
                message = name;
                icon = PlatformIconGroup.actionsLightning();
            }
        }
        if (ContainerUtil.find(threadDump, DEADLOCK_CONDITION) != null) {
            message = IdeLocalize.unscrambleUnscrambledDeadlockTab().get();
            icon = PlatformIconGroup.debuggerKillprocess();
        }
        return AnalyzeStacktraceUtil.addConsole(project, threadDump.size() > 1 ? (consoleView, toolbarActions) -> {
            ThreadDumpConsoleFactory factory = new ThreadDumpConsoleFactory(project, threadDump);
            return factory.createConsoleComponent(consoleView, toolbarActions);
        } : null, message, unscrambledTrace, icon);
    }

    @Override
    protected String getDimensionServiceKey() {
        return getClass().getName();
    }

    @RequiredUIAccess
    @Override
    public JComponent getPreferredFocusedComponent() {
        return myStacktraceEditorPanel.getEditorComponent();
    }
}
