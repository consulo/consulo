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
package consulo.ui.ex.awt.valueEditor;

/**
 * @author VISTALL
 * @since 13-Mar-22
 */
public class ValueValidationException extends Exception {
  public ValueValidationException() {
  }

  public ValueValidationException(String message) {
    super(message);
  }

  public ValueValidationException(String message, Throwable cause) {
    super(message, cause);
  }

  public ValueValidationException(Throwable cause) {
    super(cause);
  }

  public ValueValidationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
