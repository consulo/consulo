/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.process.local;

import consulo.logging.Logger;
import consulo.process.ExecutionException;
import consulo.process.KillableProcessHandler;
import consulo.process.ProcessHandler;
import consulo.process.ProcessOutputTypes;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.event.ProcessAdapter;
import consulo.process.event.ProcessEvent;
import consulo.process.event.ProcessListener;
import consulo.process.internal.CapturingProcessHandler;
import consulo.process.localize.ProcessLocalize;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import consulo.util.lang.function.Condition;
import consulo.util.lang.function.Conditions;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.util.function.Predicate;

/**
 * FIXME [VISTALL] duplicate code {@link CapturingProcessHandler}

 * @author Elena Shaverdova
 * @author Nikolay Matveev
 */
public final class ScriptRunnerUtil {

  private static final Logger LOG = Logger.getInstance(ScriptRunnerUtil.class);

  public static final Condition<Key> STDOUT_OUTPUT_KEY_FILTER = ProcessOutputTypes.STDOUT::equals;

  public static final Condition<Key> STDERR_OUTPUT_KEY_FILTER = ProcessOutputTypes.STDERR::equals;

  public static final Condition<Key> STDOUT_OR_STDERR_OUTPUT_KEY_FILTER = Conditions.or(STDOUT_OUTPUT_KEY_FILTER, STDERR_OUTPUT_KEY_FILTER);

  private static final int DEFAULT_TIMEOUT = 30000;

  private ScriptRunnerUtil() {
  }

  public static String getProcessOutput(@Nonnull GeneralCommandLine commandLine) throws ExecutionException {
    return getProcessOutput(commandLine, STDOUT_OUTPUT_KEY_FILTER, DEFAULT_TIMEOUT);
  }

  public static String getProcessOutput(@Nonnull GeneralCommandLine commandLine, @Nonnull Predicate<Key> outputTypeFilter, long timeout) throws ExecutionException {
    return getProcessOutput(ProcessHandlerFactory.getInstance().createProcessHandler(commandLine), outputTypeFilter, timeout);
  }

  public static String getProcessOutput(@Nonnull final ProcessHandler processHandler, @Nonnull final Predicate<Key> outputTypeFilter, final long timeout) throws ExecutionException {
    LOG.assertTrue(!processHandler.isStartNotified());
    final StringBuilder outputBuilder = new StringBuilder();
    processHandler.addProcessListener(new ProcessAdapter() {
      @Override
      public void onTextAvailable(ProcessEvent event, Key outputType) {
        if (outputTypeFilter.test(outputType)) {
          final String text = event.getText();
          outputBuilder.append(text);
          if (LOG.isDebugEnabled()) {
            LOG.debug(text);
          }
        }
      }
    });
    processHandler.startNotify();
    if (!processHandler.waitFor(timeout)) {
      throw new ExecutionException(ProcessLocalize.scriptExecutionTimeout(String.valueOf(timeout / 1000)).get());
    }
    return outputBuilder.toString();
  }

  @Nullable
  private static File getShell() {
    final String shell = System.getenv("SHELL");
    if (shell != null && (shell.contains("bash") || shell.contains("zsh"))) {
      File file = new File(shell);
      if (file.isAbsolute() && file.isFile() && file.canExecute()) {
        return file;
      }
    }
    return null;
  }

  @Nonnull
  public static ProcessHandler execute(@Nonnull String exePath, @Nullable String workingDirectory, @Nullable VirtualFile scriptFile, String[] parameters) throws ExecutionException {
    GeneralCommandLine commandLine = new GeneralCommandLine();
    commandLine.setExePath(exePath);
    commandLine.setPassParentEnvironment(true);
    if (scriptFile != null) {
      commandLine.addParameter(scriptFile.getPresentableUrl());
    }
    commandLine.addParameters(parameters);

    if (workingDirectory != null) {
      commandLine.setWorkDirectory(workingDirectory);
    }

    LOG.debug("Command line: " + commandLine.getCommandLineString());
    LOG.debug("Command line env: " + commandLine.getEnvironment());

    final ProcessHandler processHandler = ProcessHandlerFactory.getInstance().createColoredProcessHandler(commandLine);
    if (LOG.isDebugEnabled()) {
      processHandler.addProcessListener(new ProcessListener() {
        @Override
        public void onTextAvailable(ProcessEvent event, Key outputType) {
          LOG.debug(outputType + ": " + event.getText());
        }
      });
    }

    //ProcessTerminatedListener.attach(processHandler, project);
    return processHandler;
  }

  public static ScriptOutput executeScriptInConsoleWithFullOutput(String exePathString,
                                                                  @Nullable VirtualFile scriptFile,
                                                                  @Nullable String workingDirectory,
                                                                  long timeout,
                                                                  Condition<Key> scriptOutputType,
                                                                  @NonNls String... parameters) throws ExecutionException {
    final ProcessHandler processHandler = execute(exePathString, workingDirectory, scriptFile, parameters);

    ScriptOutput output = new ScriptOutput(scriptOutputType);
    processHandler.addProcessListener(output);
    processHandler.startNotify();

    if (!processHandler.waitFor(timeout)) {
      LOG.warn("Process did not complete in " + timeout / 1000 + "s");
      throw new ExecutionException(ProcessLocalize.scriptExecutionTimeout(String.valueOf(timeout / 1000)).get());
    }
    LOG.debug("script output: " + output.myFilteredOutput);
    return output;
  }

  public static class ScriptOutput implements ProcessListener {
    private final Predicate<Key> myScriptOutputType;
    public final StringBuilder myFilteredOutput;
    public final StringBuilder myMergedOutput;

    private ScriptOutput(Predicate<Key> scriptOutputType) {
      myScriptOutputType = scriptOutputType;
      myFilteredOutput = new StringBuilder();
      myMergedOutput = new StringBuilder();
    }

    public String getFilteredOutput() {
      return myFilteredOutput.toString();
    }

    public String getMergedOutput() {
      return myMergedOutput.toString();
    }

    public String[] getOutputToParseArray() {
      return getFilteredOutput().split("\n");
    }

    public String getDescriptiveOutput() {
      String outputToParse = getFilteredOutput();
      return StringUtil.isEmpty(outputToParse) ? getMergedOutput() : outputToParse;
    }

    @Override
    public void onTextAvailable(ProcessEvent event, Key outputType) {
      final String text = event.getText();
      if (myScriptOutputType.test(outputType)) {
        myFilteredOutput.append(text);
      }
      myMergedOutput.append(text);
    }
  }

  /**
   * Gracefully terminates a process handler.
   * Initially, 'soft kill' is performed (on UNIX it's equivalent to SIGINT signal sending).
   * If the process isn't terminated within a given timeout, 'force quite' is performed (on UNIX it's equivalent to SIGKILL
   * signal sending).
   *
   * @param processHandler {@link ProcessHandler} instance
   * @param millisTimeout  timeout in milliseconds between 'soft kill' and 'force quite'
   * @param commandLine    command line
   */
  public static void terminateProcessHandler(@Nonnull ProcessHandler processHandler, long millisTimeout, @Nullable String commandLine) {
    if (processHandler.isProcessTerminated()) {
      LOG.warn("Process '" + commandLine + "' is already terminated!");
      return;
    }
    processHandler.destroyProcess();
    if (processHandler instanceof KillableProcessHandler) {
      KillableProcessHandler killableProcess = (KillableProcessHandler)processHandler;
      if (killableProcess.canKillProcess()) {
        if (!processHandler.waitFor(millisTimeout)) {
          // doing 'force quite'
          killableProcess.killProcess();
        }
      }
    }
  }
}
