/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.psi.stubs;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ReadAction;
import consulo.component.ProcessCanceledException;
import consulo.index.io.StorageException;
import consulo.virtualFileSystem.FileAttribute;
import consulo.virtualFileSystem.NewVirtualFile;
import consulo.ide.impl.idea.util.indexing.*;
import consulo.index.io.internal.DebugAssertions;
import consulo.index.io.IndexStorage;
import consulo.ide.impl.idea.util.indexing.impl.InputDataDiffBuilder;
import consulo.ide.impl.idea.util.indexing.impl.forward.EmptyForwardIndex;
import consulo.index.io.PersistentHashMapValueStorage;
import consulo.index.io.ID;
import consulo.index.io.data.DataExternalizer;
import consulo.index.io.data.DataInputOutputUtil;
import consulo.language.Language;
import consulo.language.ast.IFileElementType;
import consulo.language.file.LanguageFileType;
import consulo.language.internal.SubstitutedFileType;
import consulo.language.parser.ParserDefinition;
import consulo.language.psi.stub.*;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectLocator;
import consulo.util.lang.BitUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@ExtensionImpl
public class StubUpdatingIndex extends SingleEntryFileBasedIndexExtension<SerializedStubTree> implements CustomImplementationFileBasedIndexExtension<Integer, SerializedStubTree> {
  static final Logger LOG = Logger.getInstance(StubUpdatingIndex.class);
  private static final int VERSION = 43 + (PersistentHashMapValueStorage.COMPRESSION_ENABLED ? 1 : 0);

  // todo remove once we don't need this for stub-ast mismatch debug info
  private static final FileAttribute INDEXED_STAMP = new FileAttribute("stubIndexStamp", 3, true);

  public static final ID<Integer, SerializedStubTree> INDEX_ID = ID.create("Stubs");

  private final FileBasedIndex.InputFilter myInputFilter;

  public static boolean canHaveStub(@Nonnull ProjectLocator projectLocator, @Nonnull VirtualFile file) {
    return canHaveStub(projectLocator, null, file);
  }

  public static boolean canHaveStub(@Nonnull ProjectLocator projectLocator, @Nullable Project project, @Nonnull VirtualFile file) {
    FileType fileType = SubstitutedFileType.substituteFileType(file, file.getFileType(), project == null ? projectLocator.guessProjectForFile(file) : project);
    if (fileType instanceof LanguageFileType) {
      final Language l = ((LanguageFileType)fileType).getLanguage();
      final ParserDefinition parserDefinition = ParserDefinition.forLanguage(l);
      if (parserDefinition == null) {
        return false;
      }

      final IFileElementType elementType = parserDefinition.getFileNodeType();
      if (elementType instanceof IStubFileElementType) {
        if (((IStubFileElementType)elementType).shouldBuildStubFor(file)) {
          return true;
        }
        FileBasedIndex fileBasedIndex = FileBasedIndex.getInstance();
        if (file instanceof NewVirtualFile &&
            fileBasedIndex instanceof FileBasedIndexImpl &&
            ((FileBasedIndexImpl)fileBasedIndex).getIndex(INDEX_ID).isIndexedStateForFile(((NewVirtualFile)file).getId(), file)) {
          return true;
        }
      }
    }
    final BinaryFileStubBuilder builder = BinaryFileStubBuilder.forFileType(fileType);
    return builder != null && builder.acceptsFile(file);
  }

  private final ProjectLocator myProjectLocator;

  @Inject
  public StubUpdatingIndex(ProjectLocator projectLocator) {
    myProjectLocator = projectLocator;

    myInputFilter = (project, file) -> canHaveStub(myProjectLocator, project, file);
  }

  @Nonnull
  @Override
  public ID<Integer, SerializedStubTree> getName() {
    return INDEX_ID;
  }

  @Nonnull
  @Override
  public SingleEntryIndexer<SerializedStubTree> getIndexer() {
    return new SingleEntryIndexer<SerializedStubTree>(false) {
      @Override
      @Nullable
      public SerializedStubTree computeValue(@Nonnull final FileContent inputData) {
        return ReadAction.compute(() -> {
          SerializedStubTree serializedStubTree = null;

          try {
            //if (Registry.is("use.prebuilt.indices")) {
            //  final PrebuiltStubsProvider prebuiltStubsProvider = PrebuiltStubsProviders.INSTANCE.forFileType(inputData.getFileType());
            //  if (prebuiltStubsProvider != null) {
            //    serializedStubTree = prebuiltStubsProvider.findStub(inputData);
            //    if (PrebuiltIndexProviderBase.DEBUG_PREBUILT_INDICES) {
            //      Stub stub = StubTreeBuilder.buildStubTree(inputData);
            //      if (serializedStubTree != null && stub != null) {
            //        check(serializedStubTree.getStub(false), stub);
            //        checkStubIndexes(serializedStubTree, stub);
            //      }
            //    }
            //  }
            //}

            if (serializedStubTree == null) {
              Stub rootStub = StubTreeBuilder.buildStubTree(inputData);
              if (rootStub != null) {
                serializedStubTree = new SerializedStubTree(rootStub, SerializationManagerEx.getInstanceEx(), StubForwardIndexExternalizer.IdeStubForwardIndexesExternalizer.INSTANCE);
                if (DebugAssertions.DEBUG) {
                  Stub deserialized = serializedStubTree.retrieveStubFromBytes(SerializationManagerEx.getInstanceEx());
                  check(deserialized, rootStub);
                }
              }
            }
          }
          catch (ProcessCanceledException pce) {
            throw pce;
          }
          catch (SerializerNotFoundException e) {
            throw new RuntimeException(e);
          }
          catch (Throwable t) {
            LOG.error("Error indexing:" + inputData.getFile(), t);
          }

          if (serializedStubTree == null) return null;

          VirtualFile file = inputData.getFile();
          boolean isBinary = file.getFileType().isBinary();
          int contentLength = isBinary ? -1 : inputData.getPsiFile().getTextLength();
          long byteLength = file.getLength();
          rememberIndexingStamp(file, isBinary, byteLength, contentLength);

          if (LOG.isDebugEnabled()) {
            LOG.debug("Indexing " + file + "; " + IndexingStampInfo.dumpSize(byteLength, contentLength));
          }
          return serializedStubTree;
        });
      }
    };
  }

  private static void checkStubIndexes(@Nonnull SerializedStubTree prebuiltSerializedTree, @Nonnull Stub calculatedStub) {
    Map<StubIndexKey, Map<Object, StubIdList>> calculatedStubIndexes = SerializedStubTree.indexTree(calculatedStub);
    assert calculatedStubIndexes.equals(prebuiltSerializedTree.getStubIndicesValueMap());
  }

  private static void check(@Nonnull Stub stub, @Nonnull Stub stub2) {
    assert stub.getStubType() == stub2.getStubType();
    List<? extends Stub> stubs = stub.getChildrenStubs();
    List<? extends Stub> stubs2 = stub2.getChildrenStubs();
    assert stubs.size() == stubs2.size();
    for (int i = 0, len = stubs.size(); i < len; ++i) {
      check(stubs.get(i), stubs2.get(i));
    }
  }

  private static final byte IS_BINARY_MASK = 1;
  private static final byte BYTE_AND_CHAR_LENGTHS_ARE_THE_SAME_MASK = 1 << 1;

  private static void rememberIndexingStamp(@Nonnull VirtualFile file, boolean isBinary, long contentByteLength, int contentCharLength) {
    try (DataOutputStream stream = INDEXED_STAMP.writeAttribute(file)) {
      DataInputOutputUtil.writeTIME(stream, file.getTimeStamp());
      DataInputOutputUtil.writeLONG(stream, contentByteLength);

      boolean lengthsAreTheSame = contentByteLength == contentCharLength;
      byte flags = 0;
      flags = BitUtil.set(flags, IS_BINARY_MASK, isBinary);
      flags = BitUtil.set(flags, BYTE_AND_CHAR_LENGTHS_ARE_THE_SAME_MASK, lengthsAreTheSame);
      stream.writeByte(flags);

      if (!lengthsAreTheSame && !isBinary) {
        DataInputOutputUtil.writeINT(stream, contentCharLength);
      }
    }
    catch (IOException e) {
      LOG.error(e);
    }
  }

  @Nullable
  public static IndexingStampInfo getIndexingStampInfo(@Nonnull VirtualFile file) {
    try (DataInputStream stream = INDEXED_STAMP.readAttribute(file)) {
      if (stream == null) {
        return null;
      }
      long stamp = DataInputOutputUtil.readTIME(stream);
      long byteLength = DataInputOutputUtil.readLONG(stream);

      byte flags = stream.readByte();
      boolean isBinary = BitUtil.isSet(flags, IS_BINARY_MASK);
      boolean readOnlyOneLength = BitUtil.isSet(flags, BYTE_AND_CHAR_LENGTHS_ARE_THE_SAME_MASK);

      int charLength;
      if (isBinary) {
        charLength = -1;
      }
      else if (readOnlyOneLength) {
        charLength = (int)byteLength;
      }
      else {
        charLength = DataInputOutputUtil.readINT(stream);
      }
      return new IndexingStampInfo(stamp, byteLength, charLength);
    }
    catch (IOException e) {
      LOG.error(e);
      return null;
    }
  }

  @Nonnull
  @Override
  public DataExternalizer<SerializedStubTree> getValueExternalizer() {
    return new SerializedStubTreeDataExternalizer();
  }

  @Nonnull
  @Override
  public FileBasedIndex.InputFilter getInputFilter() {
    return myInputFilter;
  }

  @Override
  public int getVersion() {
    return VERSION;
  }

  @Nonnull
  @Override
  public UpdatableIndex<Integer, SerializedStubTree, FileContent> createIndexImplementation(@Nonnull final FileBasedIndexExtension<Integer, SerializedStubTree> extension,
                                                                                            @Nonnull IndexStorage<Integer, SerializedStubTree> storage) throws StorageException, IOException {
    if (storage instanceof MemoryIndexStorage) {
      final MemoryIndexStorage<Integer, SerializedStubTree> memStorage = (MemoryIndexStorage<Integer, SerializedStubTree>)storage;
      memStorage.addBufferingStateListener(new MemoryIndexStorage.BufferingStateListener() {
        @Override
        public void bufferingStateChanged(final boolean newState) {
          ((StubIndexImpl)StubIndex.getInstance()).setDataBufferingEnabled(newState);
        }

        @Override
        public void memoryStorageCleared() {
          ((StubIndexImpl)StubIndex.getInstance()).cleanupMemoryStorage();
        }
      });
    }
    return new MyIndex(extension, storage);
  }

  private static class MyIndex extends VfsAwareMapReduceIndex<Integer, SerializedStubTree, FileContent> {
    private StubIndexImpl myStubIndex;
    private final StubVersionMap myStubVersionMap = new StubVersionMap();

    MyIndex(@Nonnull FileBasedIndexExtension<Integer, SerializedStubTree> extension, @Nonnull IndexStorage<Integer, SerializedStubTree> storage) throws StorageException, IOException {
      super(extension, storage, new EmptyForwardIndex(), new StubUpdatingForwardIndexAccessor(), null, null);
      ((StubUpdatingForwardIndexAccessor)getForwardIndexAccessor()).setIndex(this);
      checkNameStorage();
    }

    @Override
    protected void doFlush() throws IOException, StorageException {
      final StubIndexImpl stubIndex = getStubIndex();
      try {
        stubIndex.flush();
      }
      finally {
        super.doFlush();
      }
    }

    @Nonnull
    private StubIndexImpl getStubIndex() {
      StubIndexImpl index = myStubIndex;
      if (index == null) {
        myStubIndex = index = (StubIndexImpl)StubIndex.getInstance();
      }
      return index;
    }

    private static void checkNameStorage() throws StorageException {
      final SerializationManagerEx serializationManager = SerializationManagerEx.getInstanceEx();
      if (serializationManager.isNameStorageCorrupted()) {
        serializationManager.repairNameStorage();
        throw new StorageException("NameStorage for stubs serialization has been corrupted");
      }
    }


    @Override
    protected void removeTransientDataForInMemoryKeys(int inputId, @Nonnull Map<? extends Integer, ? extends SerializedStubTree> map) {
      super.removeTransientDataForInMemoryKeys(inputId, map);
      removeStubIndexKeys(inputId, getStubIndexMaps(map));
    }

    @Override
    public void removeTransientDataForKeys(int inputId, @Nonnull Collection<? extends Integer> keys) {
      Map<StubIndexKey, Map<Object, StubIdList>> maps;
      try {
        Map<Integer, SerializedStubTree> data = getIndexedFileData(inputId);
        maps = getStubIndexMaps(data);
      }
      catch (StorageException e) {
        throw new RuntimeException(e);
      }
      super.removeTransientDataForKeys(inputId, keys);
      removeStubIndexKeys(inputId, maps);
    }

    private static void removeStubIndexKeys(int inputId, @Nonnull Map<StubIndexKey, Map<Object, StubIdList>> indexedStubs) {
      final StubIndexImpl stubIndex = (StubIndexImpl)StubIndex.getInstance();
      for (StubIndexKey key : indexedStubs.keySet()) {
        stubIndex.removeTransientDataForFile(key, inputId, indexedStubs.get(key).keySet());
      }
    }

    @Nonnull
    private static Map<StubIndexKey, Map<Object, StubIdList>> getStubIndexMaps(@Nonnull Map<? extends Integer, ? extends SerializedStubTree> data) {
      if (data.isEmpty()) return Collections.emptyMap();
      SerializedStubTree tree = data.values().iterator().next();
      return tree == null ? Collections.emptyMap() : tree.getStubIndicesValueMap();
    }

    @Override
    protected void doClear() throws StorageException, IOException {
      final StubIndexImpl stubIndex = StubIndexImpl.getInstanceOrInvalidate();
      if (stubIndex != null) {
        stubIndex.clearAllIndices();
      }
      myStubVersionMap.clear();
      super.doClear();
    }

    @Override
    protected void doDispose() throws StorageException {
      try {
        super.doDispose();
      }
      finally {
        getStubIndex().dispose();
      }
    }

    @Nonnull
    @Override
    protected InputDataDiffBuilder<Integer, SerializedStubTree> getKeysDiffBuilderInMemoryMode(int inputId, @Nonnull Map<Integer, SerializedStubTree> keysAndValues) {
      return new StubCumulativeInputDiffBuilder(inputId, keysAndValues.isEmpty() ? null : keysAndValues.values().iterator().next());
    }

    @Override
    public void setIndexedStateForFile(int fileId, @Nonnull VirtualFile file) {
      super.setIndexedStateForFile(fileId, file);
      try {
        myStubVersionMap.persistIndexedState(fileId, file);
      }
      catch (IOException e) {
        LOG.error(e);
      }

    }

    @Override
    public boolean isIndexedStateForFile(int fileId, @Nonnull VirtualFile file) {
      boolean indexedStateForFile = super.isIndexedStateForFile(fileId, file);
      if (!indexedStateForFile) return false;

      try {
        return myStubVersionMap.isIndexed(fileId, file);
      }
      catch (IOException e) {
        LOG.error(e);
        return false;
      }
    }
  }
}
