package consulo.xdebugger.breakpoints;

import com.intellij.openapi.fileTypes.FileTypeExtension;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.breakpoints.XLineBreakpointType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 19.03.2015
 */
public class XLineBreakpointResolverTypeExtension extends FileTypeExtension<XLineBreakpointTypeResolver> {
  public static XLineBreakpointResolverTypeExtension INSTANCE = new XLineBreakpointResolverTypeExtension();

  public XLineBreakpointResolverTypeExtension() {
    super("com.intellij.xdebugger.lineBreakpointTypeResolver");
  }

  @Nullable
  public XLineBreakpointType<?> resolveBreakpointType(@NotNull Project project, @NotNull VirtualFile virtualFile, int line) {
    List<XLineBreakpointTypeResolver> resolvers = new ArrayList<XLineBreakpointTypeResolver>(allForFileType(virtualFile.getFileType()));
    for (XLineBreakpointTypeResolver resolver : resolvers) {
      XLineBreakpointType<?> breakpointType = resolver.resolveBreakpointType(project, virtualFile, line);
      if(breakpointType != null) {
        return breakpointType;
      }
    }
    return null;
  }
}
