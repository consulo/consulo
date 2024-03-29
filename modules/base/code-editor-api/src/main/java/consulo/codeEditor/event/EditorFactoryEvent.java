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
package consulo.codeEditor.event;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import jakarta.annotation.Nonnull;

import java.util.EventObject;

public class EditorFactoryEvent extends EventObject {
  private final Editor myEditor;

  public EditorFactoryEvent(@Nonnull EditorFactory editorFactory, @Nonnull Editor editor) {
    super(editorFactory);
    myEditor = editor;
  }

  @Nonnull
  public EditorFactory getFactory(){
    return (EditorFactory) getSource();
  }

  @Nonnull
  public Editor getEditor() {
    return myEditor;
  }
}
