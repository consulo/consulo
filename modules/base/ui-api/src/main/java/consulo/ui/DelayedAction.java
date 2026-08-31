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
import consulo.ui.event.ComponentEvent;
import consulo.ui.internal.UIInternal;

/**
 * Tells the user that something they asked for is being worked out, at the point they asked for it - the
 * position comes from the event's {@link consulo.ui.event.details.InputDetails}, so it lands wherever the
 * frontend put them.
 * <p>
 * It says nothing about what the work is or what is done with the result; start it when the work starts and
 * {@link #stop()} it when the work is done. What it looks like, and whether it is drawn at all, is entirely
 * the frontend's business.
 *
 * @author VISTALL
 * @since 2026-08-27
 */
public interface DelayedAction {
    @RequiredUIAccess
    static DelayedAction start(ComponentEvent<?> anchor) {
        return UIInternal.get()._DelayedAction_start(anchor);
    }

    @RequiredUIAccess
    void stop();
}
