/*
 * Copyright 2013-2016 consulo.io
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
package consulo.xdebugger.breakpoints;

import com.intellij.openapi.fileTypes.FileTypeExtension;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.breakpoints.XLineBreakpointType;
import javax.annotation.Nonnull;

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

  @javax.annotation.Nullable
  public XLineBreakpointType<?> resolveBreakpointType(@Nonnull Project project, @Nonnull VirtualFile virtualFile, int line) {
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
