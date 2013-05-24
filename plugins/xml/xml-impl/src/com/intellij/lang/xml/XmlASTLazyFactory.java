/*
 * Copyright 2013 Consulo.org
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
package com.intellij.lang.xml;

import com.intellij.lang.ASTLazyFactory;
import com.intellij.psi.impl.source.tree.HtmlFileElement;
import com.intellij.psi.impl.source.tree.LazyParseableElement;
import com.intellij.psi.impl.source.tree.XmlFileElement;
import com.intellij.psi.templateLanguages.TemplateDataElementType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.ILazyParseableElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.psi.xml.XmlElementType.*;

/**
 * @author VISTALL
 * @since 2:39/02.04.13
 */
public class XmlASTLazyFactory implements ASTLazyFactory {
  @NotNull
  @Override
  public LazyParseableElement createLazy(ILazyParseableElementType type, CharSequence text) {
    if (type == XML_FILE) {
      return new XmlFileElement(type, text);
    }
    else if (type == DTD_FILE) {
      return new XmlFileElement(type, text);
    }
    else if (type == XHTML_FILE) {
      return new XmlFileElement(type, text);
    }
    else if (type == HTML_FILE) {
      return new HtmlFileElement(text);
    }
    else if (type instanceof TemplateDataElementType) {
      return new XmlFileElement(type, text);
    }
    return null;
  }

  @Override
  public boolean apply(@Nullable IElementType input) {
    return input == XML_FILE || input == DTD_FILE || input == XHTML_FILE || input == HTML_FILE || input instanceof TemplateDataElementType;
  }
}
