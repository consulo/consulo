/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.patterns;

import com.intellij.patterns.compiler.PatternCompilerFactory;
import consulo.language.psi.PsiElement;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.util.xml.serializer.annotation.Tag;
import consulo.util.xml.serializer.annotation.Text;
import javax.annotation.Nullable;

@Tag("pattern")
public class ElementPatternBean {

  @Attribute("type")
  public String type;

  @Text
  public String text;

  @Nullable
  public ElementPattern<PsiElement> compilePattern() {
    return PatternCompilerFactory.getFactory().<PsiElement>getPatternCompiler(type).compileElementPattern(text);
  }
}
