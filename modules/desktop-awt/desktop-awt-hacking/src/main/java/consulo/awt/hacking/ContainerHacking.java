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
package consulo.awt.hacking;

import javax.annotation.Nullable;
import java.awt.*;
import java.lang.reflect.Field;

/**
 * @author VISTALL
 * @since 2019-11-19
 */
public class ContainerHacking {
  @Nullable
  public static FocusTraversalPolicy getFocusTraversalPolicyAwtImpl(final Container component) {
    try {
      Field field = Container.class.getDeclaredField("focusTraversalPolicy");
      field.setAccessible(true);
      return (FocusTraversalPolicy)field.get(component);
    }
    catch (NoSuchFieldException | IllegalAccessException ignored) {
    }
    return null;
  }
}
