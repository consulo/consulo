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
package consulo.desktop.awt.fileEditor.impl.text;

import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.ide.impl.fileEditor.text.TextEditorComponentContainer;
import consulo.ide.impl.fileEditor.text.TextEditorComponentContainerFactory;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 07/08/2021
 */
@Singleton
@ServiceImpl
public class DesktopTextEditorComponentContainerFactory implements TextEditorComponentContainerFactory {
  @Override
  public TextEditorComponentContainer createTextComponentContainer(Editor editor, Disposable parentDisposable, DataProvider dataProvider) {
    return new DesktopAwtTextEditorComponentContainer(editor, parentDisposable, dataProvider);
  }
}
