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
package consulo.web.gwtUI.client.util;

import com.google.gwt.user.client.ui.*;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class GwtUIUtil2 {
  public static Widget loadingPanel() {
    VerticalPanel verticalPanel = fillAndReturn(new VerticalPanel());
    verticalPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    verticalPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);

    verticalPanel.add(new Label("Loading..."));

    return verticalPanel;
  }

  public static <T extends UIObject> T fillAndReturn(T object) {
    object.setWidth("100%");
    object.setHeight("100%");
    return object;
  }

  public static void fill(UIObject object) {
    object.setWidth("100%");
    object.setHeight("100%");
  }
}
