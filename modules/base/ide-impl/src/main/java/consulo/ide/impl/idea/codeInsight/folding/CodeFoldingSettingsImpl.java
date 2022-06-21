/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package consulo.ide.impl.idea.codeInsight.folding;

import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.language.editor.folding.CodeFoldingSettings;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.inject.Singleton;

/**
 * @author yole
 */
@Singleton
@State(name = "CodeFoldingSettings", storages = @Storage("editor.codeinsight.xml"))
@ServiceImpl
public class CodeFoldingSettingsImpl extends CodeFoldingSettings implements PersistentStateComponent<CodeFoldingSettings> {

  @Override
  public CodeFoldingSettings getState() {
    return this;
  }

  @Override
  public void loadState(final CodeFoldingSettings state) {
    XmlSerializerUtil.copyBean(state, this);
  }
}
