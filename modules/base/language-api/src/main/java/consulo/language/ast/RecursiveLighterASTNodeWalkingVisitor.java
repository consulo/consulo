/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package consulo.language.ast;

import consulo.util.collection.util.WalkingState;
import consulo.util.collection.Stack;
import jakarta.annotation.Nonnull;

import java.util.List;

public abstract class RecursiveLighterASTNodeWalkingVisitor extends LighterASTNodeVisitor {
  @Nonnull
  private final LighterAST ast;
  private final Stack<IndexedLighterASTNode[]> childrenStack = new Stack<>();
  private final Stack<IndexedLighterASTNode> parentStack = new Stack<>();

  // wrapper around LighterASTNode which remembers its position in parents' children list for performance
  private static class IndexedLighterASTNode {
    private static final IndexedLighterASTNode[] EMPTY_ARRAY = new IndexedLighterASTNode[0];
    private final LighterASTNode node;
    private final IndexedLighterASTNode prev;
    private IndexedLighterASTNode next;

    IndexedLighterASTNode(@Nonnull LighterASTNode node, IndexedLighterASTNode prev) {
      this.node = node;
      this.prev = prev;
    }
  }

  private class LighterASTGuide implements WalkingState.TreeGuide<IndexedLighterASTNode> {
    @Override
    public IndexedLighterASTNode getNextSibling(@Nonnull IndexedLighterASTNode element) {
      return element.next;
    }

    @Override
    public IndexedLighterASTNode getPrevSibling(@Nonnull IndexedLighterASTNode element) {
      return element.prev;
    }

    @Override
    public IndexedLighterASTNode getFirstChild(@Nonnull IndexedLighterASTNode element) {
      List<LighterASTNode> children = ast.getChildren(element.node);
      IndexedLighterASTNode[] indexedChildren = children.isEmpty() ? IndexedLighterASTNode.EMPTY_ARRAY : new IndexedLighterASTNode[children.size()];
      for (int i = 0; i < children.size(); i++) {
        LighterASTNode child = children.get(i);
        IndexedLighterASTNode indexedNode = new IndexedLighterASTNode(child, i == 0 ? null : indexedChildren[i - 1]);
        indexedChildren[i] = indexedNode;
        if (i != 0) {
          indexedChildren[i-1].next = indexedNode;
        }
      }
      childrenStack.push(indexedChildren);
      parentStack.push(element);
      return children.isEmpty() ? null : indexedChildren[0];
    }

    @Override
    public IndexedLighterASTNode getParent(@Nonnull IndexedLighterASTNode element) {
      return parentStack.peek();
    }
  }

  protected RecursiveLighterASTNodeWalkingVisitor(@Nonnull final LighterAST ast) {
    this.ast = ast;

    myWalkingState = new WalkingState<IndexedLighterASTNode>(new LighterASTGuide()) {
      @Override
      public void elementFinished(@Nonnull IndexedLighterASTNode element) {
        RecursiveLighterASTNodeWalkingVisitor.this.elementFinished(element.node);

        if (parentStack.peek() == element) { // getFirstChild returned nothing. otherwise getFirstChild() was not called, i.e. super.visitNode() was not called i.e. just ignore
          childrenStack.pop();
          parentStack.pop();
        }
      }

      @Override
      public void visit(@Nonnull IndexedLighterASTNode iNode) {
        LighterASTNode element = iNode.node;
        RecursiveLighterASTNodeWalkingVisitor visitor = RecursiveLighterASTNodeWalkingVisitor.this;
        if (element instanceof LighterLazyParseableNode) {
          visitor.visitLazyParseableNode((LighterLazyParseableNode)element);
        }
        else if (element instanceof LighterASTTokenNode) {
          visitor.visitTokenNode((LighterASTTokenNode)element);
        }
        else {
          visitor.visitNode(element);
        }
      }
    };
  }

  private final WalkingState<IndexedLighterASTNode> myWalkingState;

  protected void elementFinished(@Nonnull LighterASTNode element) {
  }

  @Override
  public void visitNode(@Nonnull LighterASTNode element) {
    myWalkingState.elementStarted(new IndexedLighterASTNode(element, null));
  }

  public void stopWalking() {
    myWalkingState.stopWalking();
  }
}
