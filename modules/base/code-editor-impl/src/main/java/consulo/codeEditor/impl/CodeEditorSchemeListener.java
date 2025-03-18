/*
 * Copyright 2013-2025 consulo.io
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
package consulo.codeEditor.impl;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.codeEditor.EditorFactory;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.event.EditorColorsListener;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

/**
 * @author VISTALL
 * @since 2025-03-17
 */
@TopicImpl(ComponentScope.APPLICATION)
public class CodeEditorSchemeListener implements EditorColorsListener {
    private final Provider<EditorFactory> myEditorFactory;

    @Inject
    public CodeEditorSchemeListener(Provider<EditorFactory> editorFactory) {
        myEditorFactory = editorFactory;
    }

    @Override
    public void globalSchemeChange(EditorColorsScheme scheme) {
        myEditorFactory.get().refreshAllEditors();
    }
}
