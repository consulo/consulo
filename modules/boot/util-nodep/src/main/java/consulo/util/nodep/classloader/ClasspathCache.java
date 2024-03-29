// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.util.nodep.classloader;

import consulo.util.nodep.ArrayUtilRt;
import consulo.util.nodep.BloomFilterBase;
import consulo.util.nodep.io.DataInputOutputUtilRt;
import consulo.util.nodep.text.StringHash;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author max
 */
public class ClasspathCache {
  static final int NUMBER_OF_ACCESSES_FOR_LAZY_CACHING = 1000;
  private final IntObjectHashMap myResourcePackagesCache = new IntObjectHashMap();
  private final IntObjectHashMap myClassPackagesCache = new IntObjectHashMap();

  private static final double PROBABILITY = 0.005d;

  static class LoaderData {
    private final int[] myResourcePackageHashes;
    private final int[] myClassPackageHashes;
    private final NameFilter myNameFilter;

    @Deprecated
    LoaderData() {
      this(new int[0], new int[0], null);
    }

    LoaderData(int[] resourcePackageHashes, int[] classPackageHashes, NameFilter nameFilter) {
      myResourcePackageHashes = resourcePackageHashes;
      myClassPackageHashes = classPackageHashes;
      myNameFilter = nameFilter;
    }

    LoaderData(DataInput dataInput) throws IOException {
      this(readIntList(dataInput), readIntList(dataInput), new ClasspathCache.NameFilter(dataInput));
    }

    private static int[] readIntList(DataInput reader) throws IOException {
      int numberOfElements = DataInputOutputUtilRt.readINT(reader);
      int[] ints = new int[numberOfElements];
      for (int i = 0; i < numberOfElements; ++i) ints[i] = (DataInputOutputUtilRt.readINT(reader));
      return ints;
    }

    void save(DataOutput dataOutput) throws IOException {
      writeIntArray(dataOutput, myResourcePackageHashes);
      writeIntArray(dataOutput, myClassPackageHashes);
      myNameFilter.save(dataOutput);
    }

    private static void writeIntArray(DataOutput writer, int[] hashes) throws IOException {
      DataInputOutputUtilRt.writeINT(writer, hashes.length);
      for (int hash : hashes) DataInputOutputUtilRt.writeINT(writer, hash);
    }

    NameFilter getNameFilter() {
      return myNameFilter;
    }
  }

  static class LoaderDataBuilder {
    private final LongHashSet myUsedNameFingerprints = new LongHashSet();
    private final IntHashSet myResourcePackageHashes = new IntHashSet();
    private final IntHashSet myClassPackageHashes = new IntHashSet();

    LoaderDataBuilder() {
    }

    void addPossiblyDuplicateNameEntry(String name) {
      name = transformName(name);
      myUsedNameFingerprints.add(NameFilter.toNameFingerprint(name));
    }

    void addResourcePackageFromName(String path) {
      myResourcePackageHashes.add(getPackageNameHash(path));
    }

    void addClassPackageFromName(String path) {
      myClassPackageHashes.add(getPackageNameHash(path));
    }

    LoaderData build() {
      int uniques = myUsedNameFingerprints.size();
      if (uniques > 20000) {
        uniques += (int)(uniques * 0.03d); // allow some growth for Idea main loader
      }
      final NameFilter nameFilter = new NameFilter(uniques, PROBABILITY);
      myUsedNameFingerprints.forEach(new LongHashSet.LongConsumer() {
        @Override
        public void consume(long value) {
          nameFilter.addNameFingerprint(value);
        }
      });

      return new ClasspathCache.LoaderData(myResourcePackageHashes.toArray(), myClassPackageHashes.toArray(), nameFilter);
    }
  }

  private final ReadWriteLock myLock = new ReentrantReadWriteLock();

  public void applyLoaderData(LoaderData loaderData, final Loader loader) {
    myLock.writeLock().lock();
    try {
      for (int resourcePackageHash : loaderData.myResourcePackageHashes) {
        addResourceEntry(resourcePackageHash, myResourcePackagesCache, loader);
      }

      for (int classPackageHash : loaderData.myClassPackageHashes) {
        addResourceEntry(classPackageHash, myClassPackagesCache, loader);
      }

      loader.applyData(loaderData);
    }
    finally {
      myLock.writeLock().unlock();
    }
  }

  abstract static class LoaderIterator<ResultType, ParameterType, ParameterType2> {
    abstract ResultType process(Loader loader, ParameterType parameter, ParameterType2 parameter2, String shortName);
  }

  <ResultType, ParameterType, ParameterType2> ResultType iterateLoaders(String resourcePath,
                                                                        LoaderIterator<ResultType, ParameterType, ParameterType2> iterator,
                                                                        ParameterType parameter,
                                                                        ParameterType2 parameter2,
                                                                        String shortName) {
    Object o;

    myLock.readLock().lock();
    try {
      IntObjectHashMap map = resourcePath.endsWith(UrlClassLoader.CLASS_EXTENSION) ? myClassPackagesCache : myResourcePackagesCache;

      o = map.get(getPackageNameHash(resourcePath));
    }
    finally {
      myLock.readLock().unlock();
    }

    if (o == null) return null;
    if (o instanceof Loader) return iterator.process((Loader)o, parameter, parameter2, shortName);
    Loader[] loaders = (Loader[])o;
    for (Loader l : loaders) {
      ResultType result = iterator.process(l, parameter, parameter2, shortName);
      if (result != null) return result;
    }
    return null;
  }

  static int getPackageNameHash(String resourcePath) {
    final int idx = resourcePath.lastIndexOf('/');
    int h = 0;
    for (int off = 0; off < idx; off++) {
      h = 31 * h + resourcePath.charAt(off);
    }
    return h;
  }

  private static void addResourceEntry(int hash, IntObjectHashMap map, Loader loader) {
    Object o = map.get(hash);
    if (o == null) {
      map.put(hash, loader);
    }
    else if (o instanceof Loader) {
      if (ClassPath.ourLogTiming) assert loader != o;
      map.put(hash, new Loader[]{(Loader)o, loader});
    }
    else {
      Loader[] loadersArray = (Loader[])o;
      if (ClassPath.ourLogTiming) assert ArrayUtilRt.find(loadersArray, loader) == -1;
      Loader[] newArray = new Loader[loadersArray.length + 1];
      System.arraycopy(loadersArray, 0, newArray, 0, loadersArray.length);
      newArray[loadersArray.length] = loader;
      map.put(hash, newArray);
    }
  }

  static String transformName(String name) {
    int nameEnd = !name.isEmpty() && name.charAt(name.length() - 1) == '/' ? name.length() - 1 : name.length();
    name = name.substring(name.lastIndexOf('/', nameEnd - 1) + 1, nameEnd);

    if (name.endsWith(UrlClassLoader.CLASS_EXTENSION)) {
      String name1 = name;
      int $ = name1.indexOf('$');
      if ($ != -1) {
        name1 = name1.substring(0, $);
      }
      else {
        int index = name1.lastIndexOf('.');
        if (index >= 0) name1 = name1.substring(0, index);
      }
      name = name1;
    }
    return name;
  }

  static class NameFilter extends BloomFilterBase {
    private static final int SEED = 31;

    protected NameFilter(int _maxElementCount, double probability) {
      super(_maxElementCount, probability);
    }

    protected NameFilter(DataInput input) throws IOException {
      super(input);
    }

    private void addNameFingerprint(long nameFingerprint) {
      int hash = (int)(nameFingerprint >> 32);
      int hash2 = (int)nameFingerprint;
      addIt(hash, hash2);
    }

    boolean maybeContains(String name) {
      int hash = name.hashCode();
      int hash2 = StringHash.murmur(name, SEED);
      return maybeContains(hash, hash2);
    }

    @Override
    protected void save(DataOutput output) throws IOException {
      super.save(output);
    }

    private static long toNameFingerprint(String name) {
      int hash = name.hashCode();
      int hash2 = StringHash.murmur(name, SEED);
      return ((long)hash << 32) | (hash2 & 0xFFFFFFFFL);
    }
  }
}