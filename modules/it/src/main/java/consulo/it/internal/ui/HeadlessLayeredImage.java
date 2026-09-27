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

import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Images drawn over each other, first at the bottom.
 *
 * @author VISTALL
 */
public final class HeadlessLayeredImage implements Image {
    private final List<@Nullable Image> myLayers;

    public HeadlessLayeredImage(@Nullable Image[] layers) {
        myLayers = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(layers)));
    }

    public List<@Nullable Image> getLayers() {
        return myLayers;
    }

    @Override
    public int getWidth() {
        int width = 0;
        for (Image layer : myLayers) {
            if (layer != null) {
                width = Math.max(width, layer.getWidth());
            }
        }
        return width;
    }

    @Override
    public int getHeight() {
        int height = 0;
        for (Image layer : myLayers) {
            if (layer != null) {
                height = Math.max(height, layer.getHeight());
            }
        }
        return height;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return HeadlessImages.unwrap(o) instanceof HeadlessLayeredImage that && myLayers.equals(that.myLayers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myLayers);
    }

    @Override
    public String toString() {
        return "layered" + myLayers;
    }
}
