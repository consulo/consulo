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
import consulo.execution.debug.breakpoint.SuspendPolicy;
import consulo.execution.debug.impl.internal.breakpoint.XBreakpointBase;
import consulo.execution.debug.impl.internal.breakpoint.XBreakpointManagerImpl;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.project.Project;
import consulo.ui.Button;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.RadioButton;
import consulo.ui.RadioGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.HorizontalLayout;
import org.jspecify.annotations.Nullable;

public class XSuspendPolicyPanel extends XBreakpointPropertiesSubPanel {
    public interface Delegate {
        void showMoreOptionsIfNeeded();
    }

    private final HorizontalLayout myContentPane = HorizontalLayout.create();

    private CheckBox mySuspendCheckBox;
    private @Nullable RadioGroup<SuspendPolicy> mySuspendPolicyGroup;
    private @Nullable RadioButton mySuspendAll;
    private @Nullable RadioButton mySuspendThread;
    private @Nullable Button myMakeDefaultButton;

    private @Nullable Delegate myDelegate;

    @RequiredUIAccess
    @Override
    public void init(Project project, XBreakpointManager breakpointManager, XBreakpointBase breakpoint) {
        super.init(project, breakpointManager, breakpoint);

        mySuspendCheckBox = CheckBox.create(XDebuggerLocalize.suspendPolicyPanelSuspend());
        myContentPane.add(mySuspendCheckBox);

        mySuspendCheckBox.addValueListener(event -> {
            boolean selected = mySuspendCheckBox.getValueOrError();

            if (myBreakpoint.getType().isSuspendThreadSupported()) {
                changeEnableState(selected);
            }

            Delegate delegate = myDelegate;
            if (delegate != null && !selected) {
                delegate.showMoreOptionsIfNeeded();
            }
        });

        if (!myBreakpoint.getType().isSuspendThreadSupported()) {
            return;
        }

        RadioGroup<SuspendPolicy> group = RadioGroup.create();
        mySuspendPolicyGroup = group;
        mySuspendAll = group.newButton(XDebuggerLocalize.suspendPolicyPanelAll(), SuspendPolicy.ALL);
        mySuspendThread = group.newButton(XDebuggerLocalize.suspendPolicyPanelThread(), SuspendPolicy.THREAD);
        myContentPane.add(mySuspendAll);
        myContentPane.add(mySuspendThread);

        Button makeDefaultButton = Button.create(XDebuggerLocalize.suspendPolicyPanelMakedefault());
        myMakeDefaultButton = makeDefaultButton;
        myContentPane.add(makeDefaultButton);

        group.addValueListener(policy -> updateMakeDefaultEnableState());
        updateMakeDefaultEnableState();

        makeDefaultButton.addClickListener(event -> {
            SuspendPolicy suspendPolicy = getSelectedSuspendPolicy();
            ((XBreakpointManagerImpl) myBreakpointManager).getBreakpointDefaults(myBreakpointType).setSuspendPolicy(suspendPolicy);
            RadioButton focusTarget = SuspendPolicy.THREAD == suspendPolicy ? mySuspendThread : mySuspendAll;
            if (focusTarget != null) {
                focusTarget.focus();
            }
            updateMakeDefaultEnableState();
        });
    }

    @Override
    public Component getComponent() {
        return myContentPane;
    }

    @RequiredUIAccess
    private void updateMakeDefaultEnableState() {
        Button makeDefaultButton = myMakeDefaultButton;
        if (makeDefaultButton == null) {
            return;
        }
        boolean enabled = !getSelectedSuspendPolicy().equals(
            ((XBreakpointManagerImpl) myBreakpointManager).getBreakpointDefaults(myBreakpointType).getSuspendPolicy()
        );
        makeDefaultButton.setVisible(enabled);
        makeDefaultButton.setEnabled(enabled);
    }

    @RequiredUIAccess
    private void changeEnableState(boolean selected) {
        if (mySuspendAll != null) {
            mySuspendAll.setEnabled(selected);
        }
        if (mySuspendThread != null) {
            mySuspendThread.setEnabled(selected);
        }
        if (selected) {
            updateMakeDefaultEnableState();
        }
        else if (myMakeDefaultButton != null) {
            myMakeDefaultButton.setEnabled(false);
        }
    }

    public void hide() {
        myContentPane.setVisible(false);
    }

    @RequiredUIAccess
    @Override
    void loadProperties() {
        SuspendPolicy suspendPolicy = myBreakpoint.getSuspendPolicy();
        boolean selected = suspendPolicy != SuspendPolicy.NONE;

        RadioGroup<SuspendPolicy> group = mySuspendPolicyGroup;
        if (group != null) {
            group.setValue(suspendPolicy == SuspendPolicy.THREAD ? SuspendPolicy.THREAD : SuspendPolicy.ALL, false);
            changeEnableState(selected);
        }

        mySuspendCheckBox.setValue(selected, false);
        Delegate delegate = myDelegate;
        if (!selected && delegate != null) {
            delegate.showMoreOptionsIfNeeded();
        }
    }

    private SuspendPolicy getSelectedSuspendPolicy() {
        if (!mySuspendCheckBox.getValueOrError()) {
            return SuspendPolicy.NONE;
        }
        RadioGroup<SuspendPolicy> group = mySuspendPolicyGroup;
        if (group != null) {
            SuspendPolicy value = group.getValue();
            return value == null ? SuspendPolicy.ALL : value;
        }
        return myBreakpoint.getType().getDefaultSuspendPolicy();
    }

    @RequiredUIAccess
    @Override
    void saveProperties() {
        myBreakpoint.setSuspendPolicy(getSelectedSuspendPolicy());
    }

    public @Nullable Delegate getDelegate() {
        return myDelegate;
    }

    public void setDelegate(@Nullable Delegate delegate) {
        myDelegate = delegate;
    }
}
