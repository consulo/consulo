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
package consulo.repository.ui.impl.internal;

import consulo.platform.base.icon.PlatformIconGroup;
import consulo.repository.ui.PackageManagementService;
import consulo.repository.ui.localize.RepositoryUILocalize;
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.ListBox;
import consulo.ui.Space;
import consulo.ui.TextBox;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.util.lang.StringUtil;

import java.util.List;

/**
 * The repositories of the service, added and removed in place.
 */
public class RepositoriesTab {
    private final PackagesPanel myPanel;
    private final PackageManagementService myService;
    private final UIAccess myUIAccess;

    private final DockLayout myRoot;
    private final ListBox<String> myList;
    private final TextBox myUrlBox;
    private final Button myRemoveButton;

    @RequiredUIAccess
    public RepositoriesTab(PackagesPanel panel) {
        myPanel = panel;
        myService = panel.getService();
        myUIAccess = UIAccess.current();

        myList = ListBox.create(FlatDataModel.of(List.of()));
        myList.addValueListener(event -> updateRemoveButton());

        myUrlBox = TextBox.create("");
        myUrlBox.setPlaceholder(RepositoryUILocalize.repositoryUrlPlaceholder());

        Button addButton = Button.create(RepositoryUILocalize.actionAddRepositoryText(), event -> add());
        addButton.setIcon(PlatformIconGroup.generalAdd());
        myRemoveButton = Button.create(RepositoryUILocalize.actionRemoveRepositoryText(), event -> remove());
        myRemoveButton.setIcon(PlatformIconGroup.generalRemove());

        DockLayout addLine = DockLayout.create(Space.SMALL);
        addLine.center(myUrlBox);
        HorizontalLayout buttons = HorizontalLayout.create(Space.SMALL);
        buttons.add(addButton);
        buttons.add(myRemoveButton);
        addLine.right(buttons);
        addLine.paddingBuilder().allSet(Space.MEDIUM).apply();

        myRoot = DockLayout.create(Space.NONE);
        myRoot.center(ScrollableLayout.create(myList));
        myRoot.bottom(addLine);

        updateRemoveButton();
    }

    public Component getComponent() {
        return myRoot;
    }

    @RequiredUIAccess
    public void reload() {
        myService.fetchAllRepositories().doWhenDone(repositories -> myUIAccess.give(() -> {
            ((MutableFlatDataModel<String>) myList.getDataModel()).replaceAll(repositories == null ? List.of() : repositories);
            updateRemoveButton();
        }));
    }

    @RequiredUIAccess
    private void add() {
        String url = StringUtil.nullize(myUrlBox.getValue(), true);
        if (url == null) {
            return;
        }
        myService.addRepository(url.trim());
        myUrlBox.setValue("");
        reload();
        myPanel.repositoriesChanged();
    }

    @RequiredUIAccess
    private void remove() {
        String url = myList.getValue();
        if (url == null || !myService.canModifyRepository(url)) {
            return;
        }
        myService.removeRepository(url);
        reload();
        myPanel.repositoriesChanged();
    }

    @RequiredUIAccess
    private void updateRemoveButton() {
        String url = myList.getValue();
        myRemoveButton.setEnabled(url != null && myService.canModifyRepository(url));
    }
}
