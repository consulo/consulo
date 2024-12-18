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
package consulo.execution.configuration;

import consulo.execution.ExecutionTarget;
import consulo.execution.RuntimeConfigurationException;
import consulo.execution.configuration.log.ui.AdditionalTabComponentManager;
import consulo.execution.configuration.log.LogConsole;
import consulo.execution.configuration.log.LogFileOptions;
import consulo.execution.configuration.log.PredefinedLogFile;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.execution.runner.ProgramRunner;
import consulo.process.ProcessHandler;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Standard base class for run configuration implementations.
 *
 * @author dyoma
 */
public abstract class RunConfigurationBase extends UserDataHolderBase implements RunConfiguration, TargetAwareRunProfile, ConfigurationCreationListener {
  private final ConfigurationFactory myFactory;
  private final Project myProject;
  private String myName = "";

  private ArrayList<LogFileOptions> myLogFiles = new ArrayList<>();
  private ArrayList<PredefinedLogFile> myPredefinedLogFiles = new ArrayList<>();

  private static final String LOG_FILE = "log_file";
  private static final String PREDEFINED_LOG_FILE_ELEMENT = "predefined_log_file";
  private static final String FILE_OUTPUT = "output_file";
  private static final String SAVE = "is_save";
  private static final String OUTPUT_FILE = "path";
  private static final String SHOW_CONSOLE_ON_STD_OUT = "show_console_on_std_out";
  private static final String SHOW_CONSOLE_ON_STD_ERR = "show_console_on_std_err";

  private final Image myIcon;
  private boolean mySaveOutput = false;
  private boolean myShowConsoleOnStdOut = false;
  private boolean myShowConsoleOnStdErr = false;
  private String myFileOutputPath = null;

  protected RunConfigurationBase(final Project project, final ConfigurationFactory factory, final String name) {
    myProject = project;
    myFactory = factory;
    myName = name;
    myIcon = factory.getIcon();
  }

  @Override
  public int getUniqueID() {
    return System.identityHashCode(this);
  }

  @Override
  public final ConfigurationFactory getFactory() {
    return myFactory;
  }

  @Override
  public final void setName(final String name) {
    myName = name;
  }

  @Override
  public final Project getProject() {
    return myProject;
  }

  @Override
  @Nonnull
  public ConfigurationType getType() {
    return myFactory.getType();
  }

  @Override
  public Image getIcon() {
    return myIcon;
  }

  @Override
  public final String getName() {
    return myName;
  }

  public final int hashCode() {
    return super.hashCode();
  }

  public void checkRunnerSettings(@Nonnull ProgramRunner runner, @Nullable RunnerSettings runnerSettings, @Nullable ConfigurationPerRunnerSettings configurationPerRunnerSettings)
          throws RuntimeConfigurationException {
  }

  public void checkSettingsBeforeRun() throws RuntimeConfigurationException {
  }

  @Override
  public boolean canRunOn(@Nonnull ExecutionTarget target) {
    return true;
  }

  public final boolean equals(final Object obj) {
    return super.equals(obj);
  }

  @Override
  public RunConfiguration clone() {
    final RunConfigurationBase runConfiguration = (RunConfigurationBase)super.clone();
    runConfiguration.myLogFiles = new ArrayList<>(myLogFiles);
    runConfiguration.myPredefinedLogFiles = new ArrayList<>(myPredefinedLogFiles);
    runConfiguration.myFileOutputPath = myFileOutputPath;
    runConfiguration.mySaveOutput = mySaveOutput;
    runConfiguration.myShowConsoleOnStdOut = myShowConsoleOnStdOut;
    runConfiguration.myShowConsoleOnStdErr = myShowConsoleOnStdErr;
    copyCopyableDataTo(runConfiguration);
    return runConfiguration;
  }

  @Nullable
  public LogFileOptions getOptionsForPredefinedLogFile(PredefinedLogFile predefinedLogFile) {
    return null;
  }

  public void removeAllPredefinedLogFiles() {
    myPredefinedLogFiles.clear();
  }

  public void addPredefinedLogFile(PredefinedLogFile predefinedLogFile) {
    myPredefinedLogFiles.add(predefinedLogFile);
  }

  public ArrayList<PredefinedLogFile> getPredefinedLogFiles() {
    return myPredefinedLogFiles;
  }

  public ArrayList<LogFileOptions> getAllLogFiles() {
    final ArrayList<LogFileOptions> list = new ArrayList<>(myLogFiles);
    for (PredefinedLogFile predefinedLogFile : myPredefinedLogFiles) {
      final LogFileOptions options = getOptionsForPredefinedLogFile(predefinedLogFile);
      if (options != null) {
        list.add(options);
      }
    }
    return list;
  }

  public ArrayList<LogFileOptions> getLogFiles() {
    return myLogFiles;
  }

  public void addLogFile(String file, String alias, boolean checked) {
    myLogFiles.add(new LogFileOptions(alias, file, checked, true, false));
  }

  public void addLogFile(String file, String alias, boolean checked, boolean skipContent, final boolean showAll) {
    myLogFiles.add(new LogFileOptions(alias, file, checked, skipContent, showAll));
  }

  public void removeAllLogFiles() {
    myLogFiles.clear();
  }

  //invoke before run/debug tabs are shown.
  //Should be overridden to add additional tabs for run/debug toolwindow
  public void createAdditionalTabComponents(AdditionalTabComponentManager manager, ProcessHandler startedProcess) {
  }

  public void customizeLogConsole(LogConsole console) {
  }

  @Override
  public void readExternal(Element element) throws InvalidDataException {
    myLogFiles.clear();
    for (final Object o : element.getChildren(LOG_FILE)) {
      LogFileOptions logFileOptions = new LogFileOptions();
      logFileOptions.readExternal((Element)o);
      myLogFiles.add(logFileOptions);
    }
    myPredefinedLogFiles.clear();
    final List list = element.getChildren(PREDEFINED_LOG_FILE_ELEMENT);
    for (Object fileElement : list) {
      final PredefinedLogFile logFile = new PredefinedLogFile();
      logFile.readExternal((Element)fileElement);
      myPredefinedLogFiles.add(logFile);
    }
    final Element fileOutputElement = element.getChild(FILE_OUTPUT);
    if (fileOutputElement != null) {
      myFileOutputPath = fileOutputElement.getAttributeValue(OUTPUT_FILE);
      final String isSave = fileOutputElement.getAttributeValue(SAVE);
      mySaveOutput = isSave != null && Boolean.parseBoolean(isSave);
    }
    myShowConsoleOnStdOut = Boolean.parseBoolean(element.getAttributeValue(SHOW_CONSOLE_ON_STD_OUT));
    myShowConsoleOnStdErr = Boolean.parseBoolean(element.getAttributeValue(SHOW_CONSOLE_ON_STD_ERR));
  }

  @Override
  public void writeExternal(Element element) throws WriteExternalException {
    for (final LogFileOptions options : myLogFiles) {
      Element logFile = new Element(LOG_FILE);
      options.writeExternal(logFile);
      element.addContent(logFile);
    }
    for (PredefinedLogFile predefinedLogFile : myPredefinedLogFiles) {
      Element fileElement = new Element(PREDEFINED_LOG_FILE_ELEMENT);
      predefinedLogFile.writeExternal(fileElement);
      element.addContent(fileElement);
    }
    final Element fileOutputPathElement = new Element(FILE_OUTPUT);
    if (myFileOutputPath != null) {
      fileOutputPathElement.setAttribute(OUTPUT_FILE, myFileOutputPath);
    }
    fileOutputPathElement.setAttribute(SAVE, String.valueOf(mySaveOutput));
    if (myFileOutputPath != null || mySaveOutput) {
      element.addContent(fileOutputPathElement);
    }
    if (myShowConsoleOnStdOut) {//default value shouldn't be written
      element.setAttribute(SHOW_CONSOLE_ON_STD_OUT, String.valueOf(myShowConsoleOnStdOut));
    }
    if (myShowConsoleOnStdErr) {//default value shouldn't be written
      element.setAttribute(SHOW_CONSOLE_ON_STD_ERR, String.valueOf(myShowConsoleOnStdErr));
    }
  }

  public boolean isSaveOutputToFile() {
    return mySaveOutput;
  }

  public void setSaveOutputToFile(boolean redirectOutput) {
    mySaveOutput = redirectOutput;
  }

  public boolean isShowConsoleOnStdOut() {
    return myShowConsoleOnStdOut;
  }

  public void setShowConsoleOnStdOut(boolean showConsoleOnStdOut) {
    myShowConsoleOnStdOut = showConsoleOnStdOut;
  }

  public boolean isShowConsoleOnStdErr() {
    return myShowConsoleOnStdErr;
  }

  public void setShowConsoleOnStdErr(boolean showConsoleOnStdErr) {
    myShowConsoleOnStdErr = showConsoleOnStdErr;
  }

  public String getOutputFilePath() {
    return myFileOutputPath;
  }

  public void setFileOutputPath(String fileOutputPath) {
    myFileOutputPath = fileOutputPath;
  }

  public boolean collectOutputFromProcessHandler() {
    return true;
  }

  public boolean excludeCompileBeforeLaunchOption() {
    return false;
  }

  /**
   * @return true if "Make" Before Launch task should be added automatically on run configuration creation
   * @see RunProfileWithCompileBeforeLaunchOption
   */
  public boolean isCompileBeforeLaunchAddedByDefault() {
    return true;
  }

  @Override
  public String toString() {
    return getType().getDisplayName() + ": " + getName();
  }

  @SuppressWarnings("deprecation")
  @Override
  public ConfigurationPerRunnerSettings createRunnerSettings(ConfigurationInfoProvider provider) {
    return null;
  }

  @Override
  public SettingsEditor<ConfigurationPerRunnerSettings> getRunnerSettingsEditor(ProgramRunner runner) {
    return null;
  }
}
