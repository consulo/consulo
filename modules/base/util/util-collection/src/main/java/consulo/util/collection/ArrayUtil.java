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
package consulo.util.collection;

import consulo.util.lang.CharArrayCharSequence;
import consulo.util.lang.Comparing;
import org.jspecify.annotations.Nullable;
import org.jetbrains.annotations.Contract;

import java.io.File;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntFunction;

/**
 * @author msk
 */
@SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
public class ArrayUtil {
    public static final short[] EMPTY_SHORT_ARRAY = new short[0];
    public static final char[] EMPTY_CHAR_ARRAY = new char[0];
    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    public static final int[] EMPTY_INT_ARRAY = new int[0];
    public static final boolean[] EMPTY_BOOLEAN_ARRAY = new boolean[0];
    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
    public static final String[] EMPTY_STRING_ARRAY = new String[0];
    public static final Class[] EMPTY_CLASS_ARRAY = new Class[0];
    public static final long[] EMPTY_LONG_ARRAY = new long[0];
    public static final Collection[] EMPTY_COLLECTION_ARRAY = new Collection[0];
    public static final File[] EMPTY_FILE_ARRAY = new File[0];
    public static final Runnable[] EMPTY_RUNNABLE_ARRAY = new Runnable[0];
    public static final CharSequence EMPTY_CHAR_SEQUENCE = new CharArrayCharSequence(EMPTY_CHAR_ARRAY);

    public static final ArrayFactory<String> STRING_ARRAY_FACTORY = ArrayUtil::newStringArray;
    public static final ArrayFactory<Object> OBJECT_ARRAY_FACTORY = ArrayUtil::newObjectArray;

    private ArrayUtil() {
    }

    @Contract(pure = true)
    public static byte[] realloc(byte[] array, int newSize) {
        if (newSize == 0) {
            return EMPTY_BYTE_ARRAY;
        }

        int oldSize = array.length;
        if (oldSize == newSize) {
            return array;
        }

        byte[] result = new byte[newSize];
        System.arraycopy(array, 0, result, 0, Math.min(oldSize, newSize));
        return result;
    }

    @Contract(pure = true)
    public static short[] realloc(short[] array, int newSize) {
        if (newSize == 0) {
            return EMPTY_SHORT_ARRAY;
        }

        int oldSize = array.length;
        return oldSize == newSize ? array : Arrays.copyOf(array, newSize);
    }

    @Contract(pure = true)
    public static boolean[] realloc(boolean[] array, int newSize) {
        if (newSize == 0) {
            return EMPTY_BOOLEAN_ARRAY;
        }

        int oldSize = array.length;
        if (oldSize == newSize) {
            return array;
        }

        boolean[] result = new boolean[newSize];
        System.arraycopy(array, 0, result, 0, Math.min(oldSize, newSize));
        return result;
    }

    @Contract(pure = true)
    public static long[] realloc(long[] array, int newSize) {
        if (newSize == 0) {
            return EMPTY_LONG_ARRAY;
        }

        int oldSize = array.length;
        if (oldSize == newSize) {
            return array;
        }

        long[] result = new long[newSize];
        System.arraycopy(array, 0, result, 0, Math.min(oldSize, newSize));
        return result;
    }

    @Contract(pure = true)
    public static int[] realloc(int[] array, int newSize) {
        if (newSize == 0) {
            return EMPTY_INT_ARRAY;
        }

        int oldSize = array.length;
        if (oldSize == newSize) {
            return array;
        }

        int[] result = new int[newSize];
        System.arraycopy(array, 0, result, 0, Math.min(oldSize, newSize));
        return result;
    }

    @Contract(pure = true)
    public static <T> T[] realloc(T[] array, int newSize, ArrayFactory<T> factory) {
        int oldSize = array.length;
        if (oldSize == newSize) {
            return array;
        }

        T[] result = factory.create(newSize);
        if (newSize == 0) {
            return result;
        }

        System.arraycopy(array, 0, result, 0, Math.min(oldSize, newSize));
        return result;
    }

    @Contract(pure = true)
    public static long[] append(long[] array, long value) {
        array = realloc(array, array.length + 1);
        array[array.length - 1] = value;
        return array;
    }

    @Contract(pure = true)
    public static int[] append(int[] array, int value) {
        array = realloc(array, array.length + 1);
        array[array.length - 1] = value;
        return array;
    }

    @Contract(pure = true)
    public static <T> T[] insert(T[] array, int index, T value) {
        T[] result = newArray(getComponentType(array), array.length + 1);
        System.arraycopy(array, 0, result, 0, index);
        result[index] = value;
        System.arraycopy(array, index, result, index + 1, array.length - index);
        return result;
    }

    @Contract(pure = true)
    public static int[] insert(int[] array, int index, int value) {
        int[] result = new int[array.length + 1];
        System.arraycopy(array, 0, result, 0, index);
        result[index] = value;
        System.arraycopy(array, index, result, index + 1, array.length - index);
        return result;
    }

    @Contract(pure = true)
    public static byte[] append(byte[] array, byte value) {
        array = realloc(array, array.length + 1);
        array[array.length - 1] = value;
        return array;
    }

    @Contract(pure = true)
    public static boolean[] append(boolean[] array, boolean value) {
        array = realloc(array, array.length + 1);
        array[array.length - 1] = value;
        return array;
    }

    @Contract(pure = true)
    public static char[] realloc(char[] array, int newSize) {
        if (newSize == 0) {
            return EMPTY_CHAR_ARRAY;
        }

        int oldSize = array.length;
        if (oldSize == newSize) {
            return array;
        }

        char[] result = new char[newSize];
        System.arraycopy(array, 0, result, 0, Math.min(oldSize, newSize));
        return result;
    }

    @Contract(pure = true)
    public static <T> T[] toObjectArray(Collection<? extends T> collection, Class<T> aClass) {
        T[] array = newArray(aClass, collection.size());
        return collection.toArray(array);
    }

    @Contract(pure = true)
    public static <T> T[] toObjectArray(Class<T> aClass, Object... source) {
        T[] array = newArray(aClass, source.length);
        //noinspection SuspiciousSystemArraycopy
        System.arraycopy(source, 0, array, 0, array.length);
        return array;
    }

    @Contract(pure = true)
    public static Object[] toObjectArray(Collection<?> collection) {
        if (collection.isEmpty()) {
            return EMPTY_OBJECT_ARRAY;
        }
        //noinspection SSBasedInspection
        return collection.toArray(new Object[collection.size()]);
    }

    @Contract(pure = true)
    public static int[] toIntArray(Collection<Integer> list) {
        int[] ret = newIntArray(list.size());
        int i = 0;
        for (Integer e : list) {
            ret[i++] = e.intValue();
        }
        return ret;
    }

    @Contract(pure = true)
    public static int[] toIntArray(byte[] byteArray) {
        int[] ret = newIntArray(byteArray.length);
        for (int i = 0; i < byteArray.length; i++) {
            ret[i] = byteArray[i];
        }
        return ret;
    }

    @Contract(pure = true)
    public static <T> T[] mergeArrays(T[] a1, T[] a2) {
        if (a1.length == 0) {
            return a2;
        }
        if (a2.length == 0) {
            return a1;
        }

        Class<T> class1 = getComponentType(a1);
        Class<T> class2 = getComponentType(a2);
        Class<T> aClass = class1.isAssignableFrom(class2) ? class1 : class2;

        T[] result = newArray(aClass, a1.length + a2.length);
        System.arraycopy(a1, 0, result, 0, a1.length);
        System.arraycopy(a2, 0, result, a1.length, a2.length);
        return result;
    }

    @Contract(pure = true)
    public static <T> T[] mergeCollections(
        Collection<? extends T> c1,
        Collection<? extends T> c2,
        ArrayFactory<T> factory
    ) {
        T[] res = factory.create(c1.size() + c2.size());

        int i = 0;

        for (T t : c1) {
            res[i++] = t;
        }

        for (T t : c2) {
            res[i++] = t;
        }

        return res;
    }

    @Contract(pure = true)
    public static <T> T[] mergeArrays(T[] a1, T[] a2, ArrayFactory<T> factory) {
        if (a1.length == 0) {
            return a2;
        }
        if (a2.length == 0) {
            return a1;
        }
        T[] result = factory.create(a1.length + a2.length);
        System.arraycopy(a1, 0, result, 0, a1.length);
        System.arraycopy(a2, 0, result, a1.length, a2.length);
        return result;
    }

    @Contract(pure = true)
    public static String[] mergeArrays(String[] a1, String... a2) {
        return mergeArrays(a1, a2, STRING_ARRAY_FACTORY);
    }

    @Contract(pure = true)
    public static int[] mergeArrays(int[] a1, int[] a2) {
        if (a1.length == 0) {
            return a2;
        }
        if (a2.length == 0) {
            return a1;
        }
        int[] result = new int[a1.length + a2.length];
        System.arraycopy(a1, 0, result, 0, a1.length);
        System.arraycopy(a2, 0, result, a1.length, a2.length);
        return result;
    }

    @Contract(pure = true)
    public static byte[] mergeArrays(byte[] a1, byte[] a2) {
        if (a1.length == 0) {
            return a2;
        }
        if (a2.length == 0) {
            return a1;
        }
        byte[] result = new byte[a1.length + a2.length];
        System.arraycopy(a1, 0, result, 0, a1.length);
        System.arraycopy(a2, 0, result, a1.length, a2.length);
        return result;
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
    @Contract(pure = true)
    public static <T> T[] mergeArrayAndCollection(
        T[] array,
        Collection<T> collection,
        ArrayFactory<T> factory
    ) {
        if (collection.isEmpty()) {
            return array;
        }

        T[] array2;
        try {
            array2 = collection.toArray(factory.create(collection.size()));
        }
        catch (ArrayStoreException e) {
            throw new RuntimeException("Bad elements in collection: " + collection, e);
        }

        if (array.length == 0) {
            return array2;
        }

        T[] result = factory.create(array.length + collection.size());
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
    @Contract(pure = true)
    public static <T> T[] append(T[] src, T element) {
        //noinspection unchecked
        return append(src, element, (Class<T>)src.getClass().getComponentType());
    }

    @Contract(pure = true)
    public static <T> T[] prepend(T element, T[] array) {
        //noinspection unchecked
        return prepend(element, array, (Class<T>)array.getClass().getComponentType());
    }

    @Contract(pure = true)
    public static <T> T[] prepend(T element, T[] array, Class<T> type) {
        int length = array.length;
        T[] result = newArray(type, length + 1);
        System.arraycopy(array, 0, result, 1, length);
        result[0] = element;
        return result;
    }

    @Contract(pure = true)
    public static <T> T[] prepend(T element, T[] src, ArrayFactory<T> factory) {
        int length = src.length;
        T[] result = factory.create(length + 1);
        System.arraycopy(src, 0, result, 1, length);
        result[0] = element;
        return result;
    }

    @Contract(pure = true)
    public static byte[] prepend(byte element, byte[] array) {
        int length = array.length;
        byte[] result = new byte[length + 1];
        result[0] = element;
        System.arraycopy(array, 0, result, 1, length);
        return result;
    }

    @Contract(pure = true)
    public static <T> T[] append(T[] src, T element, ArrayFactory<T> factory) {
        int length = src.length;
        T[] result = factory.create(length + 1);
        System.arraycopy(src, 0, result, 0, length);
        result[length] = element;
        return result;
    }

    @Contract(pure = true)
    public static <T> T[] append(T[] src, T element, Class<T> componentType) {
        int length = src.length;
        T[] result = newArray(componentType, length + 1);
        System.arraycopy(src, 0, result, 0, length);
        result[length] = element;
        return result;
    }

    /**
     * Removes element with index {@code idx} from array {@code src}.
     *
     * @param src array.
     * @param idx index of element to be removed.
     * @return modified array.
     */
    @Contract(pure = true)
    public static <T> T[] remove(T[] src, int idx) {
        int length = src.length;
        if (idx < 0 || idx >= length) {
            throw new IllegalArgumentException("invalid index: " + idx);
        }
        T[] result = newArray(getComponentType(src), length - 1);
        System.arraycopy(src, 0, result, 0, idx);
        System.arraycopy(src, idx + 1, result, idx, length - idx - 1);
        return result;
    }

    public static <T> T[] newArray(Class<T> type, int length) {
        //noinspection unchecked
        return (T[])Array.newInstance(type, length);
    }

    @Contract(pure = true)
    public static <T> T[] remove(T[] src, int idx, IntFunction<T[]> factory) {
        int length = src.length;
        if (idx < 0 || idx >= length) {
            throw new IllegalArgumentException("invalid index: " + idx);
        }
        T[] result = factory.apply(length - 1);
        System.arraycopy(src, 0, result, 0, idx);
        System.arraycopy(src, idx + 1, result, idx, length - idx - 1);
        return result;
    }

    @Contract(pure = true)
    public static <T> T[] remove(T[] src, T element) {
        int idx = find(src, element);
        return idx == -1 ? src : remove(src, idx);
    }

    @Contract(pure = true)
    public static <T> T[] remove(T[] src, T element, IntFunction<T[]> factory) {
        int idx = find(src, element);
        return idx == -1 ? src : remove(src, idx, factory);
    }

    @Contract(pure = true)
    public static int[] remove(int[] src, int idx) {
        int length = src.length;
        if (idx < 0 || idx >= length) {
            throw new IllegalArgumentException("invalid index: " + idx);
        }
        int[] result = newIntArray(src.length - 1);
        System.arraycopy(src, 0, result, 0, idx);
        System.arraycopy(src, idx + 1, result, idx, length - idx - 1);
        return result;
    }

    @Contract(pure = true)
    public static short[] remove(short[] src, int idx) {
        int length = src.length;
        if (idx < 0 || idx >= length) {
            throw new IllegalArgumentException("invalid index: " + idx);
        }
        short[] result = src.length == 1 ? EMPTY_SHORT_ARRAY : new short[src.length - 1];
        System.arraycopy(src, 0, result, 0, idx);
        System.arraycopy(src, idx + 1, result, idx, length - idx - 1);
        return result;
    }

    @Contract(pure = true)
    public static int find(int[] src, int obj) {
        return indexOf(src, obj);
    }

    @Contract(pure = true)
    public static <T> int find(T[] src, T obj) {
        for (int i = 0; i < src.length; i++) {
            T o = src[i];
            if (o == null) {
                if (obj == null) {
                    return i;
                }
            }
            else if (o.equals(obj)) {
                return i;
            }
        }
        return -1;
    }

    @Contract(pure = true)
    public static boolean startsWith(byte[] array, byte[] prefix) {
        //noinspection ArrayEquality
        if (array == prefix) {
            return true;
        }
        int length = prefix.length;
        if (array.length < length) {
            return false;
        }

        for (int i = 0; i < length; i++) {
            if (array[i] != prefix[i]) {
                return false;
            }
        }

        return true;
    }

    @Contract(pure = true)
    public static <E> boolean startsWith(E[] array, E[] subArray) {
        //noinspection ArrayEquality
        if (array == subArray) {
            return true;
        }
        int length = subArray.length;
        if (array.length < length) {
            return false;
        }

        for (int i = 0; i < length; i++) {
            if (!Comparing.equal(array[i], subArray[i])) {
                return false;
            }
        }

        return true;
    }

    @Contract(pure = true)
    public static boolean startsWith(byte[] array, int start, byte[] subArray) {
        int length = subArray.length;
        if (array.length - start < length) {
            return false;
        }

        for (int i = 0; i < length; i++) {
            if (array[start + i] != subArray[i]) {
                return false;
            }
        }

        return true;
    }

    @Contract(pure = true)
    public static <T> boolean equals(T[] a1, T[] a2, HashingStrategy<? super T> comparator) {
        //noinspection ArrayEquality
        if (a1 == a2) {
            return true;
        }

        int length = a2.length;
        if (a1.length != length) {
            return false;
        }

        for (int i = 0; i < length; i++) {
            if (!comparator.equals(a1[i], a2[i])) {
                return false;
            }
        }
        return true;
    }

    @Contract(pure = true)
    public static <T> boolean equals(T[] a1, T[] a2, Comparator<? super T> comparator) {
        //noinspection ArrayEquality
        if (a1 == a2) {
            return true;
        }
        int length = a2.length;
        if (a1.length != length) {
            return false;
        }

        for (int i = 0; i < length; i++) {
            if (comparator.compare(a1[i], a2[i]) != 0) {
                return false;
            }
        }
        return true;
    }

    @Contract(pure = true)
    public static <T> T[] reverseArray(T[] array) {
        T[] newArray = array.clone();
        for (int i = 0; i < array.length; i++) {
            newArray[array.length - i - 1] = array[i];
        }
        return newArray;
    }

    @Contract(pure = true)
    public static int[] reverseArray(int[] array) {
        int[] newArray = array.clone();
        for (int i = 0; i < array.length; i++) {
            newArray[array.length - i - 1] = array[i];
        }
        return newArray;
    }

    @Contract(pure = true)
    public static int lexicographicCompare(int[] obj1, int[] obj2) {
        for (int i = 0; i < Math.min(obj1.length, obj2.length); i++) {
            int res = Integer.compare(obj1[i], obj2[i]);
            if (res != 0) {
                return res;
            }
        }
        return Integer.compare(obj1.length, obj2.length);
    }

    @Contract(pure = true)
    public static int lexicographicCompare(String[] obj1, String[] obj2) {
        for (int i = 0; i < Math.max(obj1.length, obj2.length); i++) {
            String o1 = i < obj1.length ? obj1[i] : null;
            String o2 = i < obj2.length ? obj2[i] : null;
            if (o1 == null) {
                return -1;
            }
            if (o2 == null) {
                return 1;
            }
            int res = o1.compareToIgnoreCase(o2);
            if (res != 0) {
                return res;
            }
        }
        return 0;
    }

    //must be Comparables
    @Contract(pure = true)
    public static <T> int lexicographicCompare(T[] obj1, T[] obj2) {
        for (int i = 0; i < Math.max(obj1.length, obj2.length); i++) {
            T o1 = i < obj1.length ? obj1[i] : null;
            T o2 = i < obj2.length ? obj2[i] : null;
            if (o1 == null) {
                return -1;
            }
            if (o2 == null) {
                return 1;
            }
            //noinspection unchecked
            int res = ((Comparable)o1).compareTo(o2);
            if (res != 0) {
                return res;
            }
        }
        return 0;
    }

    public static <T> void swap(T[] array, int i1, int i2) {
        T t = array[i1];
        array[i1] = array[i2];
        array[i2] = t;
    }

    public static void swap(int[] array, int i1, int i2) {
        int t = array[i1];
        array[i1] = array[i2];
        array[i2] = t;
    }

    public static void swap(boolean[] array, int i1, int i2) {
        boolean t = array[i1];
        array[i1] = array[i2];
        array[i2] = t;
    }

    public static void swap(char[] array, int i1, int i2) {
        char t = array[i1];
        array[i1] = array[i2];
        array[i2] = t;
    }

    public static <T> void rotateLeft(T[] array, int i1, int i2) {
        T t = array[i1];
        System.arraycopy(array, i1 + 1, array, i1, i2 - i1);
        array[i2] = t;
    }

    public static <T> void rotateRight(T[] array, int i1, int i2) {
        T t = array[i2];
        System.arraycopy(array, i1, array, i1 + 1, i2 - i1);
        array[i1] = t;
    }

    @Contract(pure = true)
    public static int indexOf(Object[] objects, @Nullable Object object) {
        return indexOf(objects, object, 0, objects.length);
    }

    @Contract(pure = true)
    public static int indexOf(Object[] objects, @Nullable Object object, int start, int end) {
        if (object == null) {
            for (int i = start; i < end; i++) {
                if (objects[i] == null) {
                    return i;
                }
            }
        }
        else {
            for (int i = start; i < end; i++) {
                if (object.equals(objects[i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Contract(pure = true)
    public static <T> int indexOf(List<T> objects, T object, HashingStrategy<T> comparator) {
        for (int i = 0, n = objects.size(); i < n; i++) {
            if (comparator.equals(objects.get(i), object)) {
                return i;
            }
        }
        return -1;
    }

    @Contract(pure = true)
    public static <T> int indexOf(List<T> objects, T object, Comparator<T> comparator) {
        for (int i = 0, n = objects.size(); i < n; i++) {
            if (comparator.compare(objects.get(i), object) == 0) {
                return i;
            }
        }
        return -1;
    }

    @Contract(pure = true)
    public static <T> int indexOf(T[] objects, T object, HashingStrategy<T> comparator) {
        for (int i = 0, n = objects.length; i < n; i++) {
            if (comparator.equals(objects[i], object)) {
                return i;
            }
        }
        return -1;
    }

    @Contract(pure = true)
    public static int indexOf(long[] ints, long value) {
        for (int i = 0, n = ints.length; i < n; i++) {
            if (ints[i] == value) {
                return i;
            }
        }

        return -1;
    }

    @Contract(pure = true)
    public static int indexOf(int[] ints, int value) {
        for (int i = 0, n = ints.length; i < n; i++) {
            if (ints[i] == value) {
                return i;
            }
        }

        return -1;
    }

    @Contract(pure = true)
    public static int indexOf(short[] ints, short value) {
        for (int i = 0, n = ints.length; i < n; i++) {
            if (ints[i] == value) {
                return i;
            }
        }

        return -1;
    }

    @Contract(pure = true)
    public static <T> int lastIndexOf(T[] src, T obj) {
        for (int i = src.length - 1; i >= 0; i--) {
            T o = src[i];
            if (o == null) {
                if (obj == null) {
                    return i;
                }
            }
            else if (o.equals(obj)) {
                return i;
            }
        }
        return -1;
    }

    @Contract(pure = true)
    public static int lastIndexOf(int[] src, int obj) {
        for (int i = src.length - 1; i >= 0; i--) {
            int o = src[i];
            if (o == obj) {
                return i;
            }
        }
        return -1;
    }

    @Contract(pure = true)
    public static <T> int lastIndexOf(T[] src, T obj, HashingStrategy<? super T> comparator) {
        for (int i = src.length - 1; i >= 0; i--) {
            T o = src[i];
            if (comparator.equals(obj, o)) {
                return i;
            }
        }
        return -1;
    }

    @Contract(pure = true)
    public static <T> int lastIndexOf(List<T> src, T obj, HashingStrategy<? super T> comparator) {
        for (int i = src.size() - 1; i >= 0; i--) {
            T o = src.get(i);
            if (comparator.equals(obj, o)) {
                return i;
            }
        }
        return -1;
    }

    @Contract(pure = true)
    public static int lastIndexOfNot(int[] src, int obj) {
        for (int i = src.length - 1; i >= 0; i--) {
            int o = src[i];
            if (o != obj) {
                return i;
            }
        }
        return -1;
    }

    @Contract(pure = true)
    public static <T> boolean contains(@Nullable T o, T... objects) {
        return indexOf(objects, o) >= 0;
    }

    @Contract(pure = true)
    public static boolean contains(@Nullable String s, String... strings) {
        if (s == null) {
            for (String str : strings) {
                if (str == null) {
                    return true;
                }
            }
        }
        else {
            for (String str : strings) {
                if (s.equals(str)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Contract(pure = true)
    public static int[] newIntArray(int count) {
        return count == 0 ? EMPTY_INT_ARRAY : new int[count];
    }

    @Contract(pure = true)
    public static byte[] newByteArray(int count) {
        return count == 0 ? EMPTY_BYTE_ARRAY : new byte[count];
    }

    @Contract(pure = true)
    public static long[] newLongArray(int count) {
        return count == 0 ? EMPTY_LONG_ARRAY : new long[count];
    }

    @Contract(pure = true)
    public static String[] newStringArray(int count) {
        return count == 0 ? EMPTY_STRING_ARRAY : new String[count];
    }

    @Contract(pure = true)
    public static Object[] newObjectArray(int count) {
        return count == 0 ? EMPTY_OBJECT_ARRAY : new Object[count];
    }

    @Contract(pure = true)
    public static <E> E[] ensureExactSize(int count, E[] sample) {
        return count == sample.length ? sample : newArray(getComponentType(sample), count);
    }

    @Contract(pure = true)
    public static <T> @Nullable T getFirstElement(@Nullable T[] array) {
        return array != null && array.length > 0 ? array[0] : null;
    }

    @Contract(pure = true)
    public static <T> @Nullable T getLastElement(@Nullable T[] array) {
        return array != null && array.length > 0 ? array[array.length - 1] : null;
    }

    @Contract(pure = true)
    public static int getLastElement(@Nullable int[] array, int defaultValue) {
        return array == null || array.length == 0 ? defaultValue : array[array.length - 1];
    }

    @Contract(value = "null -> true", pure = true)
    public static <T> boolean isEmpty(@Nullable T[] array) {
        return array == null || array.length == 0;
    }

    @Contract(pure = true)
    public static String[] toStringArray(@Nullable Collection<String> collection) {
        return collection == null || collection.isEmpty() ? EMPTY_STRING_ARRAY : collection.toArray(new String[collection.size()]);
    }

    public static <T> void copy(Collection<? extends T> src, T[] dst, int dstOffset) {
        int i = dstOffset;
        for (T t : src) {
            dst[i++] = t;
        }
    }

    @Contract("null -> null; !null -> !null")
    public static <T> T @Nullable [] copyOf(T @Nullable [] original) {
        return original == null ? null : Arrays.copyOf(original, original.length);
    }

    @Contract("null -> null; !null -> !null")
    public static boolean @Nullable [] copyOf(boolean @Nullable [] original) {
        return original == null ? null : Arrays.copyOf(original, original.length);
    }

    @Contract("null -> null; !null -> !null")
    public static int @Nullable [] copyOf(int @Nullable [] original) {
        return original == null ? null : Arrays.copyOf(original, original.length);
    }

    public static <T> T[] stripTrailingNulls(T[] array) {
        return array.length != 0 && array[array.length - 1] == null ? Arrays.copyOf(array, trailingNullsIndex(array)) : array;
    }

    private static <T> int trailingNullsIndex(T[] array) {
        for (int i = array.length - 1; i >= 0; i--) {
            if (array[i] != null) {
                return i + 1;
            }
        }
        return 0;
    }

    // calculates average of the median values in the selected part of the array. E.g. for part=3 returns average in the middle third.
    public static long averageAmongMedians(long[] time, int part) {
        assert part >= 1;
        int n = time.length;
        Arrays.sort(time);
        long total = 0;
        for (int i = n / 2 - n / part / 2; i < n / 2 + n / part / 2; i++) {
            total += time[i];
        }
        int middlePartLength = n / part;
        return middlePartLength == 0 ? 0 : total / middlePartLength;
    }

    public static long averageAmongMedians(int[] time, int part) {
        assert part >= 1;
        int n = time.length;
        Arrays.sort(time);
        long total = 0;
        for (int i = n / 2 - n / part / 2; i < n / 2 + n / part / 2; i++) {
            total += time[i];
        }
        int middlePartLength = n / part;
        return middlePartLength == 0 ? 0 : total / middlePartLength;
    }

    public static int min(int[] values) {
        int min = Integer.MAX_VALUE;
        for (int value : values) {
            if (value < min) {
                min = value;
            }
        }
        return min;
    }

    public static int max(int[] values) {
        int max = Integer.MIN_VALUE;
        for (int value : values) {
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    public static <T> Class<T> getComponentType(T[] collection) {
        //noinspection unchecked
        return (Class<T>)collection.getClass().getComponentType();
    }
}
