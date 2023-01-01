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
package consulo.ide.impl.idea.openapi.vcs.history;

import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsException;
import consulo.application.util.function.Processor;

import java.util.List;

/**
 * @author irengrig
 *         Date: 6/6/11
 *         Time: 5:28 PM
 */
public interface VcsBaseRevisionAdviser {
  /**
   * @return true if base revision was found by this provider
   */
  boolean getBaseVersionContent(final FilePath filePath, Processor<CharSequence> processor, String beforeVersionId, List<String> warnings) throws VcsException;
}
