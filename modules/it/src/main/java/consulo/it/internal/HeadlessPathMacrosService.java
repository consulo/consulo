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
import consulo.component.store.internal.SystemOnlyPathMacros;
import consulo.component.store.internal.SystemOnlyPathMacrosService;
import jakarta.inject.Singleton;

/**
 * Headless {@code PathMacrosService}: only system macros (no user-defined path macros), which is
 * enough for a temp-directory project store in tests.
 *
 * @author VISTALL
 */
@Singleton
@ServiceImpl
public class HeadlessPathMacrosService extends SystemOnlyPathMacrosService {
    public HeadlessPathMacrosService() {
        super(new SystemOnlyPathMacros());
    }
}
