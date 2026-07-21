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
package consulo.it.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.internal.ProgressActivityFactory;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * Headless {@link ProgressActivityFactory}: no OS-level activity (the real impl toggles macOS app
 * nap suppression), so no activity is created.
 *
 * @author VISTALL
 */
@Singleton
@ServiceImpl
public class HeadlessProgressActivityFactory implements ProgressActivityFactory {
    @Override
    public @Nullable Runnable createActivity() {
        return null;
    }
}
