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

import consulo.component.extension.ExtensionPoint;
import consulo.execution.ExecutionTarget;
import consulo.execution.RuntimeConfigurationException;
import consulo.execution.configuration.log.ui.AdditionalTabComponentManager;
import consulo.execution.configuration.log.LogConsole;
import consulo.execution.configuration.log.LogFileOptions;
import consulo.execution.configuration.log.PredefinedLogFile;
import consulo.execution.configuration.fragment.FragmentStateHolder;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.runner.ProgramRunner;
import consulo.process.ProcessHandler;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.SkipDefaultsSerializationFilter;
import consulo.util.xml.serializer.WriteExternalException;
import consulo.util.xml.serializer.XmlSerializer;
import org.jdom.Element;

import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Standard base class for run configuration implementations.
 *
 * @author dyoma
 */
public abstract class RunConfigurationBase extends UserDataHolderBase
  implements RunConfiguration, TargetAwareRunProfile, ConfigurationCreationListener, FragmentStateHolder {
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
  private static final String FORMAT = "format";
  private static final String FORMAT_VERSION = "2";
  private static final String OPTIONS = "options";
  private static final String FRAGMENT = "fragment";
  private static final String FRAGMENT_ID = "id";
  private static final String SELECTED_OPTIONS = "selectedOptions";
  private static final String SELECTED_OPTION = "option";
  private static final String SELECTED_OPTION_NAME = "name";
  private static final String SELECTED_OPTION_VISIBLE = "visible";
  private static final String LEGACY_EXTENSIONS = "legacyExtensions";

  private final Image myIcon;
  private boolean mySaveOutput = false;
  private boolean myShowConsoleOnStdOut = false;
  private boolean myShowConsoleOnStdErr = false;
  private String myFileOutputPath = null;

  private @Nullable RunConfigurationOptions myOptions;
  private Map<String, Object> myFragmentStates = new LinkedHashMap<>();
  private Map<String, Element> myUnknownFragments = new LinkedHashMap<>();
  private Map<String, Boolean> myFragmentVisibility = new LinkedHashMap<>();
  private List<Element> myLegacyExtensions = new ArrayList<>();

  protected RunConfigurationBase(Project project, ConfigurationFactory factory, String name) {
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
  public final void setName(String name) {
    myName = name;
  }

  @Override
  public final Project getProject() {
    return myProject;
  }

  @Override
  
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

  @Override
  public final int hashCode() {
    return super.hashCode();
  }

  public void checkRunnerSettings(ProgramRunner runner, @Nullable RunnerSettings runnerSettings, @Nullable ConfigurationPerRunnerSettings configurationPerRunnerSettings)
          throws RuntimeConfigurationException {
  }

  public void checkSettingsBeforeRun() throws RuntimeConfigurationException {
  }

  @Override
  public boolean canRunOn(ExecutionTarget target) {
    return true;
  }

  @Override
  public final boolean equals(@Nullable Object obj) {
    return super.equals(obj);
  }

  /**
   * The class of this configuration's options POJO. {@code null} means the configuration still persists itself through its own
   * {@link #readExternal(Element)}/{@link #writeExternal(Element)}.
   */
  protected @Nullable Class<? extends RunConfigurationOptions> getOptionsClass() {
    return null;
  }

  public RunConfigurationOptions getOptions() {
    RunConfigurationOptions options = myOptions;
    if (options == null) {
      Class<? extends RunConfigurationOptions> optionsClass = getOptionsClass();
      Class<? extends RunConfigurationOptions> type = optionsClass != null ? optionsClass : RunConfigurationOptions.class;
      options = LegacyRunConfigurationConverter.newInstance(type);
      myOptions = options;
    }
    return options;
  }

  @Override
  public <S> S getFragmentState(Class<S> stateClass) {
    for (Object state : myFragmentStates.values()) {
      if (stateClass.isInstance(state)) {
        return stateClass.cast(state);
      }
    }

    RunConfigurationFragment<?> fragment = getProject().getApplication().getExtensionPoint(RunConfigurationFragment.class)
      .findFirstSafe(it -> stateClass.equals(it.getStateClass()));
    if (fragment == null) {
      throw new IllegalArgumentException("No run configuration fragment for " + stateClass.getName());
    }

    S state = LegacyRunConfigurationConverter.newInstance(stateClass);
    myFragmentStates.put(fragment.getId(), state);
    return state;
  }

  /**
   * The visibility the user chose for a fragment in "Modify options", {@code null} when the fragment follows its default.
   */
  public @Nullable Boolean getFragmentVisibility(String fragmentId) {
    return myFragmentVisibility.get(fragmentId);
  }

  public void setFragmentVisibility(String fragmentId, @Nullable Boolean visible) {
    if (visible == null) {
      myFragmentVisibility.remove(fragmentId);
    }
    else {
      myFragmentVisibility.put(fragmentId, visible);
    }
  }

  /**
   * Converts the old XML of this configuration into its new state: read the old element ({@link LegacyRunConfigurationConverter#readXml}),
   * then {@link LegacyRunConfigurationConverter#setOptions} and {@link LegacyRunConfigurationConverter#addFragment}.
   */
  protected void convertLegacyState(LegacyRunConfigurationConverter converter) {
  }

  @Override
  public RunConfiguration clone() {
    RunConfigurationBase runConfiguration = (RunConfigurationBase)super.clone();
    runConfiguration.myOptions = null;
    runConfiguration.myFragmentStates = new LinkedHashMap<>();
    runConfiguration.myUnknownFragments = new LinkedHashMap<>();
    runConfiguration.myFragmentVisibility = new LinkedHashMap<>(myFragmentVisibility);
    runConfiguration.myLegacyExtensions = new ArrayList<>();
    if (getOptionsClass() != null) {
      Element state = new Element("state");
      writeState(state);
      runConfiguration.readState(state);
    }
    runConfiguration.myLogFiles = new ArrayList<>(myLogFiles);
    runConfiguration.myPredefinedLogFiles = new ArrayList<>(myPredefinedLogFiles);
    runConfiguration.myFileOutputPath = myFileOutputPath;
    runConfiguration.mySaveOutput = mySaveOutput;
    runConfiguration.myShowConsoleOnStdOut = myShowConsoleOnStdOut;
    runConfiguration.myShowConsoleOnStdErr = myShowConsoleOnStdErr;
    copyCopyableDataTo(runConfiguration);
    return runConfiguration;
  }

  public @Nullable LogFileOptions getOptionsForPredefinedLogFile(PredefinedLogFile predefinedLogFile) {
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
    ArrayList<LogFileOptions> list = new ArrayList<>(myLogFiles);
    for (PredefinedLogFile predefinedLogFile : myPredefinedLogFiles) {
      LogFileOptions options = getOptionsForPredefinedLogFile(predefinedLogFile);
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

  public void addLogFile(String file, String alias, boolean checked, boolean skipContent, boolean showAll) {
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
    for (Object o : element.getChildren(LOG_FILE)) {
      LogFileOptions logFileOptions = new LogFileOptions();
      logFileOptions.readExternal((Element)o);
      myLogFiles.add(logFileOptions);
    }
    myPredefinedLogFiles.clear();
    List list = element.getChildren(PREDEFINED_LOG_FILE_ELEMENT);
    for (Object fileElement : list) {
      PredefinedLogFile logFile = new PredefinedLogFile();
      logFile.readExternal((Element)fileElement);
      myPredefinedLogFiles.add(logFile);
    }
    Element fileOutputElement = element.getChild(FILE_OUTPUT);
    if (fileOutputElement != null) {
      myFileOutputPath = fileOutputElement.getAttributeValue(OUTPUT_FILE);
      String isSave = fileOutputElement.getAttributeValue(SAVE);
      mySaveOutput = isSave != null && Boolean.parseBoolean(isSave);
    }
    myShowConsoleOnStdOut = Boolean.parseBoolean(element.getAttributeValue(SHOW_CONSOLE_ON_STD_OUT));
    myShowConsoleOnStdErr = Boolean.parseBoolean(element.getAttributeValue(SHOW_CONSOLE_ON_STD_ERR));

    if (getOptionsClass() == null) {
      return;
    }

    if (FORMAT_VERSION.equals(element.getAttributeValue(FORMAT))) {
      readState(element);
    }
    else {
      LegacyRunConfigurationConverter converter = new LegacyRunConfigurationConverter(element);
      convertLegacyState(converter);
      getProject().getApplication().getExtensionPoint(RunConfigurationFragment.class).forEach(fragment -> {
        if (fragment.isApplicable(this)) {
          fragment.convertLegacyState(this, converter);
        }
      });

      Element state = new Element("state");
      writeState(state, converter.getOptions(), converter.getFragmentStates());
      writeLegacyExtensions(state, converter.getUnconvertedExtensions());
      readState(state);
    }
  }

  @Override
  public void writeExternal(Element element) throws WriteExternalException {
    for (LogFileOptions options : myLogFiles) {
      Element logFile = new Element(LOG_FILE);
      options.writeExternal(logFile);
      element.addContent(logFile);
    }
    for (PredefinedLogFile predefinedLogFile : myPredefinedLogFiles) {
      Element fileElement = new Element(PREDEFINED_LOG_FILE_ELEMENT);
      predefinedLogFile.writeExternal(fileElement);
      element.addContent(fileElement);
    }
    Element fileOutputPathElement = new Element(FILE_OUTPUT);
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

    if (getOptionsClass() != null) {
      element.setAttribute(FORMAT, FORMAT_VERSION);
      writeState(element);
    }
  }

  private void writeState(Element element) {
    List<Object> states = new ArrayList<>(myFragmentStates.values());
    writeState(element, getOptions(), states);
    for (Map.Entry<String, Element> entry : myUnknownFragments.entrySet()) {
      element.addContent(entry.getValue().clone());
    }

    writeLegacyExtensions(element, myLegacyExtensions);

    if (!myFragmentVisibility.isEmpty()) {
      Element selectedOptions = new Element(SELECTED_OPTIONS);
      for (Map.Entry<String, Boolean> entry : myFragmentVisibility.entrySet()) {
        selectedOptions.addContent(new Element(SELECTED_OPTION)
          .setAttribute(SELECTED_OPTION_NAME, entry.getKey())
          .setAttribute(SELECTED_OPTION_VISIBLE, String.valueOf(entry.getValue())));
      }
      element.addContent(selectedOptions);
    }
  }

  private void writeState(Element element, @Nullable Object options, List<Object> fragmentStates) {
    if (options != null) {
      Element optionsElement = new Element(OPTIONS);
      XmlSerializer.serializeInto(options, optionsElement, new SkipDefaultsSerializationFilter());
      if (!isEmpty(optionsElement)) {
        element.addContent(optionsElement);
      }
    }

    getProject().getApplication().getExtensionPoint(RunConfigurationFragment.class).forEach(fragment -> {
      Class<?> stateClass = fragment.getStateClass();
      if (stateClass == null) {
        return;
      }

      for (Object state : fragmentStates) {
        if (stateClass.isInstance(state)) {
          Element fragmentElement = new Element(FRAGMENT).setAttribute(FRAGMENT_ID, fragment.getId());
          XmlSerializer.serializeInto(state, fragmentElement, new SkipDefaultsSerializationFilter());
          if (fragmentElement.getAttributes().size() > 1 || fragmentElement.getContentSize() > 0) {
            element.addContent(fragmentElement);
          }
          return;
        }
      }
    });
  }

  private void readState(Element element) {
    RunConfigurationOptions options = LegacyRunConfigurationConverter.newInstance(getOptions().getClass());
    Element optionsElement = element.getChild(OPTIONS);
    if (optionsElement != null) {
      XmlSerializer.deserializeInto(options, optionsElement);
    }
    myOptions = options;

    myFragmentStates.clear();
    myUnknownFragments.clear();
    for (Element fragmentElement : element.getChildren(FRAGMENT)) {
      String id = fragmentElement.getAttributeValue(FRAGMENT_ID);
      if (id == null) {
        continue;
      }

      RunConfigurationFragment<?> fragment = getProject().getApplication().getExtensionPoint(RunConfigurationFragment.class)
        .findFirstSafe(it -> id.equals(it.getId()) && it.getStateClass() != null);
      Class<?> stateClass = fragment == null ? null : fragment.getStateClass();
      if (stateClass == null) {
        myUnknownFragments.put(id, fragmentElement.clone());
        continue;
      }

      Object state = LegacyRunConfigurationConverter.newInstance(stateClass);
      XmlSerializer.deserializeInto(state, fragmentElement);
      myFragmentStates.put(id, state);
    }

    myFragmentVisibility.clear();
    Element selectedOptions = element.getChild(SELECTED_OPTIONS);
    if (selectedOptions != null) {
      for (Element option : selectedOptions.getChildren(SELECTED_OPTION)) {
        String name = option.getAttributeValue(SELECTED_OPTION_NAME);
        if (name != null) {
          myFragmentVisibility.put(name, Boolean.parseBoolean(option.getAttributeValue(SELECTED_OPTION_VISIBLE)));
        }
      }
    }

    myLegacyExtensions = new ArrayList<>();
    Element legacyExtensions = element.getChild(LEGACY_EXTENSIONS);
    if (legacyExtensions != null && !legacyExtensions.getChildren().isEmpty()) {
      convertLegacyExtensions(legacyExtensions.getChildren());
    }
  }

  private static void writeLegacyExtensions(Element element, List<Element> extensions) {
    if (extensions.isEmpty()) {
      return;
    }

    Element legacyExtensions = new Element(LEGACY_EXTENSIONS);
    for (Element extension : extensions) {
      legacyExtensions.addContent(extension.clone());
    }
    element.addContent(legacyExtensions);
  }

  /**
   * Offers old run configuration extension settings to the applicable fragments; what none converts is kept for later.
   */
  private void convertLegacyExtensions(List<Element> extensions) {
    Element legacy = new Element("configuration");
    for (Element extension : extensions) {
      legacy.addContent(extension.clone());
    }

    LegacyRunConfigurationConverter converter = new LegacyRunConfigurationConverter(legacy);
    ExtensionPoint<RunConfigurationFragment> point = getProject().getApplication().getExtensionPoint(RunConfigurationFragment.class);
    point.forEach(fragment -> {
      if (fragment.isApplicable(this)) {
        fragment.convertLegacyState(this, converter);
      }
    });

    for (Object state : converter.getFragmentStates()) {
      RunConfigurationFragment<?> fragment = point.findFirstSafe(it -> state.getClass().equals(it.getStateClass()));
      if (fragment != null) {
        myFragmentStates.putIfAbsent(fragment.getId(), state);
      }
    }
    myLegacyExtensions = new ArrayList<>(converter.getUnconvertedExtensions());
  }

  private static boolean isEmpty(Element element) {
    return !element.hasAttributes() && element.getContentSize() == 0;
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
}
