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
package consulo.language;

import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * @author peter
 */
public class WeighingService {
  private WeighingService() {
  }

  @Nonnull
  public static <T, Loc> WeighingComparable<T, Loc> weigh(Key<? extends Weigher<T, Loc>> key,
                                                          T element,
                                                          @Nullable Loc location) {
    return weigh(key, (Supplier<T>)() -> element, location);
  }

  @Nonnull
  public static <T, Loc> WeighingComparable<T, Loc> weigh(Key<? extends Weigher<T, Loc>> key,
                                                          Supplier<T> element,
                                                          @Nullable Loc location) {
    List<Weigher> weighers = getWeighers(key);
    return new WeighingComparable<>(element, location, weighers.toArray(new Weigher[weighers.size()]));
  }

  public static <T, Loc> List<Weigher> getWeighers(Key<? extends Weigher<T, Loc>> key) {
    return Weigher.forKey(key);
  }
}
