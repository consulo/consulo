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
package consulo.application.impl.internal.start;

import consulo.container.ExitCodes;

/**
 * @author VISTALL
 * @since 22-Mar-22
 */
public class StartupAbortedException extends RuntimeException {
  private int exitCode = ExitCodes.STARTUP_EXCEPTION;
  private boolean logError = true;

  public StartupAbortedException(Throwable cause) {
    super(cause);
  }

  public StartupAbortedException(String message, Throwable cause) {
    super(message, cause);
  }

  public int exitCode() {
    return exitCode;
  }

  public StartupAbortedException exitCode(int exitCode) {
    this.exitCode = exitCode;
    return this;
  }

  public boolean logError() {
    return logError;
  }

  public StartupAbortedException logError(boolean logError) {
    this.logError = logError;
    return this;
  }
}
