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

package consulo.language.editor.refactoring.rename;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.dataContext.DataContext;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;

import jakarta.annotation.Nonnull;

/**
 * @author dsl
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface RenameHandler extends RefactoringActionHandler {
  ExtensionPointName<RenameHandler> EP_NAME = ExtensionPointName.create(RenameHandler.class);

  // called during rename action update. should not perform any user interactions
  boolean isAvailableOnDataContext(DataContext dataContext);

  // called on rename actionPerformed. Can obtain additional info from user
  boolean isRenaming(DataContext dataContext);

  @Nonnull
  default String getActionTitle() {
    return toString();
  }
}
