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
package consulo.ui.web.internal.image;

import consulo.desktop.util.awt.UIModificationTracker;
import consulo.ui.image.IconLibraryManager;
import consulo.ui.image.Image;
import consulo.ui.image.ImageKey;
import consulo.ui.impl.image.BaseIconLibraryManager;
import consulo.web.gwt.shared.ui.state.image.ImageState;
import consulo.web.gwt.shared.ui.state.image.MultiImageState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2020-10-03
 */
public class WebImageKeyImpl implements ImageKey, WebImageWithVaadinState {
  private static final BaseIconLibraryManager ourLibraryManager = (BaseIconLibraryManager)IconLibraryManager.get();
  private static final UIModificationTracker ourUIModificationTracker = UIModificationTracker.getInstance();

  private final String myGroupId;
  private final String myImageId;
  private final int myWidth;
  private final int myHeight;

  public WebImageKeyImpl(String groupId, String imageId, int width, int height) {
    myGroupId = groupId;
    myImageId = imageId;
    myWidth = width;
    myHeight = height;
  }

  @Nonnull
  @Override
  public String getGroupId() {
    return myGroupId;
  }

  @Override
  public void toState(MultiImageState state) {
    Image icon = calcImage();

    state.myWidth = myWidth;
    state.myHeight = myHeight;

    if (icon != null) {
      state.myImageState = new ImageState();
      state.myImageState.myURL = "/app/image?groupId=" + myGroupId + "&imageId=" + myImageId;
    }
  }

  @Nullable
  public Image calcImage() {
    return ourLibraryManager.getIcon(null, myGroupId, myImageId, myWidth, myHeight);
  }

  @Nonnull
  @Override
  public String getImageId() {
    return myImageId;
  }

  @Override
  public int getHeight() {
    return myHeight;
  }

  @Override
  public int getWidth() {
    return myWidth;
  }
}
