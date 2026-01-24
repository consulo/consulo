// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.impl.internal.stream.ui;

import consulo.execution.debug.stream.trace.CollectionTreeBuilder;
import consulo.execution.debug.stream.trace.DebuggerCommandLauncher;
import consulo.execution.debug.stream.trace.GenericEvaluationContext;
import consulo.execution.debug.stream.trace.TraceElement;
import consulo.execution.debug.stream.ui.LinkedValuesMapping;
import consulo.execution.debug.stream.ui.TraceController;
import consulo.execution.debug.stream.ui.ValueWithPosition;
import consulo.execution.debug.stream.ui.ValuesPositionsListener;
import consulo.execution.debug.stream.wrapper.StreamCall;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Vitaliy.Bibaev
 */
public class FlatView extends JPanel {
    private final Map<TraceElement, ValueWithPositionImpl> myPool = new HashMap<>();

    public FlatView(@Nonnull List<? extends TraceController> controllers,
                    @Nonnull DebuggerCommandLauncher launcher,
                    @Nonnull GenericEvaluationContext context,
                    @Nonnull CollectionTreeBuilder builder,
                    @Nonnull String debugName) {
        super(new GridLayout(1, 2 * controllers.size() - 1));

        assert !controllers.isEmpty();

        MappingPane prevMappingPane = null;
        List<ValueWithPositionImpl> lastValues = null;

        List<? extends TraceController> allButLast = controllers.subList(0, controllers.size() - 1);
        for (int index = 0; index < allButLast.size(); index++) {
            TraceController controller = allButLast.get(index);
            LinkedValuesWithPositions resolved = resolve(controller, controllers.get(index + 1));
            List<ValueWithPositionImpl> valuesBefore = resolved.valuesBefore;
            LinkedValuesMapping mapping = resolved.mapping;

            StreamCall nextCall = controller.getNextCall();
            if (nextCall == null) {
                throw new IllegalStateException("intermediate state should know about next call");
            }

            MappingPane mappingPane = new MappingPane(nextCall.getTabTitle(), nextCall.getTabTooltip(), valuesBefore, mapping, controller);

            List<TraceElement> traceElements = valuesBefore.stream().map(ValueWithPosition::getTraceElement).collect(Collectors.toList());
            CollectionTree tree = CollectionTree.create(controller.getStreamResult(), traceElements, launcher, context, builder, debugName + "FlatView#controller#" + index);
            PositionsAwareCollectionView view = new PositionsAwareCollectionView(tree, valuesBefore);
            controller.register(view);

            view.addValuesPositionsListener(new ValuesPositionsListener() {
                @Override
                public void valuesPositionsChanged() {
                    mappingPane.repaint();
                }
            });

            MappingPane finalPrevMapping = prevMappingPane;
            if (finalPrevMapping != null) {
                view.addValuesPositionsListener(new ValuesPositionsListener() {
                    @Override
                    public void valuesPositionsChanged() {
                        finalPrevMapping.repaint();
                    }
                });

                finalPrevMapping.addMouseWheelListener(e -> view.getInstancesTree().dispatchEvent(e));
            }

            mappingPane.addMouseWheelListener(e -> view.getInstancesTree().dispatchEvent(e));

            add(view);
            add(mappingPane);

            prevMappingPane = mappingPane;
            lastValues = resolved.valuesAfter;
        }

        if (lastValues != null) {
            TraceController lastController = controllers.get(controllers.size() - 1);
            List<TraceElement> traceElements = lastValues.stream().map(ValueWithPosition::getTraceElement).collect(Collectors.toList());
            CollectionTree tree = CollectionTree.create(lastController.getStreamResult(), traceElements, launcher, context, builder, debugName + "FlatView#lastValues#CollectionTree");
            PositionsAwareCollectionView view = new PositionsAwareCollectionView(tree, lastValues);
            lastController.register(view);

            MappingPane finalPrevMappingPane = prevMappingPane;
            view.addValuesPositionsListener(new ValuesPositionsListener() {
                @Override
                public void valuesPositionsChanged() {
                    if (finalPrevMappingPane != null) {
                        finalPrevMappingPane.repaint();
                    }
                }
            });

            if (prevMappingPane != null) {
                prevMappingPane.addMouseWheelListener(e -> view.getInstancesTree().dispatchEvent(e));
            }

            add(view);
        }

        if (controllers.size() == 1) {
            TraceController controller = controllers.get(0);
            CollectionTree tree = CollectionTree.create(controller.getStreamResult(), controller.getTrace(), launcher, context, builder, "FlatView#singleController");
            CollectionView view = new CollectionView(tree);
            add(view);
            controller.register(view);
        }
    }

    @Override
    public Component add(Component component) {
        return super.add(component);
    }

    private ValueWithPositionImpl getValue(TraceElement element) {
        return myPool.computeIfAbsent(element, ValueWithPositionImpl::new);
    }

    private LinkedValuesWithPositions resolve(TraceController controller, TraceController nextController) {
        List<ValueWithPositionImpl> prevValues = new ArrayList<>();
        Map<ValueWithPositionImpl, Set<ValueWithPositionImpl>> mapping = new HashMap<>();

        for (TraceElement element : controller.getTrace()) {
            ValueWithPositionImpl prevValue = getValue(element);
            prevValues.add(prevValue);
            for (TraceElement nextElement : controller.getNextValues(element)) {
                ValueWithPositionImpl nextValue = getValue(nextElement);
                mapping.computeIfAbsent(prevValue, k -> new HashSet<>()).add(nextValue);
                mapping.computeIfAbsent(nextValue, k -> new HashSet<>()).add(prevValue);
            }
        }

        Map<ValueWithPosition, List<ValueWithPosition>> resultMapping = new HashMap<>();
        for (ValueWithPositionImpl key : mapping.keySet()) {
            resultMapping.put(key, new ArrayList<>(mapping.get(key)));
        }

        List<ValueWithPositionImpl> nextValues = nextController.getTrace().stream().map(this::getValue).collect(Collectors.toList());

        return new LinkedValuesWithPositions(prevValues, nextValues, new LinkedValuesMapping() {
            @Nullable
            @Override
            public List<ValueWithPosition> getLinkedValues(ValueWithPosition value) {
                return resultMapping.get(value);
            }
        });
    }

    private static class LinkedValuesWithPositions {
        final List<ValueWithPositionImpl> valuesBefore;
        final List<ValueWithPositionImpl> valuesAfter;
        final LinkedValuesMapping mapping;

        LinkedValuesWithPositions(List<ValueWithPositionImpl> valuesBefore,
                                  List<ValueWithPositionImpl> valuesAfter,
                                  LinkedValuesMapping mapping) {
            this.valuesBefore = valuesBefore;
            this.valuesAfter = valuesAfter;
            this.mapping = mapping;
        }
    }
}
