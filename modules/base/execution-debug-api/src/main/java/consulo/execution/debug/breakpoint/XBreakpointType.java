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

package consulo.execution.debug.breakpoint;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.breakpoint.ui.XBreakpointCustomPropertiesPanel;
import consulo.execution.debug.breakpoint.ui.XBreakpointGroupingRule;
import consulo.execution.debug.evaluation.XDebuggerEditorsProvider;
import consulo.execution.debug.icon.ExecutionDebugIconGroup;
import consulo.project.Project;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * Implement this class to support new type of breakpoints. An implementation should be registered in a plugin.xml:
 * <p>
 * &lt;extensions defaultExtensionNs="com.intellij"&gt;<br>
 * &nbsp;&nbsp;&lt;xdebugger.breakpointType implementation="qualified-class-name"/&gt;<br>
 * &lt;/extensions&gt;
 * <p><p>
 * <p>
 * Use this class only for breakpoints like exception breakpoints in Java. If a breakpoint will be put on some line in a file use
 * {@link XLineBreakpointType} instead
 *
 * @author nik
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class XBreakpointType<B extends XBreakpoint<P>, P extends XBreakpointProperties> {
    public static final ExtensionPointName<XBreakpointType> EXTENSION_POINT_NAME = ExtensionPointName.create(XBreakpointType.class);
    @Nonnull
    private final String myId;
    @Nonnull
    private final String myTitle;
    private final boolean mySuspendThreadSupported;

    /**
     * @param id    an unique id of breakpoint type
     * @param title title of tab in the breakpoints dialog
     */
    protected XBreakpointType(@Nonnull String id, @Nls @Nonnull String title) {
        this(id, title, false);
    }

    /**
     * @param id                     an unique id of breakpoint type
     * @param title                  title of tab in the breakpoints dialog
     * @param suspendThreadSupported <code>true</code> if suspending only one thread is supported for this type of breakpoints
     */
    protected XBreakpointType(@Nonnull String id, @Nls @Nonnull String title, boolean suspendThreadSupported) {
        myId = id;
        myTitle = title;
        mySuspendThreadSupported = suspendThreadSupported;
    }

    @Nullable
    public P createProperties() {
        return null;
    }

    /**
     * @return {@code true} if suspending only one thread is supported
     */
    public boolean isSuspendThreadSupported() {
        return mySuspendThreadSupported;
    }

    public SuspendPolicy getDefaultSuspendPolicy() {
        return SuspendPolicy.ALL;
    }

    public enum StandardPanels {
        SUSPEND_POLICY,
        ACTIONS,
        DEPENDENCY
    }

    public EnumSet<StandardPanels> getVisibleStandardPanels() {
        return EnumSet.allOf(StandardPanels.class);
    }

    @Nonnull
    public final String getId() {
        return myId;
    }

    @Nonnull
    public String getTitle() {
        return myTitle;
    }

    @Nonnull
    public Image getEnabledIcon() {
        return ExecutionDebugIconGroup.breakpointBreakpoint();
    }

    @Nonnull
    public Image getDisabledIcon() {
        return ExecutionDebugIconGroup.breakpointBreakpointdisabled();
    }

    @Nonnull
    public Image getMutedEnabledIcon() {
        return ExecutionDebugIconGroup.breakpointBreakpointmuted();
    }

    @Nonnull
    public Image getMutedDisabledIcon() {
        return ExecutionDebugIconGroup.breakpointBreakpointmuteddisabled();
    }

    /**
     * @return the icon which is shown for a dependent breakpoint until its master breakpoint is reached
     */
    @Nonnull
    public Image getInactiveDependentIcon() {
        return ExecutionDebugIconGroup.breakpointBreakpointdependent();
    }

    public abstract String getDisplayText(B breakpoint);

    @Nullable
    public XBreakpointCustomPropertiesPanel<B> createCustomConditionsPanel() {
        return null;
    }

    @Nullable
    public XBreakpointCustomPropertiesPanel<B> createCustomPropertiesPanel(@Nonnull Project project) {
        return null;
    }

    @Nullable
    public XBreakpointCustomPropertiesPanel<B> createCustomRightPropertiesPanel(@Nonnull Project project) {
        return null;
    }

    @Nullable
    public XBreakpointCustomPropertiesPanel<B> createCustomTopPropertiesPanel(@Nonnull Project project) {
        return null;
    }

    @Nullable
    public XDebuggerEditorsProvider getEditorsProvider(@Nonnull B breakpoint, @Nonnull Project project) {
        return null;
    }

    public List<XBreakpointGroupingRule<B, ?>> getGroupingRules() {
        return Collections.emptyList();
    }

    @Nonnull
    public Comparator<B> getBreakpointComparator() {
        return (b, b1) -> (int) (b1.getTimeStamp() - b.getTimeStamp());
        //return XDebuggerUtil.getInstance().getDefaultBreakpointComparator(this);
    }

    /**
     * Return <code>true</code> from this method in order to allow adding breakpoints from the "Breakpoints" dialog. Also override
     * {@link XBreakpointType#addBreakpoint(Project, JComponent)} method.
     *
     * @return <code>true</code> if "Add" button should be visible in "Breakpoints" dialog
     */
    public boolean isAddBreakpointButtonVisible() {
        return false;
    }

    /**
     * This method is called then "Add" button is pressed in the "Breakpoints" dialog
     *
     * @param project
     * @param parentComponent
     * @return the created breakpoint or <code>null</code> if breakpoint wasn't created
     */
    @Nullable
    public B addBreakpoint(Project project, JComponent parentComponent) {
        return null;
    }

    /**
     * Returns properties of the default breakpoint. The default breakpoints cannot be deleted and is always shown on top of the breakpoints
     * list in the dialog.
     *
     * @return a default breakpoint or {@code null} if default breakpoint isn't supported
     */
    @Nullable
    public XBreakpoint<P> createDefaultBreakpoint(@Nonnull XBreakpointCreator<P> creator) {
        return null;
    }

    public boolean shouldShowInBreakpointsDialog(@Nonnull Project project) {
        return true;
    }

    @Nullable
    public String getBreakpointsDialogHelpTopic() {
        return null;
    }

    /**
     * Override this method to define source position for a breakpoint. It will be used e.g. by 'Go To' and 'View Source' buttons in 'Breakpoints' dialog
     */
    @Nullable
    public XSourcePosition getSourcePosition(@Nonnull XBreakpoint<P> breakpoint) {
        return null;
    }

    public String getShortText(B breakpoint) {
        return getDisplayText(breakpoint);
    }

    public interface XBreakpointCreator<P extends XBreakpointProperties> {
        @Nonnull
        XBreakpoint<P> createBreakpoint(@Nullable P properties);
    }
}
