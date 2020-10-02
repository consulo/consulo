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
package consulo.ui.impl.image;

import consulo.logging.Logger;
import consulo.ui.image.Image;
import consulo.util.lang.ref.SimpleReference;
import consulo.util.lang.ref.SoftReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author VISTALL
 * @since 2020-09-26
 */
public abstract class IconLibrary {
  public static class ImageState {
    private byte[] my1xData;
    private byte[] my2xData;
    private final boolean myIsSVG;

    private final AtomicBoolean myInitialized = new AtomicBoolean();

    private SimpleReference<Image> myImageRef;

    public ImageState(@Nonnull byte[] _1xData, @Nullable byte[] _2xdata, boolean isSVG) {
      my1xData = _1xData;
      my2xData = _2xdata;
      myIsSVG = isSVG;
    }

    @Nullable
    public Image getOrCreateImage(@Nonnull IconLibrary library, int width, int height, String groupId, String imageId) {
      if (myInitialized.get()) {
        return Objects.requireNonNull(SoftReference.deref(myImageRef));
      }

      if (myInitialized.compareAndSet(false, true)) {
        Image image = library.createImage(my1xData, my2xData, myIsSVG, width, height, groupId, imageId);
        myImageRef = SimpleReference.create(image);

        // reset data
        my1xData = null;
        my2xData = null;
        return image;
      }

      throw new IllegalArgumentException("Image is not initialized");
    }
  }

  public static class IconGroup {
    private final Map<String, ImageState> myRegisteredIcons = new HashMap<>();

    public IconGroup(String groupId) {
    }

    protected void registerIcon(String imageId, byte[] _1xdata, byte[] _2xdata, boolean isSVG) {
      myRegisteredIcons.put(imageId, new ImageState(_1xdata, _2xdata, isSVG));
    }
  }

  private static final Logger LOG = Logger.getInstance(IconLibrary.class);

  private final IconLibraryId myId;

  private final BaseIconLibraryManager myIconLibraryManager;

  private final Map<String, IconGroup> myRegisteredGroups = new HashMap<>();

  public IconLibrary(@Nonnull IconLibraryId id, @Nonnull BaseIconLibraryManager baseIconLibraryManager) {
    myId = id;
    myIconLibraryManager = baseIconLibraryManager;
  }

  protected void registerIcon(String groupId, String imageId, byte[] _1xdata, byte[] _2xdata, boolean isSVG) {
    myRegisteredGroups.computeIfAbsent(groupId, IconGroup::new).registerIcon(imageId, _1xdata, _2xdata, isSVG);
  }

  @Nonnull
  public IconLibraryId getId() {
    return myId;
  }

  @Nullable
  protected abstract Image createImage(@Nonnull byte[] _1xData, @Nullable byte[] _2xdata, boolean isSVG, int width, int height, String groupId, String imageId);

  @Nullable
  public Image getIcon(String groupId, String imageId, int width, int height) {
    Image image = getIconNoLog(groupId, imageId, width, height);
    if (image != null) {
      return image;
    }

    String baseId = myId.getBaseId();
    if (baseId != null) {
      IconLibrary library = myIconLibraryManager.getLibrary(baseId);
      if (library != null) {
        image = library.getIconNoLog(groupId, imageId, width, height);
        if (image != null) {
          return image;
        }
      }
    }

    LOG.warn("Icon: " + groupId + "@" + imageId + " not found.");
    return null;
  }

  @Nullable
  protected Image getIconNoLog(String groupId, String imageId, int width, int height) {
    IconGroup iconGroup = myRegisteredGroups.get(groupId);
    if (iconGroup != null) {
      ImageState imageState = iconGroup.myRegisteredIcons.get(imageId);
      if (imageState != null) {
        return imageState.getOrCreateImage(this, width, height, groupId, imageId);
      }
    }
    return null;
  }
}
