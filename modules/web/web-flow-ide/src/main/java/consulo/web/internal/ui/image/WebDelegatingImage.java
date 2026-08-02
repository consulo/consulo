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
package consulo.web.internal.ui.image;

import consulo.ui.image.Image;

/**
 * An image standing for another one it only knows at the moment it is asked. The delegate is never held on
 * to by anything building a spec out of it, since a lazy and a stated image both answer with something else
 * over their lifetime.
 *
 * @author VISTALL
 * @since 2026-08-02
 */
public interface WebDelegatingImage extends Image {
    Image getDelegate();

    @Override
    default int getWidth() {
        return getDelegate().getWidth();
    }

    @Override
    default int getHeight() {
        return getDelegate().getHeight();
    }

    static Image unwrap(Image image) {
        Image current = image;
        while (current instanceof WebDelegatingImage delegating) {
            current = delegating.getDelegate();
        }
        return current;
    }
}
