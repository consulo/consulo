/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.bookmarks.actions;

import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.bookmark.Bookmark;
import consulo.bookmark.BookmarkManager;
import consulo.bookmark.localize.BookmarkLocalize;
import consulo.bookmark.ui.view.internal.BookmarkItem;
import consulo.codeEditor.util.popup.ItemWrapper;
import consulo.ide.impl.idea.ui.popup.util.MasterDetailPopupBuilder;
import consulo.language.editor.ui.awt.DetailViewImpl;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CommonShortcuts;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awt.speedSearch.FilteringListModel;
import consulo.ui.ex.popup.JBPopup;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author max
 */
// TODO: remove duplication with BaseShowRecentFilesAction, there's quite a bit of it
@ActionImpl(id = "ShowBookmarks")
public class BookmarksAction extends AnAction implements DumbAware, MasterDetailPopupBuilder.Delegate {
    public static final Font MNEMONIC_FONT = new Font("Monospaced", 0, 11);

    private JBPopup myPopup;

    @Inject
    public BookmarksAction() {
        this(BookmarkLocalize.actionBookmarksShowText(), BookmarkLocalize.actionBookmarksShowDescription());
    }

    public BookmarksAction(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description) {
        super(text, description);
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(e.hasData(Project.KEY));
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        final Project project = e.getRequiredData(Project.KEY);

        if (myPopup != null && myPopup.isVisible()) {
            return;
        }

        DefaultListModel<BookmarkItem> model = buildModel(project);

        final JBList<BookmarkItem> list = new JBList<>(model);
        list.getEmptyText().setText("No Bookmarks");

        EditBookmarkDescriptionAction editDescriptionAction = new EditBookmarkDescriptionAction(project, list);
        List<AnAction> actions = new ArrayList<>();
        actions.add(editDescriptionAction);
        actions.add(new DeleteBookmarkAction(project, list));
        actions.add(new MoveBookmarkUpAction(project, list));
        actions.add(new MoveBookmarkDownAction(project, list));

        myPopup = new MasterDetailPopupBuilder(project)
            .setActionsGroup(actions)
            .setList(list)
            .setDetailView(new DetailViewImpl(project))
            .setCloseOnEnter(false)
            .setDoneRunnable(() -> myPopup.cancel())
            .setDelegate(this).createMasterDetailPopup();
        new AnAction() {
            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent e) {
                Object selectedValue = list.getSelectedValue();
                if (selectedValue instanceof BookmarkItem) {
                    itemChosen((BookmarkItem) selectedValue, project, myPopup, true);
                }
            }
        }.registerCustomShortcutSet(CommonShortcuts.getEditSource(), list);
        editDescriptionAction.setPopup(myPopup);
        myPopup.showCenteredInCurrentWindow(project);
        //todo[zaec] selection mode shouldn't be set in builder.setList() method
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    }

    @Override
    public String getTitle() {
        return "Bookmarks";
    }

    @Override
    public void handleMnemonic(KeyEvent e, Project project, JBPopup popup) {
        char mnemonic = e.getKeyChar();
        Bookmark bookmark = BookmarkManager.getInstance(project).findBookmarkForMnemonic(mnemonic);
        if (bookmark != null) {
            popup.cancel();
            ProjectIdeFocusManager.getInstance(project).doWhenFocusSettlesDown(() -> bookmark.navigate(true));
        }
    }

    @Override
    @Nullable
    public JComponent createAccessoryView(Project project) {
        if (!BookmarkManager.getInstance(project).hasBookmarksWithMnemonics()) {
            return null;
        }
        JLabel mnemonicLabel = new JLabel();
        mnemonicLabel.setFont(MNEMONIC_FONT);

        mnemonicLabel.setPreferredSize(new JLabel("W.").getPreferredSize());
        mnemonicLabel.setOpaque(false);
        return mnemonicLabel;
    }

    @Override
    public Object[] getSelectedItemsInTree() {
        return new Object[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void itemChosen(ItemWrapper item, Project project, JBPopup popup, boolean withEnterOrDoubleClick) {
        if (item instanceof BookmarkItem && withEnterOrDoubleClick) {
            Bookmark bookmark = ((BookmarkItem) item).getBookmark();
            popup.cancel();
            bookmark.navigate(true);
        }
    }

    @Override
    public void removeSelectedItemsInTree() {

    }

    private static DefaultListModel<BookmarkItem> buildModel(Project project) {
        DefaultListModel<BookmarkItem> model = new DefaultListModel<>();

        for (Bookmark bookmark : BookmarkManager.getInstance(project).getValidBookmarks()) {
            model.addElement(new BookmarkItem(bookmark));
        }

        return model;
    }

    static List<Bookmark> getSelectedBookmarks(JList list) {
        List<Bookmark> answer = new ArrayList<>();

        for (Object value : list.getSelectedValues()) {
            if (value instanceof BookmarkItem bookmarkItem) {
                answer.add(bookmarkItem.getBookmark());
            }
            else {
                return Collections.emptyList();
            }
        }

        return answer;
    }

    static boolean notFiltered(JList list) {
        return !(list.getModel() instanceof FilteringListModel model && model.getOriginalModel().getSize() != model.getSize());
    }
}
