/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ui.popup.util;

import consulo.codeEditor.util.popup.*;
import consulo.ide.impl.ui.impl.PopupChooserBuilder;
import consulo.language.editor.ui.awt.DetailViewImpl;
import consulo.language.editor.ui.awt.ItemWrapperListRenderer;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.Gray;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.speedSearch.FilteringListModel;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class MasterDetailPopupBuilder implements MasterController {

    private static final Color BORDER_COLOR = Gray._135;
    private final Project myProject;
    private List<AnAction> myActions = List.of();
    private Delegate myDelegate;
    private boolean myCloseOnEnter;

    private DetailView myDetailView;

    private JLabel myPathLabel;

    private JBPopup myPopup;
    private JComponent myChooserComponent;
    private ActionToolbar myActionToolbar;
    private boolean myAddDetailViewToEast = true;
    private Dimension myMinSize;
    private boolean myCancelOnWindowDeactivation = true;
    private Runnable myDoneRunnable;
    private boolean myCancelOnClickOutside;

    private final DetailController myDetailController = new DetailController(this);

    private boolean myUseDimensionServiceForXYLocation;

    public void setUseDimensionServiceForXYLocation(boolean useDimensionServiceForXYLocation) {
        myUseDimensionServiceForXYLocation = useDimensionServiceForXYLocation;
    }


    public String getDimensionServiceKey() {
        return myDimensionServiceKey;
    }

    public void setDimensionServiceKey(String dimensionServiceKey) {
        myDimensionServiceKey = dimensionServiceKey;
    }

    private String myDimensionServiceKey;


    public MasterDetailPopupBuilder setDetailView(DetailView detailView) {
        myDetailView = detailView;
        myDetailController.setDetailView(myDetailView);
        return this;
    }

    public ActionToolbar getActionToolbar() {
        return myActionToolbar;
    }

    public MasterDetailPopupBuilder(Project project) {
        myProject = project;
    }

    public JBPopup createMasterDetailPopup() {

        setupRenderer();

        myPathLabel = new JLabel(" ");
        myPathLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        Font font = myPathLabel.getFont();
        myPathLabel.setFont(font.deriveFont((float) 10));

        if (myDetailView == null) {
            myDetailView = new DetailViewImpl(myProject);
        }

        JPanel footerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(BORDER_COLOR);
                g.drawLine(0, 0, getWidth(), 0);
            }
        };


        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                ProjectIdeFocusManager.getInstance(myProject).doWhenFocusSettlesDown(new Runnable() {
                    @Override
                    public void run() {
                        Object[] values = getSelectedItems();
                        if (values.length == 1) {
                            myDelegate.itemChosen((ItemWrapper) values[0], myProject, myPopup, false);
                        }
                        else {
                            for (Object value : values) {
                                if (value instanceof ItemWrapper) {
                                    myDelegate.itemChosen((ItemWrapper) value, myProject, myPopup, false);
                                }
                            }
                        }
                    }
                });
            }
        };

        footerPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        footerPanel.add(myPathLabel);

        PopupChooserBuilder builder = createInnerBuilder().
            setMovable(true).
            setResizable(true).
            setAutoselectOnMouseMove(false).
            setHeaderLeftActions(myActions).
            setSouthComponent(footerPanel).
            setCancelOnWindowDeactivation(myCancelOnWindowDeactivation).
            setCancelOnClickOutside(myCancelOnClickOutside).
            setUseDimensionServiceForXYLocation(myUseDimensionServiceForXYLocation);


        if (myDoneRunnable != null) {
            builder.setHeaderRightActions(List.of(new DumbAwareAction(LocalizeValue.localizeTODO("Close"), LocalizeValue.of(), PlatformIconGroup.actionsClose()) {
                @RequiredUIAccess
                @Override
                public void actionPerformed(@Nonnull AnActionEvent e) {
                    myDoneRunnable.run();
                }
            }));
        }

        String title = myDelegate.getTitle();
        if (title != null) {
            builder.setTitle(title);
        }


        builder.
            setItemChoosenCallback(runnable).
            setCloseOnEnter(myCloseOnEnter).
            setMayBeParent(true).
            setDimensionServiceKey(myDimensionServiceKey).
            setFilteringEnabled(o -> ((ItemWrapper) o).speedSearchText());

        if (myMinSize != null) {
            builder.setMinSize(myMinSize);
        }

        myPopup = builder.createPopup();
        builder.getScrollPane().setBorder(IdeBorderFactory.createBorder(SideBorder.RIGHT));
        myPopup.addListener(new JBPopupListener() {
            @Override
            public void beforeShown(LightweightWindowEvent event) {
            }

            @Override
            public void onClosed(LightweightWindowEvent event) {
                myDetailView.clearEditor();
            }
        });

        if (myDoneRunnable != null) {
            new AnAction("Done") {
                @RequiredUIAccess
                @Override
                public void actionPerformed(AnActionEvent e) {
                    myDoneRunnable.run();
                }
            }.registerCustomShortcutSet(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK, myPopup.getContent());
        }

        return myPopup;
    }

    private void setupRenderer() {
        if (myChooserComponent instanceof JList) {
            JList list = (JList) myChooserComponent;
            list.setCellRenderer(new ListItemRenderer(myDelegate, myProject));
        }
    }

    private PopupChooserBuilder createInnerBuilder() {
        if (myChooserComponent instanceof JList) {
            return new MyPopupChooserBuilder((JList) myChooserComponent);
        }
        else if (myChooserComponent instanceof JTree) {
            return new MyPopupChooserBuilder((JTree) myChooserComponent);
        }
        return null;
    }

    @Override
    public ItemWrapper[] getSelectedItems() {
        Object[] values = new Object[0];
        if (myChooserComponent instanceof JList) {
            values = ((JList) myChooserComponent).getSelectedValues();
        }
        else if (myChooserComponent instanceof JTree) {
            values = myDelegate.getSelectedItemsInTree();
        }
        ItemWrapper[] items = new ItemWrapper[values.length];
        for (int i = 0; i < values.length; i++) {
            items[i] = (ItemWrapper) values[i];
        }
        return items;
    }

    public void setAddDetailViewToEast(boolean addDetailViewToEast) {
        myAddDetailViewToEast = addDetailViewToEast;
    }

    public MasterDetailPopupBuilder setMinSize(Dimension minSize) {
        myMinSize = minSize;
        return this;
    }

    public MasterDetailPopupBuilder setCancelOnWindowDeactivation(boolean cancelOnWindowDeactivation) {
        myCancelOnWindowDeactivation = cancelOnWindowDeactivation;
        return this;
    }

    public MasterDetailPopupBuilder setDoneRunnable(Runnable doneRunnable) {
        myDoneRunnable = doneRunnable;
        return this;
    }

    public void setCancelOnClickOutside(boolean cancelOnClickOutside) {
        myCancelOnClickOutside = cancelOnClickOutside;
    }

    @Override
    public JLabel getPathLabel() {
        return myPathLabel;
    }

    public static boolean allowedToRemoveItems(Object[] values) {
        for (Object value : values) {
            ItemWrapper item = (ItemWrapper) value;
            if (!item.allowedToRemove()) {
                return false;
            }
        }
        return values.length > 0;
    }

    public void removeSelectedItems(Project project) {
        if (myChooserComponent instanceof JList) {
            JList list = (JList) myChooserComponent;
            int index = list.getSelectedIndex();
            if (index == -1 || index >= list.getModel().getSize()) {
                return;
            }
            Object[] values = list.getSelectedValues();
            for (Object value : values) {
                ItemWrapper item = (ItemWrapper) value;

                DefaultListModel model = list.getModel() instanceof DefaultListModel
                    ? (DefaultListModel) list.getModel()
                    : (DefaultListModel) ((FilteringListModel) list.getModel()).getOriginalModel();
                if (item.allowedToRemove()) {
                    model.removeElement(item);

                    if (model.getSize() > 0) {
                        if (model.getSize() == index) {
                            list.setSelectedIndex(model.getSize() - 1);
                        }
                        else if (model.getSize() > index) {
                            list.setSelectedIndex(index);
                        }
                    }
                    else {
                        list.clearSelection();
                    }
                    item.removed(project);
                }
            }
        }
        else {
            myDelegate.removeSelectedItemsInTree();
        }
    }

    public MasterDetailPopupBuilder setActionsGroup(@Nonnull List<AnAction> actionsGroup) {
        myActions = new ArrayList<>(actionsGroup);
        return this;
    }

    public MasterDetailPopupBuilder setList(JBList list) {
        setChooser(list);
        myDetailController.setList(list);
        list.getSelectionModel().addListSelectionListener(event -> myDetailController.updateDetailView());
        return this;
    }

    private void setChooser(JComponent list) {
        myChooserComponent = list;
        list.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    removeSelectedItems(myProject);
                }
                else if (e.getModifiersEx() == 0) {
                    myDelegate.handleMnemonic(e, myProject, myPopup);
                }
            }
        });
        new AnAction() {
            @RequiredUIAccess
            @Override
            public void actionPerformed(AnActionEvent e) {
                chooseItemWithEnterOrDoubleClick();
            }
        }.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)), list);
        new AnAction() {
            @RequiredUIAccess
            @Override
            public void actionPerformed(AnActionEvent e) {
                chooseItemWithEnterOrDoubleClick();
            }
        }.registerCustomShortcutSet(CommonShortcuts.DOUBLE_CLICK_1, list);
    }

    private void chooseItemWithEnterOrDoubleClick() {
        ItemWrapper[] items = getSelectedItems();
        if (items.length > 0) {
            myDelegate.itemChosen(items[0], myProject, myPopup, true);
        }
    }

    public MasterDetailPopupBuilder setDelegate(Delegate delegate) {
        myDelegate = delegate;
        return this;
    }

    public MasterDetailPopupBuilder setCloseOnEnter(boolean closeOnEnter) {
        myCloseOnEnter = closeOnEnter;
        return this;
    }

    public interface Delegate {
        @Nullable
        String getTitle();

        void handleMnemonic(KeyEvent e, Project project, JBPopup popup);

        @Nullable
        JComponent createAccessoryView(Project project);

        Object[] getSelectedItemsInTree();

        void itemChosen(ItemWrapper item, Project project, JBPopup popup, boolean withEnterOrDoubleClick);

        void removeSelectedItemsInTree();
    }

    public static class ListItemRenderer extends JPanel implements ListCellRenderer {
        private final Project myProject;
        private final ColoredListCellRenderer myRenderer;
        private final Delegate myDelegate;

        private ListItemRenderer(Delegate delegate, Project project) {
            super(new BorderLayout());
            myProject = project;
            setBackground(UIUtil.getListBackground());
            this.myDelegate = delegate;
            JComponent accessory = myDelegate.createAccessoryView(project);

            if (accessory != null) {
                add(accessory, BorderLayout.WEST);
            }

            myRenderer = new ItemWrapperListRenderer(myProject, accessory);
            add(myRenderer, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value instanceof SplitterItem) {
                String label = ((SplitterItem) value).getText();
                TitledSeparator separator = new TitledSeparator(label);
                separator.setBackground(UIUtil.getListBackground());
                separator.setForeground(UIUtil.getListForeground());
                return separator;
            }
            myRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            myRenderer.revalidate();

            return this;
        }
    }

    private class MyPopupChooserBuilder extends PopupChooserBuilder {
        public MyPopupChooserBuilder(@Nonnull JList list) {
            super(list);
        }

        private MyPopupChooserBuilder(@Nonnull JTree tree) {
            super(tree);
        }

        @Override
        protected void addCenterComponentToContentPane(JPanel contentPane, JComponent component) {
            if (myAddDetailViewToEast) {
                JBSplitter splitPane = new JBSplitter(0.3f);
                splitPane.setSplitterProportionKey(getSplitterProportionKey());
                splitPane.setFirstComponent(component);
                splitPane.setSecondComponent((JComponent) myDetailView);

                contentPane.add(splitPane, BorderLayout.CENTER);
            }
            else {
                super.addCenterComponentToContentPane(contentPane, component);
            }
        }
    }

    private String getSplitterProportionKey() {
        return myDimensionServiceKey + ".splitter";
    }
}
