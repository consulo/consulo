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

package consulo.execution.configuration.log;

import consulo.application.ApplicationManager;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.codeEditor.Editor;
import consulo.content.scope.SearchScope;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.execution.configuration.log.ui.AdditionalTabComponent;
import consulo.execution.util.ConsoleBuffer;
import consulo.execution.ui.console.ConsoleView;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.execution.ui.console.TextConsoleBuilder;
import consulo.execution.ui.console.TextConsoleBuilderFactory;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.logging.Logger;
import consulo.process.NopProcessHandler;
import consulo.process.ProcessHandler;
import consulo.process.event.ProcessAdapter;
import consulo.process.event.ProcessEvent;
import consulo.project.Project;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.FilterComponent;
import consulo.ui.ex.awt.util.Alarm;
import consulo.util.collection.Lists;
import consulo.util.dataholder.Key;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.util.List;
import java.util.Objects;

/**
 * @author Eugene.Kudelevsky
 */
public abstract class LogConsoleBase extends AdditionalTabComponent implements LogConsole, LogFilterListener {
    private static final Logger LOG = Logger.getInstance(LogConsoleBase.class);
    @NonNls
    public static final String APPLYING_FILTER_TITLE = "Applying filter...";

    private boolean myDisposed;
    private ConsoleView myConsole;
    private final ProcessHandler myProcessHandler = new NopProcessHandler();
    private ReaderThread myReaderThread;
    private StringBuffer myOriginalDocument = null;
    private String myLineUnderSelection = null;
    private int myLineOffset = -1;
    private LogContentPreprocessor myContentPreprocessor;
    private final Project myProject;
    private String myTitle = null;
    private boolean myWasInitialized;
    private final JPanel myTopComponent = new JPanel(new BorderLayout());
    private ActionGroup myActions;
    private final boolean myBuildInActions;
    private LogFilterModel myModel;

    private final List<LogConsoleListener> myListeners = Lists.newLockFreeCopyOnWriteList();
    private final List<? extends LogFilter> myFilters;

    private FilterComponent myFilter = new FilterComponent("LOG_FILTER_HISTORY", 5) {
        @Override
        public void filter() {
            Task.Backgroundable task = new Task.Backgroundable(myProject, APPLYING_FILTER_TITLE) {
                @Override
                public void run(@Nonnull ProgressIndicator indicator) {
                    myModel.updateCustomFilter(getFilter());
                }
            };
            ProgressManager.getInstance().run(task);
        }
    };
    private JPanel mySearchComponent;
    private JComboBox myLogFilterCombo;
    private JPanel myTextFilterWrapper;

    public LogConsoleBase(
        @Nonnull Project project,
        @Nullable Reader reader,
        String title,
        boolean buildInActions,
        LogFilterModel model
    ) {
        this(project, reader, title, buildInActions, model, GlobalSearchScope.allScope(project));
    }

    public LogConsoleBase(
        @Nonnull Project project,
        @Nullable Reader reader,
        String title,
        boolean buildInActions,
        LogFilterModel model,
        @Nonnull SearchScope scope
    ) {
        super(new BorderLayout());
        myProject = project;
        myTitle = title;
        myModel = model;
        myFilters = myModel.getLogFilters();
        myReaderThread = new ReaderThread(reader);
        myBuildInActions = buildInActions;
        TextConsoleBuilder builder = TextConsoleBuilderFactory.getInstance().createBuilder(project, scope);
        myConsole = builder.getConsole();
        myConsole.attachToProcess(myProcessHandler);
        myDisposed = false;
        myModel.addFilterListener(this);
    }

    @Override
    public void setFilterModel(LogFilterModel model) {
        if (myModel != null) {
            myModel.removeFilterListener(this);
        }
        myModel = model;
        myModel.addFilterListener(this);
    }

    @Override
    public LogFilterModel getFilterModel() {
        return myModel;
    }

    @Override
    public LogContentPreprocessor getContentPreprocessor() {
        return myContentPreprocessor;
    }

    @Override
    public void setContentPreprocessor(LogContentPreprocessor contentPreprocessor) {
        myContentPreprocessor = contentPreprocessor;
    }

    @Nullable
    protected BufferedReader updateReaderIfNeeded(@Nullable BufferedReader reader) throws IOException {
        return reader;
    }

    @SuppressWarnings({"NonStaticInitializer"})
    private JComponent createToolbar() {
        String customFilter = myModel.getCustomFilter();

        myFilter.reset();
        myFilter.setSelectedItem(customFilter != null ? customFilter : "");
        new AnAction() {
            {
                registerCustomShortcutSet(
                    new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, KeyEvent.SHIFT_DOWN_MASK)),
                    LogConsoleBase.this
                );
            }

            @Override
            public void actionPerformed(AnActionEvent e) {
                myFilter.requestFocusInWindow();
            }
        };

        if (myBuildInActions) {
            JComponent tbComp =
                ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, getOrCreateActions(), true).getComponent();
            myTopComponent.add(tbComp, BorderLayout.CENTER);
            myTopComponent.add(getSearchComponent(), BorderLayout.EAST);
        }


        return myTopComponent;
    }

    public ActionGroup getOrCreateActions() {
        if (myActions != null) {
            return myActions;
        }
        DefaultActionGroup group = new DefaultActionGroup();

        AnAction[] actions = getConsoleNotNull().createConsoleActions();
        for (AnAction action : actions) {
            group.add(action);
        }

        group.addSeparator();

        /*for (final LogFilter filter : filters) {
            group.add(new ToggleAction(filter.getName(), filter.getName(), filter.getIcon()) {
                public boolean isSelected(AnActionEvent e) {
                    return prefs.isFilterSelected(filter);
                }

                public void setSelected(AnActionEvent e, boolean state) {
                    prefs.setFilterSelected(filter, state);
                }
            });
        }*/

        myActions = group;

        return myActions;
    }

    @Override
    public void onFilterStateChange(LogFilter filter) {
        filterConsoleOutput();
    }

    @Override
    public void onTextFilterChange() {
        filterConsoleOutput();
    }

    @Override
    @Nonnull
    public JComponent getComponent() {
        if (!myWasInitialized) {
            myWasInitialized = true;
            add(getConsoleNotNull().getComponent(), BorderLayout.CENTER);
            add(createToolbar(), BorderLayout.NORTH);
        }
        return this;
    }

    public abstract boolean isActive();

    public void activate() {
        ReaderThread readerThread = myReaderThread;
        if (readerThread == null) {
            return;
        }
        if (isActive() && !readerThread.myRunning) {
            resetLogFilter();
            myFilter.setSelectedItem(myModel.getCustomFilter());
            readerThread.startRunning();
            ApplicationManager.getApplication().executeOnPooledThread(readerThread);
        }
        else if (!isActive() && readerThread.myRunning) {
            readerThread.stopRunning();
        }
    }

    public void stateChanged(ChangeEvent e) {
        activate();
    }

    @Override
    public String getTabTitle() {
        return myTitle;
    }

    @Override
    public void dispose() {
        myModel.removeFilterListener(this);
        stopRunning(false);
        synchronized (this) {
            myDisposed = true;
            if (myConsole != null) {
                Disposer.dispose(myConsole);
                myConsole = null;
            }
        }
        if (myFilter != null) {
            myFilter.dispose();
            myFilter = null;
        }
        myOriginalDocument = null;
    }

    private void stopRunning(boolean checkActive) {
        if (!checkActive) {
            fireLoggingWillBeStopped();
        }

        ReaderThread readerThread = myReaderThread;
        if (readerThread != null && readerThread.myReader != null) {
            if (!checkActive) {
                readerThread.stopRunning();
                try {
                    readerThread.myReader.close();
                }
                catch (IOException e) {
                    LOG.warn(e);
                }
                readerThread.myReader = null;
                myReaderThread = null;
            }
            else {
                try {
                    BufferedReader reader = readerThread.myReader;
                    while (reader != null && reader.ready()) {
                        addMessage(reader.readLine());
                    }
                }
                catch (IOException ignore) {
                }
                stopRunning(false);
            }
        }
    }

    protected synchronized void addMessage(String text) {
        if (text == null) {
            return;
        }
        if (myContentPreprocessor != null) {
            List<LogFragment> fragments = myContentPreprocessor.parseLogLine(text + "\n");
            myOriginalDocument = getOriginalDocument();
            for (LogFragment fragment : fragments) {
                myProcessHandler.notifyTextAvailable(fragment.getText(), fragment.getOutputType());
                if (myOriginalDocument != null) {
                    myOriginalDocument.append(fragment.getText());
                }
            }
        }
        else {
            LogFilterModel.MyProcessingResult processingResult = myModel.processLine(text);
            if (processingResult.isApplicable()) {
                Key key = processingResult.getKey();
                if (key != null) {
                    String messagePrefix = processingResult.getMessagePrefix();
                    if (messagePrefix != null) {
                        myProcessHandler.notifyTextAvailable(messagePrefix, key);
                    }
                    myProcessHandler.notifyTextAvailable(text + "\n", key);
                }
            }
            myOriginalDocument = getOriginalDocument();
            if (myOriginalDocument != null) {
                myOriginalDocument.append(text).append("\n");
            }
        }
    }

    public void attachStopLogConsoleTrackingListener(final ProcessHandler process) {
        if (process != null) {
            ProcessAdapter stopListener = new ProcessAdapter() {
                @Override
                public void processTerminated(ProcessEvent event) {
                    process.removeProcessListener(this);
                    stopRunning(true);
                }
            };
            process.addProcessListener(stopListener);
        }
    }

    public StringBuffer getOriginalDocument() {
        if (myOriginalDocument == null) {
            Editor editor = getEditor();
            if (editor != null) {
                myOriginalDocument = new StringBuffer(editor.getDocument().getText());
            }
        }
        else {
            if (ConsoleBuffer.useCycleBuffer()) {
                int toRemove = myOriginalDocument.length() - ConsoleBuffer.getCycleBufferSize();
                if (toRemove > 0) {
                    myOriginalDocument.delete(0, toRemove);
                }
            }
        }
        return myOriginalDocument;
    }

    @Nullable
    private Editor getEditor() {
        ConsoleView console = getConsole();
        return console != null ? ((DataProvider) console).getDataUnchecked(Editor.KEY) : null;
    }

    private void filterConsoleOutput() {
        ApplicationManager.getApplication().invokeLater(() -> computeSelectedLineAndFilter());
    }

    private synchronized void computeSelectedLineAndFilter() {
        // we have to do this in dispatch thread, because ConsoleViewImpl can flush something to document otherwise
        myOriginalDocument = getOriginalDocument();
        if (myOriginalDocument != null) {
            Editor editor = getEditor();
            LOG.assertTrue(editor != null);
            Document document = editor.getDocument();
            int caretOffset = editor.getCaretModel().getOffset();
            myLineUnderSelection = null;
            myLineOffset = -1;
            if (caretOffset > -1 && caretOffset < document.getTextLength()) {
                int line;
                try {
                    line = document.getLineNumber(caretOffset);
                }
                catch (IllegalStateException e) {
                    throw new IllegalStateException(
                        "document.length=" + document.getTextLength() + ", caret offset = " + caretOffset + "; " + e.getMessage(),
                        e
                    );
                }
                if (line > -1 && line < document.getLineCount()) {
                    int startOffset = document.getLineStartOffset(line);
                    myLineUnderSelection = document.getText().substring(startOffset, document.getLineEndOffset(line));
                    myLineOffset = caretOffset - startOffset;
                }
            }
        }
        ApplicationManager.getApplication().executeOnPooledThread((Runnable) () -> doFilter());
    }

    private synchronized void doFilter() {
        if (myDisposed) {
            return;
        }
        ConsoleView console = getConsoleNotNull();
        console.clear();
        myModel.processingStarted();

        String[] lines = myOriginalDocument.toString().split("\n");
        int offset = 0;
        boolean caretPositioned = false;

        for (String line : lines) {
            int printed = printMessageToConsole(line);
            if (printed > 0) {
                if (!caretPositioned) {
                    if (Objects.equals(myLineUnderSelection, line)) {
                        caretPositioned = true;
                        offset += myLineOffset != -1 ? myLineOffset : 0;
                    }
                    else {
                        offset += printed;
                    }
                }
            }
        }

        // we need this, because, document can change before actual scrolling, so offset may be already not at the end
        if (caretPositioned) {
            console.scrollTo(offset);
        }
        else {
            console.requestScrollingToEnd();
        }
    }

    private int printMessageToConsole(String line) {
        ConsoleView console = getConsoleNotNull();
        if (myContentPreprocessor != null) {
            List<LogFragment> fragments = myContentPreprocessor.parseLogLine(line + '\n');
            for (LogFragment fragment : fragments) {
                ConsoleViewContentType consoleViewType = ConsoleViewContentType.getConsoleViewType(fragment.getOutputType());
                if (consoleViewType != null) {
                    console.print(fragment.getText(), consoleViewType);
                }
            }
            return line.length() + 1;
        }
        else {
            LogFilterModel.MyProcessingResult processingResult = myModel.processLine(line);
            if (processingResult.isApplicable()) {
                Key key = processingResult.getKey();
                if (key != null) {
                    ConsoleViewContentType type = ConsoleViewContentType.getConsoleViewType(key);
                    if (type != null) {
                        String messagePrefix = processingResult.getMessagePrefix();
                        if (messagePrefix != null) {
                            console.print(messagePrefix, type);
                        }
                        console.print(line + "\n", type);
                        return (messagePrefix != null ? messagePrefix.length() : 0) + line.length() + 1;
                    }
                }
            }
            return 0;
        }
    }

    @Nullable
    public synchronized ConsoleView getConsole() {
        return myConsole;
    }

    /**
     * A shortcut for "getConsole()+assert console != null"
     * Use this method when you are sure that console must not be null.
     * If we get the assertion then it is a time to revisit logic of caller ;)
     */

    @Nonnull
    private synchronized ConsoleView getConsoleNotNull() {
        ConsoleView console = getConsole();
        assert console != null : "it looks like console has been disposed";
        return console;
    }

    @Override
    public ActionGroup getToolbarActions() {
        return getOrCreateActions();
    }

    @Override
    public String getToolbarPlace() {
        return ActionPlaces.UNKNOWN;
    }

    @Override
    @Nullable
    public JComponent getToolbarContextComponent() {
        ConsoleView console = getConsole();
        return console == null ? null : console.getComponent();
    }

    @Override
    public JComponent getPreferredFocusableComponent() {
        return getConsoleNotNull().getPreferredFocusableComponent();
    }

    public String getTitle() {
        return myTitle;
    }

    public synchronized void clear() {
        getConsoleNotNull().clear();
        myOriginalDocument = null;
    }

    @Override
    public JComponent getSearchComponent() {
        myLogFilterCombo.setModel(new DefaultComboBoxModel(myFilters.toArray(new LogFilter[myFilters.size()])));
        resetLogFilter();
        myLogFilterCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final LogFilter filter = (LogFilter) myLogFilterCombo.getSelectedItem();
                Task.Backgroundable task = new Task.Backgroundable(myProject, APPLYING_FILTER_TITLE) {
                    @Override
                    public void run(@Nonnull ProgressIndicator indicator) {
                        myModel.selectFilter(filter);
                    }
                };
                ProgressManager.getInstance().run(task);
            }
        });
        myTextFilterWrapper.removeAll();
        myTextFilterWrapper.add(getTextFilterComponent());
        return mySearchComponent;
    }

    private void resetLogFilter() {
        for (LogFilter filter : myFilters) {
            if (myModel.isFilterSelected(filter)) {
                if (myLogFilterCombo.getSelectedItem() != filter) {
                    myLogFilterCombo.setSelectedItem(filter);
                    break;
                }
            }
        }
    }

    @Nonnull
    protected Component getTextFilterComponent() {
        return myFilter;
    }

    @Override
    public boolean isContentBuiltIn() {
        return myBuildInActions;
    }

    public void writeToConsole(String text, Key outputType) {
        myProcessHandler.notifyTextAvailable(text, outputType);
    }

    public void addListener(LogConsoleListener listener) {
        myListeners.add(listener);
    }

    private void fireLoggingWillBeStopped() {
        for (LogConsoleListener listener : myListeners) {
            listener.loggingWillBeStopped();
        }
    }

    private class ReaderThread implements Runnable {
        private BufferedReader myReader;
        private boolean myRunning = false;
        private Alarm myAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, LogConsoleBase.this);

        public ReaderThread(@Nullable Reader reader) {
            myReader = reader != null ? new BufferedReader(reader) : null;
        }

        @Override
        public void run() {
            if (myReader == null) {
                return;
            }
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (myRunning) {
                        try {

                            myReader = updateReaderIfNeeded(myReader);

                            int i = 0;
                            while (i++ < 1000) {
                                BufferedReader reader = myReader;
                                if (myRunning && reader != null && reader.ready()) {
                                    addMessage(reader.readLine());
                                }
                                else {
                                    break;
                                }
                            }
                        }
                        catch (IOException e) {
                            LOG.info(e);
                            addMessage("I/O Error" + (e.getMessage() != null ? ": " + e.getMessage() : ""));
                            return;
                        }
                    }
                    if (myAlarm.isDisposed()) {
                        return;
                    }
                    myAlarm.addRequest(this, 100);
                }
            };
            if (myAlarm.isDisposed()) {
                return;
            }
            myAlarm.addRequest(runnable, 10);
        }

        public void startRunning() {
            myRunning = true;
        }

        public void stopRunning() {
            myRunning = false;
            synchronized (this) {
                notifyAll();
            }
        }
    }
}
