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
package consulo.test.light.impl;

import consulo.ui.Component;
import consulo.ui.Window;
import consulo.ui.impl.BaseInputBoxBuilder;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Light tests have no UI: an input box fails its result.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
public class LightInputBoxBuilder<V, C extends Component> extends BaseInputBoxBuilder<V, C> {
    @Override
    public CompletableFuture<V> showAsync(@Nullable Window owner) {
        return CompletableFuture.failedFuture(new IllegalStateException("input boxes cannot be shown in light tests: " + myText.get()));
    }
}
