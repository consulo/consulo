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
package consulo.desktop.swt.editor.impl;

import consulo.codeEditor.impl.EditorMarkupModelImpl;
import consulo.language.editor.impl.internal.markup.EditorMarkupModel;
import consulo.language.editor.impl.internal.markup.ErrorStripTooltipRendererProvider;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.PopupHandler;

/**
 * @author VISTALL
 * @since 18/12/2021
 */
public class DesktopSwtMarkupModelImpl extends EditorMarkupModelImpl<DesktopSwtEditorImpl> implements EditorMarkupModel {
  public DesktopSwtMarkupModelImpl(DesktopSwtEditorImpl editor) {
    super(editor);
  }

  @Override
  public void setErrorStripeVisible(boolean val) {

  }

  @RequiredUIAccess
  @Override
  public void setErrorPanelPopupHandler(PopupHandler handler) {

  }

  @Override
  public void setErrorStripTooltipRendererProvider(ErrorStripTooltipRendererProvider provider) {

  }

  @Override
  public ErrorStripTooltipRendererProvider getErrorStripTooltipRendererProvider() {
    return null;
  }

  @Override
  public void setMinMarkHeight(int minMarkHeight) {

  }

  @Override
  public int getMinMarkHeight() {
    return 0;
  }

  @Override
  public boolean isErrorStripeVisible() {
    return false;
  }
}
