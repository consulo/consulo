// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.codeStyle;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToMany;
import consulo.language.parser.ParserDefinition;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.util.collection.ContainerUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;

/**
 * Allows a custom language plugin to build a formatting model for a file in the language, or
 * for a portion of a file.
 * A formatting model defines how a file is broken into non-whitespace blocks and different
 * types of whitespace (alignment, indents and wraps) between them.
 * <p>For certain aspects of the custom formatting to work properly, it is recommended to use TokenType.WHITE_SPACE
 * as the language's whitespace tokens. See {@link ParserDefinition}
 *
 * @apiNote in case you getting a {@link StackOverflowError}, with your builder, most likely you haven't implemented any model building
 * methods. Please, implement {@link #createModel(FormattingContext)}
 * @see FormattingModelProvider#createFormattingModelForPsiFile(PsiFile, Block, CodeStyleSettings)
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface FormattingModelBuilder extends LanguageExtension {
  ExtensionPointCacheKey<FormattingModelBuilder, ByLanguageValue<List<FormattingModelBuilder>>> KEY = ExtensionPointCacheKey.create("FormattingModelBuilder", LanguageOneToMany.build(false));

  @Nonnull
  static List<FormattingModelBuilder> forLanguage(@Nonnull Language language) {
    return Application.get().getExtensionPoint(FormattingModelBuilder.class).getOrBuildCache(KEY).requiredGet(language);
  }

  @Nullable
  @RequiredReadAction
  static FormattingModelBuilder forContext(@Nonnull PsiElement context) {
    return forContext(context.getLanguage(), context);
  }

  @Nullable
  @RequiredReadAction
  static FormattingModelBuilder forContext(@Nonnull Language language, @Nonnull PsiElement context) {
    for (LanguageFormattingRestriction each : LanguageFormattingRestriction.EXTENSION.getExtensionList()) {
      if (!each.isFormatterAllowed(context)) return null;
    }
    for (FormattingModelBuilder builder : forLanguage(language)) {
      if (builder instanceof CustomFormattingModelBuilder) {
        final CustomFormattingModelBuilder custom = (CustomFormattingModelBuilder)builder;
        if (custom.isEngagedToFormat(context)) return builder;
      }
    }

    return ContainerUtil.getFirstItem(forLanguage(language));
  }

  /**
   * Requests building the formatting model for a section of the file containing
   * the specified PSI element and its children.
   *
   * @return the formatting model for the file.
   * @see FormattingContext
   */
  @Nonnull
  FormattingModel createModel(@Nonnull FormattingContext formattingContext);

  /**
   * Returns the TextRange which should be processed by the formatter in order to detect proper indent options.
   *
   * @param file            the file in which the line break is inserted.
   * @param offset          the line break offset.
   * @param elementAtOffset the parameter at {@code offset}
   * @return the range to reformat, or null if the default range should be used
   */
  @Nullable
  default TextRange getRangeAffectingIndent(PsiFile file, int offset, ASTNode elementAtOffset) {
    return null;
  }
}
