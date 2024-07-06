/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.desktop.awt.internal.diff.dir.action.popup;

import consulo.diff.dir.DirDiffElement;
import consulo.diff.dir.DirDiffOperation;
import jakarta.annotation.Nonnull;

/**
 * @author Konstantin Bulenkov
 */
public class SetCopyToRight extends SetOperationToBase {
  @Nonnull
  @Override
  protected DirDiffOperation getOperation() {
    return DirDiffOperation.COPY_TO;
  }

  @Override
  protected boolean isEnabledFor(DirDiffElement element) {
    return element.getSource() != null;
  }
}
