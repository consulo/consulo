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

package consulo.ide.impl.idea.tools;

import consulo.component.persist.scheme.SchemeElement;
import consulo.dataContext.DataContext;
import consulo.document.FileDocumentManager;
import consulo.execution.runner.RunnerRegistry;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.ExecutionEnvironmentBuilder;
import consulo.execution.runner.ProgramRunner;
import consulo.ide.impl.idea.execution.util.ExecutionErrorDialog;
import consulo.pathMacro.Macro;
import consulo.pathMacro.MacroManager;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.event.ProcessListener;
import consulo.process.internal.OSProcessHandler;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class Tool implements SchemeElement {
  @NonNls public final static String ACTION_ID_PREFIX = "Tool_";
  public static final String DEFAULT_GROUP_NAME = "External Tools";

  private String myName;
  private String myDescription;
  private String myGroup = DEFAULT_GROUP_NAME;
  private boolean myShownInMainMenu;
  private boolean myShownInEditor;
  private boolean myShownInProjectViews;
  private boolean myShownInSearchResultsPopup;
  private boolean myEnabled;

  private boolean myUseConsole;
  private boolean myShowConsoleOnStdOut;
  private boolean myShowConsoleOnStdErr;
  private boolean mySynchronizeAfterExecution;

  private String myWorkingDirectory;
  private String myProgram;
  private String myParameters;

  private ArrayList<FilterInfo> myOutputFilters = new ArrayList<>();

  public Tool() {
  }

  public String getName() {
    return myName;
  }

  public String getDescription() {
    return myDescription;
  }

  @Nonnull
  public String getGroup() {
    return myGroup;
  }

  public boolean isShownInMainMenu() {
    return myShownInMainMenu;
  }

  public boolean isShownInEditor() {
    return myShownInEditor;
  }

  public boolean isShownInProjectViews() {
    return myShownInProjectViews;
  }

  public boolean isShownInSearchResultsPopup() {
    return myShownInSearchResultsPopup;
  }

  public boolean isEnabled() {
    return myEnabled;
  }

  public void setEnabled(boolean enabled) {
    myEnabled = enabled;
  }

  public boolean isUseConsole() {
    return myUseConsole;
  }

  public boolean isShowConsoleOnStdOut() {
    return myShowConsoleOnStdOut;
  }

  public boolean isShowConsoleOnStdErr() {
    return myShowConsoleOnStdErr;
  }

  public boolean synchronizeAfterExecution() {
    return mySynchronizeAfterExecution;
  }

  void setName(String name) {
    myName = name;
  }

  void setDescription(String description) {
    myDescription = description;
  }

  void setGroup(@Nullable String group) {
    myGroup = StringUtil.notNullize(group, DEFAULT_GROUP_NAME);
  }

  void setShownInMainMenu(boolean shownInMainMenu) {
    myShownInMainMenu = shownInMainMenu;
  }

  void setShownInEditor(boolean shownInEditor) {
    myShownInEditor = shownInEditor;
  }

  void setShownInProjectViews(boolean shownInProjectViews) {
    myShownInProjectViews = shownInProjectViews;
  }

  public void setShownInSearchResultsPopup(boolean shownInSearchResultsPopup) {
    myShownInSearchResultsPopup = shownInSearchResultsPopup;
  }

  void setUseConsole(boolean useConsole) {
    myUseConsole = useConsole;
  }

  void setShowConsoleOnStdOut(boolean showConsole) {
    myShowConsoleOnStdOut = showConsole;
  }

  void setShowConsoleOnStdErr(boolean showConsole) {
    myShowConsoleOnStdErr = showConsole;
  }

  public void setFilesSynchronizedAfterRun(boolean synchronizeAfterRun) {
    mySynchronizeAfterExecution = synchronizeAfterRun;
  }

  public String getWorkingDirectory() {
    return myWorkingDirectory;
  }

  public void setWorkingDirectory(String workingDirectory) {
    myWorkingDirectory = workingDirectory;
  }

  public String getProgram() {
    return myProgram;
  }

  public void setProgram(String program) {
    myProgram = program;
  }

  public String getParameters() {
    return myParameters;
  }

  public void setParameters(String parameters) {
    myParameters = parameters;
  }

  public void addOutputFilter(FilterInfo filter) {
    myOutputFilters.add(filter);
  }

  public void setOutputFilters(FilterInfo[] filters) {
    myOutputFilters = new ArrayList<>();
    if (filters != null) {
      for (int i = 0; i < filters.length; i++) {
        myOutputFilters.add(filters[i]);
      }
    }
  }

  public FilterInfo[] getOutputFilters() {
    return myOutputFilters.toArray(new FilterInfo[myOutputFilters.size()]);
  }

  public void copyFrom(Tool source) {
    myName = source.getName();
    myDescription = source.getDescription();
    myGroup = source.getGroup();
    myShownInMainMenu = source.isShownInMainMenu();
    myShownInEditor = source.isShownInEditor();
    myShownInProjectViews = source.isShownInProjectViews();
    myShownInSearchResultsPopup = source.isShownInSearchResultsPopup();
    myEnabled = source.isEnabled();
    myUseConsole = source.isUseConsole();
    myShowConsoleOnStdOut = source.isShowConsoleOnStdOut();
    myShowConsoleOnStdErr = source.isShowConsoleOnStdErr();
    mySynchronizeAfterExecution = source.synchronizeAfterExecution();
    myWorkingDirectory = source.getWorkingDirectory();
    myProgram = source.getProgram();
    myParameters = source.getParameters();
    myOutputFilters = new ArrayList<>(Arrays.asList(source.getOutputFilters()));
  }

  @SuppressWarnings("EqualsHashCode")
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Tool)) return false;
    Tool secondTool = (Tool)obj;

    Tool source = secondTool;

    return
      Comparing.equal(myName, source.myName) &&
      Comparing.equal(myDescription, source.myDescription) &&
      Comparing.equal(myGroup, source.myGroup) &&
      myShownInMainMenu == source.myShownInMainMenu &&
      myShownInEditor == source.myShownInEditor &&
      myShownInProjectViews == source.myShownInProjectViews &&
      myShownInSearchResultsPopup == source.myShownInSearchResultsPopup &&
      myEnabled == source.myEnabled &&
      myUseConsole == source.myUseConsole &&
      myShowConsoleOnStdOut == source.myShowConsoleOnStdOut &&
      myShowConsoleOnStdErr == source.myShowConsoleOnStdErr &&
      mySynchronizeAfterExecution == source.mySynchronizeAfterExecution &&
      Comparing.equal(myWorkingDirectory, source.myWorkingDirectory) &&
      Comparing.equal(myProgram, source.myProgram) &&
      Comparing.equal(myParameters, source.myParameters) &&
      Comparing.equal(myOutputFilters, source.myOutputFilters);
  }

  public String getActionId() {
    StringBuilder name = new StringBuilder(getActionIdPrefix());
    if (myGroup != null) {
      name.append(myGroup);
      name.append('_');
    }
    if (myName != null) {
      name.append(myName);
    }
    return name.toString();
  }

  /**
   * @return <code>true</code> if task has been started successfully
   */
  public boolean execute(AnActionEvent event, DataContext dataContext, long executionId, @Nullable final ProcessListener processListener) {
    final Project project = dataContext.getData(Project.KEY);
    if (project == null) {
      return false;
    }
    FileDocumentManager.getInstance().saveAllDocuments();
    try {
      if (isUseConsole()) {
        final ToolRunProfile profile = new ToolRunProfile(this, dataContext);
        final ProgramRunner runner = RunnerRegistry.getInstance().getRunner(DefaultRunExecutor.EXECUTOR_ID, profile);
        assert runner != null;

        ExecutionEnvironment executionEnvironment = new ExecutionEnvironmentBuilder(project, DefaultRunExecutor.getRunExecutorInstance())
          .runProfile(profile)
          .build();
        executionEnvironment.setExecutionId(executionId);
        runner.execute(executionEnvironment, descriptor -> {
          ProcessHandler processHandler = descriptor.getProcessHandler();
          if (processHandler != null && processListener != null) {
            processHandler.addProcessListener(processListener);
          }
        });
        return true;
      }
      else {
        GeneralCommandLine commandLine = createCommandLine(dataContext);
        if (commandLine == null) {
          return false;
        }
        OSProcessHandler handler = new OSProcessHandler(commandLine.createProcess(), commandLine.getCommandLineString());
        handler.addProcessListener(new ToolProcessAdapter(project, synchronizeAfterExecution(), getName()));
        if (processListener != null) {
          handler.addProcessListener(processListener);
        }
        handler.startNotify();
        return true;
      }
    }
    catch (ExecutionException ex) {
      ExecutionErrorDialog.show(ex, ToolsBundle.message("tools.process.start.error"), project);
    }
    return false;
  }

  @Nullable
  public GeneralCommandLine createCommandLine(DataContext dataContext) {
    if (StringUtil.isEmpty(getWorkingDirectory())) {
      setWorkingDirectory(dataContext.getData(Project.KEY).getBasePath());
    }

    GeneralCommandLine commandLine = new GeneralCommandLine();
    try {
      String paramString = MacroManager.getInstance().expandMacrosInString(getParameters(), true, dataContext);
      String workingDir = MacroManager.getInstance().expandMacrosInString(getWorkingDirectory(), true, dataContext);
      String exePath = MacroManager.getInstance().expandMacrosInString(getProgram(), true, dataContext);

      commandLine.getParametersList().addParametersString(
              MacroManager.getInstance().expandMacrosInString(paramString, false, dataContext));
      final String workDirExpanded = MacroManager.getInstance().expandMacrosInString(workingDir, false, dataContext);
      if (!StringUtil.isEmpty(workDirExpanded)) {
        commandLine.setWorkDirectory(workDirExpanded);
      }
      exePath = MacroManager.getInstance().expandMacrosInString(exePath, false, dataContext);
      if (exePath == null) return null;

      File exeFile = new File(exePath);
      if (exeFile.isDirectory() && exeFile.getName().endsWith(".app")) {
        commandLine.setExePath("open");
        commandLine.getParametersList().prependAll("-a", exePath);
      }
      else {
        commandLine.setExePath(exePath);
      }
    }
    catch (Macro.ExecutionCancelledException e) {
      return null;
    }
    return commandLine;
  }

  @Override
  public void setGroupName(final String name) {
    setGroup(name);
  }

  @Override
  public String getKey() {
    return getName();
  }

  @Override
  public SchemeElement copy() {
    Tool copy = new Tool();
    copy.copyFrom(this);
    return copy;
  }

  @Override
  public String toString() {
    return myGroup + ": " + myName;
  }

  public String getActionIdPrefix() {
    return ACTION_ID_PREFIX;
  }
}
