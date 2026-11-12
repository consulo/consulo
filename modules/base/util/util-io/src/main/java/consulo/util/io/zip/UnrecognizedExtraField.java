/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package consulo.util.io.zip;

import consulo.util.collection.ArrayUtil;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

class UnrecognizedExtraField implements JBZipExtraField {
    private final ZipShort myHeaderId;

    private byte @Nullable [] myLocalData;
    private byte @Nullable [] myCentralData;

    UnrecognizedExtraField(ZipShort headerId) {
        myHeaderId = headerId;
    }

    @Override
    public ZipShort getHeaderId() {
        return myHeaderId;
    }

    public void setLocalFileDataData(final byte @Nullable [] data) {
        myLocalData = copyOf(data);
    }

    @Override
    public ZipShort getLocalFileDataLength() {
        return new ZipShort(myLocalData != null ? myLocalData.length : 0);
    }

    @Override
    public byte[] getLocalFileDataData() {
        byte[] copy = copyOf(myLocalData);
        return copy != null ? copy : ArrayUtil.EMPTY_BYTE_ARRAY;
    }

    public void setCentralDirectoryData(final byte @Nullable [] data) {
        myCentralData = copyOf(data);
    }

    @Override
    public ZipShort getCentralDirectoryLength() {
        if (myCentralData != null) {
            return new ZipShort(myCentralData.length);
        }
        return getLocalFileDataLength();
    }

    @Override
    public byte[] getCentralDirectoryData() {
        if (myCentralData != null) {
            return myCentralData.clone();
        }
        return getLocalFileDataData();
    }

    @Override
    public void parseFromLocalFileData(byte[] data,
                                       int offset,
                                       int length) {
        setLocalFileDataData(Arrays.copyOfRange(data, offset, offset + length));
    }

    @Override
    public void parseFromCentralDirectoryData(byte[] data,
                                              int offset,
                                              int length) {
        final byte[] tmp = Arrays.copyOfRange(data, offset, offset + length);
        setCentralDirectoryData(tmp);
        if (myLocalData == null) {
            setLocalFileDataData(tmp);
        }
    }

    private static byte @Nullable [] copyOf(byte @Nullable [] original) {
        return original == null ? null : original.clone();
    }
}
