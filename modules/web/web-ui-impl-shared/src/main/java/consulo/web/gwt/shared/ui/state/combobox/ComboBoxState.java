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
package consulo.web.gwt.shared.ui.state.combobox;

import com.vaadin.shared.AbstractComponentState;
import consulo.web.gwt.shared.ui.state.border.BorderListState;
import consulo.web.gwt.shared.ui.state.image.MultiImageState;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 11-Sep-17
 */
public class ComboBoxState extends AbstractComponentState {
  private static final long serialVersionUID = -7536312961415788171L;

  public static class ItemSegment implements Serializable {
    private static final long serialVersionUID = 2337453173338728592L;
    public String myText;
  }

  public static class Item implements Serializable {
    private static final long serialVersionUID = 9012316012065662376L;

    public MultiImageState myImageState;

    public List<ItemSegment> myItemSegments = new ArrayList<>();
  }

  public int mySelectedIndex = -1;
  public List<Item> myItems = new ArrayList<>();

  public BorderListState myBorderState = new BorderListState();
}
