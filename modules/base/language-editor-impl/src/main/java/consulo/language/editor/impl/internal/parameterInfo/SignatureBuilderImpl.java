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
package consulo.language.editor.impl.internal.parameterInfo;

import consulo.language.editor.parameterInfo.SignatureBuilder;
import consulo.language.editor.parameterInfo.SignatureStyle;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
final class SignatureBuilderImpl implements SignatureBuilder {
    private final Consumer<SignatureBuilderImpl> myApply;
    private final List<ParameterInfoSegment> mySegments = new ArrayList<>();
    private boolean myDisabled;
    private boolean myDeprecated;

    SignatureBuilderImpl(Consumer<SignatureBuilderImpl> apply) {
        myApply = apply;
    }

    @Override
    public SignatureBuilder text(String text, SignatureStyle... styles) {
        mySegments.add(new ParameterInfoSegment(ParameterInfoSegmentKind.TEXT, text, toSet(styles)));
        return this;
    }

    @Override
    public SignatureBuilder parameter(String text, SignatureStyle... styles) {
        mySegments.add(new ParameterInfoSegment(ParameterInfoSegmentKind.PARAMETER, text, toSet(styles)));
        return this;
    }

    @Override
    public SignatureBuilder comma() {
        return separator(",");
    }

    @Override
    public SignatureBuilder separator(String token) {
        if (token.isEmpty() || Character.isWhitespace(token.charAt(0)) || Character.isWhitespace(token.charAt(token.length() - 1))) {
            throw new IllegalArgumentException("Separator token must be a symbol without whitespace around it: '" + token + "'");
        }

        mySegments.add(new ParameterInfoSegment(ParameterInfoSegmentKind.SEPARATOR, token, Set.of()));
        return this;
    }

    @Override
    public SignatureBuilder separator() {
        mySegments.add(new ParameterInfoSegment(ParameterInfoSegmentKind.SEPARATOR, "", Set.of()));
        return this;
    }

    @Override
    public SignatureBuilder disabled() {
        myDisabled = true;
        return this;
    }

    @Override
    public SignatureBuilder deprecated() {
        myDeprecated = true;
        return this;
    }

    @Override
    public void apply() {
        myApply.accept(this);
    }

    List<ParameterInfoSegment> getSegments() {
        return List.copyOf(mySegments);
    }

    boolean isDisabled() {
        return myDisabled;
    }

    boolean isDeprecated() {
        return myDeprecated;
    }

    private static Set<SignatureStyle> toSet(SignatureStyle[] styles) {
        if (styles.length == 0) {
            return Set.of();
        }

        Set<SignatureStyle> set = EnumSet.noneOf(SignatureStyle.class);
        for (SignatureStyle style : styles) {
            set.add(style);
        }
        return set;
    }
}
