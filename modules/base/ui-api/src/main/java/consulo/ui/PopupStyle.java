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

/**
 * How serious the message a {@link LightPopup} carries is - the popup is filled and bordered in the colours of a
 * {@link Toast} of the same style. A popup without one is drawn plain.
 * <p/>
 * {@link #GLASSY} is not about the message - it goes together with any of the others.
 *
 * @author VISTALL
 * @since 2026-10-20
 */
public enum PopupStyle implements ComponentStyle {
    INFO,
    SUCCESS,
    WARNING,
    ERROR,
    /**
     * The fill of the popup lets a blurred view of what lies behind it through, the way a {@link ToastStyle#GLASSY}
     * toast does. The content stays solid.
     */
    GLASSY
}
