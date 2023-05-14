// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.scopeView;

import consulo.content.scope.NamedScope;
import consulo.content.scope.NamedScopesHolder;
import consulo.content.scope.PackageSet;
import consulo.ide.impl.idea.ide.scratch.ScratchesNamedScope;
import consulo.ide.impl.idea.packageDependencies.DefaultScopesProvider;
import consulo.ide.impl.psi.search.scope.NonProjectFilesScope;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileFilter;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public final class NamedScopeFilter implements VirtualFileFilter {
  private static final Logger LOG = Logger.getInstance(NamedScopeFilter.class);
  private final NamedScopesHolder holder;
  private final NamedScope scope;
  private final String string;

  public NamedScopeFilter(@Nonnull NamedScopesHolder holder, @Nonnull NamedScope scope) {
    this.holder = holder;
    this.scope = scope;
    this.string = scope + "; " + scope.getClass();
  }

  @Nonnull
  public NamedScopesHolder getHolder() {
    return holder;
  }

  @Nonnull
  public NamedScope getScope() {
    return scope;
  }

  @Nonnull
  @Override
  public String toString() {
    return string;
  }

  @Override
  public boolean accept(@Nonnull VirtualFile file) {
    if (file == null) return false;

    PackageSet set = scope.getValue();
    if (set == null) return false;

    Project project = holder.getProject();
    return set.contains(file, project, holder);
  }

  static boolean isVisible(@Nonnull NamedScope scope) {
    return !(scope instanceof NonProjectFilesScope || scope == DefaultScopesProvider.getAllScope());
  }

  @Nonnull
  static List<NamedScopeFilter> list(NamedScopesHolder... holders) {
    List<NamedScopeFilter> list = new ArrayList<>();
    NamedScope scratchesScope = null;
    for (NamedScopesHolder holder : holders) {
      for (NamedScope scope : holder.getScopes()) {
        String name = scope.getScopeId();
        if (null == scope.getValue()) {
          LOG.debug("ignore scope without package set: ", name, "; holder: ", holder);
        }
        else if (scope instanceof ScratchesNamedScope) {
          // the scopes here are not sorted as in scopes combobox, so we need to
          // add scratches scope last to fix Project/Production/Tests scopes order
          scratchesScope = scope;
        }
        else if (!isVisible(scope)) {
          LOG.debug("ignore hidden scope: ", name, "; holder: ", holder);
        }
        else {
          list.add(new NamedScopeFilter(holder, scope));
        }
      }
      if (scratchesScope != null) {
        list.add(new NamedScopeFilter(holder, scratchesScope));
        scratchesScope = null;
      }
    }
    return list;
  }
}
