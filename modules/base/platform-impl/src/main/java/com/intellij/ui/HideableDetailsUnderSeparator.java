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
package com.intellij.ui;

import consulo.awt.TargetAWT;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 8/22/11
 * Time: 2:54 PM
 */
public abstract class HideableDetailsUnderSeparator extends AbstractTitledSeparatorWithIcon {
  public HideableDetailsUnderSeparator(@Nonnull Image icon,
                                       @Nonnull Image iconOpen,
                                       @Nonnull String text) {
    super(icon, iconOpen, text);
  }

  public void on() {
    initDetails();
    myLabel.setIcon(TargetAWT.to(myIconOpen));
    myWrapper.add(myDetailsComponent.getPanel(), BorderLayout.CENTER);
    myOn = true;
    revalidate();
    repaint();
  }

  public void off() {
    myLabel.setIcon(TargetAWT.to(myIcon));
    myWrapper.removeAll();
    myOn = false;
    revalidate();
    repaint();
  }
}
