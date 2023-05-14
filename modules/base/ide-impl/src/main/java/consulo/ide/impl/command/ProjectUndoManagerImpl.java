/*
 * Copyright 2013-2018 consulo.io
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
package consulo.ide.impl.command;

import consulo.annotation.component.ServiceImpl;
import consulo.ide.impl.idea.openapi.command.impl.UndoManagerImpl;
import consulo.project.Project;
import consulo.project.internal.ProjectEx;
import consulo.undoRedo.CommandProcessor;
import consulo.undoRedo.ProjectUndoManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2018-08-24
 */
@Singleton
@ServiceImpl
public class ProjectUndoManagerImpl extends UndoManagerImpl implements ProjectUndoManager {
  @Inject
  public ProjectUndoManagerImpl(@Nullable Project project, CommandProcessor commandProcessor) {
    super((ProjectEx)project, commandProcessor);
  }
}
