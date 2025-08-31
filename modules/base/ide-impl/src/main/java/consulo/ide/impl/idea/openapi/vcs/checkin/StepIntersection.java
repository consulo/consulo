/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.checkin;

import consulo.ide.impl.idea.openapi.util.Getter;
import consulo.document.util.TextRange;
import consulo.util.lang.function.PairConsumer;
import consulo.ide.impl.idea.util.containers.Convertor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * keeps state (position in areas) between invocations
 *
 * @author irengrig
 * @since 2011-02-18
 */
public class StepIntersection<Data, Area> {
  private final Convertor<Data,TextRange> myDataConvertor;
  private final Convertor<Area,TextRange> myAreasConvertor;
  private TextRange myDataRange;
  private TextRange myAreaRange;
  private Data myCurData;
  private Iterator<Data> myDataIterator;
  private int myAreaIndex;
  private Area myCurArea;
  private final List<Area> myAreas;
  private final HackSearch<Data,Area,TextRange> myHackSearch;
  // EA-28497, EA-26379
  private final Getter<String> myDebugDocumentTextGetter;

  public StepIntersection(Convertor<Data, TextRange> dataConvertor,
                          Convertor<Area, TextRange> areasConvertor,
                          List<Area> areas,
                          Getter<String> debugDocumentTextGetter) {
    myAreas = areas;
    myDebugDocumentTextGetter = debugDocumentTextGetter;
    myAreaIndex = 0;
    myDataConvertor = dataConvertor;
    myAreasConvertor = areasConvertor;
    myHackSearch = new HackSearch<Data, Area, TextRange>(myDataConvertor, myAreasConvertor, new Comparator<TextRange>() {
      @Override
      public int compare(TextRange o1, TextRange o2) {
        return o1.intersects(o2) ? 0 : o1.getStartOffset() < o2.getStartOffset() ? -1 : 1;
      }
    });
  }

  public void resetIndex() {
    myAreaIndex = 0;
  }

  public List<Data> process(Iterable<Data> data) {
    final List<Data> result = new ArrayList<Data>();
    process(data, new PairConsumer<Data, Area>() {
      @Override
      public void consume(Data data, Area area) {
        result.add(data);
      }
    });
    return result;
  }

  public void process(Iterable<Data> data, PairConsumer<Data, Area> consumer) {
    myDataIterator = data.iterator();

    if (! myDataIterator.hasNext() || noMoreAreas()) return;
    dataStep();
    initArea();
    while (! noMoreAreas()) {
      boolean intersects = myAreaRange.intersects(myDataRange);
      if (intersects) {
        consumer.consume(myCurData, myCurArea);
      }
      // take next
      if (! myDataIterator.hasNext() && noMoreAreas()) break;
      if (! myDataIterator.hasNext()) {
        areaStep();
        continue;
      }
      if (noMoreAreas()) {
        dataStep();
        continue;
      }
      if (myDataRange.getEndOffset() < myAreaRange.getEndOffset()) {
        dataStep();
      } else {
        areaStep();
      }
    }
  }

  private boolean noMoreAreas() {
    return (myAreaIndex >= myAreas.size());
  }

  private void initArea() {
    myAreaIndex = 0;
    myCurArea = myAreas.get(myAreaIndex);
    myAreaRange = myAreasConvertor.convert(myCurArea);
  }

  private void areaStep() {
    // a hack here
    int idx = myHackSearch.search(myAreas.subList(myAreaIndex + 1, myAreas.size()), myCurData);
    myAreaIndex = myAreaIndex + 1 + idx;
    if (myAreaIndex >= myAreas.size()) {
      return;
    }
    /*assert myAreaRange == null || myAreaRange.getEndOffset() < myAreasConvertor.convert(myAreas.get(myAreaIndex)).getStartOffset() :
      "Area ranges intersect: first: " + myAreaRange + ", second: " + myAreasConvertor.convert(myAreas.get(myAreaIndex)) + ", text: '" +
      myDebugDocumentTextGetter.get() + "'";*/
    myCurArea = myAreas.get(myAreaIndex);
    myAreaRange = myAreasConvertor.convert(myCurArea);
  }

  private void dataStep() {
    myCurData = myDataIterator.next();
    /*assert myDataRange == null || myDataRange.getEndOffset() < myDataConvertor.convert(myCurData).getStartOffset() :
      "Data ranges intersect: first: " + myDataRange + ", second: " + myDataConvertor.convert(myCurData) + ", text: '" +
      myDebugDocumentTextGetter.get() + "'";*/
    myDataRange = myDataConvertor.convert(myCurData);
  }
}
