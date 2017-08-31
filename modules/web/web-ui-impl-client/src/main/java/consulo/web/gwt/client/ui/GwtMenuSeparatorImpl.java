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

import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Map;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
public class GwtMenuSeparatorImpl extends UIObject implements InternalGwtComponent {
  @Override
  public void updateState(@NotNull Map<String, Serializable> map) {

  }

  @Override
  public void setWidth(String width) {
    // can't change
  }

  @Override
  public void setHeight(String height) {
    // can't change
  }

  @Override
  public void setVisible(boolean visible) {
    // can't change
  }

  @Override
  public boolean isVisible() {
    return true;
  }

  @Override
  public Widget asWidget() {
    return null;
  }
}
