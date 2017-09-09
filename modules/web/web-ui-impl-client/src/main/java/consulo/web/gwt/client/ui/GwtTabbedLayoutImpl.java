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

import com.google.gwt.user.client.ui.TabPanel;
import consulo.web.gwt.client.UIConverter;
import consulo.web.gwt.client.WebSocketProxy;
import consulo.web.gwt.shared.UIComponent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
public class GwtTabbedLayoutImpl extends TabPanel implements InternalGwtComponentWithChildren {
  @Override
  public void updateState(@NotNull Map<String, Object> map) {
    selectTab((Integer)map.get("selected"));
  }

  @Override
  public void addChildren(WebSocketProxy proxy, List<UIComponent.Child> children) {
    for (int i = 0; i < children.size(); i ++) {
      final UIComponent.Child tabChild = children.get(i);
      // inc i
      final UIComponent.Child contentChild = children.get(++i);

      final InternalGwtComponent tab = UIConverter.create(proxy, tabChild.getComponent());
      final InternalGwtComponent content = UIConverter.create(proxy, contentChild.getComponent());

      add(content, tab);
    }
  }
}
