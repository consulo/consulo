// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.folding;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.document.Document;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.editor.internal.CompositeFoldingBuilder;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToMany;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Allows a custom language plugin to define rules for folding code in the language handled
 * by the plugin.
 *
 * @author max
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface FoldingBuilder extends LanguageExtension {
  ExtensionPointCacheKey<FoldingBuilder, ByLanguageValue<List<FoldingBuilder>>> KEY = ExtensionPointCacheKey.create("FoldingBuilder", LanguageOneToMany.build(false));

  @Nonnull
  public static List<FoldingBuilder> forLanguage(@Nonnull Language language) {
    return Application.get().getExtensionPoint(FoldingBuilder.class).getOrBuildCache(KEY).requiredGet(language);
  }

  @Nullable
  public static FoldingBuilder forLanguageComposite(@Nonnull Language language) {
    List<FoldingBuilder> foldingBuilders = Application.get().getExtensionPoint(FoldingBuilder.class).getOrBuildCache(KEY).requiredGet(language);
    if (foldingBuilders.isEmpty()) {
      return null;
    }
    if (foldingBuilders.size() == 1) {
      return foldingBuilders.get(0);
    }
    return new CompositeFoldingBuilder(foldingBuilders);
  }

  /**
   * Builds the folding regions for the specified node in the AST tree and its children.
   * Note that you can have several folding regions for one AST node, i.e. several {@link FoldingDescriptor} with similar AST node.
   *
   * @param node     the node for which folding is requested.
   * @param document the document for which folding is built. Can be used to retrieve line
   *                 numbers for folding regions.
   * @return the array of folding descriptors.
   */
  @Nonnull
  @RequiredReadAction
  FoldingDescriptor[] buildFoldRegions(@Nonnull ASTNode node, @Nonnull Document document);

  /**
   * Returns the text which is displayed in the editor for the folding region related to the
   * specified node when the folding region is collapsed.
   *
   * @param node the node for which the placeholder text is requested.
   * @return the placeholder text.
   */
  @Nullable
  @RequiredReadAction
  String getPlaceholderText(@Nonnull ASTNode node);

  /**
   * Returns the default collapsed state for the folding region related to the specified node.
   *
   * @param node the node for which the collapsed state is requested.
   * @return true if the region is collapsed by default, false otherwise.
   */
  @RequiredReadAction
  boolean isCollapsedByDefault(@Nonnull ASTNode node);

  @RequiredReadAction
  default boolean isCollapsedByDefault(@Nonnull FoldingDescriptor foldingDescriptor) {
    return isCollapsedByDefault(foldingDescriptor.getElement());
  }
}
