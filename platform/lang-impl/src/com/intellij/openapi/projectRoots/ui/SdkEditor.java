/*
 * Copyright 2013-2014 must-be.org
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
package com.intellij.openapi.projectRoots.ui;

import com.intellij.openapi.projectRoots.SdkModel;
import com.intellij.openapi.projectRoots.impl.SdkImpl;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.ui.OrderRootTypeUIFactory;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.ui.TabbedPaneWrapper;
import com.intellij.ui.navigation.History;
import com.intellij.ui.navigation.Place;
import consulo.lombok.annotations.Logger;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * @author VISTALL
 * @since 21.03.14
 */
@Logger
public class SdkEditor extends BaseSdkEditor {
  @NonNls
  private static final String SDK_TAB = "sdkTab";
  @NotNull private final History myHistory;

  private TabbedPaneWrapper myTabbedPane;

  public SdkEditor(@NotNull SdkModel sdkModel, @NotNull History history, @NotNull SdkImpl sdk) {
    super(sdkModel, sdk);
    myHistory = history;
  }

  @NotNull
  @Override
  protected JComponent createCenterComponent() {
    myTabbedPane = new TabbedPaneWrapper(myDisposable);
    for (OrderRootType type : OrderRootType.getAllTypes()) {
      if (showTabForType(type)) {
        final OrderRootTypeUIFactory factory = OrderRootTypeUIFactory.FACTORY.getByKey(type);
        if(factory == null) {
          continue;
        }

        SdkPathEditor pathEditor = getPathEditor(type);

        myTabbedPane.addTab(pathEditor.getDisplayName(), pathEditor.createComponent());
      }
    }

    myTabbedPane.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(final ChangeEvent e) {
        myHistory.pushQueryPlace();
      }
    });
    return myTabbedPane.getComponent();
  }

  @Override
  public ActionCallback navigateTo(@Nullable final Place place, final boolean requestFocus) {
    if (place == null) return new ActionCallback.Done();
    myTabbedPane.setSelectedTitle((String)place.getPath(SDK_TAB));
    return new ActionCallback.Done();
  }

  @Override
  public void queryPlace(@NotNull final Place place) {
    place.putPath(SDK_TAB, myTabbedPane.getSelectedTitle());
  }
}
