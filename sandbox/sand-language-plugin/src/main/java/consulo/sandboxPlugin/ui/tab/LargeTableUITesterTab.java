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
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Table;
import consulo.ui.TextAttribute;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.tester.UITesterTab;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.ui.style.StandardColors;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
@ExtensionImpl(id = "largeTable", order = "after table")
public class LargeTableUITesterTab implements UITesterTab {
    private static final int ROWS = 100_000;
    private static final String[] AUTHORS = {"VISTALL", "Nikolay Yurchenko", "Jane Doe", "John Smith", "Bot"};

    private record Row(int index, String hash, String subject, String author, String date) {
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Components > Table (100k rows)");
    }

    @RequiredUIAccess
    @Override
    public Component createComponent(Disposable uiDisposable) {
        MutableFlatDataModel<Row> model = FlatDataModel.lazyOf(createRows(ROWS));

        Table<Row> table = Table.create(model);
        table.setAllowMultipleSelect(true);
        table.setShowHeader(true);
        table.setRowBackgroundGetter(row -> row.index() % 50 == 0 ? StandardColors.LIGHT_YELLOW : null);

        table.addColumn(LocalizeValue.localizeTODO("Hash"), Row::hash).setWidth(90);
        table.addColumn(LocalizeValue.localizeTODO("Subject"), row -> row)
            .setRenderer((presentation, item) -> {
                Row row = item.getValue();
                if (row == null) {
                    return;
                }
                presentation.append(row.subject(), row.index() % 7 == 0 ? TextAttribute.REGULAR_BOLD : TextAttribute.REGULAR);
            });
        table.addColumn(LocalizeValue.localizeTODO("Author"), Row::author)
            .setSortable(Comparator.naturalOrder())
            .setWidth(160);
        table.addColumn(LocalizeValue.localizeTODO("Date"), Row::date).setWidth(140);

        Label status = Label.create(LocalizeValue.localizeTODO("Nothing selected"));
        table.addSelectListener(event -> status.setText(LocalizeValue.of("Selected: " + event.getValues().size())));

        Button scroll = Button.create(LocalizeValue.localizeTODO("Select row 75000"), event -> {
            Row target = model.get(75_000);
            table.select(target);
            table.scrollTo(target);
        });

        Button append = Button.create(LocalizeValue.localizeTODO("Append row"), event -> {
            int index = model.getSize();
            model.add(createRow(index));
        });

        HorizontalLayout bottom = HorizontalLayout.create();
        bottom.add(scroll);
        bottom.add(append);
        bottom.add(status);

        DockLayout layout = DockLayout.create();
        layout.center(ScrollableLayout.create(table));
        layout.bottom(bottom);
        return layout;
    }

    private static List<Row> createRows(int count) {
        List<Row> rows = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            rows.add(createRow(i));
        }
        return rows;
    }

    private static Row createRow(int index) {
        String hash = Integer.toHexString(0x10000000 + index * 7919);
        String subject = "Commit #" + index + ": update " + (index % 13 == 0 ? "build scripts" : "sources");
        String author = AUTHORS[index % AUTHORS.length];
        String date = "2026-" + String.format("%02d-%02d %02d:%02d", 1 + index % 12, 1 + index % 28, index % 24, index % 60);
        return new Row(index, hash, subject, author, date);
    }
}
