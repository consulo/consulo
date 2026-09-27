/*
 * Copyright 2013-2019 consulo.io
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
import consulo.ui.ex.UIModificationTracker;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2019-01-26
 */
public class DesktopLazyImageImpl extends DesktopBaseLazyImageImpl implements DesktopAWTImage {
    private static final UIModificationTracker ourTracker = UIModificationTracker.getInstance();

    private final Supplier<Image> myImageSupplier;

    private @Nullable Image myImage;

    public DesktopLazyImageImpl(Supplier<Image> imageSupplier) {
        myImageSupplier = imageSupplier;
    }

    @Override
    protected long getModificationCount() {
        return ourTracker.getModificationCount() + DesktopIconLibraryManagerImpl.ourInstance.getModificationCount();
    }

    @Override
    protected Icon calcIcon() {
        Image image = myImageSupplier.get();
        myImage = image;
        return TargetAWT.to(image);
    }

    public synchronized Image getOrComputeImage() {
        getOrComputeIcon();
        return Objects.requireNonNull(myImage);
    }

    @Override
    public DesktopAWTImage copyWithNewSize(int width, int height) {
        return new DesktopLazyImageImpl(() -> ImageEffects.resize(getOrComputeImage(), width, height));
    }

    @Override
    public DesktopAWTImage copyWithForceLibraryId(String libraryId) {
        return new DesktopLazyImageImpl(() -> DesktopAWTImage.copyWithForceLibraryId(getOrComputeImage(), libraryId));
    }

    @Override
    public DesktopAWTImage copyGrayed() {
        return new DesktopLazyImageImpl(() -> DesktopAWTImage.copyGrayed(getOrComputeImage()));
    }
}
