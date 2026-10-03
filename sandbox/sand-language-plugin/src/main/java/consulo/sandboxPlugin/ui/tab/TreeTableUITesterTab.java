/*
 * Copyright 2013-2026 consulo.io
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
package consulo.sandboxPlugin.ui.tab;

import consulo.annotation.component.ExtensionImpl;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.HorizontalAlignment;
import consulo.ui.Label;
import consulo.ui.TreeExecutor;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.TreeTable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.tester.UITesterTab;
import consulo.ui.ex.tree.ApplicationTreeExecutorFactory;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.ScrollableLayout;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@ExtensionImpl(id = "treeTable", order = "after tree")
public class TreeTableUITesterTab implements UITesterTab {
    private static final class Frame {
        private final String myName;
        private final AtomicLong mySelfSamples;
        private final List<Frame> myChildren;

        private Frame(String name, long selfSamples, List<Frame> children) {
            myName = name;
            mySelfSamples = new AtomicLong(selfSamples);
            myChildren = children;
        }

        static Frame of(String name, long selfSamples, Frame... children) {
            return new Frame(name, selfSamples, List.of(children));
        }

        String getName() {
            return myName;
        }

        List<Frame> getChildren() {
            return myChildren;
        }

        long getSelfSamples() {
            return mySelfSamples.get();
        }

        long getTotalSamples() {
            long total = mySelfSamples.get();
            for (Frame child : myChildren) {
                total += child.getTotalSamples();
            }
            return total;
        }

        void shuffle(Random random) {
            mySelfSamples.updateAndGet(samples -> Math.max(0, samples + random.nextInt(41) - 20));
            for (Frame child : myChildren) {
                child.shuffle(random);
            }
        }
    }

    private final ApplicationTreeExecutorFactory myTreeExecutorFactory;

    @Inject
    public TreeTableUITesterTab(ApplicationTreeExecutorFactory treeExecutorFactory) {
        myTreeExecutorFactory = treeExecutorFactory;
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Components > TreeTable");
    }

    @RequiredUIAccess
    @Override
    public Component createComponent(Disposable uiDisposable) {
        Frame root = Frame.of("<all threads>", 0,
            Frame.of("main", 5,
                Frame.of("Application.run", 10,
                    Frame.of("EventQueue.dispatch", 20,
                        Frame.of("ActionManager.perform", 40,
                            Frame.of("Indexer.update", 120),
                            Frame.of("Highlighter.run", 90)),
                        Frame.of("Renderer.paint", 30,
                            Frame.of("Graphics.drawString", 160),
                            Frame.of("Layout.measure", 25,
                                Frame.of("Font.width", 70)))),
                    Frame.of("Parser.parse", 15,
                        Frame.of("Lexer.next", 140),
                        Frame.of("Parser.reduce", 60)))),
            Frame.of("GC", 80),
            Frame.of("Idle", 50));

        TreeModel<Frame> model = (nodeFactory, parentValue) -> {
            if (parentValue == null) {
                return;
            }

            for (Frame child : parentValue.getChildren()) {
                TreeNode<Frame> node = nodeFactory.apply(child);
                node.setLeaf(child.getChildren().isEmpty());
                node.setRenderer((frame, presentation) -> {
                    presentation.withIcon(PlatformIconGroup.nodesMethod());
                    presentation.append(frame.getName());
                });
            }
        };

        TreeExecutor executor = myTreeExecutorFactory.forBackgroundThreadWithoutReadAction(uiDisposable);

        TreeTable<Frame> treeTable = TreeTable.create(root, model, executor);
        Disposer.register(uiDisposable, treeTable.destroyHook());

        treeTable.setTreeColumnHeader(LocalizeValue.localizeTODO("Method"));
        treeTable.addColumn(LocalizeValue.localizeTODO("Total %"), frame -> percent(frame.getTotalSamples(), root.getTotalSamples()))
            .setRender((presentation, item) -> presentation.append(formatPercent(item.getValue())))
            .setHorizontalAlignment(HorizontalAlignment.RIGHT)
            .setSortable(Comparator.naturalOrder());
        treeTable.addColumn(LocalizeValue.localizeTODO("Self %"), frame -> percent(frame.getSelfSamples(), root.getTotalSamples()))
            .setRender((presentation, item) -> presentation.append(formatPercent(item.getValue())))
            .setHorizontalAlignment(HorizontalAlignment.RIGHT)
            .setSortable(Comparator.naturalOrder());
        treeTable.addColumn(LocalizeValue.localizeTODO("Samples"), Frame::getTotalSamples)
            .setHorizontalAlignment(HorizontalAlignment.RIGHT)
            .setSortable(Comparator.naturalOrder());

        treeTable.expandAll(2);

        Random random = new Random();
        Button refresh = Button.create(LocalizeValue.localizeTODO("Refresh"), event -> {
            root.shuffle(random);
            treeTable.refreshAll();
        });

        DockLayout layout = DockLayout.create();
        layout.top(HorizontalLayout.create()
            .add(refresh)
            .add(Label.create(LocalizeValue.localizeTODO("Click a column header to sort: ascending, descending, unsorted"))));
        layout.center(ScrollableLayout.create(treeTable));
        return layout;
    }

    private static double percent(long part, long total) {
        return total == 0 ? 0 : part * 100.0 / total;
    }

    private static String formatPercent(@Nullable Double value) {
        return value == null ? "" : String.format(Locale.ROOT, "%.1f%%", value);
    }
}
