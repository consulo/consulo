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
package consulo.language.codeStyle.template;

import consulo.language.ast.ASTNode;
import consulo.language.codeStyle.Alignment;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.Wrap;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;

/**
 * @author Alexey Chmutov
 * @since 2009-10-22
 */
public interface TemplateLanguageBlockFactory {
  TemplateLanguageBlock createTemplateLanguageBlock(@Nonnull ASTNode node,
                                                    @Nullable Wrap wrap,
                                                    @Nullable Alignment alignment,
                                                    @Nullable List<DataLanguageBlockWrapper> foreignChildren,
                                                    @Nonnull CodeStyleSettings codeStyleSettings);
}
