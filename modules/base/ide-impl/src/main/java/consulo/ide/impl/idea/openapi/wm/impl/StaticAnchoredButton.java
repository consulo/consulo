/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.wm.impl;

import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.event.MouseEvent;

/**
 * Created by IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 8/19/11
 * Time: 3:52 PM
 */
public class StaticAnchoredButton extends AnchoredButton {
  @Nonnull
  private ToolWindowAnchor myToolWindowAnchor;
  private int myMnemonic2;

  public StaticAnchoredButton(String text,
                              Icon icon,
                              boolean selected,
                              @Nonnull ToolWindowAnchor toolWindowAnchor) {
    super(text, icon, selected);
    myToolWindowAnchor = toolWindowAnchor;
    init();
  }

  public StaticAnchoredButton(String text, Icon icon, @Nonnull ToolWindowAnchor toolWindowAnchor) {
    super(text, icon);
    myToolWindowAnchor = toolWindowAnchor;
    init();
  }

  public StaticAnchoredButton(Action a, @Nonnull ToolWindowAnchor toolWindowAnchor) {
    super(a);
    myToolWindowAnchor = toolWindowAnchor;
    init();
  }

  public StaticAnchoredButton(String text, boolean selected, @Nonnull ToolWindowAnchor toolWindowAnchor) {
    super(text, selected);
    myToolWindowAnchor = toolWindowAnchor;
    init();
  }

  public StaticAnchoredButton(String text, @Nonnull ToolWindowAnchor toolWindowAnchor) {
    super(text);
    myToolWindowAnchor = toolWindowAnchor;
    init();
  }

  public StaticAnchoredButton(Icon icon, boolean selected, @Nonnull ToolWindowAnchor toolWindowAnchor) {
    super(icon, selected);
    myToolWindowAnchor = toolWindowAnchor;
    init();
  }

  public StaticAnchoredButton(Icon icon, @Nonnull ToolWindowAnchor toolWindowAnchor) {
    super(icon);
    myToolWindowAnchor = toolWindowAnchor;
    init();
  }

  public StaticAnchoredButton(@Nonnull ToolWindowAnchor toolWindowAnchor) {
    myToolWindowAnchor = toolWindowAnchor;
    init();
  }

  private void init() {
    setFocusable(false);
//    setBackground(ourBackgroundColor);
    final Border border = BorderFactory.createEmptyBorder(5, 5, 0, 5);
    setBorder(border);
    setRolloverEnabled(true);
    setOpaque(false);
    enableEvents(MouseEvent.MOUSE_EVENT_MASK);
  }

  @Override
  public int getMnemonic2() {
    return myMnemonic2;
  }

  @Override
  public ToolWindowAnchor getAnchor() {
    return myToolWindowAnchor;
  }

  public void setToolWindowAnchor(@Nonnull ToolWindowAnchor toolWindowAnchor) {
    myToolWindowAnchor = toolWindowAnchor;
  }

  public void setMnemonic2(int mnemonic2) {
    myMnemonic2 = mnemonic2;
  }
}
