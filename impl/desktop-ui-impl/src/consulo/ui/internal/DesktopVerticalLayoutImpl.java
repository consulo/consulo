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

import com.intellij.openapi.ui.VerticalFlowLayout;
import consulo.ui.Component;
import consulo.ui.VerticalLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class DesktopVerticalLayoutImpl extends JPanel implements VerticalLayout {
  public DesktopVerticalLayoutImpl() {
    super(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 0, true, false));
  }

  @NotNull
  @Override
  public VerticalLayout add(@NotNull Component component) {
    add((java.awt.Component)component);
    return this;
  }

  @Nullable
  @Override
  public Component getParentComponent() {
    return (Component)getParent();
  }

  @Override
  public void dispose() {

  }
}
