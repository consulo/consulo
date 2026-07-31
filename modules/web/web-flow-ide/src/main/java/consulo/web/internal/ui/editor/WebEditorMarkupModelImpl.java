/*
 * Copyright 2013-2020 consulo.io
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
package consulo.web.internal.ui.editor;

import consulo.codeEditor.Editor;
import consulo.language.editor.impl.internal.markup.EditorMarkupModel;
import consulo.language.editor.impl.internal.markup.ErrorStripTooltipRendererProvider;
import consulo.codeEditor.internal.ErrorStripeListener;
import consulo.codeEditor.impl.MarkupModelImpl;
import consulo.language.editor.impl.internal.markup.ErrorStripeRenderer;
import consulo.ui.ex.awt.PopupHandler;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 06/12/2020
 */
public class WebEditorMarkupModelImpl extends MarkupModelImpl implements EditorMarkupModel {
  
  private final WebEditorImpl myWebEditor;

  private ErrorStripeRenderer myErrorStripeRenderer;

  private boolean myErrorStripeVisible;

  private @Nullable ErrorStripTooltipRendererProvider myTooltipRendererProvider;

  /**
   * The awt panel looks for the highlighters within twice this distance of the pointer, which makes a two pixel
   * mark hoverable. Here the mark is a dom element and there is nothing around it that answers, so it has to be
   * tall enough to be hit on its own.
   */
  private int myMinMarkHeight = 4;

  public WebEditorMarkupModelImpl(WebEditorImpl webEditor) {
    super(webEditor.getDocument());
    myWebEditor = webEditor;
  }

  
  @Override
  public Editor getEditor() {
    return myWebEditor;
  }

  @Override
  public void setErrorStripeVisible(boolean val) {
    // the flag gates ErrorStripeUpdateManagerImpl - while it stayed false no traffic light renderer was ever
    // installed, and the editor had no analyze status to show
    myErrorStripeVisible = val;

    myWebEditor.scheduleErrorStripeUpdate();
  }

  @RequiredUIAccess
  @Override
  public void setErrorStripeRenderer(ErrorStripeRenderer renderer) {
    // the traffic light renderer subscribes to the document markup model, leaving the old one alive doubles the
    // error counting on every reinstall
    if (myErrorStripeRenderer instanceof Disposable disposable) {
      Disposer.dispose(disposable);
    }

    myErrorStripeRenderer = renderer;

    // ErrorStripeUpdateManagerImpl only repaints when the renderer was already there, so the first status of a
    // freshly opened file would otherwise wait for the next daemon pass
    myWebEditor.scheduleAnalyzeStatusUpdate();
  }

  @Override
  public void dispose() {
    if (myErrorStripeRenderer instanceof Disposable disposable) {
      Disposer.dispose(disposable);
    }

    myErrorStripeRenderer = null;

    super.dispose();
  }

  @Override
  public ErrorStripeRenderer getErrorStripeRenderer() {
    return myErrorStripeRenderer;
  }

  @Override
  public void repaintTrafficLightIcon() {
    myWebEditor.scheduleAnalyzeStatusUpdate();
  }

  @Override
  public void addErrorMarkerListener(ErrorStripeListener listener, Disposable parent) {

  }

  @RequiredUIAccess
  @Override
  public void setErrorPanelPopupHandler(PopupHandler handler) {

  }

  @Override
  public void setErrorStripTooltipRendererProvider(ErrorStripTooltipRendererProvider provider) {
    myTooltipRendererProvider = provider;
  }

  /**
   * The stripe tooltips are html pushed to the browser, they are not built through a renderer - the provider is
   * only kept for whoever asks the model for it.
   */
  @Override
  public @Nullable ErrorStripTooltipRendererProvider getErrorStripTooltipRendererProvider() {
    return myTooltipRendererProvider;
  }

  @Override
  public void setMinMarkHeight(int minMarkHeight) {
    myMinMarkHeight = minMarkHeight;

    myWebEditor.scheduleErrorStripeUpdate();
  }

  @Override
  public int getMinMarkHeight() {
    return myMinMarkHeight;
  }

  @Override
  public boolean isErrorStripeVisible() {
    return myErrorStripeVisible;
  }
}
