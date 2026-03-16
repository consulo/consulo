/*
 * Copyright 2013-2023 consulo.io
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
package consulo.test.light.impl;

import consulo.application.progress.*;
import consulo.application.util.function.ThrowableComputable;
import consulo.component.ComponentManager;
import consulo.component.ProcessCanceledException;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2023-11-05
 */
public class LightProgressManager extends ProgressManager {
  @Override
  public boolean hasProgressIndicator() {
    return false;
  }

  @Override
  public boolean hasModalProgressIndicator() {
    return false;
  }

  @Override
  public boolean hasUnsafeProgressIndicator() {
    return false;
  }

  @Override
  public void runProcess(Runnable process, @Nullable ProgressIndicator progress) throws ProcessCanceledException {
    process.run();
  }

  @Override
  public <T> T runProcess(Supplier<T> process, @Nullable ProgressIndicator progress) throws ProcessCanceledException {
    return process.get();
  }

  @Override
  public ProgressIndicator getProgressIndicator() {
    return null;
  }

  @Override
  protected void doCheckCanceled() throws ProcessCanceledException {

  }

  @Override
  public void executeNonCancelableSection(Runnable runnable) {
    runnable.run();
  }

  @Override
  public boolean runProcessWithProgressSynchronously(Runnable process,
                                                     String progressTitle,
                                                     boolean canBeCanceled,
                                                     @Nullable ComponentManager project) {
    process.run();
    return true;
  }

  @Override
  public <T, E extends Exception> T runProcessWithProgressSynchronously(ThrowableComputable<T, E> process,
                                                                        String progressTitle,
                                                                        boolean canBeCanceled,
                                                                        @Nullable ComponentManager project) throws E {
    return process.get();
  }

  @Override
  public void runProcessWithProgressAsynchronously(ComponentManager project,
                                                   String progressTitle,
                                                   Runnable process,
                                                   @Nullable Runnable successRunnable,
                                                   @Nullable Runnable canceledRunnable) {
    process.run();
  }

  @Override
  public void runProcessWithProgressAsynchronously(ComponentManager project,
                                                   String progressTitle,
                                                   Runnable process,
                                                   @Nullable Runnable successRunnable,
                                                   @Nullable Runnable canceledRunnable,
                                                   PerformInBackgroundOption option) {
    process.run();
  }

  @Override
  public void run(Task task) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void runProcessWithProgressAsynchronously(Task.Backgroundable task, ProgressIndicator progressIndicator) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void executeProcessUnderProgress(Runnable process, @Nullable ProgressIndicator progress) throws ProcessCanceledException {
    process.run();
  }

  @Override
  public boolean isInNonCancelableSection() {
    return false;
  }

  @Override
  public <T, E extends Throwable> T computePrioritized(ThrowableComputable<T, E> computable) throws E {
    return computable.get();
  }

  @Override
  public WrappedProgressIndicator wrapProgressIndicator(@Nullable ProgressIndicator indicator) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ProgressIndicator unwrapProgressIndicator(WrappedProgressIndicator indicator) {
    throw new UnsupportedOperationException();
  }
}
