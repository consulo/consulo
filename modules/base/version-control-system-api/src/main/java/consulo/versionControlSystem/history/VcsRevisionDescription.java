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
package consulo.versionControlSystem.history;

import jakarta.annotation.Nullable;
import java.util.Date;

/**
 * @author Irina.Chernushina
 * @since 2011-08-02
 */
public interface VcsRevisionDescription {
  VcsRevisionNumber getRevisionNumber();

  Date getRevisionDate();

  @Nullable
  String getAuthor();

  @Nullable
  String getCommitMessage();
}
