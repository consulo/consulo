/*
 * Copyright 2013-2017 consulo.io
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
package consulo.web.gwt.client.ui;

import com.vaadin.client.ComponentConnector;
import com.vaadin.client.StyleConstants;
import com.vaadin.client.ui.AbstractComponentContainerConnector;

/**
 * @author VISTALL
 * @since 12-Sep-17
 */
public abstract class GwtLayoutConnector extends AbstractComponentContainerConnector {
  @Override
  protected void updateWidgetStyleNames() {
    super.updateWidgetStyleNames();

    setWidgetStyleName(StyleConstants.UI_WIDGET, false);
  }

  @Override
  protected void updateComponentSize() {
    GwtComponentSizeUpdater.updateForLayout(this);
  }

  @Override
  public void updateCaption(ComponentConnector componentConnector) {
  }
}
