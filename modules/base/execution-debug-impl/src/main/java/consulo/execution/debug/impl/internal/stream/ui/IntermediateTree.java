package consulo.execution.debug.impl.internal.stream.ui;

import consulo.application.ApplicationManager;
import consulo.execution.debug.frame.*;
import consulo.execution.debug.impl.internal.ui.tree.XDebuggerTreeListener;
import consulo.execution.debug.impl.internal.ui.tree.node.RestorableStateNode;
import consulo.execution.debug.impl.internal.ui.tree.node.XValueContainerNode;
import consulo.execution.debug.impl.internal.ui.tree.node.XValueNodeImpl;
import consulo.execution.debug.stream.trace.CollectionTreeBuilder;
import consulo.execution.debug.stream.trace.GenericEvaluationContext;
import consulo.execution.debug.stream.trace.TraceElement;
import jakarta.annotation.Nonnull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IntermediateTree extends CollectionTree {
    private final Map<XValueContainer, TraceElement> myXValue2TraceElement = new HashMap<>();
    private final CollectionTreeBuilder myBuilder;
    private final int itemsCount;

    public IntermediateTree(@Nonnull List<TraceElement> traceElements,
                            @Nonnull GenericEvaluationContext context,
                            @Nonnull CollectionTreeBuilder builder,
                            @Nonnull String debugName) {
        super(traceElements, context, builder, debugName);
        myBuilder = builder;
        itemsCount = traceElements.size();

        XValueNodeImpl root = new XValueNodeImpl(this, null, "root", new MyTraceElementsRoot(traceElements, context));
        setRoot(root, false);
        root.setLeaf(false);

        addTreeListener(new XDebuggerTreeListener() {
            @Override
            public void nodeLoaded(@Nonnull RestorableStateNode node, @Nonnull String name) {
                XDebuggerTreeListener listener = this;
                if (node instanceof XValueContainerNode<?>) {
                    XValueContainer container = ((XValueContainerNode<?>) node).getValueContainer();
                    if (myBuilder.isSupported(container)) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            TraceElement element = myXValue2TraceElement.get(container);
                            if (element != null) {
                                myValue2Path.put(element, node.getPath());
                                myPath2Value.put(node.getPath(), element);
                            }
                            if (myPath2Value.size() == traceElements.size()) {
                                myXValue2TraceElement.clear();
                                removeTreeListener(listener);
                                ApplicationManager.getApplication().invokeLater(() -> repaint());
                            }
                        });
                    }
                }
            }
        });
    }

    private class MyTraceElementsRoot extends XValue {
        private final List<TraceElement> myTraceElements;
        private final GenericEvaluationContext myEvaluationContext;

        MyTraceElementsRoot(@Nonnull List<TraceElement> traceElements, @Nonnull GenericEvaluationContext context) {
            myTraceElements = traceElements;
            myEvaluationContext = context;
        }

        @Override
        public void computeChildren(@Nonnull XCompositeNode node) {
            XValueChildrenList children = new XValueChildrenList();
            for (TraceElement value : myTraceElements) {
                XNamedValue namedValue = myBuilder.createXNamedValue(value.getValue(), myEvaluationContext);
                myXValue2TraceElement.put(namedValue, value);
                children.add(namedValue);
            }

            node.addChildren(children, true);
        }

        @Override
        public void computePresentation(@Nonnull XValueNode node, @Nonnull XValuePlace place) {
            node.setPresentation(null, "", "", true);
        }
    }

    @Override
    public int getItemsCount() {
        return itemsCount;
    }
}
