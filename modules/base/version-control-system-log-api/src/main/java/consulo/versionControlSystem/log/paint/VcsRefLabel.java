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
package consulo.versionControlSystem.log.paint;

import consulo.ui.color.ColorValue;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The tag shaped label of the references of a commit: one tag per colour, each next one shifted right over the
 * previous and cut from it by a tag of the background colour. A tag is a five point outline with a round hole.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public final class VcsRefLabel {
    private final int myHeight;
    private final List<ColorValue> myColors;
    private final ColorValue myBackground;

    public VcsRefLabel(int height, List<ColorValue> colors, ColorValue background) {
        myHeight = height;
        myColors = List.copyOf(colors);
        myBackground = background;
    }

    public int getHeight() {
        return myHeight;
    }

    public int getWidth() {
        return Math.round((7 + 2 * (myColors.size() - 1)) * getScale());
    }

    public List<ColorValue> getColors() {
        return myColors;
    }

    public ColorValue getBackground() {
        return myBackground;
    }

    public List<VcsRefLabelTag> resolveTags() {
        float scale = getScale();
        List<VcsRefLabelTag> tags = new ArrayList<>();
        for (int i = myColors.size() - 1; i >= 0; i--) {
            if (i != myColors.size() - 1) {
                tags.add(new VcsRefLabelTag(scale * 2 * i + 1, myBackground));
            }
            tags.add(new VcsRefLabelTag(scale * 2 * i, myColors.get(i)));
        }
        return tags;
    }

    public float[] getOutline(VcsRefLabelTag tag) {
        float scale = getScale();
        float x = tag.x();
        return new float[]{
            x + scale, 2 * scale,
            x + 3 * scale, 2 * scale,
            x + 6 * scale, 5 * scale,
            x + 4 * scale, 7 * scale,
            x + scale, 4 * scale
        };
    }

    public float getHoleCenterX(VcsRefLabelTag tag) {
        return tag.x() + 2.5f * getScale();
    }

    public float getHoleCenterY() {
        return 3.5f * getScale();
    }

    public float getHoleRadius() {
        return getScale() / 2;
    }

    private float getScale() {
        return myHeight / 8.0f;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return o instanceof VcsRefLabel label
            && myHeight == label.myHeight
            && myColors.equals(label.myColors)
            && myBackground.equals(label.myBackground);
    }

    @Override
    public int hashCode() {
        return (myHeight * 31 + myColors.hashCode()) * 31 + myBackground.hashCode();
    }
}
