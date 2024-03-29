/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.language.codeStyle.ui.internal.arrangement;

import consulo.application.ApplicationBundle;
import consulo.ui.ex.awt.GridBag;
import consulo.ui.ex.awt.UIUtil;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;

/**
 * @author Denis Zhdanov
 * @since 11/13/12 1:31 PM
 */
public class EmptyArrangementRuleComponent extends JPanel implements ArrangementRepresentationAware {
  
  private final int myHeight;
  
  public EmptyArrangementRuleComponent(int height) {
    super(new GridBagLayout());
    myHeight = height;
    add(new JLabel(ApplicationBundle.message("arrangement.text.empty.rule")), new GridBag().anchor(GridBagConstraints.WEST));
    setBackground(UIUtil.getDecoratedRowColor());
  }

  @Nonnull
  @Override
  public JComponent getComponent() {
    return this;
  }

  @Override
  public Dimension getPreferredSize() {
    Dimension size = super.getPreferredSize();
    return new Dimension(size.width, myHeight);
  }
}
