package consulo.execution.debug.impl.internal.stream.ui;

import consulo.application.ApplicationManager;
import consulo.execution.debug.frame.*;
import consulo.execution.debug.impl.internal.ui.tree.XDebuggerTreeListener;
import consulo.execution.debug.impl.internal.ui.tree.node.RestorableStateNode;
import consulo.execution.debug.impl.internal.ui.tree.node.XValueContainerNode;
import consulo.execution.debug.impl.internal.ui.tree.node.XValueNodeImpl;
import consulo.execution.debug.stream.trace.*;
import consulo.util.lang.ObjectUtil;
import jakarta.annotation.Nonnull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TerminationTree extends CollectionTree {
  private static final Object NULL_MARKER = ObjectUtil.sentinel("CollectionTree.NULL_MARKER");

  private final CollectionTreeBuilder myBuilder;

  public TerminationTree(@Nonnull Value streamResult,
                         @Nonnull List<TraceElement> traceElements,
                         @Nonnull DebuggerCommandLauncher launcher,
                         @Nonnull GenericEvaluationContext context,
                         @Nonnull CollectionTreeBuilder builder,
                         @Nonnull String debugName) {
    super(traceElements, context, builder, debugName);
    myBuilder = builder;

    XValueNodeImpl root = new XValueNodeImpl(this, null, "root", new MyValueRoot(streamResult, context));
    setRoot(root, false);
    root.setLeaf(false);

    Map<Object, List<TraceElement>> key2TraceElements = traceElements.stream()
      .collect(Collectors.groupingBy(element -> myBuilder.getKey(element, NULL_MARKER)));
    Map<Object, Integer> key2Index = new HashMap<>(key2TraceElements.size() + 1);

    addTreeListener(new XDebuggerTreeListener() {
      @Override
      public void nodeLoaded(@Nonnull RestorableStateNode node, @Nonnull String name) {
        XDebuggerTreeListener listener = this;
        if (node instanceof XValueContainerNode<?>) {
          XValueContainer container = ((XValueContainerNode<?>)node).getValueContainer();
          if (myBuilder.isSupported(container)) {
            launcher.launchDebuggerCommand(() -> {
              Object key = myBuilder.getKey(container, NULL_MARKER);
              ApplicationManager.getApplication().invokeLater(() -> {
                List<TraceElement> elements = key2TraceElements.get(key);
                int nextIndex = key2Index.getOrDefault(key, -1) + 1;
                if (elements != null && nextIndex < elements.size()) {
                  TraceElement element = elements.get(nextIndex);
                  myValue2Path.put(element, node.getPath());
                  myPath2Value.put(node.getPath(), element);
                  key2Index.put(key, nextIndex);
                }
                if (myPath2Value.size() == traceElements.size()) {
                  //NOTE(Korovin): This will not be called if we have a big list of items and it's loaded partially
                  //If missing repaints, we need to replace this logic to some flow/debounce coroutine and repaint after a batch of nodes
                  removeTreeListener(listener);
                  ApplicationManager.getApplication().invokeLater(() -> repaint());
                }
              });
            });
          }
        }
      }
    });

    addTreeListener(new XDebuggerTreeListener() {
      @Override
      public void nodeLoaded(@Nonnull RestorableStateNode node, @Nonnull String name) {
        if (node.getPath().getPathCount() == 2) {
          ApplicationManager.getApplication().invokeLater(() -> expandPath(node.getPath()));
          removeTreeListener(this);
        }
      }
    });
  }

  private class MyValueRoot extends XValue {
    private final Value myValue;
    private final GenericEvaluationContext myContext;

    MyValueRoot(@Nonnull Value value, @Nonnull GenericEvaluationContext context) {
      myValue = value;
      myContext = context;
    }

    @Override
    public void computeChildren(@Nonnull XCompositeNode node) {
      XValueChildrenList children = new XValueChildrenList();
      children.add(myBuilder.createXNamedValue(myValue, myContext));
      node.addChildren(children, true);
    }

    @Override
    public void computePresentation(@Nonnull XValueNode node, @Nonnull XValuePlace place) {
      node.setPresentation(null, "", "", true);
    }
  }

  @Override
  public int getItemsCount() {
    return 1;
  }
}
