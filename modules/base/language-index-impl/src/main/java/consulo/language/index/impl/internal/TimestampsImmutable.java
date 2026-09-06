// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal;

import consulo.application.ApplicationManager;
import consulo.index.io.ID;
import consulo.index.io.data.DataInputOutputUtil;
import consulo.language.psi.stub.StubIndexKey;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import org.jspecify.annotations.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class TimestampsImmutable {

    private interface InputAdapter {
        long readTime() throws IOException;

        boolean hasRemaining() throws IOException;

        int readInt() throws IOException;
    }

    //FIXME RC: this call to application makes us use ApplicationRule in tests for TimestampsImmutable -- which
    //          introduce completely superficial coupling, because TimestampsImmutable logic has nothing to do with application
    private static final boolean IS_UNIT_TEST = ApplicationManager.getApplication().isUnitTestMode();

    public static final TimestampsImmutable EMPTY = new TimestampsImmutable(0, IntLists.emptyList(), IntLists.emptyList());

    public static TimestampsImmutable readTimestamps(@Nullable DataInputStream stream) throws IOException {
        if (stream != null) {
            return readTimestamps(new InputAdapter() {
                @Override
                public long readTime() throws IOException {
                    return DataInputOutputUtil.readTIME(stream);
                }

                @Override
                public boolean hasRemaining() throws IOException {
                    return stream.available() > 0;
                }

                @Override
                public int readInt() throws IOException {
                    return DataInputOutputUtil.readINT(stream);
                }
            });
        }
        else {
            return new TimestampsImmutable(Object2LongMaps.emptyMap());
        }
    }

    public static TimestampsImmutable readTimestamps(@Nullable ByteBuffer buffer) throws IOException {
        if (buffer != null) {
            buffer.order(ByteOrder.BIG_ENDIAN); //to be compatible with .writeToStream()
            return readTimestamps(new InputAdapter() {
                @Override
                public long readTime() {
                    return DataInputOutputUtil.readTIME(buffer);
                }

                @Override
                public boolean hasRemaining() {
                    return buffer.hasRemaining();
                }

                @Override
                public int readInt() {
                    return DataInputOutputUtil.readINT(buffer);
                }
            });
        }
        else {
            return new TimestampsImmutable(Object2LongMaps.emptyMap());
        }
    }

    private static TimestampsImmutable readTimestamps(InputAdapter stream) throws IOException {
        IntArrayList outdatedIndices = new IntArrayList();
        //'header' is either timestamp (dominatingIndexStamp), or, if timestamp is small enough
        // (<MAX_SHORT), it is really a number of 'outdatedIndices', followed by actual indices
        // ints (which is index id from ID class), and followed by another timestamp=dominatingIndexStamp
        // value
        long dominatingIndexStamp = stream.readTime();
        long diff = dominatingIndexStamp - DataInputOutputUtil.timeBase;
        if (diff > 0 && diff < ID.MAX_NUMBER_OF_INDICES) {
            int numberOfOutdatedIndices = (int) diff;
            outdatedIndices.ensureCapacity(numberOfOutdatedIndices);
            for (int i = 0; i < numberOfOutdatedIndices; i++) {
                outdatedIndices.add(stream.readInt());
            }
            dominatingIndexStamp = stream.readTime();
        }

        //and after is just a set of ints -- Index IDs from ID class
        IntArrayList upToDateIndexIds = new IntArrayList();
        while (stream.hasRemaining()) {
            upToDateIndexIds.add(stream.readInt());
        }

        if (upToDateIndexIds.isEmpty() && outdatedIndices.isEmpty()) {
            assert dominatingIndexStamp == DataInputOutputUtil.timeBase
                : "dominatingIndexStamp=" + dominatingIndexStamp + " != timeBase=" + DataInputOutputUtil.timeBase;
            dominatingIndexStamp = 0; //MAYBE RC: return EMPTY?
        }
        return new TimestampsImmutable(dominatingIndexStamp, outdatedIndices, upToDateIndexIds);
    }

    private final long myDominatingIndexStamp;
    private final IntList myOutdatedIndexIds;
    private final IntList myUpToDateIndexIds;

    private TimestampsImmutable(long dominatingStampIndex, IntList outdatedIndexIds, IntList upToDateIndexIds) {
        myDominatingIndexStamp = dominatingStampIndex;
        myUpToDateIndexIds = upToDateIndexIds;
        myOutdatedIndexIds = outdatedIndexIds;
    }

    TimestampsImmutable(Object2LongMap<ID<?, ?>> indexStamps) {
        if (indexStamps.isEmpty()) {
            myOutdatedIndexIds = IntLists.emptyList();
            myUpToDateIndexIds = IntLists.emptyList();
            myDominatingIndexStamp = 0;
        }
        else {
            long dominatingIndexStamp = 0;
            myOutdatedIndexIds = new IntArrayList();
            myUpToDateIndexIds = new IntArrayList();

            List<Object2LongMap.Entry<ID<?, ?>>> entries = new ArrayList<>(indexStamps.object2LongEntrySet());
            entries.sort(Comparator.comparingInt((Object2LongMap.Entry<ID<?, ?>> e) -> e.getKey().getUniqueId()));

            for (Object2LongMap.Entry<ID<?, ?>> entry : entries) {
                long indexStamp = entry.getLongValue();
                if (indexStamp == IndexingStamp.INDEX_DATA_OUTDATED_STAMP) {
                    myOutdatedIndexIds.add(entry.getKey().getUniqueId());
                    indexStamp = IndexVersion.getIndexCreationStamp(entry.getKey());
                }
                else {
                    myUpToDateIndexIds.add(entry.getKey().getUniqueId());
                }
                dominatingIndexStamp = Math.max(dominatingIndexStamp, indexStamp);

                if (IS_UNIT_TEST && indexStamp == IndexingStamp.HAS_NO_INDEXED_DATA_STAMP) {
                    FileBasedIndexImpl.LOG.info("Wrong indexing timestamp state: " + indexStamps);
                }
            }

            myDominatingIndexStamp = dominatingIndexStamp;
        }
    }

    // Indexed stamp compact format:
    // (DataInputOutputUtil.timeBase + numberOfOutdatedIndices outdated_index_id+)? (dominating_index_stamp) index_id*
    // Note, that FSRecords.REASONABLY_SMALL attribute storage allocation policy will give an attribute 32 bytes to each file
    // Compact format allows 22 indexed states in this state
    public void writeToStream(DataOutputStream stream) throws IOException {
        if (myOutdatedIndexIds.isEmpty() && myUpToDateIndexIds.isEmpty()) {
            DataInputOutputUtil.writeTIME(stream, DataInputOutputUtil.timeBase);
            return;
        }

        int numberOfOutdatedIndex = myOutdatedIndexIds.size();
        if (numberOfOutdatedIndex > 0) {
            assert numberOfOutdatedIndex < ID.MAX_NUMBER_OF_INDICES;
            DataInputOutputUtil.writeTIME(stream, DataInputOutputUtil.timeBase + numberOfOutdatedIndex);
            for (int i = 0; i < myOutdatedIndexIds.size(); i++) {
                DataInputOutputUtil.writeINT(stream, myOutdatedIndexIds.getInt(i));
            }
        }

        DataInputOutputUtil.writeTIME(stream, myDominatingIndexStamp);

        for (int i = 0; i < myUpToDateIndexIds.size(); i++) {
            DataInputOutputUtil.writeINT(stream, myUpToDateIndexIds.getInt(i));
        }
    }

    public Timestamps toMutableTimestamps() {
        Object2LongOpenHashMap<ID<?, ?>> indexStamps = new Object2LongOpenHashMap<>();

        for (int i = 0; i < myUpToDateIndexIds.size(); i++) {
            ID<?, ?> id = ID.findById(myUpToDateIndexIds.getInt(i));
            if (id != null && !(id instanceof StubIndexKey)) {
                long stamp = IndexVersion.getIndexCreationStamp(id);
                if (stamp != 0L) {
                    // All (indices) IDs should be valid in this running session
                    // (e.g. we can have ID instance existing but index is not registered)
                    if (stamp <= myDominatingIndexStamp) {
                        indexStamps.put(id, stamp);
                    }
                }
            }
        }

        for (int i = 0; i < myOutdatedIndexIds.size(); i++) {
            ID<?, ?> id = ID.findById(myOutdatedIndexIds.getInt(i));
            if (id != null && !(id instanceof StubIndexKey)) {
                if (IndexVersion.getIndexCreationStamp(id) != 0L) {
                    // All (indices) IDs should be valid in this running session
                    // (e.g. we can have ID instance existing but index is not registered)
                    long stamp = IndexingStamp.INDEX_DATA_OUTDATED_STAMP;
                    if (stamp <= myDominatingIndexStamp) {
                        indexStamps.put(id, stamp);
                    }
                }
            }
        }
        return new Timestamps(indexStamps);
    }

    @Override
    public String toString() {
        return "TimestampsImmutable(dominatingIndexStamp=" + myDominatingIndexStamp
            + ", outdatedIndexIds=" + myOutdatedIndexIds
            + ", upToDateIndexIds=" + myUpToDateIndexIds + ")";
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        TimestampsImmutable that = (TimestampsImmutable) other;

        if (myDominatingIndexStamp != that.myDominatingIndexStamp) {
            return false;
        }
        if (!myOutdatedIndexIds.equals(that.myOutdatedIndexIds)) {
            return false;
        }
        if (!myUpToDateIndexIds.equals(that.myUpToDateIndexIds)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = Long.hashCode(myDominatingIndexStamp);
        result = 31 * result + myOutdatedIndexIds.hashCode();
        result = 31 * result + myUpToDateIndexIds.hashCode();
        return result;
    }
}
