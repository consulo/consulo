package consulo.execution.debug.setting;

import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.localize.LocalizeValue;

public enum DebuggerSettingsCategory {
    ROOT(LocalizeValue.empty()) /* will be placed under root "Debugger" node, use it with care */,
    GENERAL(LocalizeValue.empty()),
    DATA_VIEWS(XDebuggerLocalize.debuggerDataviewsDisplayName()),
    STEPPING(XDebuggerLocalize.debuggerSteppingDisplayName()),
    HOTSWAP(XDebuggerLocalize.debuggerHotswapDisplayName());

    private final LocalizeValue myDisplayName;

    DebuggerSettingsCategory(LocalizeValue displayName) {
        myDisplayName = displayName;
    }

    
    public LocalizeValue getDisplayName() {
        return myDisplayName;
    }
}