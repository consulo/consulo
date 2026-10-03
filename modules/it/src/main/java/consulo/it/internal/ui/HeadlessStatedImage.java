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
import consulo.ui.image.ImageState;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * @author VISTALL
 */
public final class HeadlessStatedImage<S> implements Image {
    private final ImageState<S> myState;
    private final Function<S, Image> myImageFunction;

    public HeadlessStatedImage(ImageState<S> state, Function<S, Image> imageFunction) {
        myState = state;
        myImageFunction = imageFunction;
    }

    public Image resolve() {
        return myImageFunction.apply(myState.getState());
    }

    @Override
    public int getWidth() {
        return resolve().getWidth();
    }

    @Override
    public int getHeight() {
        return resolve().getHeight();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return this == o || resolve().equals(HeadlessImages.unwrap(o));
    }

    @Override
    public int hashCode() {
        return resolve().hashCode();
    }

    @Override
    public String toString() {
        return "stated[" + resolve() + "]";
    }
}
