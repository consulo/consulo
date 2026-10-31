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
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.ComponentItemRenderer;
import consulo.ui.DragAndDropTransferHandler;
import consulo.ui.HorizontalAlignment;
import consulo.ui.Label;
import consulo.ui.MessageBoxes;
import consulo.ui.Table;
import consulo.ui.TableItemEditor;
import consulo.ui.TextAttribute;
import consulo.ui.TextBox;
import consulo.ui.TriStateCheckBox;
import consulo.ui.ValueComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.clipboard.DataTransfer;
import consulo.ui.clipboard.DataTransferType;
import consulo.ui.ex.tester.UITesterTab;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.util.lang.ThreeState;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@ExtensionImpl(id = "table", order = "after components")
public class TableUITesterTab implements UITesterTab {
    /**
     * Rows travel inside the process only, with the table they left, so the target can take them out of it on a move.
     */
    private record DraggedRows(Table<String> source, List<String> rows, boolean move) {
    }

    private static final DataTransferType<DraggedRows> ROWS = DataTransferType.create("consulo.sandbox.table.rows");

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Components > Table");
    }

    @RequiredUIAccess
    @Override
    public Component createComponent(Disposable uiDisposable) {
        DockLayout layout = DockLayout.create();

        Map<String, String> values = new TreeMap<>();
        values.put("test1", "1");
        values.put("test2", "3");
        values.put("test3", "5");

        Map<String, ThreeState> states = new TreeMap<>();
        states.put("test1", ThreeState.YES);
        states.put("test2", ThreeState.UNSURE);
        states.put("test3", ThreeState.NO);

        MutableFlatDataModel<String> model = FlatDataModel.of(new ArrayList<>(values.keySet()));

        Table<String> table = Table.create(model);

        table.addColumn(LocalizeValue.localizeTODO("On"), key -> states.getOrDefault(key, ThreeState.NO))
            .setWidth(40)
            .setResizable(false)
            .setRenderer(ComponentItemRenderer.reusable(
                () -> TriStateCheckBox.create(LocalizeValue.empty()),
                (checkBox, item) -> checkBox.setValue(item.getValue() == null ? ThreeState.NO : item.getValue())))
            .setEditor(new TableItemEditor<>() {
                @Override
                @RequiredUIAccess
                public ValueComponent<ThreeState> createComponent(String key) {
                    TriStateCheckBox checkBox =
                        TriStateCheckBox.create(LocalizeValue.empty(), states.getOrDefault(key, ThreeState.NO));
                    checkBox.setUnsureEnabled(true);
                    return checkBox;
                }

                @Override
                @RequiredUIAccess
                public void commit(String key, @Nullable ThreeState value) {
                    states.put(key, value == null ? ThreeState.NO : value);
                    model.update(key);
                }
            });

        table.addColumn(LocalizeValue.localizeTODO("Key"), key -> key)
            .setSortable(Comparator.naturalOrder())
            .setWidth(160);

        table.addColumn(LocalizeValue.localizeTODO("Value"), values::get)
            .setHorizontalAlignment(HorizontalAlignment.RIGHT)
            .setSortable(Comparator.naturalOrder())
            .setRenderer((presentation, item) -> presentation.append(
                String.valueOf(item.getValue()),
                item.isSelected() ? TextAttribute.REGULAR_BOLD : TextAttribute.REGULAR
            ))
            .setEditor(new TableItemEditor<>() {
                @Override
                @RequiredUIAccess
                public ValueComponent<String> createComponent(String key) {
                    return TextBox.create(values.get(key));
                }

                @Override
                @RequiredUIAccess
                public void commit(String key, @Nullable String value) {
                    values.put(key, value == null ? "" : value);
                    model.update(key);
                }
            });

        table.setAllowMultipleSelect(true);
        table.setSpeedSearchConverter(key -> key);
        table.addSelectListener(event -> MessageBoxes.okInfo(LocalizeValue.of("Selected: " + event.getValues().size())).showAsync());

        TwoComponentSplitLayout split = TwoComponentSplitLayout.create(SplitLayoutPosition.VERTICAL);
        split.setFirstComponent(ScrollableLayout.create(table));
        split.setSecondComponent(createDragAndDropDemo());

        layout.center(split);

        return layout;
    }

    @RequiredUIAccess
    private static Component createDragAndDropDemo() {
        MutableFlatDataModel<String> leftModel = FlatDataModel.of(new ArrayList<>(List.of("Apple", "Banana", "Cherry", "Date")));
        MutableFlatDataModel<String> rightModel = FlatDataModel.of(new ArrayList<>(List.of("Carrot", "Potato")));

        Table<String> left = createDragTable(leftModel);
        Table<String> right = createDragTable(rightModel);

        RowsTransferHandler handler = new RowsTransferHandler();
        left.setTransferHandler(handler);
        right.setTransferHandler(handler);

        TwoComponentSplitLayout split = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        split.setFirstComponent(ScrollableLayout.create(left));
        split.setSecondComponent(ScrollableLayout.create(right));

        DockLayout demo = DockLayout.create();
        demo.top(Label.create(LocalizeValue.localizeTODO(
            "Drag rows within or between the tables (hold Ctrl to copy), or drop files on them from outside")));
        demo.center(split);
        return demo;
    }

    @RequiredUIAccess
    private static Table<String> createDragTable(MutableFlatDataModel<String> model) {
        Table<String> table = Table.create(model);
        table.addColumn(LocalizeValue.localizeTODO("Name"), name -> name).setWidth(200);
        table.setAllowMultipleSelect(true);
        return table;
    }

    private static class RowsTransferHandler implements DragAndDropTransferHandler<String> {
        @Override
        public @Nullable DataTransfer createTransfer(Component component) {
            if (!(component instanceof Table<?> table)) {
                return null;
            }
            List<String> names = new ArrayList<>();
            for (Object item : table.getSelectedItems()) {
                names.add(String.valueOf(item));
            }
            return names.isEmpty() ? null : DataTransfer.of(String.join("\n", names));
        }

        @Override
        @SuppressWarnings("unchecked")
        public @Nullable DataTransfer createDragTransfer(Component component, List<String> items, boolean move) {
            if (!(component instanceof Table<?> table)) {
                return null;
            }
            // an empty table has no row a drop could land on, so a move never takes the last rows out
            if (move && items.size() >= table.getDataModel().getSize()) {
                return null;
            }
            return DataTransfer.builder()
                .put(ROWS, new DraggedRows((Table<String>) table, List.copyOf(items), move))
                .put(DataTransferType.TEXT, String.join("\n", items))
                .build();
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean drop(Component component, DropContext<String> context) {
            if (!(component instanceof Table<?> table) || !(table.getDataModel() instanceof MutableFlatDataModel<?> rawModel)) {
                return false;
            }
            MutableFlatDataModel<String> model = (MutableFlatDataModel<String>) rawModel;

            DraggedRows dragged = context.getTransfer().get(ROWS);
            List<File> files = context.getTransfer().get(DataTransferType.FILE_LIST);
            if (dragged == null && (files == null || files.isEmpty())) {
                return false;
            }

            // dropping rows on themselves would leave nothing to name as the place to put them
            if (dragged != null && dragged.source() == table && dragged.rows().contains(context.getTarget())) {
                return false;
            }

            if (context.isCheckOnly()) {
                return true;
            }

            List<String> names = new ArrayList<>();
            if (dragged != null) {
                if (dragged.move()) {
                    MutableFlatDataModel<String> sourceModel = (MutableFlatDataModel<String>) dragged.source().getDataModel();
                    for (String row : dragged.rows()) {
                        sourceModel.remove(row);
                    }
                }
                names.addAll(dragged.rows());
            }
            else {
                for (File file : files) {
                    names.add(file.getName());
                }
            }

            // the target is still there - a move never takes it, see the check above
            int index = model.indexOf(context.getTarget());
            int insertAt = context.getPosition() == DropPosition.ABOVE ? index : index + 1;
            for (String name : names) {
                // the model knows a row by its value, so a copy needs a name of its own
                String unique = name;
                for (int i = 2; model.indexOf(unique) >= 0; i++) {
                    unique = name + " (" + i + ")";
                }
                model.add(unique, insertAt++);
            }
            return true;
        }
    }
}
