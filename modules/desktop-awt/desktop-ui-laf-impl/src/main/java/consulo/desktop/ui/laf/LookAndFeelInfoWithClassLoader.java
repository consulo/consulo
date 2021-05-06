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
package consulo.desktop.ui.laf;

import javax.annotation.Nonnull;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 2019-07-27
 */
public abstract class LookAndFeelInfoWithClassLoader extends UIManager.LookAndFeelInfo {
  @Nonnull
  public static LookAndFeelInfoWithClassLoader simple(String name, Class<?> lafClass) {
    return new LookAndFeelInfoWithClassLoader(name, lafClass.getName()) {
      @Nonnull
      @Override
      public ClassLoader getClassLoader() {
        return lafClass.getClassLoader();
      }
    };
  }

  public LookAndFeelInfoWithClassLoader(String name, String className) {
    super(name, className);
  }

  @Nonnull
  public ClassLoader getClassLoader() {
    return getClass().getClassLoader();
  }
}
