/*
 * Copyright 2013-2018 consulo.io
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
package consulo.ide.impl.fileEditor;

import consulo.application.concurrent.ApplicationConcurrency;
import consulo.disposer.Disposer;
import consulo.fileEditor.FileEditorWindow;
import consulo.fileEditor.FileEditorWithProviderComposite;
import consulo.fileEditor.impl.internal.FileEditorOpenOptions;
import consulo.fileEditor.impl.internal.AsyncConfigTreeReader;
import consulo.fileEditor.impl.internal.FileEditorHistoryUtil;
import consulo.fileEditor.impl.internal.FileEditorManagerImpl;
import consulo.fileEditor.impl.internal.FileEditorsSplittersBase;
import consulo.fileEditor.impl.internal.HistoryEntry;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.ui.docking.impl.UnifiedDockableEditorTabbedContainer;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.wm.dock.DockManager;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.WrappedLayout;
import org.jdom.Element;

import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2018-05-09
 */
public class UnifiedFileEditorsSplitters extends FileEditorsSplittersBase<UnifiedFileEditorWindow> {
  private static final Logger LOG = Logger.getInstance(UnifiedFileEditorsSplitters.class);

  private final Project myProject;

  private WrappedLayout myLayout;

  public UnifiedFileEditorsSplitters(ApplicationConcurrency applicationConcurrency,
                                     Project project,
                                     FileEditorManagerImpl editorManager,
                                     DockManager dockManager,
                                     boolean createOwnDockableContainer) {
    super(applicationConcurrency, project, editorManager);
    myProject = project;

    myLayout = WrappedLayout.create();

    if (createOwnDockableContainer) {
      UnifiedDockableEditorTabbedContainer dockable = new UnifiedDockableEditorTabbedContainer(myManager.getProject(), this, false);
      Disposer.register(editorManager.getProject(), dockable);
      dockManager.register(dockable);
    }
  }

  
  @Override
  protected UnifiedFileEditorWindow[] createArray(int size) {
    return new UnifiedFileEditorWindow[size];
  }

  
  @Override
  public Component getUIComponent() {
    return myLayout;
  }

  @Override
  public void writeExternal(Element element) {
    UnifiedFileEditorWindow window = myCurrentWindow;
    if (window == null) {
      return;
    }

    Element leaf = new Element("leaf");
    writeWindow(leaf, window);
    element.addContent(leaf);
  }

  private void writeWindow(Element res, UnifiedFileEditorWindow window) {
    FileEditorWithProviderComposite[] composites = window.getEditors();
    for (int i = 0; i < composites.length; i++) {
      VirtualFile file = window.getFileAt(i);
      res.addContent(writeComposite(file, composites[i], window.isFilePinned(file), window.getSelectedEditor()));
    }
  }

  private Element writeComposite(VirtualFile file,
                                 FileEditorWithProviderComposite composite,
                                 boolean pinned,
                                 FileEditorWithProviderComposite selectedEditor) {
    Element fileElement = new Element("file");
    fileElement.setAttribute("leaf-file-name", file.getName());
    FileEditorHistoryUtil.currentStateAsHistoryEntry(composite).writeExternal(fileElement, myProject);
    fileElement.setAttribute(AsyncConfigTreeReader.PINNED, Boolean.toString(pinned));
    fileElement.setAttribute(AsyncConfigTreeReader.CURRENT_IN_TAB, Boolean.toString(composite.equals(selectedEditor)));
    return fileElement;
  }

  @Override

  public CompletableFuture<?> openFilesAsync(UIAccess uiAccess) {
    if (mySplittersElement == null) {
      return CompletableFuture.completedFuture(null);
    }

    return myUIBuilder.process(mySplittersElement, null, uiAccess)
      .whenCompleteAsync((window, throwable) -> mySplittersElement = null, uiAccess);
  }

  private final AsyncConfigTreeReader<UnifiedFileEditorWindow> myUIBuilder = new UIBuilder();

  private class UIBuilder extends AsyncConfigTreeReader<UnifiedFileEditorWindow> {
    @Override
    protected CompletableFuture<UnifiedFileEditorWindow> processFiles(java.util.List<Element> fileElements,
                                                                      UnifiedFileEditorWindow context,
                                                                      Element parent,
                                                                      UIAccess uiAccess) {
      return uiAccess.giveAsync(() -> {
        if (myCurrentWindow == null) {
          createCurrentWindow();
        }
        return myCurrentWindow;
      }).thenApplyAsync(window -> processFilesImpl(fileElements, uiAccess, window));
    }

    /**
     * A split written by the awt frontend has no counterpart here - this one holds a single window - so both
     * sides are read into it. Dropping a side would take its files out of the state on the next write.
     */
    @Override
    protected CompletableFuture<UnifiedFileEditorWindow> processSplitter(Element element,
                                                                         Element firstChild,
                                                                         Element secondChild,
                                                                         UnifiedFileEditorWindow context,
                                                                         UIAccess uiAccess) {
      return process(firstChild, context, uiAccess)
        .thenCompose(window -> process(secondChild, window, uiAccess));
    }

    private UnifiedFileEditorWindow processFilesImpl(java.util.List<Element> fileElements,
                                                     UIAccess uiAccess,
                                                     UnifiedFileEditorWindow window) {
      VirtualFile focusedFile = null;

      for (int i = 0; i < fileElements.size(); i++) {
        Element file = fileElements.get(i);
        Element historyElement = file.getChild(HistoryEntry.TAG);

        try {
          HistoryEntry entry = HistoryEntry.createLight(myManager.getProject(), historyElement);
          VirtualFile virtualFile = entry.getFile();
          if (virtualFile == null) {
            throw new InvalidDataException("No file exists: " + entry.getFilePointer().getUrl());
          }

          FileEditorOpenOptions openOptions = new FileEditorOpenOptions()
            .withPin(Boolean.valueOf(file.getAttributeValue(PINNED)))
            .withIndex(i)
            .withReopeningEditorsOnStartup();

          myManager.openFileImpl4(uiAccess, window, virtualFile, entry, openOptions);

          if (Boolean.valueOf(file.getAttributeValue(CURRENT_IN_TAB))) {
            focusedFile = virtualFile;
          }
        }
        catch (InvalidDataException e) {
          LOG.warn(e);
        }
        catch (Throwable e) {
          // one file that cannot be reopened must not take the rest of the tab set with it - the loop is the
          // only thing restoring them, and a throw here is swallowed by the future the walk hangs on
          LOG.error("Cannot reopen editor for " + file.getAttributeValue("leaf-file-name"), e);
        }
      }

      if (focusedFile != null) {
        myManager.addSelectionRecord(focusedFile, window);

        VirtualFile finalFocusedFile = focusedFile;
        uiAccess.execute(() -> {
          FileEditorWithProviderComposite editor = window.findFileComposite(finalFocusedFile);
          if (editor != null) {
            window.setEditor(editor, true, true);
          }
        });
      }

      return window;
    }
  }

  @Override
  public int getSplitCount() {
    return 0;
  }

  @Override
  public void startListeningFocus() {

  }

  @Override
  public void clear() {
    for (UnifiedFileEditorWindow window : myWindows) {
      window.dispose();
    }
    //todo myComponent.removeAll();
    myWindows.clear();
    setCurrentWindow(null);
    //todo myComponent.repaint(); // revalidate doesn't repaint correctly after "Close All"
  }

  @RequiredUIAccess
  @Override
  protected void createCurrentWindow() {
    LOG.assertTrue(myCurrentWindow == null);
    setCurrentWindow(new UnifiedFileEditorWindow(myProject, myManager, this));
    myLayout.set(myCurrentWindow.getUIComponent());
  }

  @Override
  public FileEditorWindow[] getOrderedWindows() {
    if (myCurrentWindow != null) {
      return new FileEditorWindow[]{myCurrentWindow};
    }
    return FileEditorWindow.EMPTY_ARRAY;
  }

  @Override
  public boolean isShowing() {
    return true;
  }
}
