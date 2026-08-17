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
import consulo.ui.image.ImageState;

import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtStatedImageImpl<S> implements DesktopQtDelegatingImage {
    private final ImageState<S> myState;
    private final Function<S, Image> myImageFunction;

    public DesktopQtStatedImageImpl(ImageState<S> state, Function<S, Image> imageFunction) {
        myState = state;
        myImageFunction = imageFunction;
    }

    @Override
    public Image getDelegate() {
        return myImageFunction.apply(myState.getState());
    }
}
