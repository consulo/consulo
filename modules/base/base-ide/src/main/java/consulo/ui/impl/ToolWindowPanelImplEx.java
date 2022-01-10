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
package consulo.ui.impl;

import consulo.annotation.DeprecationInfo;
import consulo.ui.ex.ToolWindowPanel;
import consulo.desktop.util.awt.migration.AWTComponentProvider;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 14-Oct-17
 */
@Deprecated
@DeprecationInfo("class only for migration")
public interface ToolWindowPanelImplEx extends ToolWindowPanel, AWTComponentProvider {
  default boolean isBottomSideToolWindowsVisible() {
    throw new UnsupportedOperationException();
  }

  default int getBottomHeight() {
    throw new UnsupportedOperationException();
  }

  default JComponent getMyLayeredPane() {
    throw new UnsupportedOperationException();
  }
}
