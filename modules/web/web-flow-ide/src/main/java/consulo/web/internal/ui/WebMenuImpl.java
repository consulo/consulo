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

import com.vaadin.flow.component.Component;
import consulo.ui.Menu;
import consulo.ui.MenuItem;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.TargetVaddin;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import consulo.web.internal.ui.vaadin.SimpleComponent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2019-02-19
 */
public class WebMenuImpl extends VaadinComponentDelegate<WebMenuImpl.Vaadin> implements Menu {
  public class Vaadin extends SimpleComponent implements FromVaadinComponentWrapper {
    private String myText = "";
    private List<Component> myMenuItems = new ArrayList<>();

    public void add(@Nonnull MenuItem menuItem) {
      Component vaadinComponent = TargetVaddin.to(menuItem);

      myMenuItems.add(vaadinComponent);
      //addComponent(vaadinComponent);
    }


    @Nullable
    @Override
    public consulo.ui.Component toUIComponent() {
      return WebMenuImpl.this;
    }
  }

  public WebMenuImpl(String text) {
    getVaadinComponent().myText = text;
  }

  @Override
  @Nonnull
  public Vaadin createVaadinComponent() {
    return new Vaadin();
  }

  @Override
  public void setIcon(@Nullable Image icon) {
    Vaadin vaadinComponent = getVaadinComponent();
    //vaadinComponent.getState().myImageState = icon == null ? null : WebImageMapper.map(icon).getState();
    // vaadinComponent.markAsDirty();
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public Menu add(@Nonnull MenuItem menuItem) {
    getVaadinComponent().add(menuItem);
    return this;
  }

  @Nonnull
  @Override
  public String getText() {
    return getVaadinComponent().myText;
  }
}
