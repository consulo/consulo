package com.intellij.xdebugger.breakpoints;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 19.03.2015
 */
@Deprecated
public class OldXLineBreakpointResolver implements XLineBreakpointResolver {
  public static final OldXLineBreakpointResolver INSTANCE = new OldXLineBreakpointResolver();

  @Nullable
  @Override
  public XLineBreakpointType<?> resolveBreakpointType(@NotNull Project project, @NotNull VirtualFile virtualFile, int line) {
    for (XBreakpointType xBreakpointType : XBreakpointType.EXTENSION_POINT_NAME.getExtensions()) {
      if (xBreakpointType instanceof XLineBreakpointType && ((XLineBreakpointType)xBreakpointType).canPutAt(virtualFile, line, project)) {
        return (XLineBreakpointType<?>)xBreakpointType;
      }
    }
    return null;
  }
}
