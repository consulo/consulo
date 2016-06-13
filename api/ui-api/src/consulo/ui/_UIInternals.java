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

import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.SplitLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.model.ListModel;
import org.jetbrains.annotations.NotNull;

import java.net.URL;

/**
 * @author VISTALL
 * @since 09-Jun-16
 */
abstract class _UIInternals {
  public static _UIInternals impl() {
    return Holder.ourInstance;
  }

  private static class Holder {
    public static _UIInternals ourInstance = impl();

    private static _UIInternals impl() {
      _UIInternals bindingInternal = null;

      try {
        Class<?> bindingClass = Class.forName(_UIInternals.class.getName() + "Impl");
        bindingInternal = (_UIInternals)bindingClass.newInstance();
      }
      catch (Exception e) {
        throw new Error("Fail to init ui binding", e);
      }
      return bindingInternal;
    }
  }

  protected abstract CheckBox _Components_checkBox(@NotNull String text, boolean selected);

  protected abstract DockLayout _Layouts_dock();

  protected abstract VerticalLayout _Layouts_vertical();

  protected abstract SplitLayout _Layouts_horizontalSplit();

  protected abstract SplitLayout _Layouts_verticalSplit();

  protected abstract Label _Components_label(String text);

  protected abstract HtmlLabel _Components_htmlLabel(String html);

  protected abstract <E> ComboBox<E> _Components_comboBox(ListModel<E> model);

  protected abstract HorizontalLayout _Layouts_horizontal();

  protected abstract Image _Components_image(ImageRef imageRef);

  protected abstract ImageRef _imageRef(URL url);

  @RequiredUIThread
  @NotNull
  protected abstract UIAccess _get();

  protected abstract boolean _isUIThread();
}
