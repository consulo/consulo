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

package consulo.util.collection.util;

import jakarta.annotation.Nonnull;
import java.util.function.Predicate;

/**
 * @author cdr
 */
public class WalkingState<T> {
  public interface TreeGuide<T> {
    T getNextSibling(@Nonnull T element);
    T getPrevSibling(@Nonnull T element);
    T getFirstChild(@Nonnull T element);
    T getParent(@Nonnull T element);
  }
  private boolean isDown;
  protected boolean startedWalking;
  private final TreeGuide<T> myWalker;
  private boolean stopped;

  public void elementFinished(@Nonnull T element) {}

  public WalkingState(@Nonnull TreeGuide<T> delegate) {
    myWalker = delegate;
  }

  public void visit(@Nonnull T element) {
    elementStarted(element);
  }

  public void elementStarted(@Nonnull T element){
    isDown = true;
    if (!startedWalking) {
      stopped = false;
      startedWalking = true;
      try {
        walkChildren(element);
      }
      finally {
        startedWalking = false;
      }
    }
  }

  private void walkChildren(@Nonnull T root) {
    for (T element = next(root, root, isDown); element != null && !stopped; element = next(element, root, isDown)) {
      isDown = false; // if client visitor did not call default visitElement it means skip subtree
      T parent = myWalker.getParent(element);
      T next = myWalker.getNextSibling(element);
      visit(element);
      assert myWalker.getNextSibling(element) == next : "Next sibling of the element '"+element+"' changed. Was: "+next+"; Now:"+myWalker.getNextSibling(element)+"; Root:"+root;
      assert myWalker.getParent(element) == parent : "Parent of the element '"+element+"' changed. Was: "+parent+"; Now:"+myWalker.getParent(element)+"; Root:"+root;
    }
  }

  public T next(T element, @Nonnull T root, boolean isDown) {
    if (isDown) {
      T child = myWalker.getFirstChild(element);
      if (child != null) return child;
    }
    // up
    while (element != root && element!=null) {
      T next = myWalker.getNextSibling(element);

      elementFinished(element);
      if (next != null) {
        Object nextPrev = myWalker.getPrevSibling(next);
        if (nextPrev != element) {
          String msg = "Element: " + element + "; next: "+next+"; next.prev: " + nextPrev;
          while (true) {
            T top = myWalker.getParent(element);
            if (top == null) break;
            element = top;
          }
          assert false : msg+" Top:"+element;
        }
        return next;
      }
      element = myWalker.getParent(element);
    }
    if (element != null) {
      elementFinished(element);
    }
    return null;
  }

  public void startedWalking() {
    startedWalking = true;
  }

  public void stopWalking() {
    stopped = true;
  }

  /**
   * process in the in-order fashion
   */
  public static <T> boolean processAll(@Nonnull T root, @Nonnull TreeGuide<T> treeGuide, @Nonnull final Predicate<T> processor) {
    final boolean[] result = {true};
    new WalkingState<T>(treeGuide){
      @Override
      public void visit(@Nonnull T element) {
        if (!processor.test(element)) {
          stopWalking();
          result[0] = false;
        }
        else {
          super.visit(element);
        }
      }
    }.visit(root);
    return result[0];
  }
}
