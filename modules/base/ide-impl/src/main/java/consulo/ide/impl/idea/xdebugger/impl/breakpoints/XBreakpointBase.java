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
package consulo.ide.impl.idea.xdebugger.impl.breakpoints;

import consulo.application.dumb.DumbAware;
import consulo.codeEditor.markup.GutterDraggableObject;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.component.persist.ComponentSerializationUtil;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.XDebuggerUtil;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.breakpoint.*;
import consulo.execution.debug.icon.ExecutionDebugIconGroup;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.execution.debug.setting.XDebuggerSettingsManager;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.ide.impl.idea.xdebugger.impl.DebuggerSupport;
import consulo.ide.impl.idea.xdebugger.impl.XDebugSessionImpl;
import consulo.ide.impl.idea.xdebugger.impl.XDebuggerSupport;
import consulo.ide.impl.idea.xdebugger.impl.actions.EditBreakpointAction;
import consulo.ide.impl.idea.xml.CommonXmlStrings;
import consulo.ide.impl.idea.xml.util.XmlStringUtil;
import consulo.localize.LocalizeValue;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.SkipDefaultValuesSerializationFilters;
import consulo.util.xml.serializer.XmlSerializer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Element;

import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public class XBreakpointBase<Self extends XBreakpoint<P>, P extends XBreakpointProperties, S extends BreakpointState> extends UserDataHolderBase implements XBreakpoint<P>, Comparable<Self> {
    private static final SkipDefaultValuesSerializationFilters SERIALIZATION_FILTERS = new SkipDefaultValuesSerializationFilters();
    private static final String BR_NBSP = "<br>" + CommonXmlStrings.NBSP;
    private final XBreakpointType<Self, P> myType;
    private final
    @Nullable
    P myProperties;
    protected final S myState;
    private final XBreakpointManagerImpl myBreakpointManager;
    private Image myIcon;
    private CustomizedBreakpointPresentation myCustomizedPresentation;
    private boolean myConditionEnabled = true;
    private XExpression myCondition;
    private boolean myLogExpressionEnabled = true;
    private XExpression myLogExpression;
    private volatile boolean myDisposed;

    public XBreakpointBase(
        final XBreakpointType<Self, P> type,
        XBreakpointManagerImpl breakpointManager,
        final @Nullable P properties,
        final S state
    ) {
        myState = state;
        myType = type;
        myProperties = properties;
        myBreakpointManager = breakpointManager;
        initExpressions();
    }

    protected XBreakpointBase(final XBreakpointType<Self, P> type, XBreakpointManagerImpl breakpointManager, S breakpointState) {
        myState = breakpointState;
        myType = type;
        myBreakpointManager = breakpointManager;
        myProperties = type.createProperties();
        if (myProperties != null) {
            ComponentSerializationUtil.loadComponentState(myProperties, myState.getPropertiesElement());
        }
        initExpressions();
    }

    private void initExpressions() {
        myConditionEnabled = myState.isConditionEnabled();
        BreakpointState.Condition condition = myState.getCondition();
        myCondition = condition != null ? condition.toXExpression() : null;
        myLogExpressionEnabled = myState.isLogExpressionEnabled();
        BreakpointState.LogExpression expression = myState.getLogExpression();
        myLogExpression = expression != null ? expression.toXExpression() : null;
    }

    @Nonnull
    @Override
    public final Project getProject() {
        return myBreakpointManager.getProject();
    }

    @Nonnull
    @Override
    public XBreakpointManagerImpl getBreakpointManager() {
        return myBreakpointManager;
    }

    public final void fireBreakpointChanged() {
        clearIcon();
        myBreakpointManager.fireBreakpointChanged(this);
    }

    @Override
    public XSourcePosition getSourcePosition() {
        return getType().getSourcePosition(this);
    }

    @Override
    public Navigatable getNavigatable() {
        XSourcePosition position = getSourcePosition();
        if (position == null) {
            return null;
        }
        return position.createNavigatable(getProject());
    }

    @Override
    public boolean isEnabled() {
        return myState.isEnabled();
    }

    @Override
    public void setEnabled(final boolean enabled) {
        if (enabled != isEnabled()) {
            myState.setEnabled(enabled);
            fireBreakpointChanged();
        }
    }

    @Override
    @Nonnull
    public SuspendPolicy getSuspendPolicy() {
        return myState.getSuspendPolicy();
    }

    @Override
    public void setSuspendPolicy(@Nonnull SuspendPolicy policy) {
        if (myState.getSuspendPolicy() != policy) {
            myState.setSuspendPolicy(policy);
            fireBreakpointChanged();
        }
    }

    @Override
    public boolean isLogMessage() {
        return myState.isLogMessage();
    }

    @Override
    public void setLogMessage(final boolean logMessage) {
        if (logMessage != isLogMessage()) {
            myState.setLogMessage(logMessage);
            fireBreakpointChanged();
        }
    }

    public boolean isConditionEnabled() {
        return myConditionEnabled;
    }

    public void setConditionEnabled(boolean conditionEnabled) {
        if (myConditionEnabled != conditionEnabled) {
            myConditionEnabled = conditionEnabled;
            fireBreakpointChanged();
        }
    }

    public boolean isLogExpressionEnabled() {
        return myLogExpressionEnabled;
    }

    public void setLogExpressionEnabled(boolean logExpressionEnabled) {
        if (myLogExpressionEnabled != logExpressionEnabled) {
            myLogExpressionEnabled = logExpressionEnabled;
            fireBreakpointChanged();
        }
    }

    @Override
    public String getLogExpression() {
        XExpression expression = getLogExpressionObject();
        return expression != null ? expression.getExpression() : null;
    }

    @Override
    public void setLogExpression(@Nullable final String expression) {
        if (!Comparing.equal(getLogExpression(), expression)) {
            myLogExpression = XExpression.fromText(expression);
            fireBreakpointChanged();
        }
    }

    public XExpression getLogExpressionObjectInt() {
        return myLogExpression;
    }

    @Nullable
    @Override
    public XExpression getLogExpressionObject() {
        return myLogExpressionEnabled ? myLogExpression : null;
    }

    @Override
    public void setLogExpressionObject(@Nullable XExpression expression) {
        if (!Comparing.equal(myLogExpression, expression)) {
            myLogExpression = expression;
            fireBreakpointChanged();
        }
    }

    @Override
    public String getCondition() {
        XExpression expression = getConditionExpression();
        return expression != null ? expression.getExpression() : null;
    }

    @Override
    public void setCondition(@Nullable final String condition) {
        if (!Comparing.equal(condition, getCondition())) {
            myCondition = XExpression.fromText(condition);
            fireBreakpointChanged();
        }
    }

    public XExpression getConditionExpressionInt() {
        return myCondition;
    }

    @Nullable
    @Override
    public XExpression getConditionExpression() {
        return myConditionEnabled ? myCondition : null;
    }

    @Override
    public void setConditionExpression(@Nullable XExpression condition) {
        if (!Comparing.equal(condition, myCondition)) {
            myCondition = condition;
            fireBreakpointChanged();
        }
    }

    @Override
    public long getTimeStamp() {
        return myState.getTimeStamp();
    }

    public boolean isValid() {
        return true;
    }

    @Override
    @Nullable
    public P getProperties() {
        return myProperties;
    }

    @Override
    @Nonnull
    public XBreakpointType<Self, P> getType() {
        return myType;
    }

    public S getState() {
        Element propertiesElement = myProperties != null ? XmlSerializer.serialize(myProperties.getState(), SERIALIZATION_FILTERS) : null;
        myState.setCondition(BreakpointState.Condition.create(!myConditionEnabled, myCondition));
        myState.setLogExpression(BreakpointState.LogExpression.create(!myLogExpressionEnabled, myLogExpression));
        myState.setPropertiesElement(propertiesElement);
        return myState;
    }

    public XBreakpointDependencyState getDependencyState() {
        return myState.getDependencyState();
    }

    public void setDependencyState(XBreakpointDependencyState state) {
        myState.setDependencyState(state);
    }

    @Nullable
    public String getGroup() {
        return myState.getGroup();
    }

    public void setGroup(String group) {
        myState.setGroup(StringUtil.nullize(group));
    }

    public String getUserDescription() {
        return myState.getDescription();
    }

    public void setUserDescription(String description) {
        myState.setDescription(StringUtil.nullize(description));
    }

    public final void dispose() {
        myDisposed = true;
        doDispose();
    }

    protected void doDispose() {
    }

    public boolean isDisposed() {
        return myDisposed;
    }

    @Override
    public String toString() {
        return "XBreakpointBase(type=" + myType + ")";
    }

    @Nullable
    protected GutterDraggableObject createBreakpointDraggableObject() {
        return null;
    }

    protected List<? extends AnAction> getAdditionalPopupMenuActions(XDebugSession session) {
        return Collections.emptyList();
    }

    @Nonnull
    public String getDescription() {
        StringBuilder builder = new StringBuilder();
        builder.append(CommonXmlStrings.HTML_START).append(CommonXmlStrings.BODY_START);
        builder.append(XBreakpointUtil.getDisplayText(this));

        String errorMessage = getErrorMessage();
        if (!StringUtil.isEmpty(errorMessage)) {
            builder.append(BR_NBSP);
            builder.append("<font color='#").append(ColorUtil.toHex(JBColor.RED)).append("'>");
            builder.append(errorMessage);
            builder.append("</font>");
        }

        if (getSuspendPolicy() == SuspendPolicy.NONE) {
            builder.append(BR_NBSP).append(XDebuggerLocalize.xbreakpointTooltipSuspendPolicyNone());
        }
        else if (getType().isSuspendThreadSupported()) {
            builder.append(BR_NBSP);
            //noinspection EnumSwitchStatementWhichMissesCases
            switch (getSuspendPolicy()) {
                case ALL:
                    builder.append(XDebuggerLocalize.xbreakpointTooltipSuspendPolicyAll());
                    break;
                case THREAD:
                    builder.append(XDebuggerLocalize.xbreakpointTooltipSuspendPolicyThread());
                    break;
            }
        }

        String condition = getCondition();
        if (!StringUtil.isEmpty(condition)) {
            builder.append(BR_NBSP);
            builder.append(XDebuggerLocalize.xbreakpointTooltipCondition());
            builder.append(CommonXmlStrings.NBSP);
            builder.append(XmlStringUtil.escapeString(condition));
        }

        if (isLogMessage()) {
            builder.append(BR_NBSP).append(XDebuggerLocalize.xbreakpointTooltipLogMessage());
        }

        String logExpression = getLogExpression();
        if (!StringUtil.isEmpty(logExpression)) {
            builder.append(BR_NBSP);
            builder.append(XDebuggerLocalize.xbreakpointTooltipLogExpression());
            builder.append(CommonXmlStrings.NBSP);
            builder.append(XmlStringUtil.escapeString(logExpression));
        }

        XBreakpoint<?> masterBreakpoint = getBreakpointManager().getDependentBreakpointManager().getMasterBreakpoint(this);
        if (masterBreakpoint != null) {
            builder.append(BR_NBSP);
            builder.append(XDebuggerLocalize.xbreakpointTooltipDependsOn());
            builder.append(CommonXmlStrings.NBSP);
            builder.append(XBreakpointUtil.getShortText(masterBreakpoint));
        }

        builder.append(CommonXmlStrings.BODY_END).append(CommonXmlStrings.HTML_END);
        return builder.toString();
    }

    protected void updateIcon() {
        final Image icon = calculateSpecialIcon();
        setIcon(icon != null ? icon : getType().getEnabledIcon());
    }

    protected void setIcon(@Nonnull Image icon) {
        if (!XDebuggerUtil.getInstance().isEmptyExpression(getConditionExpression())) {
            myIcon = ImageEffects.canvas(icon.getWidth(), icon.getHeight(), ctx -> {
                ctx.drawImage(icon, 0, 0);
                ctx.drawImage(ExecutionDebugIconGroup.breakpointQuestionbadge(), 7, 6, 7, 10);
            });
        }
        else {
            myIcon = icon;
        }
    }

    @Nullable
    protected final Image calculateSpecialIcon() {
        XDebugSessionImpl session = getBreakpointManager().getDebuggerManager().getCurrentSession();
        if (!isEnabled()) {
            // disabled icon takes precedence to other to visually distinguish it and provide feedback then it is enabled/disabled
            // (e.g. in case of mute-mode we would like to differentiate muted but enabled breakpoints from simply disabled ones)
            if (session == null || !session.areBreakpointsMuted()) {
                return getType().getDisabledIcon();
            }
            else {
                return getType().getMutedDisabledIcon();
            }
        }

        if (session == null) {
            if (getBreakpointManager().getDependentBreakpointManager().getMasterBreakpoint(this) != null) {
                return getType().getInactiveDependentIcon();
            }
        }
        else {
            if (session.areBreakpointsMuted()) {
                return getType().getMutedEnabledIcon();
            }
            if (session.isInactiveSlaveBreakpoint(this)) {
                return getType().getInactiveDependentIcon();
            }
            CustomizedBreakpointPresentation presentation = session.getBreakpointPresentation(this);
            if (presentation != null) {
                Image icon = presentation.getIcon();
                if (icon != null) {
                    return icon;
                }
            }
        }
        if (myCustomizedPresentation != null) {
            final Image icon = myCustomizedPresentation.getIcon();
            if (icon != null) {
                return icon;
            }
        }
        return null;
    }

    @Nonnull
    public Image getIcon() {
        if (myIcon == null) {
            updateIcon();
        }
        return myIcon;
    }

    @Nullable
    public String getErrorMessage() {
        final XDebugSessionImpl currentSession = getBreakpointManager().getDebuggerManager().getCurrentSession();
        if (currentSession != null) {
            CustomizedBreakpointPresentation presentation = currentSession.getBreakpointPresentation(this);
            if (presentation != null) {
                final String message = presentation.getErrorMessage();
                if (message != null) {
                    return message;
                }
            }
        }
        return myCustomizedPresentation != null ? myCustomizedPresentation.getErrorMessage() : null;
    }

    CustomizedBreakpointPresentation getCustomizedPresentation() {
        return myCustomizedPresentation;
    }

    public void setCustomizedPresentation(CustomizedBreakpointPresentation presentation) {
        myCustomizedPresentation = presentation;
    }

    @Nonnull
    public GutterIconRenderer createGutterIconRenderer() {
        return new BreakpointGutterIconRenderer();
    }

    public void clearIcon() {
        myIcon = null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public int compareTo(@Nonnull Self self) {
        return myType.getBreakpointComparator().compare((Self)this, self);
    }

    protected class BreakpointGutterIconRenderer extends GutterIconRenderer implements DumbAware {
        @Override
        @Nonnull
        public Image getIcon() {
            return XBreakpointBase.this.getIcon();
        }

        @Override
        @Nullable
        public AnAction getClickAction() {
            XDebuggerSettingsManager.GeneralViewSettings generalSettings = XDebuggerSettingsManager.getInstance().getGeneralSettings();
            if (generalSettings.isSingleClickForDisablingBreakpoint()) {
                return new ToggleBreakpointGutterIconAction(XBreakpointBase.this);
            }
            else {
                return new RemoveBreakpointGutterIconAction(XBreakpointBase.this);
            }
        }

        @Override
        @Nullable
        public AnAction getMiddleButtonClickAction() {
            XDebuggerSettingsManager.GeneralViewSettings generalSettings = XDebuggerSettingsManager.getInstance().getGeneralSettings();
            if (generalSettings.isSingleClickForDisablingBreakpoint()) {
                return new RemoveBreakpointGutterIconAction(XBreakpointBase.this);
            }
            else {
                return new ToggleBreakpointGutterIconAction(XBreakpointBase.this);
            }
        }

        @Nullable
        @Override
        public AnAction getRightButtonClickAction() {
            return new EditBreakpointAction.ContextAction(
                this,
                XBreakpointBase.this,
                DebuggerSupport.getDebuggerSupport(XDebuggerSupport.class)
            );
        }

        @Nonnull
        @Override
        public Alignment getAlignment() {
            return EditorUtil.isBreakPointsOnLineNumbers() ? Alignment.LINE_NUMBERS : Alignment.RIGHT;
        }

        @Override
        @Nullable
        public ActionGroup getPopupMenuActions() {
            return null;
        }

        @Nonnull
        @Override
        public LocalizeValue getTooltipValue() {
            return LocalizeValue.localizeTODO(getDescription());
        }

        @Override
        public GutterDraggableObject getDraggableObject() {
            return createBreakpointDraggableObject();
        }

        private XBreakpointBase<?, ?, ?> getBreakpoint() {
            return XBreakpointBase.this;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof XLineBreakpointImpl.BreakpointGutterIconRenderer gutterIconRenderer
                && getBreakpoint() == gutterIconRenderer.getBreakpoint()
                && Comparing.equal(getIcon(), gutterIconRenderer.getIcon());
        }

        @Override
        public int hashCode() {
            return getBreakpoint().hashCode();
        }
    }
}
