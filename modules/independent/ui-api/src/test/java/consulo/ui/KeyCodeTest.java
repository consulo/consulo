/*
 * Copyright 2013-2018 consulo.io
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
package consulo.ui;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author VISTALL
 * @since 2018-05-14
 */
public class KeyCodeTest extends Assert {
  @Test
  public void testFromChar() {
    for (int i = 'a'; i <= 'z'; i++) {
      char c = (char)i;

      KeyCode fromName = KeyCode.valueOf(String.valueOf(Character.toUpperCase(c)));
      KeyCode fromCha = KeyCode.from(c);

      assertEquals(fromName, fromCha);
    }

    for (int i = 'A'; i <= 'Z'; i++) {
      char c = (char)i;

      KeyCode fromName = KeyCode.valueOf(String.valueOf(c));
      KeyCode fromCha = KeyCode.from(c);

      assertEquals(fromName, fromCha);
    }
  }
}
