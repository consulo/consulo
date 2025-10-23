/*
 * Copyright 2013-2022 consulo.io
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
package consulo.util.lang;

import org.slf4j.Logger;

/**
 * Slf4J logger util, which emulate for Logger from Consulo API
 * @author VISTALL
 * @since 2022-02-06
 */
public class LoggerAssert {
  public static void assertTrue(Logger logger, boolean value) {
    assertTrue(logger, value, null);
  }

  public static void assertTrue(Logger logger, boolean value, String message) {
    if (!value) {
      String resultMessage = "Assertion failed";
      if (message != null) resultMessage += ": " + message;
      logger.error(resultMessage, new Throwable());
    }
  }
}
