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

import consulo.ui.Window;
import consulo.ui.impl.BaseAlert;
import consulo.util.concurrent.AsyncResult;
import org.jspecify.annotations.Nullable;

/**
 * Dummy-but-creatable headless alert built on the shared {@link BaseAlert}.
 *
 * @author VISTALL
 */
public class HeadlessAlert<V> extends BaseAlert<V> {
    @Override
    public AsyncResult<V> showAsync(@Nullable Window component) {
        return AsyncResult.undefined();
    }
}
