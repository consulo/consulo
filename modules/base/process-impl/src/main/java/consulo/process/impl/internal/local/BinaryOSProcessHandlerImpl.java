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
package consulo.process.impl.internal.local;

import consulo.process.io.BinaryOutputReader;
import consulo.process.BinaryProcessHandler;
import consulo.process.ExecutionException;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.internal.OSProcessHandler;
import consulo.process.io.BaseDataReader;
import consulo.util.io.BufferExposingByteArrayOutputStream;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.concurrent.Future;

public class BinaryOSProcessHandlerImpl extends OSProcessHandler implements BinaryProcessHandler {
  private final BufferExposingByteArrayOutputStream myOutput = new BufferExposingByteArrayOutputStream();

  public BinaryOSProcessHandlerImpl(GeneralCommandLine commandLine) throws ExecutionException {
    super(commandLine);
  }

  public BinaryOSProcessHandlerImpl(Process process, String commandLine, @Nullable Charset charset) {
    super(process, commandLine, charset);
  }

  @Override
  
  public byte[] getOutput() {
    return myOutput.toByteArray();
  }

  
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
    protected void onBinaryAvailable(byte[] data, int size) {
      myOutput.write(data, 0, size);
    }

    
    @Override
    protected Future<?> executeOnPooledThread(Runnable runnable) {
      return BinaryOSProcessHandlerImpl.this.executeTask(runnable);
    }
  }
}