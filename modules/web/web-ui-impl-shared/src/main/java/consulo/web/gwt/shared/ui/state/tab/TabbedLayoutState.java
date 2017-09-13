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
package consulo.web.gwt.shared.ui.state.tab;

import com.vaadin.shared.ui.AbstractComponentContainerState;
import consulo.web.gwt.shared.ui.state.combobox.ComboBoxState;
import consulo.web.gwt.shared.ui.state.image.MultiImageState;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 12-Sep-17
 */
public class TabbedLayoutState extends AbstractComponentContainerState {
  private static final long serialVersionUID = -116499580006123898L;

  public static class TabState extends ComboBoxState.Item {
    private static final long serialVersionUID = -8442971060081651491L;

    public MultiImageState myCloseButton;
    public MultiImageState myCloseHoverButton;
  }

  public List<TabState> myTabStates = new ArrayList<>();
  public int mySelected;
}
