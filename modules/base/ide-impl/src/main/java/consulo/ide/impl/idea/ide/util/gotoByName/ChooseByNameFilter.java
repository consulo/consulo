/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.util.gotoByName;

import consulo.application.AllIcons;
import consulo.ui.ex.awt.ElementsChooser;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.disposer.Disposer;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This class contains UI related to filtering functionality.
 */
public abstract class ChooseByNameFilter<T> {
    /**
     * a parent popup
     */
    private final ChooseByNamePopup myParentPopup;
    /**
     * action toolbar
     */
    private final ActionToolbar myToolbar;
    /**
     * a file type chooser, only one instance is used
     */
    private final ElementsChooser<T> myChooser;
    /**
     * A panel that contains chooser
     */
    private final JPanel myChooserPanel;
    /**
     * a file type popup, the value is non-null if popup is active
     */
    private JBPopup myPopup;
    /**
     * a project to use. The project is used for dimension service.
     */
    private final Project myProject;

    /**
     * A constructor
     *
     * @param popup               a parent popup
     * @param model               a model for popup
     * @param filterConfiguration storage for selected filter values
     * @param project             a context project
     */
    public ChooseByNameFilter(
        @Nonnull ChooseByNamePopup popup,
        @Nonnull FilteringGotoByModel<T> model,
        @Nonnull ChooseByNameFilterConfiguration<T> filterConfiguration,
        @Nonnull Project project
    ) {
        myParentPopup = popup;
        DefaultActionGroup actionGroup = new DefaultActionGroup("go.to.file.filter", false);
        ToggleAction action = new ToggleAction("Filter", "Filter files by type", AllIcons.General.Filter) {
            @Override
            public boolean isSelected(AnActionEvent e) {
                return myPopup != null;
            }

            @Override
            public void setSelected(AnActionEvent e, boolean state) {
                if (state) {
                    createPopup();
                }
                else {
                    close();
                }
            }
        };
        actionGroup.add(action);
        myToolbar = ActionManager.getInstance().createActionToolbar("gotfile.filter", actionGroup, true);
        myToolbar.setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY);
        myToolbar.updateActionsImmediately();
        myToolbar.getComponent().setFocusable(false);
        myToolbar.getComponent().setBorder(null);
        myProject = project;
        myChooser = createChooser(model, filterConfiguration);
        myChooserPanel = createChooserPanel();
        popup.setToolArea(myToolbar.getComponent());
    }

    /**
     * @return a panel with chooser and buttons
     */
    private JPanel createChooserPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(myChooser);
        JPanel buttons = new JPanel();
        JButton all = new JButton("All");
        all.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                myChooser.setAllElementsMarked(true);
            }
        });
        buttons.add(all);
        JButton none = new JButton("None");
        none.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                myChooser.setAllElementsMarked(false);
            }
        });
        buttons.add(none);
        JButton invert = new JButton("Invert");
        invert.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                myChooser.invertSelection();
            }
        });
        buttons.add(invert);
        panel.add(buttons);
        return panel;
    }

    /**
     * Create a file type chooser
     *
     * @param model               a model to update
     * @param filterConfiguration
     * @return a created file chooser
     */
    @Nonnull
    protected ElementsChooser<T> createChooser(
        @Nonnull final FilteringGotoByModel<T> model,
        @Nonnull final ChooseByNameFilterConfiguration<T> filterConfiguration
    ) {
        List<T> elements = new ArrayList<T>(getAllFilterValues());
        final ElementsChooser<T> chooser = new ElementsChooser<T>(elements, true) {
            @Override
            protected String getItemText(@Nonnull T value) {
                return textForFilterValue(value);
            }

            @Override
            protected Image getItemIcon(@Nonnull T value) {
                return iconForFilterValue(value);
            }
        };
        chooser.setFocusable(false);
        int count = chooser.getElementCount();
        for (int i = 0; i < count; i++) {
            T type = chooser.getElementAt(i);
            if (!DumbService.getInstance(myProject).isDumb() && !filterConfiguration.isFileTypeVisible(type)) {
                chooser.setElementMarked(type, false);
            }
        }
        updateModel(model, chooser, true);
        chooser.addElementsMarkListener(new ElementsChooser.ElementsMarkListener<T>() {
            @Override
            public void elementMarkChanged(T element, boolean isMarked) {
                filterConfiguration.setVisible(element, isMarked);
                updateModel(model, chooser, false);
            }
        });
        return chooser;
    }

    protected abstract String textForFilterValue(@Nonnull T value);

    @Nullable
    protected abstract consulo.ui.image.Image iconForFilterValue(@Nonnull T value);

    @Nonnull
    protected abstract Collection<T> getAllFilterValues();

    /**
     * Update model basing on the chooser state
     *
     * @param gotoFileModel a model
     * @param chooser       a file type chooser
     */
    protected void updateModel(@Nonnull FilteringGotoByModel<T> gotoFileModel, @Nonnull ElementsChooser<T> chooser, boolean initial) {
        List<T> markedElements = chooser.getMarkedElements();
        gotoFileModel.setFilterItems(markedElements);
        myParentPopup.rebuildList(initial);
    }

    /**
     * Create and show popup
     */
    private void createPopup() {
        if (myPopup != null) {
            return;
        }
        myPopup =
            JBPopupFactory.getInstance().createComponentPopupBuilder(myChooserPanel, myChooser).setModalContext(false).setFocusable(false)
                .setResizable(true).setCancelOnClickOutside(false).setMinSize(new Dimension(200, 200))
                .setDimensionServiceKey(myProject, "GotoFile_FileTypePopup", false).createPopup();
        myPopup.addListener(new JBPopupListener.Adapter() {
            @Override
            public void onClosed(LightweightWindowEvent event) {
                myPopup = null;
            }
        });
        myPopup.showUnderneathOf(myToolbar.getComponent());
    }

    /**
     * close the file type filter
     */
    public void close() {
        if (myPopup != null) {
            Disposer.dispose(myPopup);
        }
    }
}
