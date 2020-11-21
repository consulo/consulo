/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ui.web.internal;

import consulo.localize.LocalizeValue;
import consulo.ui.HorizontalAlignment;
import consulo.ui.image.Image;
import consulo.ui.web.internal.base.VaadinComponent;
import consulo.ui.web.internal.util.Mappers;
import consulo.ui.web.servlet.WebImageMapper;
import consulo.web.gwt.shared.ui.state.LabelState;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2019-02-19
 */
public abstract class VaadinLabelComponentBase extends VaadinComponent {
  private HorizontalAlignment myHorizontalAlignment = HorizontalAlignment.LEFT;
  private LocalizeValue myTextValue = LocalizeValue.empty();
  private Image myImage;

  public void setTextValue(LocalizeValue textValue) {
    myTextValue = textValue;
    markAsDirty();
  }

  public void setHorizontalAlignment(@Nonnull HorizontalAlignment horizontalAlignment) {
    myHorizontalAlignment = horizontalAlignment;
    getState().myHorizontalAlignment = Mappers.map(horizontalAlignment);
    markAsDirty();
  }

  public LocalizeValue getTextValue() {
    return myTextValue;
  }

  public HorizontalAlignment getHorizontalAlignment() {
    return myHorizontalAlignment;
  }

  public void setImage(Image image) {
    myImage = image;
  }

  public Image getImage() {
    return myImage;
  }

  @Override
  public void beforeClientResponse(boolean initial) {
    super.beforeClientResponse(initial);

    LabelState state = getState();
    
    state.caption = myTextValue.getValue();
    if(myImage != null) {
      state.myImageState = WebImageMapper.map(myImage).getState();
    }
  }

  @Override
  public LabelState getState() {
    return (LabelState)super.getState();
  }
}
