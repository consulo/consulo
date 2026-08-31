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
package consulo.ui.clipboard;

/**
 * What a frontend is able to do beyond plain reading and writing, which always exist.
 * <p>
 * This is what the <b>platform</b> is able to do, not what the <b>user</b> currently allows. A
 * platform may support a feature while the user has it turned off - a revoked browser permission, a
 * denied android clipboard read. That is not reported here, it arrives as a
 * {@link ClipboardAccessException} on the future of the call which was refused.
 *
 * @author VISTALL
 * @since 2026-08-07
 */
public enum ClipboardFeature {
    /**
     * {@link Clipboard#getAvailableTypes()} answers without pulling the payload.
     * <p>
     * AWT, Qt, Android. Not web - asking the browser what it holds is the same gated read as taking
     * the payload, so only the types this session wrote itself are reported.
     */
    AVAILABLE_TYPES,

    /**
     * A listener also hears changes made by other applications, so nothing has to poll. A listener
     * always hears changes made through this api, so registering one is never pointless.
     * <p>
     * AWT, Android. Not Qt, which does not subscribe to the clipboard change signal, and not web, where a page is never
     * told about the clipboard.
     */
    CONTENT_LISTENER,

    /**
     * A read needs no user gesture, no input focus and no permission grant, so it may be started from
     * a menu item or a background task rather than only from the paste shortcut.
     * <p>
     * AWT, Qt. Not web, which needs transient user activation, and not android, which needs input
     * focus or the default ime role.
     */
    UNRESTRICTED_READ
}
