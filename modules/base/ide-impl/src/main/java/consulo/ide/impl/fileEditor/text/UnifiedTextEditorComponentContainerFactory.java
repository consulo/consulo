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
package consulo.ide.impl.fileEditor.text;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.dataContext.DataProvider;
import consulo.codeEditor.Editor;
import consulo.disposer.Disposable;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 07/08/2021
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
public class UnifiedTextEditorComponentContainerFactory implements TextEditorComponentContainerFactory {

  @Override
  public TextEditorComponentContainer createTextComponentContainer(Editor editor, Disposable parentDisposable, DataProvider dataProvider) {
    return new UnifiedTextEditorComponentContainer(editor, parentDisposable, dataProvider);
  }
}
