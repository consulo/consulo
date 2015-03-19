package com.intellij.xdebugger.breakpoints;

import com.intellij.openapi.fileTypes.FileTypeExtension;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 19.03.2015
 */
public class XLineBreakpointResolverExtension extends FileTypeExtension<XLineBreakpointResolver> {
  public static XLineBreakpointResolverExtension INSTANCE = new XLineBreakpointResolverExtension();

  public XLineBreakpointResolverExtension() {
    super("com.intellij.xdebugger.lineBreakpointResolver");
  }

  @Nullable
  public XLineBreakpointType<?> resolveBreakpointType(@NotNull Project project, @NotNull VirtualFile virtualFile, int line) {
    List<XLineBreakpointResolver> xLineBreakpointResolvers = new ArrayList<XLineBreakpointResolver>(allForFileType(virtualFile.getFileType()));
    xLineBreakpointResolvers.add(OldXLineBreakpointResolver.INSTANCE);

    for (XLineBreakpointResolver xLineBreakpointResolver : xLineBreakpointResolvers) {
      XLineBreakpointType<?> xLineBreakpointType = xLineBreakpointResolver.resolveBreakpointType(project, virtualFile, line);
      if(xLineBreakpointType != null) {
        return xLineBreakpointType;
      }
    }
    return null;
  }
}
