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
package consulo.language.editor.highlight.internal;

import consulo.application.Application;
import consulo.language.editor.highlight.HighlightInfo;
import consulo.language.editor.highlight.HighlightInfoType;

/**
 * @author VISTALL
 * @since 13-Feb-22
 */
public interface HighlightInfoFactory {
  static HighlightInfoFactory getInstance() {
    return Application.get().getInstance(HighlightInfoFactory.class);
  }

  HighlightInfo.Builder createBuilder(HighlightInfoType infoType);
}
