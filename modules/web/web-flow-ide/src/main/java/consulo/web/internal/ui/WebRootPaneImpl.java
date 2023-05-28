/*
 * Copyright 2013-2023 consulo.io
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
import consulo.ide.impl.wm.impl.UnifiedStatusBarImpl;
import consulo.ui.MenuBar;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.TargetVaddin;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import consulo.web.internal.ui.vaadin.CompositeComponent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 27/05/2023
 */
public class WebRootPaneImpl extends VaadinComponentDelegate<WebRootPaneImpl.Vaadin> {
  public class Vaadin extends CompositeComponent implements FromVaadinComponentWrapper {
    public Vaadin() {
      setSizeFull();
    }

    @Nullable
    @Override
    public consulo.ui.Component toUIComponent() {
      return WebRootPaneImpl.this;
    }
  }

  private consulo.ui.Component myCenterComponent;

  public WebRootPaneImpl() {
  }

  @Nonnull
  @Override
  public Vaadin createVaadinComponent() {
    return new Vaadin();
  }

  public void setCenterComponent(consulo.ui.Component content) {
    if (myCenterComponent != null) {
      toVaadinComponent().remove(TargetVaddin.to(myCenterComponent));
      myCenterComponent = null;
    }

    if (content != null) {
      Component vaadinComponent = TargetVaddin.to(content);
      toVaadinComponent().add(vaadinComponent);
      myCenterComponent = content;
    }
  }

  public void setMenuBar(MenuBar menuBar) {
  }

  public void setStatusBar(UnifiedStatusBarImpl statusBar) {
    // TODO impl
  }

  @Deprecated
  public consulo.ui.Component getComponent() {
    return this;
  }
}
