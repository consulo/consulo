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
package consulo.language.psi.util;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class PsiMatcherImpl implements PsiMatcher {
  private @Nullable PsiElement myElement;

  public PsiMatcherImpl(PsiElement element) {
    myElement = element;
  }

  @Override
  public PsiMatcher parent(@Nullable PsiMatcherExpression e) {
    myElement = Objects.requireNonNull(myElement).getParent();
    if (myElement == null || (e != null && !Objects.equals(e.match(myElement), Boolean.TRUE))) return NullPsiMatcherImpl.INSTANCE;
    return this;
  }

  @Override
  public PsiMatcher firstChild(@Nullable PsiMatcherExpression e) {
    PsiElement[] children = Objects.requireNonNull(myElement).getChildren();
    for (PsiElement child : children) {
      myElement = child;
      if (e == null || Objects.equals(e.match(myElement), Boolean.TRUE)) {
        return this;
      }
    }
    return NullPsiMatcherImpl.INSTANCE;
  }

  @Override
  public PsiMatcher ancestor(@Nullable PsiMatcherExpression e) {
    while (myElement != null) {
      Boolean res = e == null ? Boolean.TRUE : e.match(myElement);
      if (Boolean.TRUE.equals(res)) break;
      if (res == null) return NullPsiMatcherImpl.INSTANCE;
      myElement = myElement.getParent();
    }
    if (myElement == null) return NullPsiMatcherImpl.INSTANCE;
    return this;
  }

  @Override
  @RequiredReadAction
  public PsiMatcher descendant(@Nullable PsiMatcherExpression e) {
    PsiElement[] children = Objects.requireNonNull(myElement).getChildren();
    for (PsiElement child : children) {
      myElement = child;
      Boolean res = e == null ? Boolean.TRUE : e.match(myElement);
      if (Boolean.TRUE.equals(res)) {
        return this;
      }
      else if (Boolean.FALSE.equals(res)) {
        PsiMatcher grandChild = descendant(e);
        if (grandChild != NullPsiMatcherImpl.INSTANCE) return grandChild;
      }
    }
    return NullPsiMatcherImpl.INSTANCE;
  }

  @Override
  public PsiMatcher dot(@Nullable PsiMatcherExpression e) {
    return e == null || Boolean.TRUE.equals(e.match(Objects.requireNonNull(myElement))) ? this : NullPsiMatcherImpl.INSTANCE;
  }

  @Override
  public @Nullable PsiElement getElement() {
    return myElement;
  }

  private static class NullPsiMatcherImpl implements PsiMatcher {
    @Override
    public PsiMatcher parent(@Nullable PsiMatcherExpression e) {
      return this;
    }

    @Override
    public PsiMatcher firstChild(@Nullable PsiMatcherExpression e) {
      return this;
    }

    @Override
    public PsiMatcher ancestor(@Nullable PsiMatcherExpression e) {
      return this;
    }

    @Override
    public PsiMatcher descendant(@Nullable PsiMatcherExpression e) {
      return this;
    }

    @Override
    public PsiMatcher dot(@Nullable PsiMatcherExpression e) {
      return this;
    }

    @Override
    public @Nullable PsiElement getElement() {
      return null;
    }

    private static final NullPsiMatcherImpl INSTANCE = new NullPsiMatcherImpl();
  }
}
