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
package com.intellij.execution.configurations;

import com.intellij.execution.CommandLineUtil;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.FilePathSeparator;
import com.intellij.execution.process.ProcessNotCreatedException;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.util.EnvironmentUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.text.CaseInsensitiveStringHashingStrategy;
import consulo.annotation.DeprecationInfo;
import consulo.logging.Logger;
import consulo.util.collection.DelegateMap;
import consulo.util.collection.Maps;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

/**
 * OS-independent way of executing external processes with complex parameters.
 * <p>
 * Main idea of the class is to accept parameters "as-is", just as they should look to an external process, and quote/escape them
 * as required by the underlying platform.
 *
 * @see com.intellij.execution.process.OSProcessHandler
 */
public class GeneralCommandLine implements UserDataHolder {
  private static final Logger LOG = Logger.getInstance(GeneralCommandLine.class);

  /**
   * Determines the scope of a parent environment passed to a child process.
   * <p>
   * {@code NONE} means a child process will receive an empty environment. <br/>
   * {@code SYSTEM} will provide it with the same environment as an IDE. <br/>
   * {@code CONSOLE} provides the child with a similar environment as if it was launched from, well, a console.
   * On OS X, a console environment is simulated (see {@link EnvironmentUtil#getEnvironmentMap()} for reasons it's needed
   * and details on how it works). On Windows and Unix hosts, this option is no different from {@code SYSTEM}
   * since there is no drastic distinction in environment between GUI and console apps.
   */
  public enum ParentEnvironmentType {
    NONE, SYSTEM, CONSOLE
  }

  private String myExePath = null;
  private File myWorkDirectory = null;
  private final Map<String, String> myEnvParams = new MyStrictMap();
  private ParentEnvironmentType myParentEnvironmentType = ParentEnvironmentType.CONSOLE;
  private final ParametersList myProgramParams = new ParametersList();
  private Charset myCharset = CharsetToolkit.getDefaultSystemCharset();
  private boolean myRedirectErrorStream = false;
  private Map<Object, Object> myUserData = null;

  public GeneralCommandLine() {
  }

  public GeneralCommandLine(@Nonnull String... command) {
    this(Arrays.asList(command));
  }

  public GeneralCommandLine(@Nonnull List<String> command) {
    int size = command.size();
    if (size > 0) {
      setExePath(command.get(0));
      if (size > 1) {
        addParameters(command.subList(1, size));
      }
    }
  }

  protected GeneralCommandLine(@Nonnull GeneralCommandLine original) {
    myExePath = original.myExePath;
    myWorkDirectory = original.myWorkDirectory;
    myEnvParams.putAll(original.myEnvParams);
    myParentEnvironmentType = original.myParentEnvironmentType;
    original.myProgramParams.copyTo(myProgramParams);
    myCharset = original.myCharset;
    myRedirectErrorStream = original.myRedirectErrorStream;
    //myInputFile = original.myInputFile;
    // this is intentional memory waste, to avoid warning suppression. We should not copy UserData, but can't suppress a warning for a single field
    myUserData = new HashMap<>();
  }

  public String getExePath() {
    return myExePath;
  }

  @Nonnull
  public GeneralCommandLine withExePath(@Nonnull String exePath) {
    myExePath = exePath.trim();
    return this;
  }

  public void setExePath(@Nonnull String exePath) {
    withExePath(exePath);
  }

  public File getWorkDirectory() {
    return myWorkDirectory;
  }

  @Nonnull
  public GeneralCommandLine withWorkDirectory(@Nullable String path) {
    return withWorkDirectory(path != null ? new File(path) : null);
  }

  @Nonnull
  public GeneralCommandLine withWorkDirectory(@Nullable File workDirectory) {
    myWorkDirectory = workDirectory;
    return this;
  }

  public void setWorkDirectory(@Nullable String path) {
    withWorkDirectory(path);
  }

  public void setWorkDirectory(@Nullable File workDirectory) {
    withWorkDirectory(workDirectory);
  }

  /**
   * Note: the map returned is forgiving to passing null values into putAll().
   */
  @Nonnull
  public Map<String, String> getEnvironment() {
    return myEnvParams;
  }

  @Nonnull
  public GeneralCommandLine withEnvironment(@Nullable Map<String, String> environment) {
    if (environment != null) {
      getEnvironment().putAll(environment);
    }
    return this;
  }

  @Nonnull
  public GeneralCommandLine withEnvironment(@Nonnull String key, @Nonnull String value) {
    getEnvironment().put(key, value);
    return this;
  }

  @Deprecated
  @DeprecationInfo(value = "Use #getParentEnvironmentType()", until = "3.0")
  public boolean isPassParentEnvironment() {
    return myParentEnvironmentType != ParentEnvironmentType.NONE;
  }

  @Nonnull
  @Deprecated
  @DeprecationInfo(value = "Use #withParentEnvironmentType(ParentEnvironmentType)", until = "3.0")
  public GeneralCommandLine withPassParentEnvironment(boolean passParentEnvironment) {
    withParentEnvironmentType(passParentEnvironment ? ParentEnvironmentType.CONSOLE : ParentEnvironmentType.NONE);
    return this;
  }

  @Nonnull
  @Deprecated
  @DeprecationInfo(value = "Use #withParentEnvironmentType(ParentEnvironmentType)", until = "3.0")
  public void setPassParentEnvironment(boolean passParentEnvironment) {
    withPassParentEnvironment(passParentEnvironment);
  }

  @Nonnull
  public ParentEnvironmentType getParentEnvironmentType() {
    return myParentEnvironmentType;
  }

  @Nonnull
  public GeneralCommandLine withParentEnvironmentType(@Nonnull ParentEnvironmentType type) {
    myParentEnvironmentType = type;
    return this;
  }

  /**
   * @return unmodifiable map of the parent environment, that will be passed to the process if isPassParentEnvironment() == true
   */
  /**
   * Returns an environment that will be inherited by a child process.
   * @see #getEffectiveEnvironment()
   */
  @Nonnull
  public Map<String, String> getParentEnvironment() {
    switch (myParentEnvironmentType) {
      case SYSTEM:
        return System.getenv();
      case CONSOLE:
        return EnvironmentUtil.getEnvironmentMap();
      default:
        return Collections.emptyMap();
    }
  }

  /**
   * Returns an environment as seen by a child process,
   * that is the {@link #getEnvironment() environment} merged with the {@link #getParentEnvironment() parent} one.
   */
  @Nonnull
  public Map<String, String> getEffectiveEnvironment() {
    MyStrictMap env = new MyStrictMap();
    setupEnvironment(env);
    return env;
  }

  public void addParameters(String... parameters) {
    for (String parameter : parameters) {
      addParameter(parameter);
    }
  }

  public void addParameters(@Nonnull List<String> parameters) {
    for (String parameter : parameters) {
      addParameter(parameter);
    }
  }

  public void addParameter(@Nonnull String parameter) {
    myProgramParams.add(parameter);
  }

  @Nonnull
  public GeneralCommandLine withParameters(@Nonnull String... parameters) {
    for (String parameter : parameters) addParameter(parameter);
    return this;
  }

  @Nonnull
  public GeneralCommandLine withParameters(@Nonnull List<String> parameters) {
    for (String parameter : parameters) addParameter(parameter);
    return this;
  }


  public ParametersList getParametersList() {
    return myProgramParams;
  }

  @Nonnull
  public Charset getCharset() {
    return myCharset;
  }

  @Nonnull
  public GeneralCommandLine withCharset(@Nonnull Charset charset) {
    myCharset = charset;
    return this;
  }

  public void setCharset(@Nonnull Charset charset) {
    withCharset(charset);
  }

  public boolean isRedirectErrorStream() {
    return myRedirectErrorStream;
  }

  @Nonnull
  public GeneralCommandLine withRedirectErrorStream(boolean redirectErrorStream) {
    myRedirectErrorStream = redirectErrorStream;
    return this;
  }

  public void setRedirectErrorStream(boolean redirectErrorStream) {
    withRedirectErrorStream(redirectErrorStream);
  }

  /**
   * Returns string representation of this command line.<br/>
   * Warning: resulting string is not OS-dependent - <b>do not</b> use it for executing this command line.
   *
   * @return single-string representation of this command line.
   */
  public String getCommandLineString() {
    return getCommandLineString(null);
  }

  /**
   * Returns string representation of this command line.<br/>
   * Warning: resulting string is not OS-dependent - <b>do not</b> use it for executing this command line.
   *
   * @param exeName use this executable name instead of given by {@link #setExePath(String)}
   * @return single-string representation of this command line.
   */
  public String getCommandLineString(@Nullable String exeName) {
    return ParametersList.join(getCommandLineList(exeName));
  }

  public List<String> getCommandLineList(@Nullable String exeName) {
    List<String> commands = new ArrayList<String>();
    if (exeName != null) {
      commands.add(exeName);
    }
    else if (myExePath != null) {
      commands.add(myExePath);
    }
    else {
      commands.add("<null>");
    }
    commands.addAll(myProgramParams.getList());
    return commands;
  }

  /**
   * Prepares command (quotes and escapes all arguments) and returns it as a newline-separated list
   * (suitable e.g. for passing in an environment variable).
   *
   * @param filePathSeparator a target platform
   * @return command as a newline-separated list.
   */
  @Nonnull
  public String getPreparedCommandLine(@Nonnull FilePathSeparator filePathSeparator) {
    String exePath = myExePath != null ? myExePath : "";
    return StringUtil.join(CommandLineUtil.toCommandLine(exePath, myProgramParams.getList(), filePathSeparator), "\n");
  }

  @Nonnull
  public Process createProcess() throws ExecutionException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Executing [" + getCommandLineString() + "]");
      LOG.debug("  environment: " + myEnvParams + " (+" + myParentEnvironmentType + ")");
      LOG.debug("  charset: " + myCharset);
    }

    List<String> commands;
    try {
      checkWorkingDirectory();

      if (StringUtil.isEmptyOrSpaces(myExePath)) {
        throw new ExecutionException(IdeBundle.message("run.configuration.error.executable.not.specified"));
      }

      commands = CommandLineUtil.toCommandLine(myExePath, myProgramParams.getList());
    }
    catch (ExecutionException e) {
      LOG.info(e);
      throw e;
    }

    try {
      return startProcess(commands);
    }
    catch (IOException e) {
      LOG.info(e);
      throw new ProcessNotCreatedException(e.getMessage(), e, this);
    }
  }

  @Nonnull
  protected Process startProcess(@Nonnull List<String> commands) throws IOException {
    ProcessBuilder builder = new ProcessBuilder(commands);
    setupEnvironment(builder.environment());
    builder.directory(myWorkDirectory);
    builder.redirectErrorStream(myRedirectErrorStream);
    return builder.start();
  }

  private void checkWorkingDirectory() throws ExecutionException {
    if (myWorkDirectory == null) {
      return;
    }
    if (!myWorkDirectory.exists()) {
      throw new ExecutionException(IdeBundle.message("run.configuration.error.working.directory.does.not.exist", myWorkDirectory.getAbsolutePath()));
    }
    if (!myWorkDirectory.isDirectory()) {
      throw new ExecutionException(IdeBundle.message("run.configuration.error.working.directory.not.directory"));
    }
  }

  protected void setupEnvironment(@Nonnull Map<String, String> environment) {
    environment.clear();

    if (myParentEnvironmentType != ParentEnvironmentType.NONE) {
      environment.putAll(getParentEnvironment());
    }

    if (SystemInfo.isUnix) {
      File workDirectory = getWorkDirectory();
      if (workDirectory != null) {
        environment.put("PWD", FileUtil.toSystemDependentName(workDirectory.getAbsolutePath()));
      }
    }

    if (!myEnvParams.isEmpty()) {
      if (SystemInfo.isWindows) {
        Map<String, String> envVars = Maps.newHashMap(CaseInsensitiveStringHashingStrategy.INSTANCE);
        envVars.putAll(environment);
        envVars.putAll(myEnvParams);
        environment.clear();
        environment.putAll(envVars);
      }
      else {
        environment.putAll(myEnvParams);
      }
    }
  }

  /**
   * Normally, double quotes in parameters are escaped so they arrive to a called program as-is.
   * But some commands (e.g. {@code 'cmd /c start "title" ...'}) should get they quotes non-escaped.
   * Wrapping a parameter by this method (instead of using quotes) will do exactly this.
   *
   * @see com.intellij.execution.util.ExecUtil#getTerminalCommand(String, String)
   */
  @Nonnull
  public static String inescapableQuote(@Nonnull String parameter) {
    return CommandLineUtil.specialQuote(parameter);
  }

  @Override
  public String toString() {
    return myExePath + " " + myProgramParams;
  }

  @Override
  public <T> T getUserData(@Nonnull Key<T> key) {
    if (myUserData != null) {
      @SuppressWarnings({"UnnecessaryLocalVariable", "unchecked"}) T t = (T)myUserData.get(key);
      return t;
    }
    return null;
  }

  @Override
  public <T> void putUserData(@Nonnull Key<T> key, @Nullable T value) {
    if (myUserData == null) {
      myUserData = ContainerUtil.newHashMap();
    }
    myUserData.put(key, value);
  }

  private static class MyStrictMap extends DelegateMap<String, String> {
    public MyStrictMap() {
      super(Maps.newHashMap(SystemInfo.isWindows ? CaseInsensitiveStringHashingStrategy.INSTANCE : ContainerUtil.<String>canonicalStrategy()));
    }

    @Override
    public String put(String key, String value) {
      if (key == null || value == null) {
        LOG.error(new Exception("Nulls are not allowed"));
        return null;
      }
      if (key.isEmpty()) {
        // Windows: passing an environment variable with empty name causes "CreateProcess error=87, The parameter is incorrect"
        LOG.warn("Skipping environment variable with empty name, value: " + value);
        return null;
      }
      return super.put(key, value);
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> map) {
      if (map != null) {
        super.putAll(map);
      }
    }
  }
}
