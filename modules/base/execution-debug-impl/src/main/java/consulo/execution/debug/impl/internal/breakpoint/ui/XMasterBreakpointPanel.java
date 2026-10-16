/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.execution.debug.impl.internal.breakpoint.ui;

import consulo.execution.debug.XBreakpointManager;
import consulo.execution.debug.breakpoint.XBreakpoint;
import consulo.execution.debug.impl.internal.action.handler.XBreakpointPanelProvider;
import consulo.execution.debug.impl.internal.breakpoint.XBreakpointBase;
import consulo.execution.debug.impl.internal.breakpoint.XBreakpointManagerImpl;
import consulo.execution.debug.impl.internal.breakpoint.XDependentBreakpointManagerImpl;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.project.Project;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.RadioButton;
import consulo.ui.RadioGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;
import org.jspecify.annotations.Nullable;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XMasterBreakpointPanel extends XBreakpointPropertiesSubPanel {
    private final VerticalLayout myContentPane = VerticalLayout.create();

    private ComboBox<BreakpointItem> myMasterBreakpointComboBox;
    private RadioGroup<Boolean> myLeaveEnabledGroup;
    private Label myAfterBreakpointHitLabel;
    private RadioButton myDisableAgainRadioButton;
    private RadioButton myLeaveEnabledRadioButton;
    private XDependentBreakpointManagerImpl myDependentBreakpointManager;

    private List<BreakpointItem> getBreakpointItemsExceptMy() {
        List<BreakpointItem> items = new ArrayList<>();
        XBreakpointPanelProvider.getInstance().provideBreakpointItems(myProject, items);
        for (BreakpointItem item : items) {
            if (item.getBreakpoint() == myBreakpoint) {
                items.remove(item);
                break;
            }
        }
        items.add(new BreakpointNoneItem());
        return items;
    }

    @RequiredUIAccess
    @Override
    public void init(Project project, XBreakpointManager breakpointManager, XBreakpointBase breakpoint) {
        super.init(project, breakpointManager, breakpoint);
        myDependentBreakpointManager = ((XBreakpointManagerImpl) breakpointManager).getDependentBreakpointManager();

        List<BreakpointItem> items = getBreakpointItemsExceptMy();
        Map<BreakpointItem, String> texts = new HashMap<>();
        Map<BreakpointItem, Image> icons = new HashMap<>();
        for (BreakpointItem item : items) {
            texts.put(item, item.getDisplayText());
            Image icon = item.getIcon();
            if (icon != null) {
                icons.put(item, icon);
            }
        }

        myMasterBreakpointComboBox = ComboBox.create(items);
        myMasterBreakpointComboBox.setRenderer((presentation, renderItem) -> {
            BreakpointItem item = renderItem.getValue();
            if (item == null) {
                return;
            }
            presentation.withIcon(icons.get(item));
            presentation.append(texts.getOrDefault(item, ""));
        });
        myMasterBreakpointComboBox.setValue(items.get(items.size() - 1), false);
        myMasterBreakpointComboBox.addValueListener(event -> updateAfterBreakpointHitPanel());

        myLeaveEnabledGroup = RadioGroup.create();
        myDisableAgainRadioButton = myLeaveEnabledGroup.newButton(XDebuggerLocalize.xbreakpointRadioDisableAgain(), Boolean.FALSE);
        myLeaveEnabledRadioButton = myLeaveEnabledGroup.newButton(XDebuggerLocalize.xbreakpointRadioLeaveEnabled(), Boolean.TRUE);
        myLeaveEnabledGroup.setValue(Boolean.FALSE, false);

        myAfterBreakpointHitLabel = Label.create(XDebuggerLocalize.xbreakpointLabelAfterBreakpointWasHit());

        myContentPane.add(Label.create(XDebuggerLocalize.xbreakpointMasterBreakpointDescription()));
        myContentPane.add(myMasterBreakpointComboBox);
        myContentPane.add(HorizontalLayout.create()
            .add(myAfterBreakpointHitLabel)
            .add(myDisableAgainRadioButton)
            .add(myLeaveEnabledRadioButton));
    }

    @Override
    public Component getComponent() {
        return myContentPane;
    }

    @RequiredUIAccess
    @Override
    public boolean lightVariant(boolean showAllOptions) {
        XBreakpoint<?> masterBreakpoint = myDependentBreakpointManager.getMasterBreakpoint(myBreakpoint);
        if (!showAllOptions && masterBreakpoint == null) {
            myContentPane.setVisible(false);
            return true;
        }
        return false;
    }

    private @Nullable XBreakpoint<?> getSelectedBreakpoint() {
        BreakpointItem item = myMasterBreakpointComboBox.getValue();
        return item == null ? null : (XBreakpoint<?>) item.getBreakpoint();
    }

    @RequiredUIAccess
    private void updateAfterBreakpointHitPanel() {
        boolean enable = getSelectedBreakpoint() != null;
        myAfterBreakpointHitLabel.setEnabled(enable);
        myDisableAgainRadioButton.setEnabled(enable);
        myLeaveEnabledRadioButton.setEnabled(enable);
    }

    @RequiredUIAccess
    @Override
    void loadProperties() {
        XBreakpoint<?> masterBreakpoint = myDependentBreakpointManager.getMasterBreakpoint(myBreakpoint);
        if (masterBreakpoint != null) {
            for (BreakpointItem item : myMasterBreakpointComboBox.getDataModel()) {
                if (item.getBreakpoint() == masterBreakpoint) {
                    myMasterBreakpointComboBox.setValue(item, false);
                    break;
                }
            }
            myLeaveEnabledGroup.setValue(myDependentBreakpointManager.isLeaveEnabled(myBreakpoint), false);
        }
        updateAfterBreakpointHitPanel();
    }

    @RequiredUIAccess
    @Override
    void saveProperties() {
        XBreakpoint<?> masterBreakpoint = getSelectedBreakpoint();
        if (masterBreakpoint == null) {
            myDependentBreakpointManager.clearMasterBreakpoint(myBreakpoint);
        }
        else {
            myDependentBreakpointManager.setMasterBreakpoint(myBreakpoint, masterBreakpoint, Boolean.TRUE.equals(myLeaveEnabledGroup.getValue()));
        }
    }

    public void hide() {
        myContentPane.setVisible(false);
    }
}
