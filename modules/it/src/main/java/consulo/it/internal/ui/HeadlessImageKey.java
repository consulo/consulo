/*
 * Copyright 2013-2026 consulo.io
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
package consulo.it.internal.ui;

import consulo.ui.image.ImageKey;

import java.util.Objects;

/**
 * Dummy-but-creatable headless {@link ImageKey}: just the ids and size, no icon library resolution.
 * Keeps icon-group holders like {@code PlatformIconGroup} constructible headless.
 *
 * @author VISTALL
 */
public class HeadlessImageKey implements ImageKey {
    private final String myGroupId;
    private final String myImageId;
    private final int myWidth;
    private final int myHeight;

    public HeadlessImageKey(String groupId, String imageId, int width, int height) {
        myGroupId = groupId;
        myImageId = imageId;
        myWidth = width;
        myHeight = height;
    }

    @Override
    public String getGroupId() {
        return myGroupId;
    }

    @Override
    public String getImageId() {
        return myImageId;
    }

    @Override
    public int getWidth() {
        return myWidth;
    }

    @Override
    public int getHeight() {
        return myHeight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HeadlessImageKey that)) {
            return false;
        }
        return myWidth == that.myWidth
            && myHeight == that.myHeight
            && myGroupId.equals(that.myGroupId)
            && myImageId.equals(that.myImageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myGroupId, myImageId, myWidth, myHeight);
    }

    @Override
    public String toString() {
        return myGroupId + "@" + myImageId;
    }
}
