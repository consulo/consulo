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

import consulo.document.Document;
import consulo.desktop.swt.ui.impl.SWTComponentDelegate;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

/**
 * @author VISTALL
 * @since 18/12/2021
 */
public class DesktopSwtEditorComponent extends SWTComponentDelegate<Text> {
  private final Document myDocument;

  public DesktopSwtEditorComponent(Document document) {
    myDocument = document;
  }

  @Override
  protected Text createSWT(Composite parent) {
    return new Text(parent, SWT.DEFAULT);
  }

  @Override
  protected void initialize(Text component) {
    super.initialize(component);

    component.setText(myDocument.getText());
  }
}
