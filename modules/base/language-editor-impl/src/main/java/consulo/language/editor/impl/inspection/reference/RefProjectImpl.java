/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.language.editor.impl.inspection.reference;

import consulo.application.Application;
import consulo.language.editor.inspection.reference.RefManager;
import consulo.language.editor.inspection.reference.RefProject;
import consulo.ui.image.Image;

/**
 * @author max
 * @since 2001-11-16
 */
public class RefProjectImpl extends RefEntityImpl implements RefProject {
  public RefProjectImpl(RefManager refManager) {
    super(refManager.getProject().getName(), refManager);
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public Image getIcon(final boolean expanded) {
    return Application.get().getIcon();
  }
}
