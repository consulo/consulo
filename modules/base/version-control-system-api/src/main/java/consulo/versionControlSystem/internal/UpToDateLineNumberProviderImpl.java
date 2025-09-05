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
package consulo.versionControlSystem.internal;

import consulo.document.Document;
import consulo.project.Project;
import consulo.versionControlSystem.UpToDateLineNumberProvider;
import jakarta.annotation.Nullable;

public class UpToDateLineNumberProviderImpl implements UpToDateLineNumberProvider {
    private final Document myDocument;
    private final LineStatusTrackerManagerI myLineStatusTrackerManagerI;

    public UpToDateLineNumberProviderImpl(Document document, Project project) {
        myDocument = document;
        myLineStatusTrackerManagerI = LineStatusTrackerManagerI.getInstance(project);
    }

    @Override
    public boolean isRangeChanged(int start, int end) {
        LineStatusTrackerI tracker = getTracker();
        if (tracker == null) {
            return false;
        }
        else {
            return tracker.isRangeModified(start, end);
        }
    }

    @Override
    public boolean isLineChanged(int currentNumber) {
        LineStatusTrackerI tracker = getTracker();
        if (tracker == null) {
            return false;
        }
        else {
            return tracker.isLineModified(currentNumber);
        }
    }

    @Override
    public int getLineNumber(int currentNumber) {
        LineStatusTrackerI tracker = getTracker();
        if (tracker == null) {
            return currentNumber;
        }
        else {
            return tracker.transferLineToVcs(currentNumber, false);
        }
    }

    @Override
    public int getLineCount() {
        LineStatusTrackerI tracker = getTracker();
        if (tracker == null) {
            return myDocument.getLineCount();
        }
        else {
            return tracker.getVcsDocument().getLineCount();
        }
    }

    @Nullable
    private LineStatusTrackerI getTracker() {
        LineStatusTrackerI tracker = myLineStatusTrackerManagerI.getLineStatusTracker(myDocument);
        return tracker != null && tracker.isOperational() ? tracker : null;
    }
}
