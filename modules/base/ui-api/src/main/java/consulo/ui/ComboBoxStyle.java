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
 * @author VISTALL
 * @since 2026-09-21
 */
public enum ComboBoxStyle implements ComponentStyle {
    /**
     * Drops the fill the control carries so it reads as a part of whatever it stands on - a toolbar, a header -
     * rather than as a field of its own.
     */
    TRANSPARENT_BACKGROUND,

    /**
     * For a control which sits inside another surface rather than in a form of its own - the scope chooser of a
     * find popup, the thread chooser of a debugger frame. It keeps its fill, so it still reads as something to
     * press, and loses the chrome which would separate it from its host: the border, the corners and the ring
     * drawn around it while it holds the focus.
     */
    INPLACE
}
