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
package consulo.ui.ex;

import consulo.navigation.Navigatable;

public interface OccurenceNavigator {
  OccurenceNavigator EMPTY = new OccurenceNavigator() {
    @Override
    public boolean hasNextOccurence() {
      return false;
    }

    @Override
    public boolean hasPreviousOccurence() {
      return false;
    }

    @Override
    public OccurenceInfo goNextOccurence() {
      return null;
    }

    @Override
    public OccurenceInfo goPreviousOccurence() {
      return null;
    }

    @Override
    public String getNextOccurenceActionName() {
      return "";
    }

    @Override
    public String getPreviousOccurenceActionName() {
      return "";
    }
  };

  class OccurenceInfo {
    private final Navigatable myNavigateable;
    private final int myOccurrenceNumber;
    private final int myOccurrencesCount;

    public OccurenceInfo(Navigatable navigateable, int occurrenceNumber, int occurrencesCount) {
      myNavigateable = navigateable;
      myOccurrenceNumber = occurrenceNumber;
      myOccurrencesCount = occurrencesCount;
    }

    private OccurenceInfo(int occurrenceNumber, int occurrencesCount) {
      this(null, occurrenceNumber, occurrencesCount);
    }

    public static OccurenceInfo position(int occurrenceNumber, int occurrencesCount) {
      return new OccurenceInfo(occurrenceNumber, occurrencesCount);
    }

    public Navigatable getNavigateable() {
      return myNavigateable;
    }

    public int getOccurenceNumber() {
      return myOccurrenceNumber;
    }

    public int getOccurencesCount() {
      return myOccurrencesCount;
    }
  }

  boolean hasNextOccurence();

  boolean hasPreviousOccurence();

  OccurenceInfo goNextOccurence();

  OccurenceInfo goPreviousOccurence();

  String getNextOccurenceActionName();

  String getPreviousOccurenceActionName();
}
