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

import consulo.annotation.component.ServiceImpl;
import consulo.ide.impl.idea.openapi.editor.impl.EditorFactoryImpl;
import consulo.application.Application;
import consulo.codeEditor.EditorKind;
import consulo.codeEditor.RealEditor;
import consulo.document.Document;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 29/04/2021
 */
@Singleton
@ServiceImpl
public class DesktopSwtEditorFactoryImpl extends EditorFactoryImpl {
  @Inject
  public DesktopSwtEditorFactoryImpl(Application application) {
    super(application);
  }

  @Nonnull
  @Override
  protected RealEditor createEditorImpl(@Nonnull Document document, boolean isViewer, Project project, @Nonnull EditorKind kind) {
    return new DesktopSwtEditorImpl(document, isViewer, project, kind);
  }
}
