/*
 * Copyright 2013-2017 consulo.io
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
package consulo.web.gwt.client.ui.ex;

import com.vaadin.client.StyleConstants;
import com.vaadin.client.ui.AbstractComponentConnector;
import com.vaadin.shared.ui.Connect;
import consulo.web.gwt.client.ui.GwtComponentSizeUpdater;
import consulo.web.gwt.shared.ui.ex.state.editor.EditorClientRpc;
import consulo.web.gwt.shared.ui.ex.state.editor.EditorServerRpc;
import consulo.web.gwt.shared.ui.ex.state.editor.EditorState;

/**
 * @author VISTALL
 * @since 13-Sep-17
 */
@Connect(canonicalName = "consulo.ui.web.internal.ex.WebEditorImpl.Vaadin")
public class GwtEditorImplConnector extends AbstractComponentConnector {
  @Override
  protected void init() {
    super.init();

    getWidget().setEditorServerRpc(getRpcProxy(EditorServerRpc.class));

    registerRpc(EditorClientRpc.class, new EditorClientRpc() {
      @Override
      public void setText(String text) {
        getWidget().setText(text);
      }
    });
  }

  @Override
  protected void updateComponentSize() {
    GwtComponentSizeUpdater.updateForComponent(this);
  }

  @Override
  protected void updateWidgetStyleNames() {
    super.updateWidgetStyleNames();

    setWidgetStyleName(StyleConstants.UI_WIDGET, false);
  }

  @Override
  public GwtEditorImpl getWidget() {
    return (GwtEditorImpl)super.getWidget();
  }

  @Override
  public EditorState getState() {
    return (EditorState)super.getState();
  }
}
