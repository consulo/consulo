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
package consulo.platform.impl.command.impl;

import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.impl.UndoManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectEx;

import javax.annotation.Nullable;
import javax.inject.Inject;

/**
 * @author VISTALL
 * @since 2018-07-24
 */
public class ProjectUndoManagerImpl extends UndoManagerImpl {
  @Inject
  public ProjectUndoManagerImpl(@Nullable Project project, CommandProcessor commandProcessor) {
    super((ProjectEx)project, commandProcessor);
  }
}
