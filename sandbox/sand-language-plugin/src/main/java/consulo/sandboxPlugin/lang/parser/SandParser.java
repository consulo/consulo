/*
 * Copyright 2013-2016 consulo.io
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
package consulo.sandboxPlugin.lang.parser;

import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.parser.PsiBuilder;
import consulo.language.parser.PsiBuilderUtil;
import consulo.language.parser.PsiParser;
import consulo.language.version.LanguageVersion;
import consulo.sandboxPlugin.lang.psi.SandElements;
import consulo.sandboxPlugin.lang.psi.SandTokens;
import consulo.util.lang.Pair;

import java.util.List;

/**
 * C#-preprocessor model: the parse runs over {@link SandBuilderWrapper}, which evaluates
 * directives against the seeded flag variables and hides disabled-branch content behind
 * {@link SandTokens#NON_ACTIVE_SYMBOL} — the parser only ever sees directives and enabled
 * declarations, flat. Disabled blocks produce no PSI and no errors; a directive that is itself
 * inside a disabled region still becomes a directive element but never emits error elements.
 * <p>
 * Enabled-region garbage degrades with run granularity: a contiguous run of tokens that starts
 * no declaration or directive is consumed into one opaque {@link SandElements#RAW_BLOCK}
 * (no stubs, no error markers) and parsing resumes at the next declaration or directive start.
 *
 * @author VISTALL
 * @since 19.03.14
 */
public class SandParser implements PsiParser {
  private List<Pair<IElementType, IElementType>> myPairs;

  public SandParser(List<Pair<IElementType, IElementType>> list) {
    myPairs = list;
  }

  @Override
  public ASTNode parse(IElementType root, PsiBuilder builder, LanguageVersion languageVersion) {
    SandBuilderWrapper wrapper = new SandBuilderWrapper(builder);
    PsiBuilder.Marker mark = wrapper.mark();
    parseDeclarations(wrapper);
    mark.done(root);
    return builder.getTreeBuilt();
  }

  private void parseDeclarations(SandBuilderWrapper builder) {
    while (!builder.eof()) {
      IElementType token = builder.getTokenType();

      if (token == SandTokens.IF_KEYWORD || token == SandTokens.IFNDEF_KEYWORD || token == SandTokens.ELIF_KEYWORD
        || token == SandTokens.FLAG_KEYWORD || token == SandTokens.UNDEF_KEYWORD) {
        parseNamedDirective(builder, namedDirectiveElement(token));
        continue;
      }

      if (token == SandTokens.ELSE_KEYWORD || token == SandTokens.END_KEYWORD) {
        PsiBuilder.Marker directive = builder.mark();
        builder.advanceLexer();
        directive.done(token == SandTokens.ELSE_KEYWORD ? SandElements.ELSE_DIRECTIVE : SandElements.END_DIRECTIVE);
        continue;
      }

      if (token == SandTokens.INCLUDE_KEYWORD) {
        PsiBuilder.Marker directive = builder.mark();
        builder.advanceLexer();
        if (!PsiBuilderUtil.expect(builder, SandTokens.STRING_LITERAL) && !builder.isCurrentDirectiveDisabled()) {
          builder.error("Include path expected");
        }
        directive.done(SandElements.INCLUDE_DIRECTIVE);
        continue;
      }

      boolean find = false;
      for (Pair<IElementType, IElementType> pair : myPairs) {
        if (token == pair.getFirst()) {
          PsiBuilder.Marker defMark = builder.mark();
          builder.advanceLexer();

          if (!PsiBuilderUtil.expect(builder, SandTokens.IDENTIFIER)) {
            builder.error("Identifier expected");
          }

          if (builder.getTokenType() == SandTokens.COLON) {
            builder.advanceLexer();
            if (builder.getTokenType() == SandTokens.IDENTIFIER) {
              PsiBuilder.Marker extendsRef = builder.mark();
              builder.advanceLexer();
              extendsRef.done(SandElements.EXTENDS_REF);
            }
            else {
              builder.error("Identifier expected");
            }
          }

          PsiBuilderUtil.expect(builder, SandTokens.LBRACE);

          while (builder.getTokenType() == SandTokens.STRING_LITERAL) {
            PsiBuilder.Marker stringExp = builder.mark();
            builder.advanceLexer();
            stringExp.done(SandElements.STRING_EXPRESSION);
          }

          PsiBuilderUtil.expect(builder, SandTokens.RBRACE);

          defMark.done(pair.getSecond());
          find = true;
        }
      }

      if (!find) {
        parseRawRun(builder);
      }
    }
  }

  private static IElementType namedDirectiveElement(IElementType token) {
    if (token == SandTokens.IF_KEYWORD) {
      return SandElements.IF_DIRECTIVE;
    }
    if (token == SandTokens.IFNDEF_KEYWORD) {
      return SandElements.IFNDEF_DIRECTIVE;
    }
    if (token == SandTokens.ELIF_KEYWORD) {
      return SandElements.ELIF_DIRECTIVE;
    }
    if (token == SandTokens.FLAG_KEYWORD) {
      return SandElements.FLAG_DIRECTIVE;
    }
    return SandElements.UNDEF_DIRECTIVE;
  }

  private void parseNamedDirective(SandBuilderWrapper builder, IElementType elementType) {
    PsiBuilder.Marker directive = builder.mark();
    builder.advanceLexer();
    if (!PsiBuilderUtil.expect(builder, SandTokens.IDENTIFIER) && !builder.isCurrentDirectiveDisabled()) {
      builder.error("Flag name expected");
    }
    directive.done(elementType);
  }

  /**
   * Consumes a contiguous run of tokens that starts no declaration and no directive into one
   * opaque {@link SandElements#RAW_BLOCK} - no stubs, no error markers. The decision is
   * content-driven, so the parse stays a pure function of the file and its seed.
   */
  private void parseRawRun(SandBuilderWrapper builder) {
    PsiBuilder.Marker raw = builder.mark();
    while (!builder.eof() && !isRecoveryToken(builder.getTokenType())) {
      builder.advanceLexer();
    }
    raw.done(SandElements.RAW_BLOCK);
  }

  private boolean isRecoveryToken(IElementType token) {
    if (token == SandTokens.IF_KEYWORD || token == SandTokens.IFNDEF_KEYWORD
      || token == SandTokens.ELIF_KEYWORD || token == SandTokens.ELSE_KEYWORD || token == SandTokens.END_KEYWORD
      || token == SandTokens.FLAG_KEYWORD || token == SandTokens.UNDEF_KEYWORD || token == SandTokens.INCLUDE_KEYWORD) {
      return true;
    }
    for (Pair<IElementType, IElementType> pair : myPairs) {
      if (token == pair.getFirst()) {
        return true;
      }
    }
    return false;
  }
}
