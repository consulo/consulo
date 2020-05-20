/*
 * Copyright 2013-2020 consulo.io
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
package consulo.localize;

import javax.annotation.Nonnull;
import java.util.EventListener;
import java.util.Locale;

/**
 * @author VISTALL
 * @since 2020-05-20
 */
public interface LocalizeManagerListener extends EventListener  {
  void localeChanged(@Nonnull Locale oldLocale, @Nonnull Locale newLocale);
}
