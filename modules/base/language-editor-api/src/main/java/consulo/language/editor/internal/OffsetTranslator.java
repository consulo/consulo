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
package consulo.language.editor.internal;

import consulo.util.dataholder.Key;

import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 12-Mar-22
 */
public interface OffsetTranslator {
  Key<OffsetTranslator> RANGE_TRANSLATION = Key.create("completion.rangeTranslation");

  @Nullable
  Integer translateOffset(Integer offset);
}
