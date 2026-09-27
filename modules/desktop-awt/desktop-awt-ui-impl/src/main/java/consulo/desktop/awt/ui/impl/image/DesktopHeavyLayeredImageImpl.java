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
package consulo.desktop.awt.ui.impl.image;

import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.function.UnaryOperator;

/**
 * @author VISTALL
 * @since 2017-09-11
 */
public class DesktopHeavyLayeredImageImpl extends LayeredIcon implements Image, DesktopAWTImage {
    private static final int NO_CONSTRAINT = -1;

    private record Layer(Image image, int constraint, int hShift, int vShift) {
    }

    private final @Nullable Layer[] myLayers;

    public DesktopHeavyLayeredImageImpl(int layerCount) {
        super(layerCount);
        myLayers = new Layer[layerCount];
    }

    public DesktopHeavyLayeredImageImpl(Image... images) {
        this(images.length);
        for (int i = 0; i < images.length; i++) {
            setImage(images[i], i);
        }
    }

    public void setImage(Image image, int layer) {
        setImage(image, layer, 0, 0);
    }

    public void setImage(Image image, int layer, int hShift, int vShift) {
        super.setIcon(TargetAWT.to(image), layer, hShift, vShift);
        myLayers[layer] = new Layer(image, NO_CONSTRAINT, hShift, vShift);
    }

    public void setImage(Image image, int layer, int constraint) {
        super.setIcon(TargetAWT.to(image), layer, constraint);
        myLayers[layer] = new Layer(image, constraint, 0, 0);
    }

    @Override
    public void setIcon(@Nullable Icon icon, int layer, int hShift, int vShift) {
        super.setIcon(icon, layer, hShift, vShift);
        myLayers[layer] = icon == null ? null : new Layer(TargetAWT.from(icon), NO_CONSTRAINT, hShift, vShift);
    }

    @Override
    public void setIcon(Icon icon, int layer, int constraint) {
        super.setIcon(icon, layer, constraint);
        myLayers[layer] = new Layer(TargetAWT.from(icon), constraint, 0, 0);
    }

    @Override
    public int getWidth() {
        return getIconWidth();
    }

    @Override
    public int getHeight() {
        return getIconHeight();
    }

    @Override
    public DesktopAWTImage copyWithNewSize(int width, int height) {
        return copyLayers(image -> ImageEffects.resize(image, width, height));
    }

    @Override
    public DesktopAWTImage copyWithForceLibraryId(String libraryId) {
        return copyLayers(image -> DesktopAWTImage.copyWithForceLibraryId(image, libraryId));
    }

    @Override
    public DesktopAWTImage copyGrayed() {
        return copyLayers(image -> DesktopAWTImage.copyGrayed(image));
    }

    private DesktopHeavyLayeredImageImpl copyLayers(UnaryOperator<Image> copy) {
        DesktopHeavyLayeredImageImpl result = new DesktopHeavyLayeredImageImpl(myLayers.length);
        for (int i = 0; i < myLayers.length; i++) {
            Layer layer = myLayers[i];
            if (layer == null) {
                continue;
            }

            Image image = copy.apply(layer.image());
            if (layer.constraint() == NO_CONSTRAINT) {
                result.setImage(image, i, layer.hShift(), layer.vShift());
            }
            else {
                result.setImage(image, i, layer.constraint());
            }
            result.setLayerEnabled(i, isLayerEnabled(i));
        }
        return result;
    }
}
