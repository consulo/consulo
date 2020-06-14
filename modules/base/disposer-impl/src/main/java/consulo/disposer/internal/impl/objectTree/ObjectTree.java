// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.disposer.internal.impl.objectTree;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class ObjectTree {
  private static final ThreadLocal<Throwable> ourTopmostDisposeTrace = new ThreadLocal<>();

  // identity used here to prevent problems with hashCode/equals overridden by not very bright minds
  private final Set<Disposable> myRootObjects = ContainerUtil.newIdentityTroveSet(); // guarded by treeLock
  private final Map<Disposable, ObjectNode> myObject2NodeMap = ContainerUtil.newIdentityTroveMap(); // guarded by treeLock
  // Disposable to trace or boolean marker (if trace unavailable)
  private final Map<Disposable, Object> myDisposedObjects = ContainerUtil.createWeakMap(100, 0.5f, ContainerUtil.identityStrategy()); // guarded by treeLock

  private final List<ObjectNode> myExecutedNodes = new ArrayList<>(); // guarded by myExecutedNodes
  private final List<Disposable> myExecutedUnregisteredObjects = new ArrayList<>(); // guarded by myExecutedUnregisteredObjects

  final Object treeLock = new Object();

  private ObjectNode getNode(@Nonnull Disposable object) {
    return myObject2NodeMap.get(object);
  }

  /**
   * @param object
   * @param node null means remove
   */
  void putNode(@Nonnull Disposable object, @Nullable ObjectNode node) {
    if (node == null) {
      myObject2NodeMap.remove(object);
    }
    else {
      myObject2NodeMap.put(object, node);
    }
  }

  @Nonnull
  public final List<ObjectNode> getNodesInExecution() {
    return myExecutedNodes;
  }

  public final void register(@Nonnull Disposable parent, @Nonnull Disposable child) {
    if (parent == child) throw new IllegalArgumentException("Cannot register to itself: " + parent);
    synchronized (treeLock) {
      Object wasDisposed = getDisposalInfo(parent);
      if (wasDisposed != null) {
        throw new IncorrectOperationException("Sorry but parent: " + parent + " has already been disposed " + "(see the cause for stacktrace) so the child: " + child + " will never be disposed",
                                              wasDisposed instanceof Throwable ? (Throwable)wasDisposed : null);
      }

      if (isDisposing(parent)) {
        throw new IncorrectOperationException("Sorry but parent: " + parent + " is being disposed so the child: " + child + " will never be disposed");
      }

      myDisposedObjects.remove(child); // if we dispose thing and then register it back it means it's not disposed anymore
      ObjectNode parentNode = getNode(parent);
      if (parentNode == null) parentNode = createNodeFor(parent, null);

      ObjectNode childNode = getNode(child);
      if (childNode == null) {
        childNode = createNodeFor(child, parentNode);
      }
      else {
        ObjectNode oldParent = childNode.getParent();
        if (oldParent != null) {
          oldParent.removeChild(childNode);
        }
      }
      myRootObjects.remove(child);

      checkWasNotAddedAlready(parentNode, childNode);

      parentNode.addChild(childNode);
    }
  }

  public Object getDisposalInfo(@Nonnull Disposable object) {
    synchronized (treeLock) {
      return myDisposedObjects.get(object);
    }
  }

  private static void checkWasNotAddedAlready(@Nonnull ObjectNode childNode, @Nonnull ObjectNode parentNode) {
    for (ObjectNode node = childNode; node != null; node = node.getParent()) {
      if (node == parentNode) {
        throw new IncorrectOperationException("'" + childNode.getObject() + "' was already added as a child of '" + parentNode.getObject() + "'");
      }
    }
  }

  @Nonnull
  private ObjectNode createNodeFor(@Nonnull Disposable object, @Nullable ObjectNode parentNode) {
    final ObjectNode newNode = new ObjectNode(this, parentNode, object);
    if (parentNode == null) {
      myRootObjects.add(object);
    }
    putNode(object, newNode);
    return newNode;
  }

  public final void executeAll(@Nonnull Disposable object, boolean processUnregistered) {
    ObjectNode node;
    synchronized (treeLock) {
      node = getNode(object);
    }
    boolean needTrace = (node != null || processUnregistered) && Disposer.isDebugMode() && ourTopmostDisposeTrace.get() == null;
    if (needTrace) {
      ourTopmostDisposeTrace.set(ThrowableInterner.intern(new Throwable()));
    }
    try {
      if (node == null) {
        if (processUnregistered) {
          rememberDisposedTrace(object);
          executeUnregistered(object);
        }
      }
      else {
        ObjectNode parent = node.getParent();
        List<Throwable> exceptions = new SmartList<>();
        node.execute(exceptions);
        if (parent != null) {
          synchronized (treeLock) {
            parent.removeChild(node);
          }
        }
        handleExceptions(exceptions);
      }
    }
    finally {
      if (needTrace) {
        ourTopmostDisposeTrace.remove();
      }
    }
  }

  private static void handleExceptions(@Nonnull List<? extends Throwable> exceptions) {
    if (!exceptions.isEmpty()) {
      for (Throwable exception : exceptions) {
        if (!(exception instanceof ProcessCanceledException)) {
          getLogger().error(exception);
        }
      }

      ProcessCanceledException pce = ContainerUtil.findInstance(exceptions, ProcessCanceledException.class);
      if (pce != null) {
        throw pce;
      }
    }
  }

  public boolean isDisposing(@Nonnull Disposable disposable) {
    List<ObjectNode> guard = getNodesInExecution();
    //noinspection SynchronizationOnLocalVariableOrMethodParameter
    synchronized (guard) {
      for (ObjectNode node : guard) {
        if (node.getObject() == disposable) return true;
      }
    }
    return false;
  }

  static <T> void executeActionWithRecursiveGuard(@Nonnull T object, @Nonnull List<T> recursiveGuard, @Nonnull Consumer<? super T> action) {
    //noinspection SynchronizationOnLocalVariableOrMethodParameter
    synchronized (recursiveGuard) {
      if (ArrayUtil.indexOf(recursiveGuard, object, ContainerUtil.identityStrategy()) != -1) return;
      recursiveGuard.add(object);
    }

    try {
      action.accept(object);
    }
    finally {
      //noinspection SynchronizationOnLocalVariableOrMethodParameter
      synchronized (recursiveGuard) {
        int i = ArrayUtil.lastIndexOf(recursiveGuard, object, ContainerUtil.identityStrategy());
        assert i != -1;
        recursiveGuard.remove(i);
      }
    }
  }

  private void executeUnregistered(@Nonnull Disposable disposable) {
    executeActionWithRecursiveGuard(disposable, myExecutedUnregisteredObjects, Disposable::dispose);
  }

  @TestOnly
  public void assertNoReferenceKeptInTree(@Nonnull Disposable disposable) {
    synchronized (treeLock) {
      for (Map.Entry<Disposable, ObjectNode> entry : myObject2NodeMap.entrySet()) {
        Disposable key = entry.getKey();
        assert key != disposable;
        ObjectNode node = entry.getValue();
        node.assertNoReferencesKept(disposable);
      }
    }
  }

  void removeRootObject(@Nonnull Disposable object) {
    myRootObjects.remove(object);
  }

  public void assertIsEmpty(boolean throwError) {
    synchronized (treeLock) {
      for (Disposable object : myRootObjects) {
        if (object == null) continue;
        ObjectNode objectNode = getNode(object);
        if (objectNode == null) continue;
        while (objectNode.getParent() != null) {
          objectNode = objectNode.getParent();
        }
        final Throwable trace = objectNode.getTrace();
        RuntimeException exception =
                new RuntimeException("Memory leak detected: '" + object + "' of " + object.getClass() + "\nSee the cause for the corresponding Disposer.register() stacktrace:\n", trace);
        if (throwError) {
          throw exception;
        }
        getLogger().error(exception);
      }
    }
  }

  @Nonnull
  private static Logger getLogger() {
    return Logger.getInstance(ObjectTree.class);
  }

  void rememberDisposedTrace(@Nonnull Disposable object) {
    synchronized (treeLock) {
      Throwable trace = ourTopmostDisposeTrace.get();
      myDisposedObjects.put(object, trace != null ? trace : Boolean.TRUE);
    }
  }

  @Nullable
  public <D extends Disposable> D findRegisteredObject(@Nonnull Disposable parentDisposable, @Nonnull D object) {
    synchronized (treeLock) {
      ObjectNode parentNode = getNode(parentDisposable);
      if (parentNode == null) return null;
      return parentNode.findChildEqualTo(object);
    }
  }

}
