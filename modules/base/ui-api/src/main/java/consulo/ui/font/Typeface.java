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
package consulo.ui.font;

/**
 * A family the environment can render, as {@link FontManager#getAvailableTypefacesAsync} lists them. What it
 * says was true when it was asked for and is not kept - a family installed or removed since is seen by asking
 * again, not by holding one of these.
 *
 * @author VISTALL
 * @since 2026-09-04
 */
public interface Typeface {
    String getName();

    /**
     * Whether the glyphs of the family are all of one width, which is what an editor font has to be. An
     * environment which will not answer says no rather than guessing.
     */
    boolean isMonospaced();
}
