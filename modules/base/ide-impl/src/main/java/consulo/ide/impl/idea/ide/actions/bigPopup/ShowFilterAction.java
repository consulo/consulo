// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.bigPopup;

import consulo.application.dumb.DumbAware;
import consulo.execution.ExecutionUtil;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import consulo.ui.ex.action.Toggleable;
import consulo.ui.ex.awt.ElementsChooser;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

public abstract class ShowFilterAction extends ToggleAction implements DumbAware {
    private JBPopup myFilterPopup;

    public ShowFilterAction() {
        super("Filter", "Show filters popup", PlatformIconGroup.generalFilter());
    }

    @Override
    public boolean isSelected(@Nonnull final AnActionEvent e) {
        return myFilterPopup != null && !myFilterPopup.isDisposed();
    }

    @Override
    public void setSelected(@Nonnull final AnActionEvent e, final boolean state) {
        if (state) {
            showPopup(e.getRequiredData(Project.KEY), e.getInputEvent().getComponent());
        }
        else {
            if (myFilterPopup != null && !myFilterPopup.isDisposed()) {
                myFilterPopup.cancel();
            }
        }
    }

    @Override
    @RequiredUIAccess
    public void update(@Nonnull AnActionEvent e) {
        Image icon = getTemplatePresentation().getIcon();
        e.getPresentation().setIcon(isActive() ? ExecutionUtil.getIconWithLiveIndicator(icon) : icon);
        e.getPresentation().setEnabled(isEnabled());
        Toggleable.setSelected(e.getPresentation(), isSelected(e));
    }

    protected abstract boolean isEnabled();

    protected abstract boolean isActive();

    private void showPopup(@Nonnull Project project, @Nonnull Component anchor) {
        if (myFilterPopup != null || !anchor.isValid()) {
            return;
        }
        JBPopupListener popupCloseListener = new JBPopupListener() {
            @Override
            public void onClosed(@Nonnull LightweightWindowEvent event) {
                myFilterPopup = null;
            }
        };
        myFilterPopup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(createFilterPanel(), null)
            .setModalContext(false)
            .setFocusable(false)
            .setResizable(true)
            .setCancelOnClickOutside(false)
            .setMinSize(new Dimension(200, 200))
            .setDimensionServiceKey(project, getDimensionServiceKey(), false)
            .addListener(popupCloseListener)
            .createPopup();
        anchor.addHierarchyListener(new HierarchyListener() {
            @Override
            public void hierarchyChanged(HierarchyEvent e) {
                if (e.getID() == HierarchyEvent.HIERARCHY_CHANGED && !anchor.isValid()) {
                    anchor.removeHierarchyListener(this);
                    if (myFilterPopup != null) {
                        myFilterPopup.cancel();
                    }
                }
            }
        });
        myFilterPopup.showUnderneathOf(anchor);
    }

    @Nonnull
    public String getDimensionServiceKey() {
        return "ShowFilterAction_Filter_Popup";
    }

    private JComponent createFilterPanel() {
        ElementsChooser<?> chooser = createChooser();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(chooser);
        JPanel buttons = new JPanel();
        JButton all = new JButton("All");
        all.addActionListener(e -> chooser.setAllElementsMarked(true));
        buttons.add(all);
        JButton none = new JButton("None");
        none.addActionListener(e -> chooser.setAllElementsMarked(false));
        buttons.add(none);
        JButton invert = new JButton("Invert");
        invert.addActionListener(e -> chooser.invertSelection());
        buttons.add(invert);
        panel.add(buttons);
        return panel;
    }

    protected abstract ElementsChooser<?> createChooser();
}
