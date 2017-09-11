/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ui.internal;

import consulo.ui.ImageRef;
import consulo.ui.ListItemPresentation;
import consulo.ui.TextStyle;
import consulo.web.gwt.shared.ui.state.combobox.UIComboBoxState;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 12-Jun-16
 */
class WGwtListItemPresentationImpl implements ListItemPresentation {
  private UIComboBoxState.Item myItem = new UIComboBoxState.Item();

  @Override
  public void append(@NotNull ImageRef... imageRefs) {
  }

  @Override
  public void append(@NotNull String text) {
    UIComboBoxState.ItemSegment segment = new UIComboBoxState.ItemSegment();
    segment.myText = text;
    myItem.myItemSegments.add(segment);
  }

  @Override
  public void append(@NotNull String text, @NotNull TextStyle... styles) {
    UIComboBoxState.ItemSegment segment = new UIComboBoxState.ItemSegment();
    segment.myText = text;
    //TODO [VISTALL] style!
    myItem.myItemSegments.add(segment);
  }

  public UIComboBoxState.Item getItem() {
    return myItem;
  }
}
