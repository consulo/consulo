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
package consulo.execution.debug.step;

import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.frame.XSuspendContext;
import consulo.localize.LocalizeValue;

import java.util.concurrent.CompletableFuture;

/**
 * Implemented by a {@link consulo.execution.debug.XDebugProcess} which can move the execution point of the suspended thread
 * to another line - the Jump To Cursor action is hidden for other processes.
 *
 * @author VISTALL
 * @since 2026-11-17
 */
public interface HasSetNextStatement {
    /**
     * Makes {@code position} the next statement executed by the thread of {@code context}.
     *
     * @return a future of an empty value when the execution point moved, otherwise of the reason why it did not
     */
    CompletableFuture<LocalizeValue> startSetNextStatement(XSuspendContext context, XSourcePosition position);
}
