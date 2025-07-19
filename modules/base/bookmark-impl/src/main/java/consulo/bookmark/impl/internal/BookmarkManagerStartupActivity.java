/*
 * Copyright 2013-2022 consulo.io
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
package consulo.bookmark.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.bookmark.BookmarkManager;
import consulo.language.psi.event.PsiDocumentListener;
import consulo.project.Project;
import consulo.project.startup.PostStartupActivity;
import consulo.ui.UIAccess;

import jakarta.annotation.Nonnull;

@ExtensionImpl
public class BookmarkManagerStartupActivity implements PostStartupActivity, DumbAware {
  @Override
  public void runActivity(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
    project.getMessageBus().connect().subscribe(PsiDocumentListener.class, BookmarkManagerImpl::documentCreated);

    BookmarkManagerImpl manager = (BookmarkManagerImpl)BookmarkManager.getInstance(project);

    uiAccess.give(manager::registerAllFromState);
  }
}
