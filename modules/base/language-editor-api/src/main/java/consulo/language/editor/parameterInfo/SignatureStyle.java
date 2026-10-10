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
package consulo.language.editor.parameterInfo;

/**
 * What a piece of a signature means. How each one looks is decided by the theme, never by the handler.
 *
 * @author VISTALL
 * @see SignatureBuilder
 * @since 2026-10-10
 */
public enum SignatureStyle {
    /**
     * The parameter the caret is at.
     */
    HIGHLIGHT,
    /**
     * Shown but not applicable - an implicit parameter, an argument which does not fit.
     */
    DISABLED,
    /**
     * Shown struck out - a deprecated parameter.
     */
    STRIKEOUT
}
