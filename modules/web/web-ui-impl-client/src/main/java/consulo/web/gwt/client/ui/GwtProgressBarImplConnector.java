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
package consulo.web.gwt.client.ui;

import com.vaadin.client.communication.StateChangeEvent;
import com.vaadin.client.ui.AbstractComponentConnector;
import com.vaadin.shared.ui.Connect;
import consulo.web.gwt.shared.ui.state.ProgressBarState;

/**
 * @author VISTALL
 * @since 2020-05-11
 */
@Connect(canonicalName = "consulo.ui.web.internal.WebProgressBarImpl.Vaadin")
public class GwtProgressBarImplConnector extends AbstractComponentConnector {
  @Override
  protected void updateComponentSize() {
    // nothing
  }

  @Override
  public void onStateChanged(StateChangeEvent stateChangeEvent) {
    super.onStateChanged(stateChangeEvent);

    ProgressBarState state = getState();

    getWidget().setIndeterminate(state.indeterminate);

    int minimum = state.minimum;
    int maximum = state.maximum;

    // TODO [VISTALL] handle if maximum not equal 100
    
    getWidget().setState((float)(state.value / 100.));
  }

  @Override
  public GwtProgressBarImpl getWidget() {
    return (GwtProgressBarImpl)super.getWidget();
  }

  @Override
  public ProgressBarState getState() {
    return (ProgressBarState)super.getState();
  }
}
