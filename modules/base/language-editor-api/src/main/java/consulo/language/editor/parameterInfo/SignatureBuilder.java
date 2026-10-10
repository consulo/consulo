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
 * Describes one overload of a parameter info popup. All text is plain - it is never read as markup, and how it looks is
 * decided by the platform. Parameters are numbered in the order they are added; the current one is the one marked
 * {@link SignatureStyle#HIGHLIGHT}.
 * <p/>
 * Spacing around separators belongs to the platform: a separator is given as its token only, and a long signature may
 * only be broken into lines after a separator.
 * <pre>
 * context.signature()
 *     .parameter("int a")
 *     .comma()
 *     .parameter("String b", SignatureStyle.HIGHLIGHT)
 *     .apply();
 * </pre>
 *
 * @author VISTALL
 * @see ParameterInfoUIContext#signature()
 * @since 2026-10-10
 */
public interface SignatureBuilder {
    /**
     * Text which is not a parameter - a prefix, a suffix, a message such as {@code <no parameters>}. Shown exactly as given.
     */
    SignatureBuilder text(String text, SignatureStyle... styles);

    SignatureBuilder parameter(String text, SignatureStyle... styles);

    SignatureBuilder comma();

    /**
     * @param token the separator symbol itself, without any whitespace around it
     * @throws IllegalArgumentException if the token is empty or has whitespace around it
     */
    SignatureBuilder separator(String token);

    /**
     * A boundary between parameters which has no symbol of its own, for languages whose arguments are separated by
     * whitespace only.
     */
    SignatureBuilder separator();

    /**
     * The whole overload is not applicable.
     */
    SignatureBuilder disabled();

    /**
     * The whole overload is deprecated.
     */
    SignatureBuilder deprecated();

    /**
     * Makes this the presentation of the overload the context is currently describing.
     */
    void apply();
}
