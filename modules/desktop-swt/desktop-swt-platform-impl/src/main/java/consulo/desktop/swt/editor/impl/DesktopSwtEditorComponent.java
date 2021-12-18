/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.swt.editor.impl;

import com.intellij.openapi.editor.Document;
import consulo.desktop.swt.ui.impl.SWTComponentDelegate;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;

/**
 * @author VISTALL
 * @since 18/12/2021
 */
public class DesktopSwtEditorComponent extends SWTComponentDelegate<StyledText> {
  private final Document myDocument;

  public DesktopSwtEditorComponent(Document document) {
    myDocument = document;
  }

  @Override
  protected StyledText createSWT(Composite parent) {
    return new StyledText(parent, SWT.DEFAULT);
  }

  @Override
  protected void initialize(StyledText component) {
    super.initialize(component);

    component.setText(myDocument.getText());
  }
}
