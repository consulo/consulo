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

import consulo.ui.Toast;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * What only the host showing a {@link Toast} over the window decides: a toast inside a layout lies flat.
 *
 * @author VISTALL
 * @since 2026-10-16
 */
public interface ToastInternal extends Toast {
    @RequiredUIAccess
    void setShadow(boolean shadow);
}
