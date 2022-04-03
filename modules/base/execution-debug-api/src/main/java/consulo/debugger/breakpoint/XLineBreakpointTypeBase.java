package consulo.debugger.breakpoint;

import consulo.debugger.evaluation.XDebuggerEditorsProvider;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class XLineBreakpointTypeBase extends XLineBreakpointType<XBreakpointProperties> {
  private final XDebuggerEditorsProvider myEditorsProvider;

  protected XLineBreakpointTypeBase(@NonNls @Nonnull final String id, @Nls @Nonnull final String title, @Nullable XDebuggerEditorsProvider editorsProvider) {
    super(id, title);

    myEditorsProvider = editorsProvider;
  }

  @javax.annotation.Nullable
  @Override
  public XDebuggerEditorsProvider getEditorsProvider(@Nonnull XLineBreakpoint<XBreakpointProperties> breakpoint, @Nonnull Project project) {
    return myEditorsProvider;
  }

  @Override
  @Nullable
  public XBreakpointProperties createBreakpointProperties(@Nonnull final VirtualFile file, final int line) {
    return null;
  }
}