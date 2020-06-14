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
package consulo.logging.internal;

import consulo.logging.Logger;

import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2019-08-10
 */
public class DefaultLogger implements Logger {
  public DefaultLogger(String category) {
  }

  @Override
  public boolean isDebugEnabled() {
    return false;
  }

  @Override
  public void debug(String message) {
  }

  @Override
  public void debug(Throwable t) {
  }

  @Override
  public void debug(String message, Throwable t) {
  }

  @Override
  @SuppressWarnings({"UseOfSystemOutOrSystemErr", "CallToPrintStackTrace"})
  public void error(String message, @Nullable Throwable t, String... details) {
    System.err.println("ERROR: " + message);
    if (t != null) t.printStackTrace();
    if (details != null && details.length > 0) {
      System.out.println("details: ");
      for (String detail : details) {
        System.out.println(detail);
      }
    }

    throw new AssertionError(message);
  }

  @Override
  public void info(String message) {
  }

  @Override
  public void info(String message, Throwable t) {
  }

  @Override
  public void warn(String message, Throwable t) {
  }
}
