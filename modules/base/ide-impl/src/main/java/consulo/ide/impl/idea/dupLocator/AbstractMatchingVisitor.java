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
package consulo.ide.impl.idea.dupLocator;

import consulo.ide.impl.idea.dupLocator.util.NodeFilter;
import consulo.language.psi.PsiElement;
import consulo.ide.impl.idea.dupLocator.iterators.ArrayBackedNodeIterator;
import consulo.ide.impl.idea.dupLocator.iterators.FilteringNodeIterator;
import consulo.ide.impl.idea.dupLocator.iterators.NodeIterator;
import consulo.ide.impl.idea.dupLocator.iterators.SiblingNodeIterator;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Eugene.Kudelevsky
 */
public abstract class AbstractMatchingVisitor {

  public abstract boolean matchSequentially(@Nonnull NodeIterator nodes, @Nonnull NodeIterator nodes2);

  public abstract boolean match(@Nullable PsiElement element1, @Nullable PsiElement element2);

  protected abstract boolean doMatchInAnyOrder(@Nonnull NodeIterator elements, @Nonnull NodeIterator elements2);

  public boolean matchSequentially(@Nonnull PsiElement[] elements1, @Nonnull PsiElement[] element2) {
    return matchSequentially(new FilteringNodeIterator(new ArrayBackedNodeIterator(elements1), getNodeFilter()),
                             new FilteringNodeIterator(new ArrayBackedNodeIterator(element2), getNodeFilter()));
  }

  @Nonnull
  protected abstract NodeFilter getNodeFilter();

  public boolean matchOptionally(@Nullable PsiElement element1, @Nullable PsiElement element2) {
    return element1 == null && isLeftLooseMatching() ||
           element2 == null && isRightLooseMatching() ||
           match(element1, element2);
  }

  public boolean matchSons(@Nullable PsiElement el1, @Nullable PsiElement el2) {
    if (el1 == null || el2 == null) return el1 == el2;
    return matchSequentially(el1.getFirstChild(), el2.getFirstChild());
  }

  public boolean matchSonsOptionally(final PsiElement element, final PsiElement element2) {
    if (element == null && isLeftLooseMatching()) {
      return true;
    }
    if (element2 == null && isRightLooseMatching()) {
      return true;
    }
    if (element == null || element2 == null) {
      return element == element2;
    }
    return matchSequentiallyOptionally(element.getFirstChild(), element2.getFirstChild());
  }

  public final boolean matchSonsInAnyOrder(PsiElement element1, PsiElement element2) {
    if (element1 == null && isLeftLooseMatching()) {
      return true;
    }
    if (element2 == null && isRightLooseMatching()) {
      return true;
    }
    if (element1 == null || element2 == null) {
      return element1 == element2;
    }
    PsiElement e = element1.getFirstChild();
    PsiElement e2 = element2.getFirstChild();
    return (e == null && isLeftLooseMatching()) ||
           (e2 == null && isRightLooseMatching()) ||
           matchInAnyOrder(new FilteringNodeIterator(new SiblingNodeIterator(e), getNodeFilter()),
                           new FilteringNodeIterator(new SiblingNodeIterator(e2), getNodeFilter()));
  }

  public boolean matchOptionally(@Nonnull PsiElement[] elements1, @Nonnull PsiElement[] elements2) {
    return (elements1.length == 0 && isLeftLooseMatching()) ||
           (elements2.length == 0 && isRightLooseMatching()) ||
           matchSequentially(elements1, elements2);
  }

  public final boolean matchInAnyOrder(@Nonnull PsiElement[] elements, @Nonnull PsiElement[] elements2) {
    return elements == elements2 || matchInAnyOrder(new ArrayBackedNodeIterator(elements), new ArrayBackedNodeIterator(elements2));
  }

  public boolean isLeftLooseMatching() {
    return true;
  }

  public boolean isRightLooseMatching() {
    return true;
  }

  public boolean matchSequentially(PsiElement el1, PsiElement el2) {
    //if (el1==null || el2==null) return el1 == el2;
    return matchSequentially(new FilteringNodeIterator(new SiblingNodeIterator(el1), getNodeFilter()),
                             new FilteringNodeIterator(new SiblingNodeIterator(el2), getNodeFilter()));
  }

  public boolean matchSequentiallyOptionally(PsiElement el1, PsiElement el2) {
    return (el1 == null && isLeftLooseMatching()) ||
           (el2 == null && isRightLooseMatching()) ||
           matchSequentially(new FilteringNodeIterator(new SiblingNodeIterator(el1), getNodeFilter()),
                             new FilteringNodeIterator(new SiblingNodeIterator(el2), getNodeFilter()));
  }

  public final boolean matchInAnyOrder(@Nonnull NodeIterator elements, @Nonnull NodeIterator elements2) {
    if ((!elements.hasNext() && isLeftLooseMatching()) ||
        (!elements2.hasNext() && isRightLooseMatching()) ||
        (!elements.hasNext() && !elements2.hasNext())
      ) {
      return true;
    }

    return doMatchInAnyOrder(elements, elements2);
  }
}
