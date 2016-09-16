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
package consulo.ui.internal;

import com.intellij.ui.IdeBorderFactory;
import consulo.ui.Component;
import consulo.ui.LabeledLayout;
import consulo.ui.shared.Size;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 15-Jun-16
 */
public class DesktopLabeledLayoutImpl extends JPanel implements LabeledLayout {
  public DesktopLabeledLayoutImpl(String text) {
    super(new BorderLayout());

    setBorder(IdeBorderFactory.createTitledBorder(text));
  }

  @Override
  public void setSize(@NotNull Size size) {
    setSize(new Dimension(size.getWidth(), size.getHeight()));
  }

  @NotNull
  @Override
  public LabeledLayout set(@NotNull Component component) {
    add((java.awt.Component)component, BorderLayout.CENTER);
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
