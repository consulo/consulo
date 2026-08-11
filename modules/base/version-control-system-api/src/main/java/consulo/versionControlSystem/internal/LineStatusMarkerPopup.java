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
package consulo.versionControlSystem.internal;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.details.InputDetails;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-11
 */
public interface LineStatusMarkerPopup {
    /**
     * Shows the popup for the range it was created for.
     *
     * @param details where the pointer was when the range was asked about, or null when the request came from an
     *                action rather than from a click - the popup is placed at the caret then
     */
    @RequiredUIAccess
    void showHintAt(@Nullable InputDetails details);

    /**
     * Brings the range into view first, so the popup is not placed against a line which is off screen.
     */
    @RequiredUIAccess
    void scrollAndShow();

    /**
     * Waits for a scroll which is already running to end and shows the popup then.
     */
    @RequiredUIAccess
    void showAfterScroll();
}
