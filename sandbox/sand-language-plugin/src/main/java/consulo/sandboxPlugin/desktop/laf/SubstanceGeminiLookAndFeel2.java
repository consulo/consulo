/*
 * Copyright 2013-2019 consulo.io
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
package consulo.sandboxPlugin.desktop.laf;

import consulo.actionSystem.ex.ComboBoxButtonImpl;
import consulo.desktop.util.awt.laf.BuildInLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceGeminiLookAndFeel;
import org.pushingpixels.substance.internal.animation.StateTransitionTracker;
import org.pushingpixels.substance.internal.animation.TransitionAwareUI;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.plaf.ComboBoxUI;
import java.awt.event.MouseEvent;

/**
 * @author VISTALL
 * @since 2019-11-04
 */
public class SubstanceGeminiLookAndFeel2 extends SubstanceGeminiLookAndFeel implements BuildInLookAndFeel {
  private static class SubstanceComboBoxButtonUI extends ComboBoxButtonImpl.HackyComboBoxUI implements TransitionAwareUI {

    public SubstanceComboBoxButtonUI(ComboBoxUI ui, ComboBoxButtonImpl button) {
      super(ui, button);
    }

    @Override
    public boolean isInside(MouseEvent me) {
      return ((TransitionAwareUI)myDelegateUI).isInside(me);
    }

    @Override
    public StateTransitionTracker getTransitionTracker() {
      return ((TransitionAwareUI)myDelegateUI).getTransitionTracker();
    }
  }

  private static class SubstanceComboBoxUIFactory implements ComboBoxButtonImpl.ComboBoxUIFactory {
    private static final SubstanceComboBoxUIFactory INSTANCE = new SubstanceComboBoxUIFactory();

    @Nonnull
    @Override
    public ComboBoxButtonImpl.HackyComboBoxUI create(@Nonnull ComboBoxUI delegate, @Nonnull ComboBoxButtonImpl comboBoxButton) {
      return new SubstanceComboBoxButtonUI(delegate, comboBoxButton);
    }
  }

  @Override
  protected void initComponentDefaults(UIDefaults table) {
    super.initComponentDefaults(table);
    table.put(ComboBoxButtonImpl.ComboBoxUIFactory.class, SubstanceComboBoxUIFactory.INSTANCE);
  }

  @Override
  public boolean isDark() {
    return true;
  }
}
