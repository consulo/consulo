/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package consulo.language.impl.internal.psi.diff;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.progress.ProgressIndicator;
import consulo.application.util.registry.Registry;
import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.ast.FileASTNode;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.lang.ControlFlowException;
import consulo.util.lang.Pair;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;

@ServiceAPI(ComponentScope.PROJECT)
public abstract class BlockSupport {
  public static BlockSupport getInstance(Project project) {
    return project.getInstance(BlockSupport.class);
  }

  public abstract void reparseRange(@Nonnull PsiFile file, int startOffset, int endOffset, @NonNls @Nonnull CharSequence newText) throws IncorrectOperationException;

  @Nonnull
  public abstract DiffLog reparseRange(@Nonnull PsiFile file,
                                       @Nonnull FileASTNode oldFileNode,
                                       @Nonnull TextRange changedPsiRange,
                                       @Nonnull CharSequence newText,
                                       @Nonnull ProgressIndicator progressIndicator,
                                       @Nonnull CharSequence lastCommittedText) throws IncorrectOperationException;

  public static final Key<Boolean> DO_NOT_REPARSE_INCREMENTALLY = Key.create("DO_NOT_REPARSE_INCREMENTALLY");
  public static final Key<Pair<ASTNode, CharSequence>> TREE_TO_BE_REPARSED = Key.create("TREE_TO_BE_REPARSED");

  public static class ReparsedSuccessfullyException extends RuntimeException implements ControlFlowException {
    private final DiffLog myDiffLog;

    public ReparsedSuccessfullyException(@Nonnull DiffLog diffLog) {
      myDiffLog = diffLog;
    }

    @Nonnull
    public DiffLog getDiffLog() {
      return myDiffLog;
    }

    @Nonnull
    @Override
    public synchronized Throwable fillInStackTrace() {
      return this;
    }
  }

  // maximal tree depth for which incremental reparse is allowed
  // if tree is deeper then it will be replaced completely - to avoid SOEs
  public static final int INCREMENTAL_REPARSE_DEPTH_LIMIT = Registry.intValue("psi.incremental.reparse.depth.limit");

  public static final Key<Boolean> TREE_DEPTH_LIMIT_EXCEEDED = Key.create("TREE_IS_TOO_DEEP");

  public static boolean isTooDeep(final UserDataHolder element) {
    return element != null && Boolean.TRUE.equals(element.getUserData(TREE_DEPTH_LIMIT_EXCEEDED));
  }
}
