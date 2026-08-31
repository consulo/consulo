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

import consulo.ui.image.Image;
import io.qt.gui.QPixmap;

/**
 * An image standing for another one it only knows at the moment it is asked - a lazy and a stated image both
 * answer with something else over their lifetime, so nothing may hold on to the delegate.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public interface DesktopQtDelegatingImage extends Image, DesktopQtImage {
    Image getDelegate();

    @Override
    default int getWidth() {
        return getDelegate().getWidth();
    }

    @Override
    default int getHeight() {
        return getDelegate().getHeight();
    }

    @Override
    default QPixmap toQPixmap() {
        Image delegate = getDelegate();
        if (delegate instanceof DesktopQtImage qtImage) {
            return qtImage.toQPixmap();
        }
        return DesktopQtEmptyImageImpl.createPixmap(delegate.getWidth(), delegate.getHeight());
    }
}
