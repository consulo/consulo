/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.language.psi.resolve;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.util.PsiElementFilter;
import consulo.language.psi.util.PsiTreeUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @see PsiTreeUtil#processElements(PsiElement, PsiElementProcessor)
 */
public interface PsiElementProcessor<T extends PsiElement> {
  /**
   * Processes a PsiElement
   *
   * @param element currently processed element.
   * @return false to stop processing.
   */
  boolean execute(@Nonnull T element);

  class CollectElements<T extends PsiElement> implements PsiElementProcessor<T> {
    private final Collection<T> myCollection;

    public CollectElements() {
      this(new ArrayList<T>());
    }

    public CollectElements(@Nonnull Collection<T> collection) {
      myCollection = Collections.synchronizedCollection(collection);
    }

    @Nonnull
    public PsiElement[] toArray() {
      return PsiUtilCore.toPsiElementArray(myCollection);
    }

    @Nonnull
    public Collection<T> getCollection() {
      return myCollection;
    }

    @Nonnull
    public T[] toArray(T[] array) {
      return myCollection.toArray(array);
    }

    @Override
    public boolean execute(@Nonnull T element) {
      myCollection.add(element);
      return true;
    }
  }

  class CollectFilteredElements<T extends PsiElement> extends CollectElements<T> {
    private final PsiElementFilter myFilter;

    public CollectFilteredElements(@Nonnull PsiElementFilter filter, @Nonnull Collection<T> collection) {
      super(collection);
      myFilter = filter;
    }

    public CollectFilteredElements(@Nonnull PsiElementFilter filter) {
      myFilter = filter;
    }

    @Override
    public boolean execute(@Nonnull T element) {
      return !myFilter.isAccepted(element) || super.execute(element);
    }
  }

  class CollectElementsWithLimit<T extends PsiElement> extends CollectElements<T>{
    private final AtomicInteger myCount = new AtomicInteger(0);
    private volatile boolean myOverflow = false;
    private final int myLimit;

    public CollectElementsWithLimit(int limit) {
      myLimit = limit;
    }

    public CollectElementsWithLimit(int limit, @Nonnull Collection<T> collection) {
      super(collection);
      myLimit = limit;
    }

    @Override
    public boolean execute(@Nonnull T element) {
      if (myCount.get() == myLimit){
        myOverflow = true;
        return false;
      }
      myCount.incrementAndGet();
      return super.execute(element);
    }

    public boolean isOverflow() {
      return myOverflow;
    }
  }

  class FindElement<T extends PsiElement> implements PsiElementProcessor<T> {
    private volatile T myFoundElement = null;

    public boolean isFound() {
      return myFoundElement != null;
    }

    @Nullable
    public T getFoundElement() {
      return myFoundElement;
    }

    public boolean setFound(T element) {
      myFoundElement = element;
      return false;
    }

    @Override
    public boolean execute(@Nonnull T element) {
      return setFound(element);
    }
  }

  class FindFilteredElement<T extends PsiElement> extends FindElement<T> {
    private final PsiElementFilter myFilter;

    public FindFilteredElement(@Nonnull PsiElementFilter filter) {
      myFilter = filter;
    }

    @Override
    public boolean execute(@Nonnull T element) {
      return !myFilter.isAccepted(element) || super.execute(element);
    }
  }
}