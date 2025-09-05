/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.versionControlSystem.contentAnnotation;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.document.util.TextRange;
import consulo.versionControlSystem.VcsKey;
import consulo.versionControlSystem.annotate.FileAnnotation;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.virtualFileSystem.VirtualFile;
import consulo.util.lang.ThreeState;

import jakarta.annotation.Nullable;

/**
 * @author Irina.Chernushina
 * @since 2011-08-08
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface ContentAnnotationCache {
  @Nullable
  ThreeState isRecent(VirtualFile vf, VcsKey vcsKey, VcsRevisionNumber number, TextRange range, long boundTime);

  void register(VirtualFile vf, VcsKey vcsKey, VcsRevisionNumber number, FileAnnotation fa);
}
