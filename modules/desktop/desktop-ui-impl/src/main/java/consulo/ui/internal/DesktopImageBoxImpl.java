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
package consulo.ui.internal;

import com.intellij.ui.roots.ScalableIconComponent;
import consulo.ui.ImageBox;
import consulo.ui.image.Image;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 12-Sep-17
 */
public class DesktopImageBoxImpl extends ScalableIconComponent implements ImageBox, SwingWrapper{
  private Image myIcon;

  public DesktopImageBoxImpl(Image icon) {
    super(((SwingIconWrapper)icon).toSwingIcon());
    myIcon = icon;
  }

  @NotNull
  @Override
  public Image getImage() {
    return myIcon;
  }
}
