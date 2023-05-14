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
package consulo.execution.configuration.ui;

import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposer;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.Pair;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class SettingsEditorGroup<T> extends SettingsEditor<T> {
  private final List<Pair<String, SettingsEditor<T>>> myEditors = new ArrayList<Pair<String, SettingsEditor<T>>>();

  public void addEditor(String name, SettingsEditor<T> editor) {
    Disposer.register(this, editor);
    myEditors.add(new Pair<>(name, editor));
  }

  public void addGroup(SettingsEditorGroup<T> group) {
    for (final Pair<String, SettingsEditor<T>> pair : group.myEditors) {
      Disposer.register(this, pair.second);
    }
    myEditors.addAll(group.myEditors);
  }

  public List<Pair<String, SettingsEditor<T>>> getEditors() {
    return myEditors;
  }

  @Override
  public void resetEditorFrom(T t) {
  }

  @Override
  public void applyEditorTo(T t) throws ConfigurationException {
  }

  @Override
  @Nonnull
  public JComponent createEditor() {
    throw new UnsupportedOperationException("This method should never be called!");
  }

  @RequiredUIAccess
  @Nullable
  @Override
  protected Component createUIComponent() {
    throw new UnsupportedOperationException("This method should never be called!");
  }

  @Override
  public void disposeEditor() {
  }
}
