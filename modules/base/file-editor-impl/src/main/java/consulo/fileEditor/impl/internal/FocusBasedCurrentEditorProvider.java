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
package consulo.fileEditor.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.dataContext.DataManager;
import consulo.fileEditor.CurrentEditorProvider;
import consulo.fileEditor.FileEditor;
import consulo.ui.Component;
import consulo.ui.FocusManager;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

@Singleton
@ServiceImpl
public class FocusBasedCurrentEditorProvider implements CurrentEditorProvider {
    @Override
    public @Nullable FileEditor getCurrentEditor() {
        Component component = FocusManager.get().getFocusedComponent();
        return component == null ? null : DataManager.getInstance().getDataContext(component).getData(FileEditor.KEY);
    }
}