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

import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.image.IconLibrary;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2020-09-26
 */
public abstract class BaseIconLibraryImpl implements IconLibrary {
    public static class ImageState {
        private byte[] my1xData;
        private byte[] my2xData;
        private final boolean myIsSVG;

        private SimpleReference<ImageReference> myImageRef;

        public ImageState(@Nonnull byte[] _1xData, @Nullable byte[] _2xdata, boolean isSVG) {
            my1xData = _1xData;
            my2xData = _2xdata;
            myIsSVG = isSVG;
        }

        @Nullable
        public ImageReference getOrCreateImage(@Nonnull BaseIconLibraryImpl library, String groupId, String imageId) {
            SimpleReference<ImageReference> imageRef = myImageRef;
            if (imageRef != null) {
                return imageRef.get();
            }

            byte[] x1Data = my1xData;
            byte[] x2Data = my2xData;

            // already initialized
            if (x1Data == null) {
                return myImageRef.get();
            }

            ImageReference image = library.createImageReference(x1Data, x2Data, myIsSVG, groupId, imageId);
            myImageRef = new SimpleReference<>(image);
            my1xData = null;
            my2xData = null;
            return image;
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

    private static final Logger LOG = Logger.getInstance(BaseIconLibraryImpl.class);

    private final String myId;

    private String myBaseId;

    private String myInverseId;

    private LocalizeValue myName;

    private boolean myIsDark;

    private final BaseIconLibraryManager myIconLibraryManager;

    private final Map<String, IconGroup> myRegisteredGroups = new HashMap<>();

    public BaseIconLibraryImpl(@Nonnull String id, @Nonnull BaseIconLibraryManager baseIconLibraryManager) {
        myId = id;
        myIconLibraryManager = baseIconLibraryManager;
    }

    protected void registerIcon(String groupId, String imageId, byte[] _1xdata, byte[] _2xdata, boolean isSVG) {
        myRegisteredGroups.computeIfAbsent(groupId, IconGroup::new).registerIcon(imageId, _1xdata, _2xdata, isSVG);
    }

    public void setBaseId(String baseId) {
        myBaseId = baseId;
    }

    public void setName(LocalizeValue name) {
        myName = name;
    }

    public void setInverseId(String inverseId) {
        myInverseId = inverseId;
    }

    public void setDark(boolean dark) {
        myIsDark = dark;
    }

    public String getInverseId() {
        return myInverseId;
    }

    @Override
    public boolean isDark() {
        return myIsDark;
    }

    @Override
    @Nonnull
    public LocalizeValue getName() {
        if (myName == null) {
            return myName = LocalizeValue.of(myId);
        }
        return myName;
    }

    @Override
    @Nonnull
    public String getId() {
        return myId;
    }

    @Nonnull
    protected abstract ImageReference createImageReference(@Nonnull byte[] _1xData,
                                                           @Nullable byte[] _2xdata,
                                                           boolean isSVG,
                                                           String groupId,
                                                           String imageId);

    @Nullable
    public ImageReference resolveImage(String groupId, String imageId) {
        ImageReference image = resolveImageNoLog(groupId, imageId);
        if (image != null) {
            return image;
        }
        LOG.warn("Icon: " + groupId + "@" + imageId + " not found.");
        return null;
    }

    @Nullable
    protected ImageReference resolveImageNoLog(String groupId, String imageId) {
        IconGroup iconGroup = myRegisteredGroups.get(groupId);
        if (iconGroup != null) {
            ImageState imageState = iconGroup.myRegisteredIcons.get(imageId);
            if (imageState != null) {
                return imageState.getOrCreateImage(this, groupId, imageId);
            }
        }

        String baseId = myBaseId;
        if (baseId != null) {
            BaseIconLibraryImpl library = myIconLibraryManager.getLibrary(baseId);
            if (library != null) {
                ImageReference image = library.resolveImageNoLog(groupId, imageId);
                if (image != null) {
                    return image;
                }
            }
        }

        return null;
    }
}
