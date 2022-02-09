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
import consulo.execution.configuration.ui.event.SettingsEditorListener;
import consulo.logging.Logger;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.function.Function;

public class SettingsEditorWrapper <Src, Dst> extends SettingsEditor<Src> {

  private static final Logger LOG = Logger.getInstance(SettingsEditorWrapper.class);

  private final Function<Src, Dst> mySrcToDstConvertor;
  private final SettingsEditor<Dst> myWrapped;

  private final SettingsEditorListener<Dst> myListener;

  public SettingsEditorWrapper(SettingsEditor<Dst> wrapped, Function<Src, Dst> convertor) {
    mySrcToDstConvertor = convertor;
    myWrapped = wrapped;
    myListener = settingsEditor -> fireEditorStateChanged();
    myWrapped.addSettingsEditorListener(myListener);
  }

  @Override
  public void resetEditorFrom(Src src) {
    myWrapped.resetFrom(mySrcToDstConvertor.apply(src));
  }

  @Override
  public void applyEditorTo(Src src) throws ConfigurationException {
    myWrapped.applyTo(mySrcToDstConvertor.apply(src));
  }

  @Override
  @Nonnull
  public JComponent createEditor() {
    return myWrapped.createEditor();
  }

  @RequiredUIAccess
  @Nullable
  @Override
  protected Component createUIComponent() {
    return myWrapped.createUIComponent();
  }

  @Override
  public void disposeEditor() {
    myWrapped.removeSettingsEditorListener(myListener);
    Disposer.dispose(myWrapped);
  }
}
