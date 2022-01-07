// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.vfs.newvfs.persistent;

import com.intellij.openapi.util.ThreadLocalCachedValue;
import com.intellij.util.io.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Arrays;

public class ContentHashesUtil {
  public static final ThreadLocalCachedValue<MessageDigest> HASHER_CACHE = new ThreadLocalCachedValue<MessageDigest>() {
    @Nonnull
    @Override
    public MessageDigest create() {
      return createHashDigest();
    }

    @Override
    protected void init(@Nonnull MessageDigest value) {
      value.reset();
    }
  };

  @Nonnull
  static MessageDigest createHashDigest() {
    return DigestUtil.sha1();
  }

  private static final int SIGNATURE_LENGTH = 20;

  public static class HashEnumerator extends PersistentBTreeEnumerator<byte[]> {
    public HashEnumerator(@Nonnull File contentsHashesFile) throws IOException {
      this(contentsHashesFile, null);
    }

    public HashEnumerator(@Nonnull File contentsHashesFile, @Nullable PagedFileStorage.StorageLockContext storageLockContext) throws IOException {
      super(contentsHashesFile, new ContentHashesDescriptor(), 64 * 1024, storageLockContext);
    }

    @Override
    protected int doWriteData(byte[] value) throws IOException {
      return super.doWriteData(value) / SIGNATURE_LENGTH;
    }

    @Override
    public int getLargestId() {
      return super.getLargestId() / SIGNATURE_LENGTH;
    }

    private final ThreadLocal<Boolean> myProcessingKeyAtIndex = new ThreadLocal<>();

    @Override
    protected boolean isKeyAtIndex(byte[] value, int idx) throws IOException {
      myProcessingKeyAtIndex.set(Boolean.TRUE);
      try {
        return super.isKeyAtIndex(value, addrToIndex(indexToAddr(idx) * SIGNATURE_LENGTH));
      }
      finally {
        myProcessingKeyAtIndex.set(null);
      }
    }

    @Override
    public byte[] valueOf(int idx) throws IOException {
      if (myProcessingKeyAtIndex.get() == Boolean.TRUE) return super.valueOf(idx);
      return super.valueOf(addrToIndex(indexToAddr(idx) * SIGNATURE_LENGTH));
    }

    @Override
    public int tryEnumerate(byte[] value) throws IOException {
      return super.tryEnumerate(value);
    }
  }

  private static class ContentHashesDescriptor implements KeyDescriptor<byte[]>, DifferentSerializableBytesImplyNonEqualityPolicy {
    @Override
    public void save(@Nonnull DataOutput out, byte[] value) throws IOException {
      out.write(value);
    }

    @Override
    public byte[] read(@Nonnull DataInput in) throws IOException {
      byte[] b = new byte[SIGNATURE_LENGTH];
      in.readFully(b);
      return b;
    }

    @Override
    public int hashCode(byte[] value) {
      int hash = 0; // take first 4 bytes, this should be good enough hash given we reference git revisions with 7-8 hex digits
      for (int i = 0; i < 4; ++i) {
        hash = (hash << 8) + (value[i] & 0xFF);
      }
      return hash;
    }

    @Override
    public boolean equals(byte[] val1, byte[] val2) {
      return Arrays.equals(val1, val2);
    }
  }
}
