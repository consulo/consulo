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
package com.intellij.openapi.options;

import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ex.DecodeDefaultsUtil;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.impl.stores.DirectoryBasedStorage;
import com.intellij.openapi.components.impl.stores.DirectoryStorageData;
import com.intellij.openapi.components.impl.stores.StorageUtil;
import com.intellij.openapi.components.impl.stores.StreamProvider;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.AbstractExtensionPointBean;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.io.BufferExposingByteArrayOutputStream;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtilRt;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.vfs.tracker.VirtualFileTracker;
import com.intellij.util.SmartList;
import com.intellij.util.ThrowableConvertor;
import com.intellij.util.containers.ContainerUtilRt;
import com.intellij.util.io.URLUtil;
import com.intellij.util.text.UniqueNameGenerator;
import gnu.trove.THashSet;
import consulo.util.pointers.Named;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Parent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.*;

public class SchemesManagerImpl<T extends Named, E extends ExternalizableScheme> extends AbstractSchemesManager<T, E> {
  private static final Logger LOG = Logger.getInstance(SchemesManagerImpl.class);

  private final String myFileSpec;
  private final SchemeProcessor<E> myProcessor;
  private final RoamingType myRoamingType;

  private final StreamProvider myProvider;
  private final File myIoDir;
  private VirtualFile myDir;

  private String mySchemeExtension = DirectoryStorageData.DEFAULT_EXT;
  private boolean myUpdateExtension;

  private final Set<String> myFilesToDelete = new THashSet<String>();

  public SchemesManagerImpl(@NotNull String fileSpec,
                            @NotNull SchemeProcessor<E> processor,
                            @NotNull RoamingType roamingType,
                            @Nullable StreamProvider provider,
                            @NotNull File baseDir) {
    myFileSpec = fileSpec;
    myProcessor = processor;
    myRoamingType = roamingType;
    myProvider = provider;
    myIoDir = baseDir;
    if (processor instanceof SchemeExtensionProvider) {
      mySchemeExtension = ((SchemeExtensionProvider)processor).getSchemeExtension();
      myUpdateExtension = ((SchemeExtensionProvider)processor).isUpgradeNeeded();
    }

    VirtualFileTracker virtualFileTracker = ServiceManager.getService(VirtualFileTracker.class);
    if (virtualFileTracker != null) {
      final String baseDirPath = myIoDir.getAbsolutePath().replace(File.separatorChar, '/');
      virtualFileTracker.addTracker(LocalFileSystem.PROTOCOL_PREFIX + baseDirPath, new VirtualFileAdapter() {
        @Override
        public void contentsChanged(@NotNull VirtualFileEvent event) {
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
        public void fileCreated(@NotNull VirtualFileEvent event) {
          if (event.getRequestor() == null && isMy(event)) {
            E readScheme = readSchemeFromFile(event.getFile(), true, false);
            if (readScheme != null) {
              myProcessor.initScheme(readScheme);
              myProcessor.onSchemeAdded(readScheme);
            }
          }
        }

        @Override
        public void fileDeleted(@NotNull VirtualFileEvent event) {
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
                setCurrentSchemeName(mySchemes.get(0).getName());
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
  }

  @Override
  public void loadBundledScheme(@NotNull String resourceName, @NotNull Object requestor, @NotNull ThrowableConvertor<Element, T, Throwable> convertor) {
    try {
      URL url = requestor instanceof AbstractExtensionPointBean
                ? (((AbstractExtensionPointBean)requestor).getLoaderForClass().getResource(resourceName))
                : DecodeDefaultsUtil.getDefaults(requestor, resourceName);
      if (url == null) {
        // Error shouldn't occur during this operation thus we report error instead of info
        LOG.error("Cannot read scheme from " + resourceName);
        return;
      }
      addNewScheme(convertor.convert(JDOMUtil.load(URLUtil.openStream(url))), false);
    }
    catch (Throwable e) {
      LOG.error("Cannot read scheme from " + resourceName, e);
    }
  }

  private boolean isMy(@NotNull VirtualFileEvent event) {
    return StringUtilRt.endsWithIgnoreCase(event.getFile().getNameSequence(), mySchemeExtension);
  }

  @Override
  @NotNull
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
      checkCurrentScheme(scheme);
    }
    return list;
  }

  private E findSchemeFor(@NotNull String ioFileName) {
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

  private void readSchemesFromProviders(@NotNull Map<String, E> result) {
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

  @NotNull
  private String checkFileNameIsFree(@NotNull String subPath, @NotNull String schemeName) {
    for (Named scheme : mySchemes) {
      if (scheme instanceof ExternalizableScheme) {
        String name = ((ExternalizableScheme)scheme).getExternalInfo().getCurrentFileName();
        if (name != null &&
            !schemeName.equals(scheme.getName()) &&
            subPath.length() == (name.length() + mySchemeExtension.length()) &&
            subPath.startsWith(name) &&
            subPath.endsWith(mySchemeExtension)) {
          return UniqueNameGenerator.generateUniqueName(FileUtil.sanitizeName(schemeName), collectAllFileNames());
        }
      }
    }
    return subPath;
  }

  @NotNull
  private Collection<String> collectAllFileNames() {
    Set<String> result = new THashSet<String>();
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

  private void loadScheme(@NotNull E scheme, boolean forceAdd, @NotNull CharSequence fileName) {
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

  private boolean canRead(@NotNull File file) {
    if (file.isDirectory()) {
      return false;
    }

    // migrate from custom extension to default
    if (myUpdateExtension && StringUtilRt.endsWithIgnoreCase(file.getName(), mySchemeExtension)) {
      return true;
    }
    else {
      return StringUtilRt.endsWithIgnoreCase(file.getName(), DirectoryStorageData.DEFAULT_EXT);
    }
  }

  @Nullable
  private E readSchemeFromFile(@NotNull VirtualFile file, boolean forceAdd, boolean duringLoad) {
    return readSchemeFromFile(VfsUtil.virtualToIoFile(file), forceAdd, duringLoad);
  }

  @Nullable
  private E readSchemeFromFile(@NotNull final File file, boolean forceAdd, boolean duringLoad) {
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
          FileUtil.copy(file, new File(myIoDir, file.getName() + ".copy"));
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
          Messages.showErrorDialog(msg, "Load Settings");
        }
      }
      );
      return null;
    }
  }

  @Nullable
  private E readScheme(@NotNull Element element, boolean duringLoad) throws InvalidDataException, IOException, JDOMException {
    E scheme;
    if (myProcessor instanceof BaseSchemeProcessor) {
      scheme = ((BaseSchemeProcessor<E>)myProcessor).readScheme(element, duringLoad);
    }
    else {
      //noinspection deprecation
      scheme = myProcessor.readScheme(new Document((Element)element.detach()));
    }
    if (scheme != null) {
      scheme.getExternalInfo().setHash(JDOMUtil.getTreeHash(element, true));
    }
    return scheme;
  }

  @NotNull
  private String createFileName(@NotNull CharSequence fileName) {
    if (StringUtilRt.endsWithIgnoreCase(fileName, mySchemeExtension)) {
      fileName = fileName.subSequence(0, fileName.length() - mySchemeExtension.length());
    }
    else if (StringUtilRt.endsWithIgnoreCase(fileName, DirectoryStorageData.DEFAULT_EXT)) {
      fileName = fileName.subSequence(0, fileName.length() - DirectoryStorageData.DEFAULT_EXT.length());
    }
    return fileName.toString();
  }

  public void updateConfigFilesFromStreamProviders() {
    // todo
  }

  private String getFileFullPath(@NotNull String subPath) {
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
          state = ((BaseSchemeProcessor<E>)myProcessor).getState(eScheme);
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
        Application app = ApplicationManager.getApplication();
        if (app.isUnitTestMode() || app.isCommandLine()) {
          LOG.error("Cannot write scheme " + scheme.getName() + " in '" + myFileSpec + "': " + e.getLocalizedMessage(), e);
        }
        else {
          app.invokeLater(new Runnable() {
            @Override
            public void run() {
              Messages.showErrorDialog("Cannot save scheme '" + scheme.getName() + ": " + e.getMessage(), "Save Settings");
            }
          });
        }
      }
    }

    deleteFiles();
  }

  private void saveScheme(@NotNull E scheme, @NotNull UniqueNameGenerator nameGenerator) throws WriteExternalException, IOException {
    ExternalInfo externalInfo = scheme.getExternalInfo();
    String currentFileNameWithoutExtension = externalInfo.getCurrentFileName();
    Parent parent = myProcessor.writeScheme(scheme);
    Element element = parent == null || parent instanceof Element ? (Element)parent : ((Document)parent).detachRootElement();
    if (JDOMUtil.isEmpty(element)) {
      ContainerUtilRt.addIfNotNull(myFilesToDelete, currentFileNameWithoutExtension);
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
    final BufferExposingByteArrayOutputStream byteOut = StorageUtil.writeToBytes(element, "\n");

    // if another new scheme uses old name of this scheme, so, we must not delete it (as part of rename operation)
    boolean renamed = currentFileNameWithoutExtension != null && fileNameWithoutExtension != currentFileNameWithoutExtension && nameGenerator.value(currentFileNameWithoutExtension);
    if (!externalInfo.isRemote()) {
      VirtualFile file = null;
      if (renamed) {
        file = myDir.findChild(currentFileNameWithoutExtension + mySchemeExtension);
        if (file != null) {
          AccessToken token = ApplicationManager.getApplication().acquireWriteActionLock(SchemesManagerImpl.class);
          try {
            file.rename(this, fileName);
          }
          finally {
            token.finish();
          }
        }
      }

      if (file == null) {
        if (myDir == null || !myDir.isValid()) {
          myDir = DirectoryBasedStorage.createDir(myIoDir, this);
        }
        file = DirectoryBasedStorage.getFile(fileName, myDir, this);
      }

      AccessToken token = ApplicationManager.getApplication().acquireWriteActionLock(SchemesManagerImpl.class);
      try {
        OutputStream out = file.getOutputStream(this);
        try {
          byteOut.writeTo(out);
        }
        finally {
          out.close();
        }
      }
      finally {
        token.finish();
      }
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
        myProvider.saveContent(fileSpec, byteOut.getInternalBuffer(), byteOut.size(), myRoamingType);
      }
    }
  }

  private static boolean isRenamed(@NotNull ExternalizableScheme scheme) {
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
      AccessToken token = ApplicationManager.getApplication().acquireWriteActionLock(SchemesManagerImpl.class);
      try {
        for (VirtualFile file : dir.getChildren()) {
          if (myFilesToDelete.contains(file.getNameWithoutExtension())) {
            DirectoryBasedStorage.deleteFile(file, this);
          }
        }
        myFilesToDelete.clear();
      }
      finally {
        token.finish();
      }
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

  private void deleteServerFile(@NotNull String path) {
    if (myProvider != null && myProvider.isEnabled()) {
      StorageUtil.delete(myProvider, getFileFullPath(path), myRoamingType);
    }
  }

  @Override
  protected void schemeDeleted(@NotNull Named scheme) {
    super.schemeDeleted(scheme);

    if (scheme instanceof ExternalizableScheme) {
      ContainerUtilRt.addIfNotNull(myFilesToDelete, ((ExternalizableScheme)scheme).getExternalInfo().getCurrentFileName());
    }
  }

  @Override
  protected void schemeAdded(@NotNull T scheme) {
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
