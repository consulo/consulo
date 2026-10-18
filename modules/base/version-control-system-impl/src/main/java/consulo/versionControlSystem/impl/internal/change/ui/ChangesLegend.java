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
package consulo.versionControlSystem.impl.internal.change.ui;

import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.HorizontalLayout;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.status.FileStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * The counts under a changes tree - "Modified: 2 of 3" - one label per entry in the colour of its file status, hidden
 * while the entry has nothing to count.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public final class ChangesLegend {
    private final HorizontalLayout myLayout = HorizontalLayout.create();
    private final List<Label> myLabels = new ArrayList<>();
    private final List<LocalizeValue> myTitles = new ArrayList<>();

    @RequiredUIAccess
    public int addEntry(LocalizeValue title, FileStatus status) {
        Label label = Label.create(title);
        label.setForegroundColor(status.getColor());
        label.setVisible(false);
        myLabels.add(label);
        myTitles.add(title);
        myLayout.add(label);
        return myLabels.size() - 1;
    }

    @RequiredUIAccess
    public void setCount(int entry, int total, int included) {
        Label label = myLabels.get(entry);
        LocalizeValue title = myTitles.get(entry);
        label.setVisible(total > 0);
        label.setText(total == included
            ? VcsLocalize.commitDialogLegendAll(title, included)
            : VcsLocalize.commitDialogLegendPartial(title, included, total));
    }

    public Component getComponent() {
        return myLayout;
    }
}
