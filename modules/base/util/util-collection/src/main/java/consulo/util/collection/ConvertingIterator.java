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
  private final Function<Domain, Range> myConvertor;

  public ConvertingIterator(Iterator<Domain> baseIterator, Function<Domain, Range> convertor) {
    myBaseIterator = baseIterator;
    myConvertor = convertor;
  }

  @Override
  public boolean hasNext() {
    return myBaseIterator.hasNext();
  }

  @Override
  public Range next() {
    return myConvertor.apply(myBaseIterator.next());
  }

  @Override
  public void remove() {
    myBaseIterator.remove();
  }

  public static <Domain, Intermediate, Range> Function<Domain, Range> composition(final Function<Domain, Intermediate> convertor1,
                                                                                   final Function<Intermediate, Range> convertor2) {
    return domain -> convertor2.apply(convertor1.apply(domain));
  }

  public static <Domain, Range> ConvertingIterator<Domain, Range>
    create(Iterator<Domain> iterator, Function<Domain, Range> convertor) {
    return new ConvertingIterator<Domain, Range>(iterator, convertor);
  }
}
