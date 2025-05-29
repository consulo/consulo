// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints;

import consulo.index.io.data.DataExternalizer;
import consulo.index.io.data.DataInputOutputUtil;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Stores up to 127 elements with a single byte payload and reference data
 */
public class TinyTree<T> {
    private static final byte NO_ELEMENT = -1;
    private final ByteArrayList firstChild;
    private final ByteArrayList nextChild;
    private final ByteArrayList payload;
    private final ArrayList<T> data;

    private TinyTree(ByteArrayList firstChild,
                     ByteArrayList nextChild,
                     ByteArrayList payload,
                     ArrayList<T> data) {
        this.firstChild = firstChild;
        this.nextChild = nextChild;
        this.payload = payload;
        this.data = data;
    }

    public TinyTree(byte rootPayload, T rootData) {
        this.firstChild = new ByteArrayList();
        this.nextChild = new ByteArrayList();
        this.payload = new ByteArrayList();
        this.data = new ArrayList<>();
        this.payload.add(rootPayload);
        this.data.add(rootData);
        this.firstChild.add(NO_ELEMENT);
        this.nextChild.add(NO_ELEMENT);
    }

    /**
     * Adds node to the tree; it will appear before other children
     *
     * @return index
     */
    public byte add(byte parent, byte nodePayload, T nodeData) {
        byte index = (byte) payload.size();
        if (index < 0) {
            throw new TooManyElementsException();
        }
        payload.add(nodePayload);
        data.add(nodeData);
        firstChild.add(NO_ELEMENT);
        int p = parent;
        byte previousFirst = firstChild.getByte(p);
        nextChild.add(previousFirst);
        firstChild.set(p, index);
        return index;
    }

    public byte getBytePayload(byte index) {
        return payload.getByte(index);
    }

    public T getDataPayload(byte index) {
        return data.get(index);
    }

    public void reverseChildren() {
        for (int i = 0; i < firstChild.size(); i++) {
            byte fc = firstChild.getByte(i);
            if (fc == NO_ELEMENT) continue;
            byte prev = NO_ELEMENT;
            byte curr = fc;
            while (curr != NO_ELEMENT) {
                byte next = nextChild.getByte(curr);
                nextChild.set(curr, prev);
                prev = curr;
                curr = next;
            }
            firstChild.set(i, prev);
        }
    }

    public void setBytePayload(byte nodePayload, byte index) {
        payload.set(index, nodePayload);
    }

    public void setDataPayload(T nodeData, byte index) {
        data.set(index, nodeData);
    }

    public void processChildren(byte index, Predicate<Byte> f) {
        int idx = index;
        byte child = firstChild.getByte(idx);
        while (child != NO_ELEMENT) {
            if (!f.test(child)) {
                break;
            }
            child = nextChild.getByte(child);
        }
    }

    /**
     * Processes sync children of this tree and another. If the number of children differs,
     * iterates up to the minimum of both.
     */
    public void syncProcessChildren(byte myIndex,
                                    byte otherIndex,
                                    TinyTree<T> other,
                                    BiFunction<Byte, Byte, Boolean> f) {
        byte curThis = firstChild.getByte(myIndex);
        byte curOther = other.firstChild.getByte(otherIndex);
        while (curThis != NO_ELEMENT && curOther != NO_ELEMENT) {
            if (!f.apply(curThis, curOther)) {
                break;
            }
            curThis = nextChild.getByte(curThis);
            curOther = other.nextChild.getByte(curOther);
        }
    }

    public boolean isSameAs(TinyTree<T> other,
                            BiFunction<Byte, Byte, Boolean> isPayloadSame,
                            BiFunction<T, T, Boolean> isDataSame) {
        if (size() != other.size()) return false;
        if (!firstChild.equals(other.firstChild)) return false;
        if (!nextChild.equals(other.nextChild)) return false;
        for (int i = 0; i < payload.size(); i++) {
            byte a = payload.getByte(i);
            byte b = other.payload.getByte(i);
            if (!isPayloadSame.apply(a, b)) return false;
        }
        for (int i = 0; i < data.size(); i++) {
            T a = data.get(i);
            T b = other.data.get(i);
            if (!isDataSame.apply(a, b)) return false;
        }
        return true;
    }

    public boolean isSameAs(TinyTree<T> other) {
        return isSameAs(other,
            (a, b) -> a.byteValue() == b.byteValue(),
            (a, b) -> a.equals(b));
    }

    public int size() {
        return payload.size();
    }

    public static class TooManyElementsException extends RuntimeException {
        public TooManyElementsException() {
            super();
        }
    }

    public static abstract class Externalizer<T> implements DataExternalizer<TinyTree<T>> {
        private static final int SERDE_VERSION = 0;

        /**
         * Increment on format change
         */
        public int serdeVersion() {
            return SERDE_VERSION;
        }

        protected abstract void writeDataPayload(DataOutput output, T payload) throws IOException;

        protected abstract T readDataPayload(DataInput input) throws IOException;

        @Override
        public void save(DataOutput output, TinyTree<T> tree) throws IOException {
            DataInputOutputUtil.writeINT(output, tree.size());
            writeByteArray(output, tree.firstChild);
            writeByteArray(output, tree.nextChild);
            writeByteArray(output, tree.payload);
            for (T payload : tree.data) {
                writeDataPayload(output, payload);
            }
        }

        @Override
        public TinyTree<T> read(DataInput input) throws IOException {
            int size = DataInputOutputUtil.readINT(input);
            ByteArrayList first = readByteArray(input, size);
            ByteArrayList next = readByteArray(input, size);
            ByteArrayList pay = readByteArray(input, size);
            ArrayList<T> dataList = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                dataList.add(readDataPayload(input));
            }
            return new TinyTree<>(first, next, pay, dataList);
        }

        private void writeByteArray(DataOutput output, ByteArrayList list) throws IOException {
            output.write(list.elements(), 0, list.size());
        }

        private ByteArrayList readByteArray(DataInput input, int size) throws IOException {
            byte[] buf = new byte[size];
            input.readFully(buf);
            return new ByteArrayList(buf);
        }
    }
}
