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
package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInspection.unusedImport.UnusedImportLocalInspection;
import com.intellij.ide.highlighter.JavaHighlightingColors;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.colors.CodeInsightColors;

/**
 * @author anna
 *         Date: 01-Feb-2008
 */
public interface JavaHighlightInfoTypes {
  HighlightInfoType UNUSED_IMPORT = new HighlightInfoType.HighlightInfoTypeSeverityByKey(
    HighlightDisplayKey.findOrRegister(UnusedImportLocalInspection.SHORT_NAME, UnusedImportLocalInspection.DISPLAY_NAME),
    CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES);

  HighlightInfoType KEYWORD = new HighlightInfoType.HighlightInfoTypeImpl(HighlightSeverity.INFORMATION, JavaHighlightingColors.KEYWORD);

  HighlightInfoType ANNOTATION_NAME =
    new HighlightInfoType.HighlightInfoTypeImpl(HighlightInfoType.SYMBOL_TYPE_SEVERITY, JavaHighlightingColors.ANNOTATION_NAME);
  HighlightInfoType CLASS_NAME =
    new HighlightInfoType.HighlightInfoTypeImpl(HighlightInfoType.SYMBOL_TYPE_SEVERITY, JavaHighlightingColors.CLASS_NAME, false);

  HighlightInfoType ENUM_NAME =
    new HighlightInfoType.HighlightInfoTypeImpl(HighlightInfoType.SYMBOL_TYPE_SEVERITY, JavaHighlightingColors.ENUM_NAME, false);

  HighlightInfoType INTERFACE_NAME =
    new HighlightInfoType.HighlightInfoTypeImpl(HighlightInfoType.SYMBOL_TYPE_SEVERITY, JavaHighlightingColors.INTERFACE_NAME, false);

  HighlightInfoType ANONYMOUS_CLASS_NAME =
    new HighlightInfoType.HighlightInfoTypeImpl(HighlightInfoType.SYMBOL_TYPE_SEVERITY, JavaHighlightingColors.ANONYMOUS_CLASS_NAME, false);

  HighlightInfoType TYPE_PARAMETER_NAME =
    new HighlightInfoType.HighlightInfoTypeImpl(HighlightInfoType.SYMBOL_TYPE_SEVERITY, JavaHighlightingColors.TYPE_PARAMETER_NAME);

  HighlightInfoType ABSTRACT_CLASS_NAME =
    new HighlightInfoType.HighlightInfoTypeImpl(HighlightInfoType.SYMBOL_TYPE_SEVERITY, JavaHighlightingColors.ABSTRACT_CLASS_NAME, false);
}