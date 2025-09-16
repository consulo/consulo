/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.keymap.impl.ui;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.configurable.ApplicationConfigurable;
import consulo.configurable.Configurable;
import consulo.configurable.SearchableConfigurable;
import consulo.configurable.StandardConfigurableIds;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.openapi.actionSystem.ex.QuickList;
import consulo.ide.impl.idea.openapi.actionSystem.ex.QuickListsManager;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionToolbarPosition;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.ex.awt.util.ListUtil;
import consulo.ui.ex.keymap.localize.KeyMapLocalize;
import consulo.util.collection.ArrayUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author anna
 * @since 2006-04-13
 */
@ExtensionImpl
public class QuickListsPanel implements SearchableConfigurable, Configurable.NoScroll, Configurable.NoMargin, ApplicationConfigurable {
    private DefaultListModel myQuickListsModel;
    private JBList myQuickListsList;
    private JPanel myRightPanel;
    private int myCurrentIndex = -1;
    private QuickListPanel myQuickListPanel = null;
    private final KeymapListener myKeymapListener;

    private JPanel myRoot;

    @Inject
    public QuickListsPanel(Application application) {
        myKeymapListener = application.getMessageBus().syncPublisher(KeymapListener.class);
    }

    @Override
    @RequiredUIAccess
    public void reset() {
        myQuickListsModel.removeAllElements();
        for (QuickList list : QuickListsManager.getInstance().getAllQuickLists()) {
            myQuickListsModel.addElement(list);
        }

        SwingUtilities.invokeLater(() -> {
            if (!myQuickListsModel.isEmpty()) {
                myQuickListsList.setSelectedIndex(0);
            }
        });
    }

    @Override
    @RequiredUIAccess
    public boolean isModified() {
        QuickList[] storedLists = QuickListsManager.getInstance().getAllQuickLists();
        QuickList[] modelLists = getCurrentQuickListIds();
        return !Arrays.equals(storedLists, modelLists);
    }

    @Override
    @RequiredUIAccess
    public void apply() {
        QuickListsManager.getInstance().setQuickLists(getCurrentQuickListIds());
    }

    private JPanel createQuickListsPanel() {
        myQuickListsModel = new DefaultListModel();

        myQuickListsList = new JBList<>(myQuickListsModel);
        myQuickListsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        myQuickListsList.setCellRenderer(new MyQuickListCellRenderer());
        myQuickListsList.addListSelectionListener(e -> {
            myRightPanel.removeAll();
            Object selectedValue = myQuickListsList.getSelectedValue();
            if (selectedValue instanceof QuickList quickList) {
                updateRightPanel(quickList);
                myQuickListsList.repaint();
            }
            else {
                addDescriptionLabel();
            }
            myRightPanel.revalidate();
        });

        addDescriptionLabel();

        return ToolbarDecorator.createDecorator(myQuickListsList)
            .setAddAction(button -> {
                QuickList quickList = new QuickList(createUniqueName(), "", ArrayUtil.EMPTY_STRING_ARRAY, false);
                myQuickListsModel.addElement(quickList);
                myQuickListsList.clearSelection();
                ScrollingUtil.selectItem(myQuickListsList, quickList);
                myKeymapListener.processCurrentKeymapChanged(getCurrentQuickListIds());
            })
            .setRemoveAction(button -> {
                ListUtil.removeSelectedItems(myQuickListsList);
                myQuickListsList.repaint();
                myKeymapListener.processCurrentKeymapChanged(getCurrentQuickListIds());
            })
            .disableUpDownActions()
            .setToolbarPosition(ActionToolbarPosition.TOP)
            .setPanelBorder(JBUI.Borders.empty())
            .createPanel();
    }

    private void addDescriptionLabel() {
        JLabel descLabel =
            new JLabel("<html>Quick Lists allow you to define commonly used groups of actions (for example, refactoring or VCS actions)" + " and to assign keyboard shortcuts to such groups.</html>");
        descLabel.setBorder(new EmptyBorder(0, 25, 0, 25));
        myRightPanel.add(descLabel, BorderLayout.CENTER);
    }

    private String createUniqueName() {
        String str = KeyMapLocalize.unnamedListDisplayName().get();
        ArrayList<String> names = new ArrayList<>();
        for (int i = 0; i < myQuickListsModel.getSize(); i++) {
            names.add(((QuickList) myQuickListsModel.getElementAt(i)).getName());
        }
        if (!names.contains(str)) {
            return str;
        }
        int i = 1;
        while (true) {
            if (!names.contains(str + i)) {
                return str + i;
            }
            i++;
        }
    }

    private void updateRightPanel(QuickList quickList) {
        final int index = myQuickListsList.getSelectedIndex();
        if (myQuickListPanel != null && myCurrentIndex > -1 && myCurrentIndex < myQuickListsModel.getSize()) {
            updateList(myCurrentIndex);
            myKeymapListener.processCurrentKeymapChanged(getCurrentQuickListIds());
        }
        myQuickListPanel = new QuickListPanel(quickList, getCurrentQuickListIds());
        DocumentAdapter documentAdapter = new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                updateList(index);
            }
        };
        myQuickListPanel.addNameListener(documentAdapter);
        myQuickListPanel.addDescriptionListener(documentAdapter);
        myRightPanel.add(myQuickListPanel.getPanel(), BorderLayout.CENTER);
        myCurrentIndex = index;
    }

    private void updateList(int index) {
        if (myQuickListPanel == null) {
            return;
        }
        QuickList oldQuickList = (QuickList) myQuickListsModel.getElementAt(index);

        QuickList newQuickList = createNewQuickListAt();

        if (oldQuickList != null) {
            newQuickList.getExternalInfo().copy(oldQuickList.getExternalInfo());
        }

        myQuickListsModel.setElementAt(newQuickList, index);

        if (oldQuickList != null && !newQuickList.getName().equals(oldQuickList.getName())) {
            myKeymapListener.quickListRenamed(oldQuickList, newQuickList);
        }
    }

    private QuickList createNewQuickListAt() {
        ListModel model = myQuickListPanel.getActionsList().getModel();
        int size = model.getSize();
        String[] ids = new String[size];
        for (int i = 0; i < size; i++) {
            ids[i] = (String) model.getElementAt(i);
        }
        return new QuickList(myQuickListPanel.getDisplayName(), myQuickListPanel.getDescription(), ids, false);
    }

    @Nonnull
    private QuickList[] getCurrentQuickListIds() {
        if (myCurrentIndex > -1 && myQuickListsModel.getSize() > myCurrentIndex) {
            updateList(myCurrentIndex);
        }
        int size = myQuickListsModel.size();
        QuickList[] lists = new QuickList[size];
        for (int i = 0; i < lists.length; i++) {
            lists[i] = (QuickList) myQuickListsModel.getElementAt(i);
        }
        return lists;
    }

    @Nonnull
    @Override
    public String getId() {
        return "quick.lists";
    }

    private static class MyQuickListCellRenderer extends ColoredListCellRenderer {
        @Override
        protected void customizeCellRenderer(@Nonnull JList list, Object value, int index, boolean selected, boolean hasFocus) {
            setBackground(UIUtil.getListBackground(selected));
            QuickList quickList = (QuickList) value;
            append(quickList.getName());
        }
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Quick Lists");
    }

    @Nullable
    @Override
    public String getParentId() {
        return StandardConfigurableIds.GENERAL_GROUP;
    }

    @RequiredUIAccess
    @Override
    public JComponent createComponent(@Nonnull Disposable uiDisposable) {
        myRoot = new JPanel(new BorderLayout());
        myRightPanel = new JPanel(new BorderLayout());

        Splitter splitter = new OnePixelSplitter(false, 0.3f);
        splitter.setFirstComponent(createQuickListsPanel());
        splitter.setSecondComponent(myRightPanel);
        myRoot.add(splitter, BorderLayout.CENTER);
        return myRoot;
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        myRoot = null;
        myRightPanel = null;
    }
}
