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

import com.intellij.openapi.util.IconLoader;
import consulo.ui.internal.*;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.SplitLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.model.ListModel;
import consulo.web.servlet.ui.UIAccessHelper;
import org.jetbrains.annotations.NotNull;

import java.net.URL;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
class _UIInternalsImpl extends _UIInternals {
  static {
    IconLoader.activate(); // TODO [VISTALL] hack until we not start Consulo app
  }

  @Override
  public CheckBox _Components_checkBox(@NotNull String text, boolean selected) {
    return new WGwtCheckBoxImpl(selected, text);
  }

  @Override
  public DockLayout _Layouts_dock() {
    return new WGwtDockLayoutImpl();
  }

  @Override
  protected VerticalLayout _Layouts_vertical() {
    return new WGwtVerticalLayoutImpl();
  }

  @Override
  protected SplitLayout _Layouts_horizontalSplit() {
    return new WGwtHorizontalSplitLayoutImpl();
  }

  @Override
  protected SplitLayout _Layouts_verticalSplit() {
    return new WGwtVerticalSplitLayoutImpl();
  }

  @Override
  protected Label _Components_label(String text) {
    return new WGwtLabelImpl(text);
  }

  @Override
  protected HtmlLabel _Components_htmlLabel(String html) {
    return new WGwtHtmlLabelImpl(html);
  }

  @Override
  protected <E> ComboBox<E> _Components_comboBox(ListModel<E> model) {
    return new WGwtComboBoxImpl<E>(model);
  }

  @Override
  protected HorizontalLayout _Layouts_horizontal() {
    return new WGwtHorizontalLayoutImpl();
  }

  @Override
  protected Image _Components_image(ImageRef imageRef) {
    return new WGwtImageImpl((WGwtImageRefImpl)imageRef);
  }

  @Override
  protected ImageRef _imageRef(URL url) {
    return new WGwtImageRefImpl(url);
  }

  @NotNull
  @Override
  protected UIAccess _get() {
    return UIAccessHelper.ourInstance.get();
  }

  @Override
  protected boolean _isUIThread() {
    return UIAccessHelper.ourInstance.isUIThread();
  }
}
