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

package com.intellij.application.options.editor;

import com.intellij.codeInsight.folding.CodeFoldingSettings;
import com.intellij.openapi.options.Configurable;
import consulo.disposer.Disposable;
import consulo.options.SimpleConfigurableByProperties;
import consulo.platform.base.localize.ApplicationLocalize;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;

import javax.annotation.Nonnull;

/**
 * @author yole
 */
public class GeneralCodeFoldingConfigurable extends SimpleConfigurableByProperties implements Configurable {
  @RequiredUIAccess
  @Nonnull
  @Override
  protected Component createLayout(PropertyBuilder propertyBuilder, @Nonnull Disposable uiDisposable) {
    VerticalLayout verticalLayout = VerticalLayout.create();

    CodeFoldingSettings settings = CodeFoldingSettings.getInstance();

    CheckBox fileHeaderBox = CheckBox.create(ApplicationLocalize.checkboxCollapseFileHeader());
    verticalLayout.add(fileHeaderBox);
    propertyBuilder.add(fileHeaderBox, () -> settings.COLLAPSE_FILE_HEADER, val -> settings.COLLAPSE_FILE_HEADER = val);

    CheckBox importsBox = CheckBox.create(ApplicationLocalize.checkboxCollapseTitleImports());
    verticalLayout.add(importsBox);
    propertyBuilder.add(importsBox, () -> settings.COLLAPSE_IMPORTS, val -> settings.COLLAPSE_IMPORTS = val);

    CheckBox docCommentsBox = CheckBox.create(ApplicationLocalize.checkboxCollapseJavadocComments());
    verticalLayout.add(docCommentsBox);
    propertyBuilder.add(docCommentsBox, () -> settings.COLLAPSE_DOC_COMMENTS, val -> settings.COLLAPSE_DOC_COMMENTS = val);

    CheckBox methodsBox = CheckBox.create(ApplicationLocalize.checkboxCollapseMethodBodies());
    verticalLayout.add(methodsBox);
    propertyBuilder.add(methodsBox, () -> settings.COLLAPSE_METHODS, val -> settings.COLLAPSE_METHODS = val);

    return verticalLayout;
  }
}
