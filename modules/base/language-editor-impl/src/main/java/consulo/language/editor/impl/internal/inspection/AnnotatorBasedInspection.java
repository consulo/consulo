/*
 * Copyright 2013-2022 consulo.io
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
package consulo.language.editor.impl.internal.inspection;

import consulo.annotation.component.ExtensionImpl;

import javax.annotation.Nonnull;

@ExtensionImpl
public class AnnotatorBasedInspection extends DefaultHighlightVisitorBasedInspection {
  private static final String ANNOTATOR_SHORT_NAME = "Annotator";

  public AnnotatorBasedInspection() {
    super(false, true);
  }

  @Override
  @Nonnull
  public String getDisplayName() {
    return "Annotator";
  }

  @Override
  @Nonnull
  public String getShortName() {
    return ANNOTATOR_SHORT_NAME;
  }
}
