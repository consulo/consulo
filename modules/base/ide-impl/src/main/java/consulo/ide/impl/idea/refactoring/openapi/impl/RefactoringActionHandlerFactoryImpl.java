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
package consulo.ide.impl.idea.refactoring.openapi.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import consulo.language.editor.refactoring.action.RefactoringActionHandlerFactory;
import consulo.language.editor.refactoring.move.MoveHandler;
import consulo.language.editor.refactoring.rename.PsiElementRenameHandler;
import consulo.language.editor.refactoring.safeDelete.SafeDeleteHandler;
import jakarta.inject.Singleton;

/**
 * @author dsl
 */
@Singleton
@ServiceImpl
public class RefactoringActionHandlerFactoryImpl extends RefactoringActionHandlerFactory {

  @Override
  public RefactoringActionHandler createSafeDeleteHandler() {
    return new SafeDeleteHandler();
  }

  @Override
  public RefactoringActionHandler createMoveHandler() {
    return new MoveHandler();
  }

  @Override
  public RefactoringActionHandler createRenameHandler() {
    return new PsiElementRenameHandler();
  }
}
