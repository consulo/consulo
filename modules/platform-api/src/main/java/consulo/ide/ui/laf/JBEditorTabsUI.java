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
package consulo.ide.ui.laf;

import com.intellij.ui.tabs.impl.JBTabsImpl;

import javax.swing.plaf.ComponentUI;
import java.awt.*;

/**
 * @author VISTALL
 * @since 15-Jun-17
 */
public abstract class JBEditorTabsUI extends ComponentUI {
  public abstract void clearLastPaintedTab();

  public abstract void setModifyTabColor(Color color);

  public abstract void paintChildren(JBTabsImpl tabs, Graphics g);
}
