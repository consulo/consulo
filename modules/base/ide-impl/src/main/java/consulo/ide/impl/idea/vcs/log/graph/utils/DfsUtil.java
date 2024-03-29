/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package consulo.ide.impl.idea.vcs.log.graph.utils;

import consulo.util.collection.primitive.ints.IntStack;
import jakarta.annotation.Nonnull;

public class DfsUtil {
  public DfsUtil() {
  }

  public interface NextNode {
    int NODE_NOT_FOUND = -1;

    int fun(int currentNode);
  }

  public void nodeDfsIterator(int startRowIndex, @Nonnull NextNode nextNodeFun) {
    IntStack myStack = new IntStack();
    myStack.push(startRowIndex);

    while (!myStack.isEmpty()) {
      int nextNode = nextNodeFun.fun(myStack.peek());
      if (nextNode != NextNode.NODE_NOT_FOUND) {
        myStack.push(nextNode);
      }
      else {
        myStack.pop();
      }
    }
    myStack.clear();
  }
}
