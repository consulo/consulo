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

package com.intellij.ide.ui;

import com.intellij.openapi.components.ServiceManager;
import consulo.disposer.Disposable;
import consulo.ui.style.Style;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.List;

/**
 * User: anna
 * Date: 17-May-2006
 */
public abstract class LafManager {
  @Nonnull
  public static LafManager getInstance(){
    return ServiceManager.getService(LafManager.class);
  }

  @Nonnull
  public abstract List<Style> getStyles();

  public abstract void setCurrentStyle(@Nonnull Style style);

  @Nonnull
  public abstract Style getCurrentStyle();

  @Nonnull
  public abstract UIManager.LookAndFeelInfo[] getInstalledLookAndFeels();

  @Nonnull
  public abstract UIManager.LookAndFeelInfo getCurrentLookAndFeel();

  public abstract void setCurrentLookAndFeel(@Nonnull UIManager.LookAndFeelInfo lookAndFeelInfo);

  public abstract void updateUI();

  public abstract void repaintUI();

  public abstract void addLafManagerListener(LafManagerListener l);

  public abstract void addLafManagerListener(LafManagerListener l, Disposable disposable);

  public abstract void removeLafManagerListener(LafManagerListener l);
}
