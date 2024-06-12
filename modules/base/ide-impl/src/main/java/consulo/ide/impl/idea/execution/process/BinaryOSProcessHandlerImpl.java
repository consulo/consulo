/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.execution.process;

import consulo.process.io.BinaryOutputReader;
import consulo.process.BinaryProcessHandler;
import consulo.process.ExecutionException;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.internal.OSProcessHandler;
import consulo.process.io.BaseDataReader;
import consulo.util.io.BufferExposingByteArrayOutputStream;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.concurrent.Future;

public class BinaryOSProcessHandlerImpl extends OSProcessHandler implements BinaryProcessHandler {
  private final BufferExposingByteArrayOutputStream myOutput = new BufferExposingByteArrayOutputStream();

  public BinaryOSProcessHandlerImpl(@Nonnull GeneralCommandLine commandLine) throws ExecutionException {
    super(commandLine);
  }

  public BinaryOSProcessHandlerImpl(@Nonnull Process process, @Nonnull String commandLine, @Nullable Charset charset) {
    super(process, commandLine, charset);
  }

  @Override
  @Nonnull
  public byte[] getOutput() {
    return myOutput.toByteArray();
  }

  @Nonnull
  @Override
  protected BaseDataReader createOutputDataReader() {
    return new SimpleBinaryReader(myProcess.getInputStream(), readerOptions().policy());
  }

  private class SimpleBinaryReader extends BinaryOutputReader {
    private SimpleBinaryReader(InputStream stream, SleepingPolicy policy) {
      super(stream, policy);
      start("output stream of " + myPresentableName);
    }

    @Override
    protected void onBinaryAvailable(@Nonnull byte[] data, int size) {
      myOutput.write(data, 0, size);
    }

    @Nonnull
    @Override
    protected Future<?> executeOnPooledThread(@Nonnull Runnable runnable) {
      return BinaryOSProcessHandlerImpl.this.executeTask(runnable);
    }
  }
}