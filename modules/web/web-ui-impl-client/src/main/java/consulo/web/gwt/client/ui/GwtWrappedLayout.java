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

import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import consulo.web.gwt.client.util.GwtUIUtil;
import javax.annotation.Nonnull;

import java.util.List;

/**
 * @author VISTALL
 * @since 26-Oct-17
 */
public class GwtWrappedLayout extends SimplePanel {
  public void build(@Nonnull List<Widget> widgets) {
    GwtUIUtil.fill(this);
    
    if(widgets.isEmpty()) {
      setWidget(null);
    }
    else {
      Widget widget = widgets.get(0);
      GwtUIUtil.fill(widget);
      setWidget(widget);
    }
  }
}
