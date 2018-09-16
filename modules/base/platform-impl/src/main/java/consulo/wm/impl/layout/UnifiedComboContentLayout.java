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

import consulo.wm.impl.UnifiedToolWindowContentUI;

/**
 * @author VISTALL
 * @since 18-Oct-17
 */
public class UnifiedComboContentLayout extends UnifiedContentLayout {
  public UnifiedComboContentLayout(UnifiedToolWindowContentUI contentUI) {

  }

  @Override
  public void init() {

  }

  @Override
  public void reset() {

  }

  @Override
  public String getCloseActionName() {
    return "Close View";
  }

  @Override
  public String getCloseAllButThisActionName() {
    return "Close Other Views";
  }

  @Override
  public String getPreviousContentActionName() {
    return "Select Previous View";
  }

  @Override
  public String getNextContentActionName() {
    return "Select Next View";
  }
}
