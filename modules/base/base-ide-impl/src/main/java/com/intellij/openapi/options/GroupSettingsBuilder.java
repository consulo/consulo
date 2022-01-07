/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.openapi.options;

import consulo.disposer.Disposable;
import com.intellij.openapi.util.Pair;
import com.intellij.ui.TabbedPaneWrapper;
import javax.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GroupSettingsBuilder<T> implements CompositeSettingsBuilder<T> {
  private final SettingsEditorGroup<T> myGroup;
  private JComponent myComponent;

  public GroupSettingsBuilder(SettingsEditorGroup<T> group) {
    myGroup = group;
  }

  @Nonnull
  @Override
  public Collection<SettingsEditor<T>> getEditors() {
    List<SettingsEditor<T>> result = new ArrayList<SettingsEditor<T>>();
    List<Pair<String,SettingsEditor<T>>> editors = myGroup.getEditors();
    for (Pair<String, SettingsEditor<T>> editor : editors) {
      result.add(editor.getSecond());
    }
    return result;
  }

  @Nonnull
  @Override
  public JComponent createCompoundEditor(@Nonnull Disposable disposable) {
    if (myComponent == null) {
      myComponent = doCreateComponent(disposable);
    }
    return myComponent;
  }

  @Nonnull
  private JComponent doCreateComponent(@Nonnull Disposable disposable) {
    List<Pair<String,SettingsEditor<T>>> editors = myGroup.getEditors();
    if (editors.size() == 0) return new JPanel();
    if (editors.size() == 1) return editors.get(0).getSecond().getComponent();

    TabbedPaneWrapper tabbedPaneWrapper = new TabbedPaneWrapper(disposable);
    for (Pair<String, SettingsEditor<T>> pair : editors) {
      JPanel panel = new JPanel(new BorderLayout());
      panel.add(pair.getSecond().getComponent(), BorderLayout.CENTER);
      tabbedPaneWrapper.addTab(pair.getFirst(), panel);
    }
    return tabbedPaneWrapper.getComponent();
  }
}