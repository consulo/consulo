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
package consulo.web.internal.ui.image;

import consulo.ui.ex.UIModificationTracker;
import consulo.ui.image.IconLibraryManager;
import consulo.ui.image.ImageKey;
import consulo.ui.impl.image.BaseIconLibraryManager;
import consulo.ui.impl.image.ImageReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2020-10-03
 */
public class WebImageKeyImpl implements ImageKey, WebImageWithURL {
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

  @Nonnull
  @Override
  public String getImageURL() {
    return "/image?groupId=" + myGroupId + "&imageId=" + myImageId;
  }

  @Nullable
  public ImageReference calcImage() {
    return ourLibraryManager.resolveImage(null, myGroupId, myImageId);
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
