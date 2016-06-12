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
package consulo.ui.internal;

import com.google.web.bindery.autobean.shared.AutoBean;
import consulo.ui.*;
import consulo.web.gwtUI.shared.UIComponent;
import consulo.web.gwtUI.shared.UIEventFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 12-Jun-16
 */
public class WGwtComboBoxImpl<E> extends WBaseGwtComponent implements ComboBox<E> {
  private ListItemRender<E> myRender = ListItemRenders.defaultRender();
  private ListModel<E> myModel;

  public WGwtComboBoxImpl(ListModel<E> model) {
    myModel = model;
  }

  @Override
  protected void getState(Map<String, String> map) {
    super.getState(map);
    map.put("size", String.valueOf(myModel.getSize()));
  }

  @NotNull
  @Override
  public ListModel<E> getModel() {
    return myModel;
  }

  @Override
  public void setRender(@NotNull ListItemRender<E> render) {
    myRender = render;
  }

  @Override
  protected void initChildren(UIEventFactory factory, List<UIComponent.Child> children) {
    int i = 0;

    for (E e : myModel) {
      final AutoBean<UIComponent> bean = factory.component();
      final UIComponent component = bean.as();
      component.setId(String.valueOf(i));

      WGwtListItemPresentationImpl render = new WGwtListItemPresentationImpl();
      myRender.render(render, e);

      i++;
    }
  }
}
