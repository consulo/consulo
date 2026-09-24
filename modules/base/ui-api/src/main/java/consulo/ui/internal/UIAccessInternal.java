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
package consulo.ui.internal;

import consulo.ui.UIAccess;

import java.util.function.BooleanSupplier;

/**
 * @author VISTALL
 * @since 2026-09-24
 */
public interface UIAccessInternal extends UIAccess {
    UIAccess makeProtection(BooleanSupplier disposed);

    void releaseProtection();

    UIAccess getOriginal();

    static UIAccess original(UIAccess uiAccess) {
        return uiAccess instanceof UIAccessInternal internal ? internal.getOriginal() : uiAccess;
    }
}
