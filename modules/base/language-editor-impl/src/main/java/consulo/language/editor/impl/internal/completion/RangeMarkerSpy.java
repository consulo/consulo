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
package consulo.language.editor.impl.internal.completion;

import consulo.document.RangeMarker;
import consulo.document.event.DocumentAdapter;
import consulo.document.event.DocumentEvent;
import jakarta.annotation.Nonnull;

import java.lang.ref.WeakReference;

/**
 * @author peter
 */
public class RangeMarkerSpy extends DocumentAdapter {
    // Do not leak the whole InsertionContext via DocumentListener.
    private final WeakReference<CompletionAssertions.WatchingInsertionContext> myContextRef;
    private final RangeMarker myMarker;

    public RangeMarkerSpy(@Nonnull CompletionAssertions.WatchingInsertionContext context, @Nonnull RangeMarker marker) {
        myContextRef = new WeakReference<>(context);
        myMarker = marker;
        assert myMarker.isValid();
    }

    protected void invalidated(@Nonnull DocumentEvent e) {
        CompletionAssertions.WatchingInsertionContext context = myContextRef.get();
        if (context != null && context.invalidateTrace == null) {
            context.invalidateTrace = new Throwable();
            context.killer = e;
        }
    }

    @Override
    public void documentChanged(@Nonnull DocumentEvent e) {
        if (!myMarker.isValid()) {
            invalidated(e);
        }
    }
}
