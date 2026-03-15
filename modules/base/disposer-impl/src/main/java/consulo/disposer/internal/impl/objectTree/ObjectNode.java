// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.disposer.internal.impl.objectTree;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;

import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

final class ObjectNode {
  private static final ObjectNode[] EMPTY_ARRAY = new ObjectNode[0];

  private static final Logger LOG = Logger.getInstance(ObjectNode.class);

  private final ObjectTree myTree;

  private @Nullable ObjectNode myParent; // guarded by myTree.treeLock
  private final Disposable myObject;

  private @Nullable List<ObjectNode> myChildren; // guarded by myTree.treeLock
  private final @Nullable Throwable myTrace;

  ObjectNode(ObjectTree tree, @Nullable ObjectNode parentNode, Disposable object) {
    myTree = tree;
    myParent = parentNode;
    myObject = object;

    myTrace = parentNode == null && Disposer.isDebugMode() ? ThrowableInterner.intern(new Throwable()) : null;
  }

  void addChild(ObjectNode child) {
    List<ObjectNode> children = myChildren;
    if (children == null) {
      myChildren = new ArrayList<>(List.of(child));
    }
    else {
      children.add(child);
    }
    child.myParent = this;
  }

  void removeChild(ObjectNode child) {
    List<ObjectNode> children = myChildren;
    if (children != null) {
      // optimisation: iterate backwards
      for (int i = children.size() - 1; i >= 0; i--) {
        ObjectNode node = children.get(i);
        if (node.equals(child)) {
          children.remove(i);
          break;
        }
      }
    }
    child.myParent = null;
  }

  @Nullable ObjectNode getParent() {
    synchronized (myTree.treeLock) {
      return myParent;
    }
  }

  void execute(List<? super Throwable> exceptions) {
    ObjectTree.executeActionWithRecursiveGuard(this, myTree.getNodesInExecution(), each -> {
      if (myTree.getDisposalInfo(myObject) != null) return; // already disposed. may happen when someone does `register(obj, ()->Disposer.dispose(t));` abomination
      try {
        if (myObject instanceof Disposable.Parent) {
          ((Disposable.Parent)myObject).beforeTreeDispose();
        }
      }
      catch (Throwable t) {
        LOG.error(t);
      }

      ObjectNode[] childrenArray;
      synchronized (myTree.treeLock) {
        List<ObjectNode> children = myChildren;
        childrenArray = children == null || children.isEmpty() ? EMPTY_ARRAY : children.toArray(EMPTY_ARRAY);
        myChildren = null;
      }

      for (int i = childrenArray.length - 1; i >= 0; i--) {
        try {
          ObjectNode childNode = childrenArray[i];
          childNode.execute(exceptions);
          synchronized (myTree.treeLock) {
            childNode.myParent = null;
          }
        }
        catch (Throwable e) {
          exceptions.add(e);
        }
      }

      try {
        //noinspection SSBasedInspection
        myObject.dispose();
        myTree.rememberDisposedTrace(myObject);
      }
      catch (Throwable e) {
        exceptions.add(e);
      }
      removeFromObjectTree();
    });
  }

  private void removeFromObjectTree() {
    synchronized (myTree.treeLock) {
      myTree.putNode(myObject, null);
      if (myParent == null) {
        myTree.removeRootObject(myObject);
      }
    }
  }

  Disposable getObject() {
    return myObject;
  }

  @Override
  public String toString() {
    return "Node: " + myObject;
  }

  @Nullable Throwable getTrace() {
    return myTrace;
  }

  void assertNoReferencesKept(Disposable aDisposable) {
    assert getObject() != aDisposable;
    if (myChildren != null) {
      for (ObjectNode node : myChildren) {
        node.assertNoReferencesKept(aDisposable);
      }
    }
  }

  <D extends Disposable> @Nullable D findChildEqualTo(D object) {
    List<ObjectNode> children = myChildren;
    if (children != null) {
      for (ObjectNode node : children) {
        Disposable nodeObject = node.getObject();
        if (nodeObject.equals(object)) {
          //noinspection unchecked
          return (D)nodeObject;
        }
      }
    }
    return null;
  }
}
