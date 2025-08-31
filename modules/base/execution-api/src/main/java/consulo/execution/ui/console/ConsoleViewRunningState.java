/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.execution.ui.console;

import consulo.execution.localize.ExecutionLocalize;
import consulo.process.ProcessHandler;
import consulo.process.event.ProcessEvent;
import consulo.process.event.ProcessListener;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.encoding.EncodingManager;
import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.function.BiPredicate;

public class ConsoleViewRunningState extends ConsoleState {
  private final ConsoleView myConsole;
  private final ProcessHandler myProcessHandler;
  private final ConsoleState myFinishedStated;
  private final Writer myUserInputWriter;

  private final ProcessListener myProcessListener = new ProcessListener() {
    @Override
    public void onTextAvailable(ProcessEvent event, Key outputType) {
      BiPredicate<ProcessEvent, Key> processTextFilter = myConsole.getProcessTextFilter();
      if (processTextFilter != null && processTextFilter.test(event, outputType)) {
        return;
      }

      myConsole.print(event.getText(), ConsoleViewContentType.getConsoleViewType(outputType));
    }
  };

  public ConsoleViewRunningState(ConsoleView console,
                                 ProcessHandler processHandler,
                                 ConsoleState finishedStated,
                                 boolean attachToStdOut,
                                 boolean attachToStdIn) {
    myConsole = console;
    myProcessHandler = processHandler;
    myFinishedStated = finishedStated;

    // attach to process stdout
    if (attachToStdOut) {
      processHandler.addProcessListener(myProcessListener);
    }

    // attach to process stdin
    if (attachToStdIn) {
      OutputStream processInput = myProcessHandler.getProcessInput();
      myUserInputWriter = processInput != null ? createOutputStreamWriter(processInput, processHandler) : null;
    }
    else {
      myUserInputWriter = null;
    }
  }

  private static OutputStreamWriter createOutputStreamWriter(OutputStream processInput, ProcessHandler processHandler) {
    Charset charset = processHandler.getCharset();

    if (charset == null) {
      charset = EncodingManager.getInstance().getDefaultCharset();
    }

    return new OutputStreamWriter(processInput, charset);
  }

  @Override
  @Nonnull
  public ConsoleState dispose() {
    if (myProcessHandler != null) {
      myProcessHandler.removeProcessListener(myProcessListener);
    }
    return myFinishedStated;
  }

  @Override
  public boolean isFinished() {
    return myProcessHandler == null || myProcessHandler.isProcessTerminated();
  }

  @Override
  public boolean isCommandLine(@Nonnull String line) {
    return line.equals((myProcessHandler).getCommandLine());
  }

  @Override
  public boolean isRunning() {
    return myProcessHandler != null && !myProcessHandler.isProcessTerminated();
  }

  @Override
  public void sendUserInput(String input) throws IOException {
    if (myUserInputWriter == null) {
      throw new IOException(ExecutionLocalize.noUserProcessInputErrorMessage().get());
    }
    myUserInputWriter.write(input);
    myUserInputWriter.flush();
  }

  @Nonnull
  @Override
  public ConsoleState attachTo(ConsoleView console, ProcessHandler processHandler) {
    return dispose().attachTo(console, processHandler);
  }

  @Override
  public String toString() {
    return "Running state";
  }
}
