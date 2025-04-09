/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package consulo.versionControlSystem.impl.internal.change.ui.awt;

import consulo.project.Project;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.versionControlSystem.change.*;
import consulo.versionControlSystem.impl.internal.change.ChangeListOwner;
import consulo.versionControlSystem.impl.internal.change.ui.ChangeListRemoteState;
import consulo.versionControlSystem.internal.ChangeListManagerEx;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

import static consulo.ui.ex.awt.FontUtil.spaceAndThinSpace;

/**
 * @author yole
 */
public class ChangesBrowserChangeListNode extends ChangesBrowserNode<ChangeList> {
    private final List<ChangeListDecorator> myDecorators;
    private final ChangeListManagerEx myClManager;
    private final ChangeListRemoteState myChangeListRemoteState;

    public ChangesBrowserChangeListNode(Project project, ChangeList userObject, ChangeListRemoteState changeListRemoteState) {
        super(userObject);
        myChangeListRemoteState = changeListRemoteState;
        myClManager = (ChangeListManagerEx)ChangeListManager.getInstance(project);
        myDecorators = ChangeListDecorator.EP_NAME.getExtensionList(project);
    }

    @Override
    public void render(
        @Nonnull ChangesBrowserNodeRenderer renderer,
        boolean selected,
        boolean expanded,
        boolean hasFocus
    ) {
        if (userObject instanceof LocalChangeList list) {
            renderer.appendTextWithIssueLinks(
                list.getName(),
                list.isDefault() ? SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES : SimpleTextAttributes.REGULAR_ATTRIBUTES
            );
            appendCount(renderer);
            for (ChangeListDecorator decorator : myDecorators) {
                decorator.decorateChangeList(list, renderer, selected, expanded, hasFocus);
            }
            String freezed = myClManager.isFreezed();
            if (freezed != null) {
                renderer.append(spaceAndThinSpace() + freezed, SimpleTextAttributes.GRAYED_ATTRIBUTES);
            }
            else if (myClManager.isInUpdate()) {
                appendUpdatingState(renderer);
            }
            if (!myChangeListRemoteState.getState()) {
                renderer.append(spaceAndThinSpace());
                renderer.append(VcsLocalize.changesNodetitleHaveOutdatedFiles().get(), SimpleTextAttributes.ERROR_ATTRIBUTES);
            }
        }
        else {
            renderer.append(getUserObject().getName(), SimpleTextAttributes.SIMPLE_CELL_ATTRIBUTES);
            appendCount(renderer);
        }
    }

    public ChangeListRemoteState getChangeListRemoteState() {
        return myChangeListRemoteState;
    }

    @Override
    public String getTextPresentation() {
        return getUserObject().getName().trim();
    }

    @Override
    public boolean canAcceptDrop(ChangeListDragBean dragBean) {
        Change[] changes = dragBean.getChanges();
        for (Change change : getUserObject().getChanges()) {
            for (Change incomingChange : changes) {
                if (change == incomingChange) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public void acceptDrop(ChangeListOwner dragOwner, ChangeListDragBean dragBean) {
        if (!(userObject instanceof LocalChangeList)) {
            return;
        }
        LocalChangeList dropList = (LocalChangeList)getUserObject();
        dragOwner.moveChangesTo(dropList, dragBean.getChanges());

        List<VirtualFile> toUpdate = new ArrayList<>();

        addIfNotNull(toUpdate, dragBean.getUnversionedFiles());
        addIfNotNull(toUpdate, dragBean.getIgnoredFiles());
        if (!toUpdate.isEmpty()) {
            dragOwner.addUnversionedFiles(dropList, toUpdate);
        }
    }

    private static void addIfNotNull(List<VirtualFile> unversionedFiles1, List<VirtualFile> ignoredFiles) {
        if (ignoredFiles != null) {
            unversionedFiles1.addAll(ignoredFiles);
        }
    }

    @Override
    public int getSortWeight() {
        return userObject instanceof LocalChangeList list && list.isDefault() ? 1 : 2;
    }

    @Override
    public int compareUserObjects(Object o2) {
        return o2 instanceof ChangeList list ? getUserObject().getName().compareToIgnoreCase(list.getName()) : 0;
    }
}
