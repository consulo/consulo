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
package consulo.desktop.startup.customize;

import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2020-08-02
 */
public class PluginTemplate {
  private final Set<String> myPluginIds;
  private final String myDescription;
  private final Image myImage;

  private final int myRow;
  private final int myCol;

  public PluginTemplate(Set<String> pluginIds, String description, Image image, int row, int col) {
    myPluginIds = pluginIds;
    myDescription = description;
    myImage = image;
    myRow = row;
    myCol = col;
  }

  public int getRow() {
    return myRow;
  }

  public int getCol() {
    return myCol;
  }

  @Nullable
  public String getDescription() {
    return myDescription;
  }

  @Nonnull
  public Set<String> getPluginIds() {
    return myPluginIds;
  }

  @Nonnull
  public Image getImage() {
    return myImage;
  }
}
