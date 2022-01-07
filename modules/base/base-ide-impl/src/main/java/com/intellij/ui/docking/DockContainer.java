/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.ui.docking;

import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.awt.RelativeRectangle;
import com.intellij.util.ui.update.Activatable;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

public interface DockContainer extends Disposable, Activatable {
  enum ContentResponse {
    ACCEPT_MOVE,
    ACCEPT_COPY,
    DENY;

    public boolean canAccept() {
      return this != DENY;
    }
  }

  RelativeRectangle getAcceptArea();

  /**
   * This area is used when nothing was found with getAcceptArea
   */
  RelativeRectangle getAcceptAreaFallback();

  @Nonnull
  ContentResponse getContentResponse(@Nonnull DockableContent content, RelativePoint point);

  default JComponent getContainerComponent() {
    throw new AbstractMethodError();
  }

  @Nonnull
  default Component getUIContainerComponent() {
    throw new AbstractMethodError();
  }

  void add(@Nonnull DockableContent content, RelativePoint dropTarget);

  void closeAll();

  void addListener(Listener listener, Disposable parent);

  boolean isEmpty();

  @Nullable
  Image startDropOver(@Nonnull DockableContent content, RelativePoint point);

  @Nullable
  Image processDropOver(@Nonnull DockableContent content, RelativePoint point);

  void resetDropOver(@Nonnull DockableContent content);


  boolean isDisposeWhenEmpty();

  interface Dialog extends DockContainer {
  }

  interface Persistent extends DockContainer {
    String getDockContainerType();

    Element getState();

  }

  interface Listener {
    void contentAdded(Object key);

    void contentRemoved(Object key);

    class Adapter implements Listener {
      @Override
      public void contentAdded(Object key) {
      }

      @Override
      public void contentRemoved(Object key) {
      }
    }
  }
}
