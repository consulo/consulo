// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.codeStyle.fileSet;

import consulo.language.psi.PsiFile;
import consulo.language.psi.search.scope.*;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.util.lang.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class NamedScopeDescriptor implements FileSetDescriptor {
  public final static String NAMED_SCOPE_TYPE = "namedScope";

  private final String myScopeId;
  private
  @Nullable
  PackageSet myFileSet;

  public NamedScopeDescriptor(@Nonnull NamedScope scope) {
    myScopeId = scope.getScopeId();
    myFileSet = scope.getValue();
  }

  public NamedScopeDescriptor(@Nonnull String scopeId) {
    myScopeId = scopeId;
  }

  @Override
  public void setPattern(@Nullable String pattern) {
    try {
      myFileSet = pattern != null ? PackageSetFactory.getInstance().compile(pattern) : null;
    }
    catch (ParsingException e) {
      myFileSet = null;
    }
  }

  @Override
  public boolean matches(@Nonnull PsiFile psiFile) {
    Pair<NamedScopesHolder, NamedScope> resolved = resolveScope(psiFile.getProject());
    if (resolved == null) {
      resolved = resolveScope(ProjectManager.getInstance().getDefaultProject());
    }
    if (resolved != null) {
      PackageSet fileSet = resolved.second.getValue();
      if (fileSet != null) {
        return fileSet.contains(psiFile, resolved.first);
      }
    }
    return false;
  }

  private Pair<NamedScopesHolder, NamedScope> resolveScope(@Nonnull Project project) {
    return NamedScopesHolder.getScopeWithHolder(project, myScopeId);
  }

  @Nonnull
  @Override
  public String getName() {
    return myScopeId;
  }

  @Nonnull
  @Override
  public String getType() {
    return NAMED_SCOPE_TYPE;
  }

  @Nullable
  @Override
  public String getPattern() {
    return myFileSet != null ? myFileSet.getText() : null;
  }

  @Nullable
  public PackageSet getFileSet() {
    return myFileSet;
  }

  @Override
  public String toString() {
    return "scope: " + myScopeId;
  }
}
