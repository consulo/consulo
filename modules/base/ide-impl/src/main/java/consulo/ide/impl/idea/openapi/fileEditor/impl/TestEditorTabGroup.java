package consulo.ide.impl.idea.openapi.fileEditor.impl;

import consulo.application.ApplicationManager;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorProvider;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nullable;
import java.util.LinkedHashMap;

public class TestEditorTabGroup {
  private String name;

  private final LinkedHashMap<VirtualFile, Pair<FileEditor, FileEditorProvider>> myOpenedTabs = new LinkedHashMap<>();
  private VirtualFile myOpenedfile;

  public TestEditorTabGroup(String name) {
    this.name = name;
  }

  public String Name() {
    return name;
  }

  public void openTab(VirtualFile virtualFile, FileEditor fileEditor, FileEditorProvider fileEditorProvider) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    myOpenedTabs.put(virtualFile, Pair.pair(fileEditor, fileEditorProvider));
    myOpenedfile = virtualFile;
  }

  @Nullable
  public Pair<FileEditor, FileEditorProvider> getOpenedEditor() {
    VirtualFile openedFile = getOpenedFile();
    if (openedFile == null) {
      return null;
    }

    return myOpenedTabs.get(openedFile);
  }

  @Nullable
  public VirtualFile getOpenedFile() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    return myOpenedfile;
  }

  public void closeTab(VirtualFile virtualFile) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    myOpenedfile = null;
    myOpenedTabs.remove(virtualFile);
  }

  @Nullable
  public Pair<FileEditor, FileEditorProvider> getEditorAndProvider(VirtualFile file) {
    return myOpenedTabs.get(file);
  }

  public boolean contains(VirtualFile file) {
    return myOpenedTabs.containsKey(file);
  }

  public int getTabCount() {
    return myOpenedTabs.size();
  }
}
