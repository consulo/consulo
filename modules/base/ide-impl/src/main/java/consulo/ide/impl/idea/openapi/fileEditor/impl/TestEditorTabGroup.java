package consulo.ide.impl.idea.openapi.fileEditor.impl;

import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorProvider;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
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

  @RequiredUIAccess
  public void openTab(VirtualFile virtualFile, FileEditor fileEditor, FileEditorProvider fileEditorProvider) {
    UIAccess.assertIsUIThread();

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
  @RequiredUIAccess
  public VirtualFile getOpenedFile() {
    UIAccess.assertIsUIThread();
    return myOpenedfile;
  }

  @RequiredUIAccess
  public void closeTab(VirtualFile virtualFile) {
    UIAccess.assertIsUIThread();
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
