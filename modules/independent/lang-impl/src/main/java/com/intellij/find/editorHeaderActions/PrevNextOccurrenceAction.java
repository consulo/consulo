/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.find.editorHeaderActions;

import com.intellij.find.SearchSession;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

public abstract class PrevNextOccurrenceAction extends DumbAwareAction implements ContextAwareShortcutProvider {
  protected final boolean mySearch;

  PrevNextOccurrenceAction(@Nonnull String templateActionId, boolean search) {
    mySearch = search;
    copyFrom(ActionManager.getInstance().getAction(templateActionId));
  }

  @Override
  public final void update(AnActionEvent e) {
    SearchSession search = e.getData(SearchSession.KEY);
    boolean invokedByShortcut = !ActionPlaces.isToolbarPlace(e.getPlace());
    e.getPresentation().setEnabled(search != null && (invokedByShortcut || search.hasMatches()));
  }

  @Nullable
  @Override
  public final ShortcutSet getShortcut(@Nonnull DataContext context) {
    SearchSession search = context.getData(SearchSession.KEY);
    boolean singleLine = search != null && !search.getFindModel().isMultiline();
    return Utils.shortcutSetOf(singleLine ? ContainerUtil.concat(getDefaultShortcuts(), getSingleLineShortcuts()) : getDefaultShortcuts());
  }

  @Nonnull
  protected abstract List<Shortcut> getDefaultShortcuts();

  @Nonnull
  protected abstract List<Shortcut> getSingleLineShortcuts();
}
