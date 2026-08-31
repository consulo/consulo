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
package consulo.desktop.qt.ui.impl.image;

import consulo.ui.ex.UIModificationTracker;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtLazyImageImpl implements DesktopQtDelegatingImage {
    private final Supplier<Image> myImageSupplier;

    private @Nullable Image myImage;
    private long myModificationCount = -1;

    public DesktopQtLazyImageImpl(Supplier<Image> imageSupplier) {
        myImageSupplier = imageSupplier;
    }

    /**
     * The supplier answers out of the ui state of the moment, so a held answer is only good while that state
     * stands - which is what the tracker counts.
     */
    @Override
    public synchronized Image getDelegate() {
        long modificationCount = UIModificationTracker.getInstance().getModificationCount();

        Image image = myImage;
        if (image == null || myModificationCount != modificationCount) {
            image = myImageSupplier.get();
            myImage = image;
            myModificationCount = modificationCount;
        }

        return image;
    }
}
