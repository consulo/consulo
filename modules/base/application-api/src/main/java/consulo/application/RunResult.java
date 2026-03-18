/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.application;

import consulo.logging.Logger;
import consulo.component.ProcessCanceledException;
import org.jspecify.annotations.Nullable;

public class RunResult<T> extends Result<T> {
  private @Nullable BaseActionRunnable<T> myActionRunnable;

  private @Nullable Throwable myThrowable;

  protected RunResult() {
  }

  public RunResult(BaseActionRunnable<T> action) {
    myActionRunnable = action;
  }

  public RunResult<T> run() {
    if (myActionRunnable == null) {
      throw new IllegalStateException("Can only run once");
    }

    try {
      myActionRunnable.run(this);
    }
    catch (ProcessCanceledException e) {
      throw e; // this exception may occur from time to time and it shouldn't be caught
    }
    catch (Throwable throwable) {
      myThrowable = throwable;
      if (!myActionRunnable.isSilentExecution()) {
        if (throwable instanceof RuntimeException re) {
          throw re;
        }
        else if (throwable instanceof Error error) {
          throw error;
        }
        throw new RuntimeException(myThrowable);
      }
    }
    finally {
      myActionRunnable = null;
    }

    return this;
  }

  public @Nullable T getResultObject() {
    return myResult;
  }

  public RunResult logException(Logger logger) {
    if (hasException()) {
      logger.error(myThrowable);
    }

    return this;
  }

  public RunResult<T> throwException() throws RuntimeException, Error {
    if (hasException()) {
      if (myThrowable instanceof RuntimeException re) {
        throw re;
      }
      else if (myThrowable instanceof Error error) {
        throw error;
      }
      throw new RuntimeException(myThrowable);
    }
    return this;
  }

  public boolean hasException() {
    return myThrowable != null;
  }

  public @Nullable Throwable getThrowable() {
    return myThrowable;
  }

  public void setThrowable(Exception throwable) {
    myThrowable = throwable;
  }
}
