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
package consulo.language.icon;

import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.collection.ArrayUtil;

import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2013-07-19
 */
public class IconDescriptor {
  private Image[] myLayerIcons = Image.EMPTY_ARRAY;
  private @Nullable Image myRightIcon = null;
  private @Nullable Image myMainIcon;

  public IconDescriptor(@Nullable Image mainIcon) {
    myMainIcon = mainIcon;
  }

  public @Nullable Image getMainIcon() {
    return myMainIcon;
  }

  public IconDescriptor setMainIcon(@Nullable Image mainIcon) {
    myMainIcon = mainIcon;
    return this;
  }

  public IconDescriptor addLayerIcon(Image icon) {
    myLayerIcons = ArrayUtil.append(myLayerIcons, icon);
    return this;
  }

  public IconDescriptor removeLayerIcon(Image icon) {
    myLayerIcons = ArrayUtil.remove(myLayerIcons, icon);
    return this;
  }

  public IconDescriptor clearLayerIcons() {
    myLayerIcons = Image.EMPTY_ARRAY;
    return this;
  }

  public @Nullable Image getRightIcon() {
    return myRightIcon;
  }

  public void setRightIcon(@Nullable Image rightIcon) {
    myRightIcon = rightIcon;
  }

  public Image toIcon() {
    Image mainIcon;
    if (myLayerIcons.length == 0) {
      mainIcon = myMainIcon;
    }
    else {
      mainIcon = ImageEffects.layered(ArrayUtil.mergeArrays(new Image[]{myMainIcon}, myLayerIcons));
    }

    if (mainIcon == null) {
      mainIcon = Image.empty(16);
    }

    return myRightIcon == null ? mainIcon : ImageEffects.appendRight(mainIcon, myRightIcon);
  }
}
