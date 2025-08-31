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

import consulo.language.psi.PsiElement;

import java.util.Objects;

public class PsiMatcherImpl implements PsiMatcher {
  private PsiElement myElement;

  public PsiMatcherImpl(PsiElement element) {
    myElement = element;
  }

  @Override
  public PsiMatcher parent(PsiMatcherExpression e) {
    myElement = myElement.getParent();
    if (myElement == null || (e != null && !Objects.equals(e.match(myElement), Boolean.TRUE))) return NullPsiMatcherImpl.INSTANCE;
    return this;
  }

  @Override
  public PsiMatcher firstChild(PsiMatcherExpression e) {
    PsiElement[] children = myElement.getChildren();
    for (PsiElement child : children) {
      myElement = child;
      if (e == null || Objects.equals(e.match(myElement), Boolean.TRUE)) {
        return this;
      }
    }
    return NullPsiMatcherImpl.INSTANCE;
  }

  @Override
  public PsiMatcher ancestor(PsiMatcherExpression e) {
    while (myElement != null) {
      Boolean res = e == null ? Boolean.TRUE : e.match(myElement);
      if (Objects.equals(res, Boolean.TRUE)) break;
      if (res == null) return NullPsiMatcherImpl.INSTANCE;
      myElement = myElement.getParent();
    }
    if (myElement == null) return NullPsiMatcherImpl.INSTANCE;
    return this;
  }

  @Override
  public PsiMatcher descendant(PsiMatcherExpression e) {
    PsiElement[] children = myElement.getChildren();
    for (PsiElement child : children) {
      myElement = child;
      Boolean res = e == null ? Boolean.TRUE : e.match(myElement);
      if (Objects.equals(res, Boolean.TRUE)) {
        return this;
      }
      else if (Objects.equals(res, Boolean.FALSE)) {
        PsiMatcher grandChild = descendant(e);
        if (grandChild != NullPsiMatcherImpl.INSTANCE) return grandChild;
      }
    }
    return NullPsiMatcherImpl.INSTANCE;
  }

  @Override
  public PsiMatcher dot(PsiMatcherExpression e) {
    return e == null || Objects.equals(e.match(myElement), Boolean.TRUE) ? this : NullPsiMatcherImpl.INSTANCE;
  }


  @Override
  public PsiElement getElement() {
    return myElement;
  }

  private static class NullPsiMatcherImpl implements PsiMatcher {
    @Override
    public PsiMatcher parent(PsiMatcherExpression e) {
      return this;
    }

    @Override
    public PsiMatcher firstChild(PsiMatcherExpression e) {
      return this;
    }

    @Override
    public PsiMatcher ancestor(PsiMatcherExpression e) {
      return this;
    }

    @Override
    public PsiMatcher descendant(PsiMatcherExpression e) {
      return this;
    }

    @Override
    public PsiMatcher dot(PsiMatcherExpression e) {
      return this;
    }

    @Override
    public PsiElement getElement() {
      return null;
    }

    private static final NullPsiMatcherImpl INSTANCE = new NullPsiMatcherImpl();
  }
}
