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
package consulo.execution.debug.impl.internal.setting;

import consulo.execution.debug.evaluation.EvaluationMode;
import consulo.execution.debug.setting.XDebuggerSettingsManager;
import consulo.util.xml.serializer.annotation.Tag;

/**
 * @author egor
 */
@Tag("general")
public class XDebuggerGeneralSettings implements XDebuggerSettingsManager.GeneralViewSettings {
    private EvaluationMode myEvaluationDialogMode = EvaluationMode.EXPRESSION;
    private boolean myUnmuteOnStop = false;

    private boolean hideDebuggerOnProcessTermination;
    private boolean myShowDebuggerOnBreakpoint = true;
    private boolean myMayBringFrameToFrontOnBreakpoint = true;
    private boolean myScrollToCenter = false;
    private boolean singleClickForDisablingBreakpoint = false;

    @Tag("evaluation-dialog-mode")
    public EvaluationMode getEvaluationDialogMode() {
        return myEvaluationDialogMode;
    }

    public void setEvaluationDialogMode(EvaluationMode evaluationDialogMode) {
        myEvaluationDialogMode = evaluationDialogMode;
    }

    @Tag("unmute-on-stop")
    public boolean isUnmuteOnStop() {
        return myUnmuteOnStop;
    }

    public void setUnmuteOnStop(boolean unmuteOnStop) {
        myUnmuteOnStop = unmuteOnStop;
    }

    public boolean isHideDebuggerOnProcessTermination() {
        return hideDebuggerOnProcessTermination;
    }

    public void setHideDebuggerOnProcessTermination(boolean hideDebuggerOnProcessTermination) {
        this.hideDebuggerOnProcessTermination = hideDebuggerOnProcessTermination;
    }

    public boolean isShowDebuggerOnBreakpoint() {
        return myShowDebuggerOnBreakpoint;
    }

    public void setShowDebuggerOnBreakpoint(boolean showDebuggerOnBreakpoint) {
        this.myShowDebuggerOnBreakpoint = showDebuggerOnBreakpoint;
    }

    public boolean isMayBringFrameToFrontOnBreakpoint() {
        return myMayBringFrameToFrontOnBreakpoint;
    }

    public void setMayBringFrameToFrontOnBreakpoint(boolean mayBringFrameToFrontOnBreakpoint) {
        myMayBringFrameToFrontOnBreakpoint = mayBringFrameToFrontOnBreakpoint;
    }

    @Tag("scroll-to-center")
    public boolean isScrollToCenter() {
        return myScrollToCenter;
    }

    public void setScrollToCenter(boolean scrollToCenter) {
        myScrollToCenter = scrollToCenter;
    }

    @Override
    @Tag("single-click-for-disabling-breakpoint")
    public boolean isSingleClickForDisablingBreakpoint() {
        return singleClickForDisablingBreakpoint;
    }

    public void setSingleClickForDisablingBreakpoint(boolean singleClickForDisablingBreakpoint) {
        this.singleClickForDisablingBreakpoint = singleClickForDisablingBreakpoint;
    }
}
