/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.codeEditor;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.document.Document;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.function.IntFunction;

/**
 * An implementation of this extension point can draw additional text fragments
 * after the end of a line in a file editor.
 * <p>
 * If you need to do this in a particular {@link Editor} instance,
 * use {@link EditorEx#registerLineExtensionPainter(IntFunction)} instead.
 *
 * @author Konstantin Bulenkov
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface EditorLinePainter {
  public static final ExtensionPointName<EditorLinePainter> EP_NAME = ExtensionPointName.create(EditorLinePainter.class);

  @Nullable
  Collection<LineExtensionInfo> getLineExtensions(@Nonnull Project project,
                                                  @Nonnull VirtualFile file,
                                                  @Nonnull Document document,
                                                  int lineNumber);
}
