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
package consulo.desktop.awt.fileEditor.impl.text;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.editor.Editor;
import com.intellij.ui.components.JBLoadingPanel;
import com.intellij.util.ui.JBSwingUtilities;
import consulo.awt.TargetAWT;
import consulo.disposer.Disposable;
import consulo.fileEditor.impl.text.TextEditorComponentContainer;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 07/08/2021
 */
public class DesktopAwtTextEditorComponentContainer implements TextEditorComponentContainer {
  private final JBLoadingPanel myComponent;

  public DesktopAwtTextEditorComponentContainer(Editor editor, Disposable parentDisposable, DataProvider dataProvider) {
    myComponent = new JBLoadingPanel(new BorderLayout(), parentDisposable) {
      @Override
      public Color getBackground() {
        //noinspection ConstantConditions
        return editor == null ? super.getBackground() : editor.getContentComponent().getBackground();
      }

      @Override
      protected Graphics getComponentGraphics(Graphics g) {
        return JBSwingUtilities.runGlobalCGTransform(this, super.getComponentGraphics(g));
      }
    };
    DataManager.registerDataProvider(myComponent, dataProvider);

    myComponent.add(editor.getComponent(), BorderLayout.CENTER);
  }

  @Override
  public void startLoading() {
    myComponent.startLoading();
  }

  @Override
  public void loadingFinished() {
    if (myComponent.isLoading()) {
      myComponent.stopLoading();
    }

    myComponent.getContentPanel().setVisible(true);
  }

  @Override
  public JBLoadingPanel getComponent() {
    return myComponent;
  }

  @Override
  public consulo.ui.Component getUIComponent() {
    return TargetAWT.wrap(myComponent);
  }

  @Override
  public void hideContent() {
    myComponent.getContentPanel().setVisible(false);
  }

  @Override
  public Editor validateEditor(Editor editor) {
    Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    if (focusOwner instanceof JComponent) {
      final JComponent jComponent = (JComponent)focusOwner;
      if (jComponent.getClientProperty("AuxEditorComponent") != null) return null; // Hack for EditorSearchComponent
    }
    return editor;
  }
}
