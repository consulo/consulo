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
package consulo.ui;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.clipboard.DataTransfer;

/**
 * Takes drops made onto a component which is not made of items, see {@link Component#setDropHandler}.
 *
 * @author VISTALL
 * @since 2026-10-12
 */
@FunctionalInterface
public interface DropHandler {
    /**
     * Called while the drop is only offered with {@code checkOnly} set, then again to carry it out. Both passes run
     * the same decision, so a handler never states its rules twice, and must not change anything until the second.
     *
     * @return whether the drop is, or was, possible
     */
    @RequiredUIAccess
    boolean drop(Component component, DataTransfer transfer, boolean checkOnly);
}
