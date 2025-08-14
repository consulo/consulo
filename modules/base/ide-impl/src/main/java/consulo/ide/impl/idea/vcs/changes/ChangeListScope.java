// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.vcs.changes;

import consulo.application.AllIcons;
import consulo.component.util.WeighedItem;
import consulo.content.scope.FilteredNamedScope;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.localize.VcsLocalize;
import jakarta.annotation.Nonnull;

public final class ChangeListScope extends FilteredNamedScope implements WeighedItem {
  static final String ALL_CHANGED_FILES_SCOPE_NAME = "All Changed Files";

  public ChangeListScope(@Nonnull ChangeListManager manager) {
    super(ALL_CHANGED_FILES_SCOPE_NAME, VcsLocalize.scopeNameChangelistAllChangedFiles(), PlatformIconGroup.scopeChangedfilesall(), 0, manager::isFileAffected);
  }

  public ChangeListScope(@Nonnull ChangeListManager manager, @Nonnull String scopeId, @Nonnull LocalizeValue presentableName) {
    super(scopeId, presentableName, PlatformIconGroup.scopeChangedfiles(), 0, file -> manager.getChangeLists(file).stream().anyMatch(list -> list.getName().equals(scopeId)));
  }

  @Override
  public int hashCode() {
    return getScopeId().hashCode();
  }

  @Override
  public boolean equals(Object object) {
    if (object == this) return true;
    if (object instanceof ChangeListScope) {
      ChangeListScope scope = (ChangeListScope)object;
      return scope.getIcon() == getIcon() && scope.getScopeId().equals(getScopeId());
    }
    return false;
  }

  @Override
  public String toString() {
    String string = super.toString();
    if (AllIcons.Scope.ChangedFilesAll == getIcon()) string += "; ALL"; // NON-NLS
    return string;
  }

  @Override
  public int getWeight() {
    return AllIcons.Scope.ChangedFilesAll == getIcon() ? 0 : 1;
  }
}
