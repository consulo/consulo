// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.log.impl.internal.util;

import java.util.HashSet;
import java.util.Set;

public class IntHashSetFlags implements Flags {
    private final int mySize;
    private final Set<Integer> myData = new HashSet<>();

    public IntHashSetFlags(int size) {
        mySize = size;
    }

    public Set<Integer> getData() {
        return myData;
    }

    @Override
    public int size() {
        return mySize;
    }

    @Override
    public boolean get(int index) {
        return myData.contains(index);
    }

    @Override
    public void set(int index, boolean value) {
        if (value) {
            myData.add(index);
        }
        else {
            myData.remove(index);
        }
    }

    @Override
    public void setAll(boolean value) {
        if (value) {
            for (int i = 0; i < mySize; i++) {
                myData.add(i);
            }
        }
        else {
            myData.clear();
        }
    }
}
