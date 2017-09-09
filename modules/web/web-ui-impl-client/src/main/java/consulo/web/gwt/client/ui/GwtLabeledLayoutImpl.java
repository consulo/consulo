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
package consulo.web.gwt.client.ui;

import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import consulo.web.gwt.client.UIConverter;
import consulo.web.gwt.client.WebSocketProxy;
import consulo.web.gwt.shared.UIComponent;
import consulo.web.gwt.shared.state.UIComponentState;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 15-Jun-16
 */
public class GwtLabeledLayoutImpl extends CaptionPanel implements InternalGwtComponentWithChildren<UIComponentState> {
  public GwtLabeledLayoutImpl() {
    final SimplePanel widget = (SimplePanel)getWidget();

    widget.addStyleName("gwtLabeledLayout");
  }

  @Override
  public void addChildren(WebSocketProxy proxy, List<UIComponent.Child> children) {
    if(!children.isEmpty()) {
      final UIComponent.Child child = children.get(0);

      final InternalGwtComponent childComponent = UIConverter.create(proxy, child.getComponent());
      add(childComponent);
    }
  }

  @Override
  public void updateState(@NotNull Map<String, Object> map) {
    setCaptionText((String)map.get("text"));
  }
}
