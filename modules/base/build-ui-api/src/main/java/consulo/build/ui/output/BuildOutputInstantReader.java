// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.build.ui.output;

import org.jspecify.annotations.Nullable;

import java.io.Closeable;

/**
 * @author Vladislav.Soroka
 */
public interface BuildOutputInstantReader {
    interface Primary extends BuildOutputInstantReader, Closeable, Appendable {
    }

    
    Object getParentEventId();

    @Nullable
    String readLine();

    void pushBack();

    void pushBack(int numberOfLines);
}
