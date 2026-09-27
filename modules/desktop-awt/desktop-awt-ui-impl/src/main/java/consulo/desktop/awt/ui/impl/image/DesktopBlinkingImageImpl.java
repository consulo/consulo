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
package consulo.desktop.awt.ui.impl.image;

import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import org.jspecify.annotations.Nullable;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 2026-09-27
 */
public final class DesktopBlinkingImageImpl extends DesktopAnimatedImageBase {
    private static final int DELAY = 1000;

    public static DesktopAWTImage of(Image original) {
        if (original instanceof DesktopAnimatedImageBase animated) {
            return animated;
        }
        return new DesktopBlinkingImageImpl(original);
    }

    private final Image myOriginal;
    private final DesktopLazyImageImpl myOffImage;

    private DesktopBlinkingImageImpl(Image original) {
        super(2);
        myOriginal = original;
        myOffImage = new DesktopLazyImageImpl(() -> DesktopAWTImage.copyGrayed(original));
    }

    @Override
    public int getWidth() {
        return myOriginal.getWidth();
    }

    @Override
    public int getHeight() {
        return myOriginal.getHeight();
    }

    @Override
    protected Icon getFrameIcon(int index) {
        return index == 0 ? getStillIcon() : myOffImage;
    }

    @Override
    protected int getFrameDelay(int index) {
        return DELAY;
    }

    @Override
    protected Icon getStillIcon() {
        return TargetAWT.to(myOriginal);
    }

    @Override
    public DesktopAWTImage copyWithNewSize(int width, int height) {
        return of(ImageEffects.resize(myOriginal, width, height));
    }

    @Override
    public DesktopAWTImage copyWithForceLibraryId(String libraryId) {
        return of(DesktopAWTImage.copyWithForceLibraryId(myOriginal, libraryId));
    }

    @Override
    public DesktopAWTImage copyGrayed() {
        return DesktopAWTImage.copyGrayed(myOriginal);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return this == o || o instanceof DesktopBlinkingImageImpl that && myOriginal.equals(that.myOriginal);
    }

    @Override
    public int hashCode() {
        return myOriginal.hashCode();
    }
}
