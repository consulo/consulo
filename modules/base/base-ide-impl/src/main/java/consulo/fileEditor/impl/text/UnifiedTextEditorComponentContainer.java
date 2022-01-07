/*
 * Copyright 2013-2021 consulo.io
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
package consulo.fileEditor.impl.text;

import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.editor.Editor;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.layout.DockLayout;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 07/08/2021
 */
public class UnifiedTextEditorComponentContainer implements TextEditorComponentContainer {
  private final DockLayout myDockLayout;

  public UnifiedTextEditorComponentContainer(Editor editor, Disposable parentDisposable, DataProvider dataProvider) {
    myDockLayout = DockLayout.create();

    myDockLayout.addUserDataProvider(dataProvider::getData);
    myDockLayout.center(editor.getUIComponent());
  }

  @Override
  public void startLoading() {

  }

  @Override
  public void loadingFinished() {

  }

  @Override
  public JComponent getComponent() {
    throw new UnsupportedOperationException("unsupported platform");
  }

  @Override
  public Component getUIComponent() {
    return myDockLayout;
  }

  @Override
  public void hideContent() {

  }
}
