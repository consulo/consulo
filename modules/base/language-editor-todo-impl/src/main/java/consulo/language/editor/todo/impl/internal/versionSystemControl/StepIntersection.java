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
package consulo.language.editor.todo.impl.internal.versionSystemControl;

import consulo.document.util.TextRange;
import consulo.util.lang.function.PairConsumer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * keeps state (position in areas) between invocations
 *
 * @author irengrig
 * @since 2011-02-18
 */
public class StepIntersection<Data, Area> {
    private final Function<Data, TextRange> myDataConverter;
    private final Function<Area, TextRange> myAreasConverter;
    private TextRange myDataRange;
    private TextRange myAreaRange;
    private Data myCurData;
    private Iterator<Data> myDataIterator;
    private int myAreaIndex;
    private Area myCurArea;
    private final List<Area> myAreas;
    private final HackSearch<Data, Area, TextRange> myHackSearch;
    // EA-28497, EA-26379
    private final Supplier<String> myDebugDocumentTextGetter;

    public StepIntersection(Function<Data, TextRange> dataConverter,
                            Function<Area, TextRange> areasConverter,
                            List<Area> areas,
                            Supplier<String> debugDocumentTextGetter) {
        myAreas = areas;
        myDebugDocumentTextGetter = debugDocumentTextGetter;
        myAreaIndex = 0;
        myDataConverter = dataConverter;
        myAreasConverter = areasConverter;
        myHackSearch = new HackSearch<>(myDataConverter, myAreasConverter, new Comparator<>() {
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
        List<Data> result = new ArrayList<>();
        process(data, (data1, area) -> result.add(data1));
        return result;
    }

    public void process(Iterable<Data> data, PairConsumer<Data, Area> consumer) {
        myDataIterator = data.iterator();

        if (!myDataIterator.hasNext() || noMoreAreas()) {
            return;
        }
        dataStep();
        initArea();
        while (!noMoreAreas()) {
            boolean intersects = myAreaRange.intersects(myDataRange);
            if (intersects) {
                consumer.consume(myCurData, myCurArea);
            }
            // take next
            if (!myDataIterator.hasNext() && noMoreAreas()) {
                break;
            }
            if (!myDataIterator.hasNext()) {
                areaStep();
                continue;
            }
            if (noMoreAreas()) {
                dataStep();
                continue;
            }
            if (myDataRange.getEndOffset() < myAreaRange.getEndOffset()) {
                dataStep();
            }
            else {
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
        myAreaRange = myAreasConverter.apply(myCurArea);
    }

    private void areaStep() {
        // a hack here
        int idx = myHackSearch.search(myAreas.subList(myAreaIndex + 1, myAreas.size()), myCurData);
        myAreaIndex = myAreaIndex + 1 + idx;
        if (myAreaIndex >= myAreas.size()) {
            return;
        }
    /*assert myAreaRange == null || myAreaRange.getEndOffset() < myAreasConverter.convert(myAreas.get(myAreaIndex)).getStartOffset() :
      "Area ranges intersect: first: " + myAreaRange + ", second: " + myAreasConverter.convert(myAreas.get(myAreaIndex)) + ", text: '" +
      myDebugDocumentTextGetter.get() + "'";*/
        myCurArea = myAreas.get(myAreaIndex);
        myAreaRange = myAreasConverter.apply(myCurArea);
    }

    private void dataStep() {
        myCurData = myDataIterator.next();
    /*assert myDataRange == null || myDataRange.getEndOffset() < myDataConverter.convert(myCurData).getStartOffset() :
      "Data ranges intersect: first: " + myDataRange + ", second: " + myDataConverter.convert(myCurData) + ", text: '" +
      myDebugDocumentTextGetter.get() + "'";*/
        myDataRange = myDataConverter.apply(myCurData);
    }
}
