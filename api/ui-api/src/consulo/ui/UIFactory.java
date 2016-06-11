/*
 * Copyright 2013-2016 must-be.org
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

import consulo.ui.internal.UIBindingInternal;
import consulo.ui.layout.DockLayout;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 09-Jun-16
 */
public class UIFactory {
  public static class Components {
    @NotNull
    public static CheckBox checkBox(@NotNull String text) {
      return checkBox(text, false);
    }

    @NotNull
    public static CheckBox checkBox(@NotNull String text, boolean selected) {
      return UIBindingInternalHolder.ourInstance._components_checkBox(text, selected);
    }
  }

  public static class Layouts {
    @NotNull
    public static DockLayout dock() {
      return UIBindingInternalHolder.ourInstance._layouts_dock();
    }
  }

  private static class UIBindingInternalHolder {
    public static UIBindingInternal ourInstance = impl();

    private static UIBindingInternal impl() {
      UIBindingInternal bindingInternal = null;

      try {
        Class<?> bindingClass = Class.forName(UIBindingInternal.class.getName() + "Impl");
        bindingInternal = (UIBindingInternal)bindingClass.newInstance();
      }
      catch (Exception e) {
        throw new Error("Fail to init ui binding", e);
      }
      return bindingInternal;
    }
  }
}
