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

import consulo.language.ast.IElementType;
import consulo.language.lexer.Lexer;
import consulo.language.localize.LanguageLocalize;
import consulo.language.parser.ITokenTypeRemapper;
import consulo.language.parser.PsiBuilder;
import consulo.util.lang.Trinity;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * @author peter
 */
public class PrattBuilderImpl extends PrattBuilder {
  private final PsiBuilder myBuilder;
  private final @Nullable PrattBuilder myParentBuilder;
  private final PrattRegistry myRegistry;
  private final LinkedList<IElementType> myLeftSiblings = new LinkedList<>();
  private boolean myParsingStarted;
  private @Nullable String myExpectedMessage = null;
  private int myPriority = Integer.MIN_VALUE;
  private @Nullable MutableMarker myStartMarker = null;

  private PrattBuilderImpl(PsiBuilder builder, @Nullable PrattBuilder parent, PrattRegistry registry) {
    myBuilder = builder;
    myParentBuilder = parent;
    myRegistry = registry;
  }

  public static PrattBuilder createBuilder(PsiBuilder builder, PrattRegistry registry) {
    return new PrattBuilderImpl(builder, null, registry);
  }

  @Override
  public PrattBuilder expecting(@Nullable String expectedMessage) {
    myExpectedMessage = expectedMessage;
    return this;
  }

  @Override
  public PrattBuilder withLowestPriority(int priority) {
    myPriority = priority;
    return this;
  }

  @Override
  public Lexer getLexer() {
    return myBuilder.getLexer();
  }

  @Override
  public void setTokenTypeRemapper(@Nullable ITokenTypeRemapper remapper) {
    myBuilder.setTokenTypeRemapper(remapper);
  }

  @Override
  public MutableMarker mark() {
    return new MutableMarker(myLeftSiblings, myBuilder.mark(), myLeftSiblings.size());
  }

  @Override
  public @Nullable IElementType parse() {
    checkParsed();
    return myLeftSiblings.size() != 1 ? null : myLeftSiblings.getLast();
  }

  @Override
  protected PrattBuilder createChildBuilder() {
    assert myParsingStarted;
    return new PrattBuilderImpl(myBuilder, this, myRegistry) {
      @Override
      protected void doParse() {
        super.doParse();
        PrattBuilderImpl.this.myLeftSiblings.addAll(getResultTypes());
      }
    };
  }

  protected void doParse() {
    if (isEof()) {
      error(myExpectedMessage != null ? myExpectedMessage : LanguageLocalize.unexpectedEof().get());
      return;
    }

    TokenParser parser = findParser();
    if (parser == null) {
      error(myExpectedMessage != null ? myExpectedMessage : LanguageLocalize.unexpectedToken().get());
      return;
    }

    myStartMarker = mark();
    while (!isEof()) {
      int startOffset = myBuilder.getCurrentOffset();

      if (!parser.parseToken(this)) break;

      assert startOffset < myBuilder.getCurrentOffset() : "Endless loop on " + getTokenType();

      parser = findParser();
      if (parser == null) break;
    }
    myStartMarker.drop();
  }

  private @Nullable TokenParser findParser() {
    IElementType tokenType = getTokenType();
    Collection<Trinity<Integer, PathPattern, TokenParser>> parsers = myRegistry.getParsers(tokenType);
    if (parsers != null) {
      for (Trinity<Integer, PathPattern, TokenParser> trinity : parsers) {
        if (trinity.first > myPriority && trinity.second.accepts(this)) {
          return trinity.third;
        }
      }
    }
    return null;
  }

  @Override
  public void advance() {
    myLeftSiblings.addLast(getTokenType());
    myBuilder.advanceLexer();
  }

  @Override
  public void error(String errorText) {
    PsiBuilder.Marker marker = myBuilder.mark();
    myBuilder.error(errorText);
    marker.drop();
  }

  @Override
  public @Nullable IElementType getTokenType() {
    return myBuilder.getTokenType();
  }

  @Override
  public @Nullable String getTokenText() {
    return myBuilder.getTokenText();
  }

  @Override
  public void reduce(IElementType type) {
    Objects.requireNonNull(myStartMarker).finish(type);
    myStartMarker = myStartMarker.precede();
  }

  @Override
  public List<IElementType> getResultTypes() {
    checkParsed();
    return myLeftSiblings;
  }

  private void checkParsed() {
    if (!myParsingStarted) {
      myParsingStarted = true;
      doParse();
    }
  }

  @Override
  public @Nullable PrattBuilder getParent() {
    return myParentBuilder;
  }

  @Override
  public int getPriority() {
    return myPriority;
  }

  @Override
  public int getCurrentOffset() {
    return myBuilder.getCurrentOffset();
  }
}
