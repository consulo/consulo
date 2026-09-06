/*
 * Copyright 2013-2026 consulo.io
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
package consulo.util.concurrent.coroutine.test;

import consulo.util.concurrent.coroutine.ObservableValue;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test of {@link ObservableValue}.
 *
 * @author VISTALL
 * @since 2026-09-05
 */
public class ObservableValueTest {
    @Test
    public void testSetAndGet() {
        ObservableValue<String> value = ObservableValue.of("a");

        assertEquals("a", value.get());

        value.set("b");

        assertEquals("b", value.get());
    }

    @Test
    public void testCompareAndSet() {
        Object first = new Object();
        Object second = new Object();
        ObservableValue<Object> value = ObservableValue.of(first);

        assertFalse(value.compareAndSet(second, first));
        assertSame(first, value.get());

        assertTrue(value.compareAndSet(first, second));
        assertSame(second, value.get());
    }

    @Test
    public void testGetAndUpdate() {
        ObservableValue<Integer> value = ObservableValue.of(1);

        assertEquals(Integer.valueOf(1), value.getAndUpdate(i -> i + 1));
        assertEquals(Integer.valueOf(2), value.get());

        assertEquals(Integer.valueOf(3), value.updateAndGet(i -> i + 1));
        assertEquals(Integer.valueOf(3), value.get());

        value.update(i -> i * 10);
        assertEquals(Integer.valueOf(30), value.get());
    }

    @Test
    public void testListenersReceiveEveryDistinctValue() {
        ObservableValue<Integer> value = ObservableValue.of(0);
        List<Integer> seen = new ArrayList<>();

        value.addListener(seen::add);

        value.set(1);
        value.set(2);
        value.set(2);
        value.compareAndSet(2, 3);
        value.getAndUpdate(i -> i + 1);
        value.updateAndGet(i -> i);

        assertEquals(List.of(1, 2, 3, 4), seen);
        assertEquals(1, value.listenerCount());
    }

    @Test
    public void testRemovedListenerIsNotNotified() {
        ObservableValue<String> value = ObservableValue.of("a");
        List<String> seen = new ArrayList<>();

        Runnable removal = value.addListener(seen::add);

        value.set("b");
        removal.run();
        value.set("c");

        assertEquals(List.of("b"), seen);
        assertEquals(0, value.listenerCount());
    }

    @Test
    public void testMapTracksSource() {
        ObservableValue<List<String>> source = ObservableValue.of(List.of("a", "b"));
        ObservableValue<Integer> size = source.map(List::size);
        List<Integer> seen = new ArrayList<>();

        size.addListener(seen::add);

        assertEquals(Integer.valueOf(2), size.get());

        source.set(List.of("a", "b", "c"));
        assertEquals(Integer.valueOf(3), size.get());

        source.set(List.of("x", "y", "z"));
        assertEquals(Integer.valueOf(3), size.get());

        source.set(List.of());
        assertEquals(Integer.valueOf(0), size.get());

        assertEquals(List.of(3, 0), seen);
    }
}
