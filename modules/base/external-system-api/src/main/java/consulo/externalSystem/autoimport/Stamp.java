// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.autoimport;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

public final class Stamp implements Comparable<Stamp> {
    public static final Stamp NONE = new Stamp(-1);

    private static final AtomicInteger ourCounter = new AtomicInteger(0);

    private final int myStamp;

    private Stamp(int stamp) {
        myStamp = stamp;
    }

    public static Stamp nextStamp() {
        return new Stamp(ourCounter.incrementAndGet());
    }

    @Override
    public int compareTo(Stamp other) {
        return Integer.compare(myStamp, other.myStamp);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Stamp that)) {
            return false;
        }
        return myStamp == that.myStamp;
    }

    @Override
    public int hashCode() {
        return myStamp;
    }

    @Override
    public String toString() {
        return "Stamp(" + myStamp + ")";
    }
}
