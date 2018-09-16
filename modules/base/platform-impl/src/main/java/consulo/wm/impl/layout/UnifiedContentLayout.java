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
package consulo.wm.impl.layout;

import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.ui.content.ContentManagerEvent;

/**
 * @author VISTALL
 * @since 18-Oct-17
 */
public abstract class UnifiedContentLayout {
  public abstract void init();

  public abstract void reset();

  public abstract String getCloseActionName();

  public abstract String getCloseAllButThisActionName();

  public abstract String getPreviousContentActionName();

  public abstract String getNextContentActionName();

  public void contentAdded(ContentManagerEvent event) {

  }

  public void contentRemoved(ContentManagerEvent event) {

  }

  public void update() {

  }

  public void rebuild() {

  }

  public void showContentPopup(ListPopup popup) {

  }
}
