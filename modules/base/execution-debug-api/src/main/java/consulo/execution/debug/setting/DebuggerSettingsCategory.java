package consulo.execution.debug.setting;

import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

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

    @Nonnull
    public LocalizeValue getDisplayName() {
        return myDisplayName;
    }
}