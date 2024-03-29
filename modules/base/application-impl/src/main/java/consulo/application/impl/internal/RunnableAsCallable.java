/*
 * Copyright 2013-2020 consulo.io
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
package consulo.application.impl.internal;

import java.util.concurrent.Callable;

/**
 * @author VISTALL
 * @since 2020-05-24
 */
public class RunnableAsCallable implements Callable<Void> {
  private final Runnable myRunnable;

  RunnableAsCallable(Runnable runnable) {
    myRunnable = runnable;
  }

  public Runnable getRunnable() {
    return myRunnable;
  }

  @Override
  public Void call() throws Exception {
    myRunnable.run();
    return null;
  }

  @Override
  public String toString() {
    return myRunnable.toString();
  }
}
