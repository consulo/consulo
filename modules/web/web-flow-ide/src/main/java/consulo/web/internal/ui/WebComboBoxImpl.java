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
package consulo.web.internal.ui;

import com.vaadin.flow.data.renderer.ComponentRenderer;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.model.ListModel;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.vaadin.WebSingleListComponentBase;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2019-02-19
 */
@SuppressWarnings("unchecked")
public class WebComboBoxImpl<V> extends WebSingleListComponentBase<V, WebComboBoxImpl.Vaadin> implements ComboBox<V> {
  public class Vaadin extends com.vaadin.flow.component.combobox.ComboBox<V> implements FromVaadinComponentWrapper {
    @Nullable
    @Override
    public Component toUIComponent() {
      return WebComboBoxImpl.this;
    }
  }

  public WebComboBoxImpl(ListModel<V> model) {
    super(model);

    toVaadinComponent().setRenderer(new ComponentRenderer((c) -> {
      WebItemPresentationImpl presentation = new WebItemPresentationImpl();
      myRender.render(presentation, myModel.indexOf((V)c), (V)c);
      return presentation.toComponent();
    }));
  }

  @Nonnull
  @Override
  public Vaadin createVaadinComponent() {
    return new Vaadin();
  }
}
