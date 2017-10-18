/*
 * Copyright 2013-2016 consulo.io
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

import consulo.annotations.Internal;
import consulo.ui.image.FoldedImage;
import consulo.ui.image.Image;
import consulo.ui.model.ListModel;
import consulo.ui.style.StyleManager;
import consulo.util.ServiceLoaderUtil;
import org.jetbrains.annotations.NotNull;

import java.net.URL;

/**
 * @author VISTALL
 * @since 09-Jun-16
 */
@Internal
public abstract class UIInternal {
  private static UIInternal ourInstance = ServiceLoaderUtil.loadSingleOrError(UIInternal.class);

  @NotNull
  public static UIInternal get() {
    return ourInstance;
  }

  abstract CheckBox _Components_checkBox(@NotNull String text, boolean selected);

  abstract DockLayout _Layouts_dock();

  abstract VerticalLayout _Layouts_vertical();

  abstract SplitLayout _Layouts_horizontalSplit();

  abstract SplitLayout _Layouts_verticalSplit();

  abstract TabbedLayout _Layouts_tabbed();

  abstract LabeledLayout _Layouts_labeled(String label);

  abstract TableLayout _Layouts_table(int rows, int columns);

  abstract HorizontalLayout _Layouts_horizontal();

  abstract Label _Components_label(String text);

  abstract HtmlLabel _Components_htmlLabel(String html);

  abstract <E> ComboBox<E> _Components_comboBox(ListModel<E> model);

  abstract TextBox _Components_textBox(String text);

  abstract <E> ListBox<E> _Components_listBox(ListModel<E> model);

  abstract RadioButton _Components_radioButton(String text, boolean selected);

  abstract Button _Components_button(String text);

  abstract ImageBox _Components_imageBox(Image image);

  abstract <E> Tree<E> _Components_tree(E rootValue, TreeModel<E> model);

  public abstract Image _Images_image(URL url);

  public abstract FoldedImage _Images_foldedImage(Image[] images);

  abstract MenuItem _MenuItems_item(String text);

  abstract Menu _MenuItems_menu(String text);

  abstract ValueGroup<Boolean> _ValueGroups_boolGroup();

  abstract MenuBar _MenuItems_menuBar();

  @NotNull
  public abstract StyleManager _StyleManager_get();

  @NotNull
  public abstract Window _Windows_modalWindow(String title);

  public abstract AlertBuilder _Alerts_builder();

  @RequiredUIAccess
  @NotNull
  abstract UIAccess _UIAccess_get();

  abstract boolean _UIAccess_isUIThread();
}
