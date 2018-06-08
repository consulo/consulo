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
package consulo.ui.awt;

import consulo.awt.TargetAWT;
import consulo.ui.KeyCode;
import org.junit.Assert;
import org.junit.Test;

import java.awt.event.KeyEvent;

/**
 * @author VISTALL
 * @since 2018-05-14
 */
public class TargetAWTTest extends Assert {
  @Test
  public void testKeyCode() {
    assertEquals(KeyEvent.VK_V, TargetAWT.to(KeyCode.V));
    assertEquals(KeyEvent.VK_A, TargetAWT.to(KeyCode.A));
    assertEquals(KeyEvent.VK_Z, TargetAWT.to(KeyCode.Z));
  }
}
