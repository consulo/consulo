package consulo.execution.debug.impl.internal.stream.ui;

import consulo.application.Application;
import consulo.execution.debug.frame.*;
import consulo.execution.debug.impl.internal.ui.tree.XDebuggerTreeListener;
import consulo.execution.debug.impl.internal.ui.tree.node.RestorableStateNode;
import consulo.execution.debug.impl.internal.ui.tree.node.XValueContainerNode;
import consulo.execution.debug.impl.internal.ui.tree.node.XValueNodeImpl;
import consulo.execution.debug.stream.trace.*;
import consulo.localize.LocalizeValue;
import consulo.util.lang.ObjectUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TerminationTree extends CollectionTree {
  private static final Object NULL_MARKER = ObjectUtil.sentinel("CollectionTree.NULL_MARKER");

  private final CollectionTreeBuilder myBuilder;

  public TerminationTree(Value streamResult,
                         List<TraceElement> traceElements,
                         DebuggerCommandLauncher launcher,
                         GenericEvaluationContext context,
                         CollectionTreeBuilder builder,
                         String debugName) {
    super(traceElements, context, builder, debugName);
    myBuilder = builder;

    XValueNodeImpl root = new XValueNodeImpl(this, null, LocalizeValue.localizeTODO("root"), new MyValueRoot(streamResult, context));
    setRoot(root, false);
    root.setLeaf(false);

    Map<Object, List<TraceElement>> key2TraceElements = traceElements.stream()
      .collect(Collectors.groupingBy(element -> myBuilder.getKey(element, NULL_MARKER)));
    Map<Object, Integer> key2Index = new HashMap<>(key2TraceElements.size() + 1);

    addTreeListener(new XDebuggerTreeListener() {
      @Override
      public void nodeLoaded(RestorableStateNode node, LocalizeValue name) {
        XDebuggerTreeListener listener = this;
        if (node instanceof XValueContainerNode<?>) {
          XValueContainer container = ((XValueContainerNode<?>)node).getValueContainer();
          if (myBuilder.isSupported(container)) {
            launcher.launchDebuggerCommand(() -> {
              Object key = myBuilder.getKey(container, NULL_MARKER);
              Application app = Application.get();
              app.invokeLater(() -> {
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
                  app.invokeLater(() -> repaint());
                }
              });
            });
          }
        }
      }
    });

    addTreeListener(new XDebuggerTreeListener() {
      @Override
      public void nodeLoaded(RestorableStateNode node, LocalizeValue name) {
        if (node.getPath().getPathCount() == 2) {
          Application.get().invokeLater(() -> expandPath(node.getPath()));
          removeTreeListener(this);
        }
      }
    });
  }

  private class MyValueRoot extends XValue {
    private final Value myValue;
    private final GenericEvaluationContext myContext;

    MyValueRoot(Value value, GenericEvaluationContext context) {
      myValue = value;
      myContext = context;
    }

    @Override
    public void computeChildren(XCompositeNode node) {
      XValueChildrenList children = new XValueChildrenList();
      children.add(myBuilder.createXNamedValue(myValue, myContext));
      node.addChildren(children, true);
    }

    @Override
    public void computePresentation(XValueNode node, XValuePlace place) {
      node.setPresentation(null, "", "", true);
    }
  }

  @Override
  public int getItemsCount() {
    return 1;
  }
}
