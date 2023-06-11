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
package consulo.process;

import consulo.process.event.ProcessListener;
import consulo.process.io.ProcessIOExecutorService;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.concurrent.Future;

/**
 * @see BaseProcessHandler
 * @see ProcessHandlerBuilder
 */
public interface ProcessHandler extends UserDataHolder, TaskExecutor {
  void startNotify();

  /**
   * @return process handler id
   */
  long getId();

  boolean detachIsDefault();

  boolean waitFor();

  boolean waitFor(long timeoutInMilliseconds);

  void destroyProcess();

  void detachProcess();

  boolean isProcessTerminated();

  boolean isProcessTerminating();

  /**
   * @return exit code if the process has already finished, null otherwise
   */
  @Nullable
  Integer getExitCode();

  void addProcessListener(final ProcessListener listener);

  void removeProcessListener(final ProcessListener listener);

  void notifyTextAvailable(final String text, final Key outputType);

  @Nullable
  abstract OutputStream getProcessInput();

  boolean isStartNotified();

  boolean isSilentlyDestroyOnClose();

  @Nullable
  <F extends ProcessHandlerFeature> F getFeature(@Nonnull Class<F> featureClass);

  @Nullable
  default Charset getCharset() {
    return null;
  }

  @Nullable
  default String getCommandLine() {
    return null;
  }

  @Override
  default Future<?> executeTask(Runnable task) {
    return ProcessIOExecutorService.INSTANCE.submit(task);
  }
}
