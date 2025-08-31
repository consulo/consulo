/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.language.editor.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.dumb.DumbAware;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.editor.folding.FoldingBuilder;
import consulo.language.editor.folding.FoldingBuilderEx;
import consulo.language.editor.folding.FoldingDescriptor;
import consulo.language.editor.folding.LanguageFolding;
import consulo.language.psi.PsiElement;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Used by LanguageFolding class if more than one FoldingBuilder were specified
 * for a particular language.
 *
 * @author Konstantin Bulenkov
 * @see LanguageFolding
 */
public class CompositeFoldingBuilder extends FoldingBuilderEx implements DumbAware {
  public static final Key<FoldingBuilder> FOLDING_BUILDER = Key.create("FOLDING_BUILDER");
  private final List<FoldingBuilder> myBuilders;

  public CompositeFoldingBuilder(List<FoldingBuilder> builders) {
    myBuilders = builders;
  }

  @Nonnull
  public List<FoldingBuilder> getAllBuilders() {
    return Collections.unmodifiableList(myBuilders);
  }

  @Override
  @RequiredReadAction
  @Nonnull
  public FoldingDescriptor[] buildFoldRegions(@Nonnull PsiElement root, @Nonnull Document document, boolean quick) {
    List<FoldingDescriptor> descriptors = new ArrayList<FoldingDescriptor>();

    for (FoldingBuilder builder : myBuilders) {
      for (FoldingDescriptor descriptor : LanguageFolding.buildFoldingDescriptors(builder, root, document, quick)) {
        descriptor.getElement().putUserData(FOLDING_BUILDER, builder);
        descriptors.add(descriptor);
      }
    }

    return descriptors.toArray(new FoldingDescriptor[descriptors.size()]);
  }

  @RequiredReadAction
  @Override
  public String getPlaceholderText(@Nonnull ASTNode node, @Nonnull TextRange range) {
    FoldingBuilder builder = node.getUserData(FOLDING_BUILDER);
    return builder == null ? node.getText() : builder instanceof FoldingBuilderEx ? ((FoldingBuilderEx)builder).getPlaceholderText(node, range) : builder.getPlaceholderText(node);
  }

  @RequiredReadAction
  @Override
  public String getPlaceholderText(@Nonnull ASTNode node) {
    FoldingBuilder builder = node.getUserData(FOLDING_BUILDER);
    return builder == null ? node.getText() : builder.getPlaceholderText(node);
  }

  @Override
  @RequiredReadAction
  public boolean isCollapsedByDefault(@Nonnull ASTNode node) {
    FoldingBuilder builder = node.getUserData(FOLDING_BUILDER);
    return builder != null && builder.isCollapsedByDefault(node);
  }

  @Nonnull
  @Override
  public Language getLanguage() {
    throw new UnsupportedOperationException();
  }
}
