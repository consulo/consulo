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
package consulo.component.store.impl.internal.scheme;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.WriteAction;
import consulo.component.persist.RoamingType;
import consulo.component.persist.scheme.*;
import consulo.component.store.impl.internal.storage.DirectoryStorageData;
import consulo.component.store.impl.internal.storage.StorageUtil;
import consulo.component.store.impl.internal.storage.vfs.VfsDirectoryBasedStorage;
import consulo.component.store.internal.StreamProvider;
import consulo.component.util.text.UniqueNameGenerator;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.Alerts;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.SmartList;
import consulo.util.io.FilePermissionCopier;
import consulo.util.io.FileUtil;
import consulo.util.io.URLUtil;
import consulo.util.jdom.JDOMUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.util.lang.function.ThrowableFunction;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.VirtualFileEvent;
import consulo.virtualFileSystem.event.VirtualFileListener;
import consulo.virtualFileSystem.internal.VirtualFileTracker;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Parent;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.*;

public class SchemeManagerImpl<T, E extends ExternalizableScheme> extends AbstractSchemeManager<T, E> {
  private static final Logger LOG = Logger.getInstance(SchemeManagerImpl.class);

  private final String myFileSpec;
  private final SchemeProcessor<T, E> myProcessor;
  private final RoamingType myRoamingType;

  private final StreamProvider myProvider;
  private final File myIoDir;
  private VirtualFile myDir;

  private String mySchemeExtension = DirectoryStorageData.DEFAULT_EXT;
  private boolean myUpdateExtension;

  private final Set<String> myFilesToDelete = new HashSet<String>();

  public SchemeManagerImpl(@Nonnull String fileSpec,
                           @Nonnull VirtualFileTracker virtualFileTracker,
                           @Nonnull SchemeProcessor<T, E> processor,
                           @Nonnull RoamingType roamingType,
                           @Nullable StreamProvider provider,
                           @Nonnull File baseDir) {
    myFileSpec = fileSpec;
    myProcessor = processor;
    myRoamingType = roamingType;
    myProvider = provider;
    myIoDir = baseDir;
    if (processor instanceof SchemeExtensionProvider) {
      mySchemeExtension = ((SchemeExtensionProvider)processor).getSchemeExtension();
      myUpdateExtension = ((SchemeExtensionProvider)processor).isUpgradeNeeded();
    }

    final String baseDirPath = myIoDir.getAbsolutePath().replace(File.separatorChar, '/');
    virtualFileTracker.addTracker(LocalFileSystem.PROTOCOL_PREFIX + baseDirPath, new VirtualFileListener() {
      @Override
      public void contentsChanged(@Nonnull VirtualFileEvent event) {
        if (event.getRequestor() != null || !isMy(event)) {
          return;
        }

        E scheme = findSchemeFor(event.getFile().getName());
        T oldCurrentScheme = null;
        if (scheme != null) {
          oldCurrentScheme = getCurrentScheme();
          //noinspection unchecked
          removeScheme((T)scheme);
          myProcessor.onSchemeDeleted(scheme);
        }

        E readScheme = readSchemeFromFile(event.getFile(), true, false);
        if (readScheme != null) {
          myProcessor.initScheme(readScheme);
          myProcessor.onSchemeAdded(readScheme);

          T newCurrentScheme = getCurrentScheme();
          if (oldCurrentScheme != null && newCurrentScheme == null) {
            setCurrentSchemeName(readScheme.getName());
            newCurrentScheme = getCurrentScheme();
          }

          if (oldCurrentScheme != newCurrentScheme) {
            myProcessor.onCurrentSchemeChanged((E)oldCurrentScheme);
          }
        }
      }

      @Override
      public void fileCreated(@Nonnull VirtualFileEvent event) {
        if (event.getRequestor() == null && isMy(event)) {
          E readScheme = readSchemeFromFile(event.getFile(), true, false);
          if (readScheme != null) {
            myProcessor.initScheme(readScheme);
            myProcessor.onSchemeAdded(readScheme);
          }
        }
      }

      @Override
      public void fileDeleted(@Nonnull VirtualFileEvent event) {
        if (event.getRequestor() == null && isMy(event)) {
          E scheme = findSchemeFor(event.getFile().getName());
          T oldCurrentScheme = null;
          if (scheme != null) {
            oldCurrentScheme = getCurrentScheme();
            //noinspection unchecked
            removeScheme((T)scheme);
            myProcessor.onSchemeDeleted(scheme);
          }

          T newCurrentScheme = getCurrentScheme();
          if (oldCurrentScheme != null && newCurrentScheme == null) {
            if (!mySchemes.isEmpty()) {
              setCurrentSchemeName(myProcessor.getName(mySchemes.get(0)));
              newCurrentScheme = getCurrentScheme();
            }
          }

          if (oldCurrentScheme != newCurrentScheme) {
            myProcessor.onCurrentSchemeChanged((E)oldCurrentScheme);
          }
        }
      }
    }, false, ApplicationManager.getApplication());
  }

  @Nonnull
  @Override
  protected String getName(T value) {
    return myProcessor.getName(value);
  }

  @Override
  public void loadBundledScheme(@Nonnull URL url, @Nonnull ThrowableFunction<Element, T, Throwable> convertor) {
    try {
      addNewScheme(convertor.apply(JDOMUtil.load(URLUtil.openStream(url))), false);
    }
    catch (Throwable e) {
      LOG.error("Cannot read scheme from " + url, e);
    }
  }

  private boolean isMy(@Nonnull VirtualFileEvent event) {
    return StringUtil.endsWithIgnoreCase(event.getFile().getNameSequence(), mySchemeExtension);
  }

  @Override
  @Nonnull
  public Collection<E> loadSchemes() {
    Map<String, E> result = new LinkedHashMap<String, E>();
    if (myProvider != null && myProvider.isEnabled()) {
      readSchemesFromProviders(result);
    }
    else {
      File[] files = myIoDir.listFiles();
      if (files != null) {
        for (File file : files) {
          E scheme = readSchemeFromFile(file, false, true);
          if (scheme != null) {
            result.put(scheme.getName(), scheme);
          }
        }
      }
    }

    Collection<E> list = result.values();
    for (E scheme : list) {
      myProcessor.initScheme(scheme);
      checkCurrentScheme((T)scheme);
    }
    return list;
  }

  private E findSchemeFor(@Nonnull String ioFileName) {
    for (T scheme : mySchemes) {
      if (scheme instanceof ExternalizableScheme) {
        if (ioFileName.equals(((ExternalizableScheme)scheme).getExternalInfo().getCurrentFileName() + mySchemeExtension)) {
          //noinspection CastConflictsWithInstanceof,unchecked
          return (E)scheme;
        }
      }
    }
    return null;
  }

  @Nullable
  private static Element loadElementOrNull(@Nullable InputStream stream) {
    try {
      return JDOMUtil.load(stream);
    }
    catch (JDOMException e) {
      LOG.warn(e);
      return null;
    }
    catch (IOException e) {
      LOG.warn(e);
      return null;
    }
  }

  private void readSchemesFromProviders(@Nonnull Map<String, E> result) {
    assert myProvider != null;
    for (String subPath : myProvider.listSubFiles(myFileSpec, myRoamingType)) {
      try {
        Element element = loadElementOrNull(myProvider.loadContent(getFileFullPath(subPath), myRoamingType));
        if (element == null) {
          return;
        }

        E scheme = readScheme(element, true);
        boolean fileRenamed = false;
        assert scheme != null;
        T existing = findSchemeByName(scheme.getName());
        if (existing instanceof ExternalizableScheme) {
          String currentFileName = ((ExternalizableScheme)existing).getExternalInfo().getCurrentFileName();
          if (currentFileName != null && !currentFileName.equals(subPath)) {
            deleteServerFile(subPath);
            subPath = currentFileName;
            fileRenamed = true;
          }
        }
        String fileName = checkFileNameIsFree(subPath, scheme.getName());
        if (!fileRenamed && !fileName.equals(subPath)) {
          deleteServerFile(subPath);
        }

        loadScheme(scheme, false, fileName);
        scheme.getExternalInfo().markRemote();
        result.put(scheme.getName(), scheme);
      }
      catch (Exception e) {
        LOG.info("Cannot load data from stream provider: " + e.getMessage());
      }
    }
  }

  @Nonnull
  private String checkFileNameIsFree(@Nonnull String subPath, @Nonnull String schemeName) {
    for (T scheme : mySchemes) {
      if (scheme instanceof ExternalizableScheme) {
        String name = ((ExternalizableScheme)scheme).getExternalInfo().getCurrentFileName();
        if (name != null &&
            !schemeName.equals(myProcessor.getName(scheme)) &&
            subPath.length() == (name.length() + mySchemeExtension.length()) &&
            subPath.startsWith(name) &&
            subPath.endsWith(mySchemeExtension)) {
          return UniqueNameGenerator.generateUniqueName(FileUtil.sanitizeName(schemeName), collectAllFileNames());
        }
      }
    }
    return subPath;
  }

  @Nonnull
  private Collection<String> collectAllFileNames() {
    Set<String> result = new HashSet<String>();
    for (T scheme : mySchemes) {
      if (scheme instanceof ExternalizableScheme) {
        ExternalInfo externalInfo = ((ExternalizableScheme)scheme).getExternalInfo();
        if (externalInfo.getCurrentFileName() != null) {
          result.add(externalInfo.getCurrentFileName());
        }
      }
    }
    return result;
  }

  private void loadScheme(@Nonnull E scheme, boolean forceAdd, @Nonnull CharSequence fileName) {
    String fileNameWithoutExtension = createFileName(fileName);
    if (!forceAdd && myFilesToDelete.contains(fileNameWithoutExtension)) {
      return;
    }

    T existing = findSchemeByName(scheme.getName());
    if (existing != null) {
      if (!Comparing.equal(existing.getClass(), scheme.getClass())) {
        LOG.warn("'" + scheme.getName() + "' " + existing.getClass().getSimpleName() + " replaced with " + scheme.getClass().getSimpleName());
      }

      mySchemes.remove(existing);
      if (existing instanceof ExternalizableScheme) {
        //noinspection unchecked,CastConflictsWithInstanceof
        myProcessor.onSchemeDeleted((E)existing);
      }
    }

    //noinspection unchecked
    addNewScheme((T)scheme, true);
    scheme.getExternalInfo().setPreviouslySavedName(scheme.getName());
    scheme.getExternalInfo().setCurrentFileName(fileNameWithoutExtension);
  }

  private boolean canRead(@Nonnull File file) {
    if (file.isDirectory()) {
      return false;
    }

    // migrate from custom extension to default
    if (myUpdateExtension && StringUtil.endsWithIgnoreCase(file.getName(), mySchemeExtension)) {
      return true;
    }
    else {
      return StringUtil.endsWithIgnoreCase(file.getName(), DirectoryStorageData.DEFAULT_EXT);
    }
  }

  @Nullable
  private E readSchemeFromFile(@Nonnull VirtualFile file, boolean forceAdd, boolean duringLoad) {
    return readSchemeFromFile(VirtualFileUtil.virtualToIoFile(file), forceAdd, duringLoad);
  }

  @Nullable
  private E readSchemeFromFile(@Nonnull final File file, boolean forceAdd, boolean duringLoad) {
    if (!canRead(file)) {
      return null;
    }

    try {
      Element element;
      try {
        element = JDOMUtil.load(file);
      }
      catch (JDOMException e) {
        try {
          FileUtil.copy(file, new File(myIoDir, file.getName() + ".copy"), FilePermissionCopier.BY_NIO2);
        }
        catch (IOException e1) {
          LOG.error(e1);
        }
        LOG.error("Error reading file " + file.getPath() + ": " + e.getMessage());
        return null;
      }

      E scheme = readScheme(element, duringLoad);
      if (scheme != null) {
        loadScheme(scheme, forceAdd, file.getName());
      }
      return scheme;
    }
    catch (final Exception e) {
      ApplicationManager.getApplication().invokeLater(new Runnable() {
        @Override
        public void run() {
          String msg = "Cannot read scheme " + file.getName() + "  from '" + myFileSpec + "': " + e.getMessage();
          LOG.info(msg, e);
          Alerts.okError(LocalizeValue.localizeTODO(msg)).title(LocalizeValue.localizeTODO("Load Settings")).showAsync();
        }
      });
      return null;
    }
  }

  @Nullable
  private E readScheme(@Nonnull Element element, boolean duringLoad) throws InvalidDataException, IOException, JDOMException {
    E scheme;
    if (myProcessor instanceof BaseSchemeProcessor) {
      scheme = ((BaseSchemeProcessor<T, E>)myProcessor).readScheme(element, duringLoad);
    }
    else {
      //noinspection deprecation
      scheme = myProcessor.readScheme(new Document(element.detach()));
    }
    if (scheme != null) {
      scheme.getExternalInfo().setHash(JDOMUtil.getTreeHash(element, true));
    }
    return scheme;
  }

  @Nonnull
  private String createFileName(@Nonnull CharSequence fileName) {
    if (StringUtil.endsWithIgnoreCase(fileName, mySchemeExtension)) {
      fileName = fileName.subSequence(0, fileName.length() - mySchemeExtension.length());
    }
    else if (StringUtil.endsWithIgnoreCase(fileName, DirectoryStorageData.DEFAULT_EXT)) {
      fileName = fileName.subSequence(0, fileName.length() - DirectoryStorageData.DEFAULT_EXT.length());
    }
    return fileName.toString();
  }

  public void updateConfigFilesFromStreamProviders() {
    // todo
  }

  private String getFileFullPath(@Nonnull String subPath) {
    return myFileSpec + '/' + subPath;
  }

  @Override
  public void save() {
    boolean hasSchemes = false;
    UniqueNameGenerator nameGenerator = new UniqueNameGenerator();
    List<E> schemesToSave = new SmartList<E>();
    for (T scheme : mySchemes) {
      if (scheme instanceof ExternalizableScheme) {
        //noinspection CastConflictsWithInstanceof,unchecked
        E eScheme = (E)scheme;
        BaseSchemeProcessor.State state;
        if (myProcessor instanceof BaseSchemeProcessor) {
          state = ((BaseSchemeProcessor<T, E>)myProcessor).getState(eScheme);
        }
        else {
          //noinspection deprecation
          state = myProcessor.shouldBeSaved(eScheme) ? BaseSchemeProcessor.State.POSSIBLY_CHANGED : BaseSchemeProcessor.State.NON_PERSISTENT;
        }

        if (state == BaseSchemeProcessor.State.NON_PERSISTENT) {
          continue;
        }

        hasSchemes = true;

        if (state != BaseSchemeProcessor.State.UNCHANGED) {
          schemesToSave.add(eScheme);
        }

        String fileName = eScheme.getExternalInfo().getCurrentFileName();
        if (fileName != null && !isRenamed(eScheme)) {
          nameGenerator.addExistingName(fileName);
        }
      }
    }

    if (!hasSchemes) {
      myFilesToDelete.clear();
      if (myIoDir.exists()) {
        FileUtil.delete(myIoDir);
      }
      return;
    }

    for (final E scheme : schemesToSave) {
      try {
        saveScheme(scheme, nameGenerator);
      }
      catch (final Exception e) {
        LOG.error("Cannot write scheme " + scheme.getName() + " in '" + myFileSpec + "': " + e.getLocalizedMessage(), e);

        Application app = Application.get();
        app.invokeLater(
                () -> Alerts.okError(LocalizeValue.localizeTODO("Cannot save scheme '" + scheme.getName() + ": " + e.getMessage())).title(LocalizeValue.localizeTODO("Save Settings")).showAsync());

      }
    }

    deleteFiles();
  }

  private void saveScheme(@Nonnull E scheme, @Nonnull UniqueNameGenerator nameGenerator) throws WriteExternalException, IOException {
    ExternalInfo externalInfo = scheme.getExternalInfo();
    String currentFileNameWithoutExtension = externalInfo.getCurrentFileName();
    Parent parent = myProcessor.writeScheme(scheme);
    Element element = parent == null || parent instanceof Element ? (Element)parent : ((Document)parent).detachRootElement();
    if (JDOMUtil.isEmpty(element)) {
      ContainerUtil.addIfNotNull(myFilesToDelete, currentFileNameWithoutExtension);
      return;
    }

    String fileNameWithoutExtension = currentFileNameWithoutExtension;
    if (fileNameWithoutExtension == null || isRenamed(scheme)) {
      fileNameWithoutExtension = nameGenerator.generateUniqueName(FileUtil.sanitizeName(scheme.getName()));
    }
    String fileName = fileNameWithoutExtension + mySchemeExtension;

    int newHash = JDOMUtil.getTreeHash(element, true);
    if (currentFileNameWithoutExtension == fileNameWithoutExtension && newHash == externalInfo.getHash()) {
      return;
    }

    // file will be overwritten, so, we don't need to delete it
    myFilesToDelete.remove(fileNameWithoutExtension);

    // stream provider always use LF separator
    final byte[] byteOut = StorageUtil.writeToBytes(element);

    // if another new scheme uses old name of this scheme, so, we must not delete it (as part of rename operation)
    boolean renamed = currentFileNameWithoutExtension != null && fileNameWithoutExtension != currentFileNameWithoutExtension && nameGenerator.test(currentFileNameWithoutExtension);
    if (!externalInfo.isRemote()) {
      VirtualFile file = null;
      if (renamed) {
        file = myDir.findChild(currentFileNameWithoutExtension + mySchemeExtension);
        if (file != null) {
          final VirtualFile finalFile = file;
          WriteAction.run(() -> finalFile.rename(this, fileName));
        }
      }

      if (file == null) {
        if (myDir == null || !myDir.isValid()) {
          myDir = VfsDirectoryBasedStorage.createDir(myIoDir, this);
        }
        file = VfsDirectoryBasedStorage.getFile(fileName, myDir, this);
      }

      final VirtualFile finalFile1 = file;
      WriteAction.run(() -> {
        try (OutputStream out = finalFile1.getOutputStream(this)) {
          out.write(byteOut);
        }
      });
    }
    else if (renamed) {
      myFilesToDelete.add(currentFileNameWithoutExtension);
    }

    externalInfo.setHash(newHash);
    externalInfo.setPreviouslySavedName(scheme.getName());
    externalInfo.setCurrentFileName(createFileName(fileName));

    if (myProvider != null && myProvider.isEnabled()) {
      String fileSpec = getFileFullPath(fileName);
      if (myProvider.isApplicable(fileSpec, myRoamingType)) {
        myProvider.saveContent(fileSpec, byteOut, myRoamingType);
      }
    }
  }

  private static boolean isRenamed(@Nonnull ExternalizableScheme scheme) {
    return !scheme.getName().equals(scheme.getExternalInfo().getPreviouslySavedName());
  }

  private void deleteFiles() {
    if (myFilesToDelete.isEmpty()) {
      return;
    }

    if (myProvider != null && myProvider.isEnabled()) {
      for (String nameWithoutExtension : myFilesToDelete) {
        deleteServerFile(nameWithoutExtension + mySchemeExtension);
        if (!DirectoryStorageData.DEFAULT_EXT.equals(mySchemeExtension)) {
          deleteServerFile(nameWithoutExtension + DirectoryStorageData.DEFAULT_EXT);
        }
      }
    }

    VirtualFile dir = getVirtualDir();
    if (dir != null) {
      WriteAction.run(() -> {
        for (VirtualFile file : dir.getChildren()) {
          if (myFilesToDelete.contains(file.getNameWithoutExtension())) {
            VfsDirectoryBasedStorage.deleteFile(file, this);
          }
        }
        myFilesToDelete.clear();
      });
    }
  }

  @Nullable
  private VirtualFile getVirtualDir() {
    VirtualFile virtualFile = myDir;
    if (virtualFile == null) {
      myDir = virtualFile = LocalFileSystem.getInstance().findFileByIoFile(myIoDir);
    }
    return virtualFile;
  }

  @Override
  public File getRootDirectory() {
    return myIoDir;
  }

  private void deleteServerFile(@Nonnull String path) {
    if (myProvider != null && myProvider.isEnabled()) {
      StorageUtil.delete(myProvider, getFileFullPath(path), myRoamingType);
    }
  }

  @Override
  protected void schemeDeleted(@Nonnull T scheme) {
    super.schemeDeleted(scheme);

    if (scheme instanceof ExternalizableScheme) {
      ContainerUtil.addIfNotNull(myFilesToDelete, ((ExternalizableScheme)scheme).getExternalInfo().getCurrentFileName());
    }
  }

  @Override
  protected void schemeAdded(@Nonnull T scheme) {
    if (!(scheme instanceof ExternalizableScheme)) {
      return;
    }

    ExternalInfo externalInfo = ((ExternalizableScheme)scheme).getExternalInfo();
    String fileName = externalInfo.getCurrentFileName();
    if (fileName != null) {
      myFilesToDelete.remove(fileName);
    }
    if (myProvider != null && myProvider.isEnabled()) {
      // do not save locally
      externalInfo.markRemote();
    }
  }
}
