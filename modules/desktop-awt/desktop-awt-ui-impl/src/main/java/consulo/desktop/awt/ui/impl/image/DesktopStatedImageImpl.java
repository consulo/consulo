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
package consulo.desktop.awt.ui.impl.image;

import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.image.ImageState;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * @author VISTALL
 * @since 2020-08-27
 */
public class DesktopStatedImageImpl<S> implements Icon, Image, DesktopAWTImage {
    private static final class CopyFunction<S> implements Function<S, Image> {
        private record Copy(Image source, Image result) {
        }

        private final Function<S, Image> myFunction;
        private final UnaryOperator<Image> myCopy;

        private volatile @Nullable Copy myLastCopy;

        private CopyFunction(Function<S, Image> function, UnaryOperator<Image> copy) {
            myFunction = function;
            myCopy = copy;
        }

        @Override
        public Image apply(S state) {
            Image source = myFunction.apply(state);
            Copy lastCopy = myLastCopy;
            if (lastCopy != null && lastCopy.source() == source) {
                return lastCopy.result();
            }

            Image result = myCopy.apply(source);
            myLastCopy = new Copy(source, result);
            return result;
        }
    }

    private final ImageState<S> myState;
    private final Function<S, Image> myImageFunction;

    public DesktopStatedImageImpl(ImageState<S> state, Function<S, Image> imageFunction) {
        myState = state;
        myImageFunction = imageFunction;
    }

    @Override
    public int getHeight() {
        return myImageFunction.apply(myState.getState()).getHeight();
    }

    @Override
    public int getWidth() {
        return myImageFunction.apply(myState.getState()).getWidth();
    }

    @Override
    public int getIconWidth() {
        return getWidth();
    }

    @Override
    public int getIconHeight() {
        return getHeight();
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Image image = myImageFunction.apply(myState.getState());

        Icon icon = TargetAWT.to(image);

        icon.paintIcon(c, g, x, y);
    }

    @Override
    public DesktopAWTImage copyWithNewSize(int width, int height) {
        return copy(image -> ImageEffects.resize(image, width, height));
    }

    @Override
    public DesktopAWTImage copyWithForceLibraryId(String libraryId) {
        return copy(image -> DesktopAWTImage.copyWithForceLibraryId(image, libraryId));
    }

    @Override
    public DesktopAWTImage copyGrayed() {
        return copy(image -> DesktopAWTImage.copyGrayed(image));
    }

    private DesktopStatedImageImpl<S> copy(UnaryOperator<Image> copy) {
        return new DesktopStatedImageImpl<>(myState, new CopyFunction<>(myImageFunction, copy));
    }
}
