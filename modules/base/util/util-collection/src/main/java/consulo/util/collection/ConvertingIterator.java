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
package consulo.util.collection;

import java.util.Iterator;
import java.util.function.Function;

/**
 * @author dsl
 */
public class ConvertingIterator <Domain, Range> implements Iterator<Range> {
  private final Iterator<Domain> myBaseIterator;
  private final Function<Domain, Range> myConverter;

  public ConvertingIterator(Iterator<Domain> baseIterator, Function<Domain, Range> converter) {
    myBaseIterator = baseIterator;
    myConverter = converter;
  }

  @Override
  public boolean hasNext() {
    return myBaseIterator.hasNext();
  }

  @Override
  public Range next() {
    return myConverter.apply(myBaseIterator.next());
  }

  @Override
  public void remove() {
    myBaseIterator.remove();
  }

  public static <Domain, Intermediate, Range> Function<Domain, Range> composition(Function<Domain, Intermediate> converter1,
                                                                                  Function<Intermediate, Range> converter2) {
    return domain -> converter2.apply(converter1.apply(domain));
  }

  public static <Domain, Range> ConvertingIterator<Domain, Range>
    create(Iterator<Domain> iterator, Function<Domain, Range> converter) {
    return new ConvertingIterator<Domain, Range>(iterator, converter);
  }
}
