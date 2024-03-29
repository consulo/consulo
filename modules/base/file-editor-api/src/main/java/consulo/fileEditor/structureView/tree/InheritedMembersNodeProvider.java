/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.fileEditor.structureView.tree;

import consulo.application.AllIcons;
import consulo.fileEditor.FileEditorBundle;
import consulo.ui.ex.action.Shortcut;

import jakarta.annotation.Nonnull;

/**
 * @author Konstantin Bulenkov
 */
public abstract class InheritedMembersNodeProvider<T extends TreeElement> implements FileStructureNodeProvider<T>, ActionShortcutProvider {
  public static final String ID = "SHOW_INHERITED";

  @Nonnull
  @Override
  public String getCheckBoxText() {
    return FileEditorBundle.message("file.structure.toggle.show.inherited");
  }

  @Nonnull
  @Override
  public Shortcut[] getShortcut() {
    throw new UnsupportedOperationException("see getActionIdForShortcut()");
  }

  @Nonnull
  @Override
  public String getActionIdForShortcut() {
    return "FileStructurePopup";
  }

  @Override
  @Nonnull
  public ActionPresentation getPresentation() {
    return new ActionPresentationData(FileEditorBundle.message("action.structureview.show.inherited"), null, AllIcons.Hierarchy.Supertypes);
  }

  @Override
  @Nonnull
  public String getName() {
    return ID;
  }
}
