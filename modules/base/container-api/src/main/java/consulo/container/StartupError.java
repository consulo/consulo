/*
 * Copyright 2013-2019 consulo.io
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
package consulo.container;

/**
 * @author VISTALL
 * @since 2019-07-25
 */
public class StartupError extends Error {
  public static boolean hasStartupError = false;

  public StartupError() {
    hasStartupError = true;
  }

  public StartupError(String message) {
    super(message);
    hasStartupError = true;
  }

  public StartupError(String message, Throwable cause) {
    super(message, cause);
    hasStartupError = true;
  }

  public StartupError(Throwable cause) {
    super(cause);
    hasStartupError = true;
  }

  public StartupError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
    hasStartupError = true;
  }
}
