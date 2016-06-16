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
package consulo.ui.internal;

import com.intellij.openapi.ui.Splitter;
import consulo.ui.Component;
import consulo.ui.shared.Size;
import consulo.ui.SplitLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

/**
 * @author VISTALL
 * @since 13-Jun-16
 */
public class DesktopSplitLayoutImpl extends Splitter implements SplitLayout {
  @Override
  public void setProportion(int percent) {
    setProportion(percent / 100f);
  }

  @Override
  public void setSize(@NotNull Size size) {
    setSize(new Dimension(size.getWidth(), size.getHeight()));
  }

  @Override
  public void setFirstComponent(@NotNull Component component) {
    setFirstComponent((javax.swing.JComponent)component);
  }

  @Override
  public void setSecondComponent(@NotNull Component component) {
    setSecondComponent((javax.swing.JComponent)component);
  }

  @Nullable
  @Override
  public Component getParentComponent() {
    return (Component)getParent();
  }
}
