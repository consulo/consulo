package consulo.execution.debug.breakpoint;

import consulo.execution.debug.evaluation.XDebuggerEditorsProvider;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

public abstract class XLineBreakpointTypeBase extends XLineBreakpointType<XBreakpointProperties> {
  private final XDebuggerEditorsProvider myEditorsProvider;

  protected XLineBreakpointTypeBase(String id, String title, @Nullable XDebuggerEditorsProvider editorsProvider) {
    super(id, title);

    myEditorsProvider = editorsProvider;
  }

  @Nullable
  @Override
  public XDebuggerEditorsProvider getEditorsProvider(XLineBreakpoint<XBreakpointProperties> breakpoint, Project project) {
    return myEditorsProvider;
  }

  @Override
  @Nullable
  public XBreakpointProperties createBreakpointProperties(VirtualFile file, int line) {
    return null;
  }
}