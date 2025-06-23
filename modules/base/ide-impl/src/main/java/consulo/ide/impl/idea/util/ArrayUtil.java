/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.util;

import consulo.annotation.DeprecationInfo;
import consulo.util.collection.ArrayFactory;
import consulo.util.collection.HashingStrategy;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Contract;

import java.io.File;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntFunction;

/**
 * @author msk
 */
@SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
@Deprecated(forRemoval = true)
@DeprecationInfo("Use consulo.util.collection.ArrayUtil")
public class ArrayUtil extends ArrayUtilRt {
    public static final short[] EMPTY_SHORT_ARRAY = consulo.util.collection.ArrayUtil.EMPTY_SHORT_ARRAY;
    public static final char[] EMPTY_CHAR_ARRAY = consulo.util.collection.ArrayUtil.EMPTY_CHAR_ARRAY;
    public static final byte[] EMPTY_BYTE_ARRAY = consulo.util.collection.ArrayUtil.EMPTY_BYTE_ARRAY;
    public static final int[] EMPTY_INT_ARRAY = consulo.util.collection.ArrayUtil.EMPTY_INT_ARRAY;
    public static final boolean[] EMPTY_BOOLEAN_ARRAY = consulo.util.collection.ArrayUtil.EMPTY_BOOLEAN_ARRAY;
    public static final Object[] EMPTY_OBJECT_ARRAY = consulo.util.collection.ArrayUtil.EMPTY_OBJECT_ARRAY;
    public static final String[] EMPTY_STRING_ARRAY = consulo.util.collection.ArrayUtil.EMPTY_STRING_ARRAY;
    public static final Class[] EMPTY_CLASS_ARRAY = consulo.util.collection.ArrayUtil.EMPTY_CLASS_ARRAY;
    public static final long[] EMPTY_LONG_ARRAY = consulo.util.collection.ArrayUtil.EMPTY_LONG_ARRAY;
    public static final Collection[] EMPTY_COLLECTION_ARRAY = consulo.util.collection.ArrayUtil.EMPTY_COLLECTION_ARRAY;
    public static final File[] EMPTY_FILE_ARRAY = consulo.util.collection.ArrayUtil.EMPTY_FILE_ARRAY;
    public static final Runnable[] EMPTY_RUNNABLE_ARRAY = consulo.util.collection.ArrayUtil.EMPTY_RUNNABLE_ARRAY;
    public static final CharSequence EMPTY_CHAR_SEQUENCE = consulo.util.collection.ArrayUtil.EMPTY_CHAR_SEQUENCE;

    public static final ArrayFactory<String> STRING_ARRAY_FACTORY = consulo.util.collection.ArrayUtil.STRING_ARRAY_FACTORY;
    public static final ArrayFactory<Object> OBJECT_ARRAY_FACTORY = consulo.util.collection.ArrayUtil.OBJECT_ARRAY_FACTORY;

    private ArrayUtil() {
    }

    @Nonnull
    @Contract(pure = true)
    public static byte[] realloc(@Nonnull byte[] array, int newSize) {
        return consulo.util.collection.ArrayUtil.realloc(array, newSize);
    }

    @Nonnull
    @Contract(pure = true)
    public static short[] realloc(@Nonnull short[] array, int newSize) {
        return consulo.util.collection.ArrayUtil.realloc(array, newSize);
    }

    @Nonnull
    @Contract(pure = true)
    public static boolean[] realloc(@Nonnull boolean[] array, int newSize) {
        return consulo.util.collection.ArrayUtil.realloc(array, newSize);
    }

    @Nonnull
    @Contract(pure = true)
    public static long[] realloc(@Nonnull long[] array, int newSize) {
        return consulo.util.collection.ArrayUtil.realloc(array, newSize);
    }

    @Nonnull
    @Contract(pure = true)
    public static int[] realloc(@Nonnull int[] array, int newSize) {
        return consulo.util.collection.ArrayUtil.realloc(array, newSize);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> T[] realloc(@Nonnull T[] array, int newSize, @Nonnull IntFunction<T[]> factory) {
        int oldSize = array.length;
        if (oldSize == newSize) {
            return array;
        }

        T[] result = factory.apply(newSize);
        if (newSize == 0) {
            return result;
        }

        System.arraycopy(array, 0, result, 0, Math.min(oldSize, newSize));
        return result;
    }

    @Nonnull
    @Contract(pure = true)
    public static long[] append(@Nonnull long[] array, long value) {
        return consulo.util.collection.ArrayUtil.append(array, value);
    }

    @Nonnull
    @Contract(pure = true)
    public static int[] append(@Nonnull int[] array, int value) {
        return consulo.util.collection.ArrayUtil.append(array, value);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> T[] insert(@Nonnull T[] array, int index, T value) {
        return consulo.util.collection.ArrayUtil.insert(array, index, value);
    }

    @Nonnull
    @Contract(pure = true)
    public static int[] insert(@Nonnull int[] array, int index, int value) {
        return consulo.util.collection.ArrayUtil.insert(array, index, value);
    }

    @Nonnull
    @Contract(pure = true)
    public static byte[] append(@Nonnull byte[] array, byte value) {
        return consulo.util.collection.ArrayUtil.append(array, value);
    }

    @Nonnull
    @Contract(pure = true)
    public static boolean[] append(@Nonnull boolean[] array, boolean value) {
        return consulo.util.collection.ArrayUtil.append(array, value);
    }

    @Nonnull
    @Contract(pure = true)
    public static char[] realloc(@Nonnull char[] array, int newSize) {
        return consulo.util.collection.ArrayUtil.realloc(array, newSize);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> T[] toObjectArray(@Nonnull Collection<? extends T> collection, @Nonnull Class<T> aClass) {
        return consulo.util.collection.ArrayUtil.toObjectArray(collection, aClass);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> T[] toObjectArray(@Nonnull Class<T> aClass, @Nonnull Object... source) {
        return consulo.util.collection.ArrayUtil.toObjectArray(aClass, source);
    }

    @Nonnull
    @Contract(pure = true)
    public static Object[] toObjectArray(@Nonnull Collection<?> collection) {
        return consulo.util.collection.ArrayUtil.toObjectArray(collection);
    }

    @Nonnull
    @Contract(pure = true)
    public static int[] toIntArray(@Nonnull Collection<Integer> list) {
        return consulo.util.collection.ArrayUtil.toIntArray(list);
    }

    @Nonnull
    @Contract(pure = true)
    public static int[] toIntArray(@Nonnull byte[] byteArray) {
        return consulo.util.collection.ArrayUtil.toIntArray(byteArray);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> T[] mergeArrays(@Nonnull T[] a1, @Nonnull T[] a2) {
        return consulo.util.collection.ArrayUtil.mergeArrays(a1, a2);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> T[] mergeCollections(
        @Nonnull Collection<? extends T> c1,
        @Nonnull Collection<? extends T> c2,
        @Nonnull ArrayFactory<T> factory
    ) {
        return consulo.util.collection.ArrayUtil.mergeCollections(c1, c2, factory);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> T[] mergeArrays(@Nonnull T[] a1, @Nonnull T[] a2, @Nonnull IntFunction<T[]> factory) {
        if (a1.length == 0) {
            return a2;
        }
        if (a2.length == 0) {
            return a1;
        }
        T[] result = factory.apply(a1.length + a2.length);
        System.arraycopy(a1, 0, result, 0, a1.length);
        System.arraycopy(a2, 0, result, a1.length, a2.length);
        return result;
    }

    @Nonnull
    @Contract(pure = true)
    public static String[] mergeArrays(@Nonnull String[] a1, @Nonnull String... a2) {
        return consulo.util.collection.ArrayUtil.mergeArrays(a1, a2);
    }

    @Nonnull
    @Contract(pure = true)
    public static int[] mergeArrays(@Nonnull int[] a1, @Nonnull int[] a2) {
        return consulo.util.collection.ArrayUtil.mergeArrays(a1, a2);
    }

    @Nonnull
    @Contract(pure = true)
    public static byte[] mergeArrays(@Nonnull byte[] a1, @Nonnull byte[] a2) {
        return consulo.util.collection.ArrayUtil.mergeArrays(a1, a2);
    }

    /**
     * Allocates new array of size {@code array.length + collection.size()} and copies elements of {@code array} and
     * {@code collection} to it.
     *
     * @param array      source array
     * @param collection source collection
     * @param factory    array factory used to create destination array of type {@code T}
     * @return destination array
     */
    @Nonnull
    @Contract(pure = true)
    public static <T> T[] mergeArrayAndCollection(
        @Nonnull T[] array,
        @Nonnull Collection<T> collection,
        @Nonnull IntFunction<T[]> factory
    ) {
        if (collection.isEmpty()) {
            return array;
        }

        T[] array2;
        try {
            array2 = collection.toArray(factory.apply(collection.size()));
        }
        catch (ArrayStoreException e) {
            throw new RuntimeException("Bad elements in collection: " + collection, e);
        }

        if (array.length == 0) {
            return array2;
        }

        T[] result = factory.apply(array.length + collection.size());
        System.arraycopy(array, 0, result, 0, array.length);
        System.arraycopy(array2, 0, result, array.length, array2.length);
        return result;
    }

    /**
     * Appends {@code element} to the {@code src} array. As you can
     * imagine the appended element will be the last one in the returned result.
     *
     * @param src     array to which the {@code element} should be appended.
     * @param element object to be appended to the end of {@code src} array.
     * @return new array
     */
    @Nonnull
    @Contract(pure = true)
    public static <T> T[] append(@Nonnull T[] src, @Nullable T element) {
        return consulo.util.collection.ArrayUtil.append(src, element);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> T[] prepend(T element, @Nonnull T[] array) {
        //noinspection unchecked
        return prepend(element, array, (Class<T>)array.getClass().getComponentType());
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> T[] prepend(T element, @Nonnull T[] array, @Nonnull Class<T> type) {
        return consulo.util.collection.ArrayUtil.prepend(element, array, type);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> T[] prepend(T element, @Nonnull T[] src, @Nonnull ArrayFactory<T> factory) {
        return consulo.util.collection.ArrayUtil.prepend(element, src, factory);
    }

    @Nonnull
    @Contract(pure = true)
    public static byte[] prepend(byte element, @Nonnull byte[] array) {
        return consulo.util.collection.ArrayUtil.prepend(element, array);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> T[] append(@Nonnull T[] src, T element, @Nonnull ArrayFactory<T> factory) {
        return consulo.util.collection.ArrayUtil.append(src, element, factory);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> T[] append(@Nonnull T[] src, @Nullable T element, @Nonnull Class<T> componentType) {
        return consulo.util.collection.ArrayUtil.append(src, element, componentType);
    }

    /**
     * Removes element with index {@code idx} from array {@code src}.
     *
     * @param src array.
     * @param idx index of element to be removed.
     * @return modified array.
     */
    @Nonnull
    @Contract(pure = true)
    public static <T> T[] remove(@Nonnull T[] src, int idx) {
        return consulo.util.collection.ArrayUtil.remove(src, idx);
    }

    @Nonnull
    public static <T> T[] newArray(@Nonnull Class<T> type, int length) {
        return consulo.util.collection.ArrayUtil.newArray(type, length);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> T[] remove(@Nonnull T[] src, int idx, @Nonnull IntFunction<T[]> factory) {
        return consulo.util.collection.ArrayUtil.remove(src, idx, factory);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> T[] remove(@Nonnull T[] src, T element) {
        return consulo.util.collection.ArrayUtil.remove(src, element);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> T[] remove(@Nonnull T[] src, T element, @Nonnull IntFunction<T[]> factory) {
        return consulo.util.collection.ArrayUtil.remove(src, element, factory);
    }

    @Nonnull
    @Contract(pure = true)
    public static int[] remove(@Nonnull int[] src, int idx) {
        return consulo.util.collection.ArrayUtil.remove(src, idx);
    }

    @Nonnull
    @Contract(pure = true)
    public static short[] remove(@Nonnull short[] src, int idx) {
        return consulo.util.collection.ArrayUtil.remove(src, idx);
    }

    @Contract(pure = true)
    public static int find(@Nonnull int[] src, int obj) {
        return consulo.util.collection.ArrayUtil.find(src, obj);
    }

    @Contract(pure = true)
    public static <T> int find(@Nonnull T[] src, T obj) {
        return consulo.util.collection.ArrayUtil.find(src, obj);
    }

    @Contract(pure = true)
    public static boolean startsWith(@Nonnull byte[] array, @Nonnull byte[] prefix) {
        return consulo.util.collection.ArrayUtil.startsWith(array, prefix);
    }

    @Contract(pure = true)
    public static <E> boolean startsWith(@Nonnull E[] array, @Nonnull E[] subArray) {
        return consulo.util.collection.ArrayUtil.startsWith(array, subArray);
    }

    @Contract(pure = true)
    public static boolean startsWith(@Nonnull byte[] array, int start, @Nonnull byte[] subArray) {
        return consulo.util.collection.ArrayUtil.startsWith(array, start, subArray);
    }

    @Contract(pure = true)
    public static <T> boolean equals(@Nonnull T[] a1, @Nonnull T[] a2, @Nonnull HashingStrategy<? super T> comparator) {
        return consulo.util.collection.ArrayUtil.equals(a1, a2, comparator);
    }

    @Contract(pure = true)
    public static <T> boolean equals(@Nonnull T[] a1, @Nonnull T[] a2, @Nonnull Comparator<? super T> comparator) {
        return consulo.util.collection.ArrayUtil.equals(a1, a2, comparator);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> T[] reverseArray(@Nonnull T[] array) {
        return consulo.util.collection.ArrayUtil.reverseArray(array);
    }

    @Nonnull
    @Contract(pure = true)
    public static int[] reverseArray(@Nonnull int[] array) {
        return consulo.util.collection.ArrayUtil.reverseArray(array);
    }

    @Contract(pure = true)
    public static int lexicographicCompare(@Nonnull int[] obj1, @Nonnull int[] obj2) {
        return consulo.util.collection.ArrayUtil.lexicographicCompare(obj1, obj2);
    }

    @Contract(pure = true)
    public static int lexicographicCompare(@Nonnull String[] obj1, @Nonnull String[] obj2) {
        return consulo.util.collection.ArrayUtil.lexicographicCompare(obj1, obj2);
    }

    //must be Comparables
    @Contract(pure = true)
    public static <T> int lexicographicCompare(@Nonnull T[] obj1, @Nonnull T[] obj2) {
        return consulo.util.collection.ArrayUtil.lexicographicCompare(obj1, obj2);
    }

    public static <T> void swap(@Nonnull T[] array, int i1, int i2) {
        consulo.util.collection.ArrayUtil.swap(array, i1, i2);
    }

    public static void swap(@Nonnull int[] array, int i1, int i2) {
        consulo.util.collection.ArrayUtil.swap(array, i1, i2);
    }

    public static void swap(@Nonnull boolean[] array, int i1, int i2) {
        consulo.util.collection.ArrayUtil.swap(array, i1, i2);
    }

    public static void swap(@Nonnull char[] array, int i1, int i2) {
        consulo.util.collection.ArrayUtil.swap(array, i1, i2);
    }

    public static <T> void rotateLeft(@Nonnull T[] array, int i1, int i2) {
        consulo.util.collection.ArrayUtil.rotateLeft(array, i1, i2);
    }

    public static <T> void rotateRight(@Nonnull T[] array, int i1, int i2) {
        consulo.util.collection.ArrayUtil.rotateRight(array, i1, i2);
    }

    @Contract(pure = true)
    public static int indexOf(@Nonnull Object[] objects, @Nullable Object object) {
        return consulo.util.collection.ArrayUtil.indexOf(objects, object);
    }

    @Contract(pure = true)
    public static int indexOf(@Nonnull Object[] objects, Object object, int start, int end) {
        return consulo.util.collection.ArrayUtil.indexOf(objects, object, start, end);
    }

    @Contract(pure = true)
    public static <T> int indexOf(@Nonnull List<T> objects, T object, @Nonnull HashingStrategy<T> comparator) {
        return consulo.util.collection.ArrayUtil.indexOf(objects, object, comparator);
    }

    @Contract(pure = true)
    public static <T> int indexOf(@Nonnull List<T> objects, T object, @Nonnull Comparator<T> comparator) {
        return consulo.util.collection.ArrayUtil.indexOf(objects, object, comparator);
    }

    @Contract(pure = true)
    public static <T> int indexOf(@Nonnull T[] objects, T object, @Nonnull HashingStrategy<T> comparator) {
        return consulo.util.collection.ArrayUtil.indexOf(objects, object, comparator);
    }

    @Contract(pure = true)
    public static int indexOf(@Nonnull long[] ints, long value) {
        return consulo.util.collection.ArrayUtil.indexOf(ints, value);
    }

    @Contract(pure = true)
    public static int indexOf(@Nonnull int[] ints, int value) {
        return consulo.util.collection.ArrayUtil.indexOf(ints, value);
    }

    @Contract(pure = true)
    public static int indexOf(@Nonnull short[] ints, short value) {
        return consulo.util.collection.ArrayUtil.indexOf(ints, value);
    }

    @Contract(pure = true)
    public static <T> int lastIndexOf(@Nonnull T[] src, T obj) {
        return consulo.util.collection.ArrayUtil.lastIndexOf(src, obj);
    }

    @Contract(pure = true)
    public static int lastIndexOf(@Nonnull int[] src, int obj) {
        return consulo.util.collection.ArrayUtil.lastIndexOf(src, obj);
    }

    @Contract(pure = true)
    public static <T> int lastIndexOf(@Nonnull T[] src, T obj, @Nonnull HashingStrategy<? super T> comparator) {
        return consulo.util.collection.ArrayUtil.lastIndexOf(src, obj, comparator);
    }

    @Contract(pure = true)
    public static <T> int lastIndexOf(@Nonnull List<T> src, T obj, @Nonnull HashingStrategy<? super T> comparator) {
        return consulo.util.collection.ArrayUtil.lastIndexOf(src, obj, comparator);
    }

    @Contract(pure = true)
    public static int lastIndexOfNot(@Nonnull int[] src, int obj) {
        return consulo.util.collection.ArrayUtil.lastIndexOfNot(src, obj);
    }

    @Contract(pure = true)
    @SafeVarargs
    public static <T> boolean contains(@Nullable T o, @Nonnull T... objects) {
        return consulo.util.collection.ArrayUtil.contains(o, objects);
    }

    @Contract(pure = true)
    public static boolean contains(@Nullable String s, @Nonnull String... strings) {
        return consulo.util.collection.ArrayUtil.contains(s, strings);
    }

    @Nonnull
    @Contract(pure = true)
    public static int[] newIntArray(int count) {
        return consulo.util.collection.ArrayUtil.newIntArray(count);
    }

    @Nonnull
    @Contract(pure = true)
    public static long[] newLongArray(int count) {
        return consulo.util.collection.ArrayUtil.newLongArray(count);
    }

    @Nonnull
    @Contract(pure = true)
    public static String[] newStringArray(int count) {
        return consulo.util.collection.ArrayUtil.newStringArray(count);
    }

    @Nonnull
    @Contract(pure = true)
    public static Object[] newObjectArray(int count) {
        return consulo.util.collection.ArrayUtil.newObjectArray(count);
    }

    @Nonnull
    @Contract(pure = true)
    public static <E> E[] ensureExactSize(int count, @Nonnull E[] sample) {
        return consulo.util.collection.ArrayUtil.ensureExactSize(count, sample);
    }

    @Nullable
    @Contract(pure = true)
    public static <T> T getFirstElement(@Nullable T[] array) {
        return consulo.util.collection.ArrayUtil.getFirstElement(array);
    }

    @Nullable
    @Contract(pure = true)
    public static <T> T getLastElement(@Nullable T[] array) {
        return consulo.util.collection.ArrayUtil.getLastElement(array);
    }

    @Contract(pure = true)
    public static int getLastElement(@Nullable int[] array, int defaultValue) {
        return consulo.util.collection.ArrayUtil.getLastElement(array, defaultValue);
    }

    @Contract(value = "null -> true", pure = true)
    public static <T> boolean isEmpty(@Nullable T[] array) {
        return consulo.util.collection.ArrayUtil.isEmpty(array);
    }

    @Nonnull
    @Contract(pure = true)
    public static String[] toStringArray(@Nullable Collection<String> collection) {
        return consulo.util.collection.ArrayUtil.toStringArray(collection);
    }

    public static <T> void copy(@Nonnull Collection<? extends T> src, @Nonnull T[] dst, int dstOffset) {
        consulo.util.collection.ArrayUtil.copy(src, dst, dstOffset);
    }

    @Nullable
    @Contract("null -> null; !null -> !null")
    public static <T> T[] copyOf(@Nullable T[] original) {
        return consulo.util.collection.ArrayUtil.copyOf(original);
    }

    @Nullable
    @Contract("null -> null; !null -> !null")
    public static boolean[] copyOf(@Nullable boolean[] original) {
        return consulo.util.collection.ArrayUtil.copyOf(original);
    }

    @Nullable
    @Contract("null -> null; !null -> !null")
    public static int[] copyOf(@Nullable int[] original) {
        return consulo.util.collection.ArrayUtil.copyOf(original);
    }

    @Nonnull
    public static <T> T[] stripTrailingNulls(@Nonnull T[] array) {
        return consulo.util.collection.ArrayUtil.stripTrailingNulls(array);
    }

    private static <T> int trailingNullsIndex(@Nonnull T[] array) {
        for (int i = array.length - 1; i >= 0; i--) {
            if (array[i] != null) {
                return i + 1;
            }
        }
        return 0;
    }

    public static long averageAmongMedians(@Nonnull long[] time, int part) {
        return consulo.util.collection.ArrayUtil.averageAmongMedians(time, part);
    }

    public static long averageAmongMedians(@Nonnull int[] time, int part) {
        return consulo.util.collection.ArrayUtil.averageAmongMedians(time, part);
    }

    public static int min(int[] values) {
        return consulo.util.collection.ArrayUtil.min(values);
    }

    @Nonnull
    public static <T> Class<T> getComponentType(@Nonnull T[] collection) {
        return consulo.util.collection.ArrayUtil.getComponentType(collection);
    }
}
