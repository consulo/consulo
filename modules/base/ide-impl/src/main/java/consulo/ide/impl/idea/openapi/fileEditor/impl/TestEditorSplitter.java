package consulo.ide.impl.idea.openapi.fileEditor.impl;

import consulo.util.lang.Pair;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorProvider;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TestEditorSplitter {
  private final HashMap<String, TestEditorTabGroup> myTabGroups = new HashMap<>();
  private static final String Default = "Default";
  private String myActiveTabGroupName = Default;

  public TestEditorSplitter() {
    myTabGroups.put(Default, new TestEditorTabGroup(Default));
    myActiveTabGroupName = Default;
  }

  private TestEditorTabGroup getActiveTabGroup() {
    return myTabGroups.get(myActiveTabGroupName);
  }

  public void openAndFocusTab(VirtualFile virtualFile, FileEditor fileEditor, FileEditorProvider provider) {
    getActiveTabGroup().openTab(virtualFile, fileEditor, provider);
  }

  public void setActiveTabGroup(@Nonnull String tabGroup) {
    TestEditorTabGroup result = myTabGroups.get(tabGroup);
    if (result == null) {
      result = new TestEditorTabGroup(tabGroup);
      myTabGroups.put(tabGroup, result);
    }
    myActiveTabGroupName = tabGroup;
  }

  @Nullable
  public FileEditor getFocusedFileEditor() {
    Pair<FileEditor, FileEditorProvider> openedEditor = getActiveTabGroup().getOpenedEditor();
    if(openedEditor == null)
      return null;

    return openedEditor.first;
  }

  @Nullable
  public FileEditorProvider getProviderFromFocused() {
    Pair<FileEditor, FileEditorProvider> openedEditor = getActiveTabGroup().getOpenedEditor();
    if(openedEditor == null)
      return null;

    return openedEditor.second;
  }

  public VirtualFile getFocusedFile() {
    return getActiveTabGroup().getOpenedFile();
  }

  public void closeFile(@Nonnull VirtualFile file) {
    TestEditorTabGroup testEditorTabGroup = getActiveTabGroup();
    String key = myActiveTabGroupName;
    if (!testEditorTabGroup.contains(file)) {
      for (Map.Entry<String, TestEditorTabGroup> next : myTabGroups.entrySet()) {
        key = next.getKey();
        TestEditorTabGroup value = next.getValue();
        if (value.contains(file)) {
          testEditorTabGroup = value;
          break;
        }
      }
    }
    testEditorTabGroup.closeTab(file);
    if (!Objects.equals(key, Default) && testEditorTabGroup.getTabCount() == 0)
      myTabGroups.remove(key);
  }

  @Nullable
  public Pair<FileEditor, FileEditorProvider> getEditorAndProvider(VirtualFile file) {
    return getActiveTabGroup().getEditorAndProvider(file);
  }
}
