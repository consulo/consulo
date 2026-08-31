/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.language.pratt;

import consulo.annotation.DeprecationInfo;
import consulo.language.parser.ITokenTypeRemapper;
import consulo.language.lexer.Lexer;
import consulo.language.ast.IElementType;
import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.ListIterator;

/**
 * @author peter
 */
public abstract class PrattBuilder {
    public abstract Lexer getLexer();

    public abstract void setTokenTypeRemapper(@Nullable ITokenTypeRemapper remapper);

    public abstract MutableMarker mark();

    public PrattBuilder createChildBuilder(int priority, LocalizeValue expectedMessage) {
        return createChildBuilder(priority).expecting(expectedMessage);
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public final PrattBuilder createChildBuilder(int priority, @Nullable String expectedMessage) {
        return createChildBuilder(priority, LocalizeValue.ofNullable(expectedMessage));
    }

    public PrattBuilder createChildBuilder(int priority) {
        return createChildBuilder().withLowestPriority(priority);
    }

    public @Nullable IElementType parseChildren(int priority, LocalizeValue expectedMessage) {
        return createChildBuilder(priority, expectedMessage).parse();
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public final @Nullable IElementType parseChildren(int priority, @Nullable String expectedMessage) {
        return parseChildren(priority, LocalizeValue.ofNullable(expectedMessage));
    }

    protected abstract PrattBuilder createChildBuilder();

    public boolean assertToken(PrattTokenType type) {
        if (checkToken(type)) {
            return true;
        }
        error(type.getExpectedText(this));
        return false;
    }

    public boolean assertToken(IElementType type, LocalizeValue errorMessage) {
        if (checkToken(type)) {
            return true;
        }
        error(errorMessage);
        return false;
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public final boolean assertToken(IElementType type, String errorMessage) {
        return assertToken(type, LocalizeValue.of(errorMessage));
    }

    public boolean checkToken(IElementType type) {
        if (isToken(type)) {
            advance();
            return true;
        }
        return false;
    }

    public abstract void advance();

    public abstract void error(LocalizeValue errorText);

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public final void error(String errorText) {
        error(LocalizeValue.of(errorText));
    }

    public boolean isEof() {
        return isToken(null);
    }

    public boolean isToken(@Nullable IElementType type) {
        return getTokenType() == type;
    }

    public abstract @Nullable IElementType getTokenType();

    public abstract @Nullable String getTokenText();

    public abstract void reduce(IElementType type);

    public ListIterator<IElementType> getBackResultIterator() {
        List<IElementType> resultTypes = getResultTypes();
        return resultTypes.listIterator(resultTypes.size());
    }

    public abstract List<IElementType> getResultTypes();

    public abstract @Nullable PrattBuilder getParent();

    public abstract int getPriority();

    public abstract int getCurrentOffset();

    public abstract PrattBuilder expecting(LocalizeValue expectedMessage);

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public final PrattBuilder expecting(@Nullable String expectedMessage) {
        return expecting(LocalizeValue.ofNullable(expectedMessage));
    }

    public abstract PrattBuilder withLowestPriority(int priority);

    public abstract @Nullable IElementType parse();
}
