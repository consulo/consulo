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
package consulo.util.nodep.xml;

import java.io.IOException;

/**
 * @author VISTALL
 * @since 2019-07-17
 */
public class SimpleXmlParsingException extends IOException {
  public SimpleXmlParsingException() {
  }

  public SimpleXmlParsingException(String message) {
    super(message);
  }

  public SimpleXmlParsingException(String message, Throwable cause) {
    super(message, cause);
  }

  public SimpleXmlParsingException(Throwable cause) {
    super(cause);
  }
}
