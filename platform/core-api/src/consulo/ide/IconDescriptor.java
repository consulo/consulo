/*
 * Copyright 2013 must-be.org
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
package consulo.ide;

import com.intellij.ui.LayeredIcon;
import com.intellij.ui.RowIcon;
import com.intellij.util.SmartList;
import com.intellij.util.ui.EmptyIcon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * @author VISTALL
 * @since 0:07/19.07.13
 */
public class IconDescriptor {
  private List<Icon> myLayerIcons;
  private Icon myRightIcon;
  private Icon myMainIcon;

  public IconDescriptor(@Nullable Icon mainIcon) {
    myMainIcon = mainIcon;
  }

  @Nullable
  public Icon getMainIcon() {
    return myMainIcon;
  }

  public IconDescriptor setMainIcon(@Nullable Icon mainIcon) {
    myMainIcon = mainIcon;
    return this;
  }

  public IconDescriptor addLayerIcon(@NotNull Icon icon) {
    if(myLayerIcons == null) {
      myLayerIcons = new SmartList<Icon>();
    }
    myLayerIcons.add(icon);
    return this;
  }

  public IconDescriptor removeLayerIcon(@NotNull Icon icon) {
    if(myLayerIcons != null) {
      myLayerIcons.remove(icon);
    }
    return this;
  }

  public IconDescriptor clearLayerIcons() {
    if(myLayerIcons != null) {
      myLayerIcons.clear();
    }
    return this;
  }

  @Nullable
  public Icon getRightIcon() {
    return myRightIcon;
  }

  public void setRightIcon(@Nullable Icon rightIcon) {
    myRightIcon = rightIcon;
  }

  @NotNull
  public Icon toIcon() {
    Icon mainIcon = null;
    if(myLayerIcons == null) {
      mainIcon = myMainIcon;
    }
    else {
      LayeredIcon layeredIcon = new LayeredIcon(myLayerIcons.size() + 1);
      layeredIcon.setIcon(myMainIcon, 0);
      for (int i = 0; i < myLayerIcons.size(); i++) {
        Icon icon = myLayerIcons.get(i);
        layeredIcon.setIcon(icon, i + 1);
      }
      mainIcon = layeredIcon;
    }

    if(myRightIcon == null) {
      return mainIcon == null ? EmptyIcon.ICON_16 : mainIcon;
    }
    else {
      RowIcon baseIcon = new RowIcon(2);
      baseIcon.setIcon(mainIcon, 0);
      baseIcon.setIcon(myRightIcon, 1);
      return baseIcon;
    }
  }
}
