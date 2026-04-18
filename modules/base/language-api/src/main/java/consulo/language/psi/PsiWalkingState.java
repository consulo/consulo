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
package consulo.language.psi;

import consulo.annotation.ReviewAfterIssueFix;
import consulo.annotation.access.RequiredReadAction;
import consulo.logging.Logger;
import consulo.util.collection.util.WalkingState;
import org.jspecify.annotations.Nullable;

/**
 * @author cdr
 */
public abstract class PsiWalkingState extends WalkingState<PsiElement> {
  private static final Logger LOG = Logger.getInstance(PsiWalkingState.class);
  private final PsiElementVisitor myVisitor;

  @ReviewAfterIssueFix(value = "github.com/uber/NullAway/issues/1500", todo = "Remove NullAway suppression: strange floating problem")
  @SuppressWarnings("NullAway")
  private static class PsiTreeGuide implements TreeGuide<PsiElement> {
    @Override
    @RequiredReadAction
    public @Nullable PsiElement getNextSibling(PsiElement element) {
      return element.getNextSibling();
    }

    @Override
    @RequiredReadAction
    public @Nullable PsiElement getPrevSibling(PsiElement element) {
      return element.getPrevSibling();
    }

    @Override
    @RequiredReadAction
    public @Nullable PsiElement getFirstChild(PsiElement element) {
      return element.getFirstChild();
    }

    @Override
    @RequiredReadAction
    public @Nullable PsiElement getParent(PsiElement element) {
      return element.getParent();
    }

    private static final PsiTreeGuide instance = new PsiTreeGuide();
  }

  protected PsiWalkingState(PsiElementVisitor delegate) {
    super(PsiTreeGuide.instance);
    myVisitor = delegate;
  }

  @Override
  public void visit(PsiElement element) {
    element.accept(myVisitor);
  }

  @Override
  public void elementStarted(PsiElement element) {
    if (!startedWalking && element instanceof PsiCompiledElement) {
      LOG.error(element+"; Do not use walking visitor inside compiled PSI since getNextSibling() is too slow there");
    }

    super.elementStarted(element);
  }
}
