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
package com.intellij.formatting.templateLanguages;

import com.intellij.formatting.Alignment;
import com.intellij.formatting.Wrap;
import com.intellij.lang.ASTNode;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

/**
 * @author Alexey Chmutov
 *         Date: Oct 22, 2009
 *         Time: 6:56:55 PM
 */
public interface TemplateLanguageBlockFactory {
  TemplateLanguageBlock createTemplateLanguageBlock(@Nonnull ASTNode node,
                                                    @Nullable Wrap wrap,
                                                    @Nullable Alignment alignment,
                                                    @Nullable List<DataLanguageBlockWrapper> foreignChildren,
                                                    @Nonnull CodeStyleSettings codeStyleSettings);
}
