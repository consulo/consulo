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
package com.intellij.execution.process;

import consulo.util.dataholder.Key;
import javax.annotation.Nonnull;

/**
 * @author traff
 */
public class CapturingProcessAdapter extends ProcessAdapter {
  private final ProcessOutput myOutput;

  public CapturingProcessAdapter() {
    this(new ProcessOutput());
  }

  public CapturingProcessAdapter(@Nonnull ProcessOutput output) {
    myOutput = output;
  }

  @Override
  public void onTextAvailable(@Nonnull ProcessEvent event, @Nonnull Key outputType) {
    addToOutput(event.getText(), outputType);
  }

  protected void addToOutput(String text, Key outputType) {
    if (outputType == ProcessOutputTypes.STDOUT) {
      myOutput.appendStdout(text);
    }
    else if (outputType == ProcessOutputTypes.STDERR) {
      myOutput.appendStderr(text);
    }
  }

  @Override
  public void processTerminated(@Nonnull ProcessEvent event) {
    myOutput.setExitCode(event.getExitCode());
  }

  public ProcessOutput getOutput() {
    return myOutput;
  }
}
