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

package consulo.ide.impl.idea.ide.ui;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.disposer.Disposable;
import consulo.ide.ServiceManager;
import consulo.ui.style.Style;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author anna
 * @since 2006-05-17
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface LafManager {
  @Nonnull
  public static LafManager getInstance() {
    return ServiceManager.getService(LafManager.class);
  }

  @Nonnull
  List<Style> getStyles();

  void setCurrentStyle(@Nonnull Style style);

  @Nonnull
  Style getCurrentStyle();

  void updateUI();

  void repaintUI();

  void addLafManagerListener(LafManagerListener l);

  void addLafManagerListener(LafManagerListener l, Disposable disposable);

  void removeLafManagerListener(LafManagerListener l);
}
