/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

/*
 * @author max
 */
package consulo.ui.ex;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Service;
import consulo.application.Application;
import consulo.ui.image.Image;
import consulo.util.lang.Comparing;

import javax.annotation.Nonnull;
import java.util.function.Function;

@Service(ComponentScope.APPLICATION)
public abstract class IconDeferrer {
  @Nonnull
  public static IconDeferrer getInstance() {
    return Application.get().getInstance(IconDeferrer.class);
  }

  public abstract <T> Image defer(Image base, T param, @Nonnull Function<T, Image> f);

  public abstract <T> Image deferAutoUpdatable(Image base, T param, @Nonnull Function<T, Image> f);

  public boolean equalIcons(Image icon1, Image icon2) {
    return Comparing.equal(icon1, icon2);
  }
}