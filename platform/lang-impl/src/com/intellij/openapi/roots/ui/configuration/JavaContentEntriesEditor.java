/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.openapi.roots.ui.configuration;

@Deprecated
public class JavaContentEntriesEditor extends CommonContentEntriesEditor {
  public JavaContentEntriesEditor(String moduleName, ModuleConfigurationState state) {
    super(moduleName, state, true, true);
  }

/*  @Override
  protected ContentEntryEditor createContentEntryEditor(final String contentEntryUrl) {
    return new JavaContentEntryEditor(contentEntryUrl) {
      @Override
      protected ModifiableRootModel getModel() {
        return JavaContentEntriesEditor.this.getModel();
      }
    };
  }      */

/*  @Override
  protected ContentEntryTreeEditor createContentEntryTreeEditor(Project project) {
    return new ContentEntryTreeEditor(project, true, true);
  }  */

 /* @Override
  protected List<ContentEntry> addContentEntries(VirtualFile[] files) {
    List<ContentEntry> contentEntries = super.addContentEntries(files);
    if (!contentEntries.isEmpty()) {
      final ContentEntry[] contentEntriesArray = contentEntries.toArray(new ContentEntry[contentEntries.size()]);
      addSourceRoots(myProject, contentEntriesArray, new Runnable() {
        @Override
        public void run() {
          addContentEntryPanels(contentEntriesArray);
        }
      });
    }
    return contentEntries;
  }       */

/*  private static void addSourceRoots(final Project project, final ContentEntry[] contentEntries, final Runnable finishRunnable) {
    final HashMap<ContentEntry, Collection<ContentEntry>> entryToRootMap = new HashMap<ContentEntry, Collection<ContentEntry>>();
    final Map<File, ContentEntry> fileToEntryMap = new HashMap<File, ContentEntry>();
    for (final ContentEntry contentEntry : contentEntries) {
      final VirtualFile file = contentEntry.getFile();
      if (file != null) {
        entryToRootMap.put(contentEntry, null);
        fileToEntryMap.put(VfsUtil.virtualToIoFile(file), contentEntry);
      }
    }

    final ProgressWindow progressWindow = new ProgressWindow(true, project);
    final ProgressIndicator progressIndicator = new SmoothProgressAdapter(progressWindow, project);

    final Runnable searchRunnable = new Runnable() {
      @Override
      public void run() {
        final Runnable process = new Runnable() {
          @Override
          public void run() {
            for (final File file : fileToEntryMap.keySet()) {
              progressIndicator.setText(ProjectBundle.message("module.paths.searching.source.roots.progress", file.getPath()));
              final Collection<ContentEntry> roots = JavaSourceRootDetectionUtil.suggestRoots(file);
              entryToRootMap.put(fileToEntryMap.get(file), roots);
            }
          }
        };
        progressWindow.setTitle(ProjectBundle.message("module.paths.searching.source.roots.title"));
        ProgressManager.getInstance().runProcess(process, progressIndicator);
      }
    };

    final Runnable addSourcesRunnable = new Runnable() {
      @Override
      public void run() {
        for (final ContentEntry contentEntry : contentEntries) {
          final Collection<JavaModuleSourceRoot> suggestedRoots = entryToRootMap.get(contentEntry);
          if (suggestedRoots != null) {
            for (final JavaModuleSourceRoot suggestedRoot : suggestedRoots) {
              final VirtualFile sourceRoot = LocalFileSystem.getInstance().findFileByIoFile(suggestedRoot.getDirectory());
              final VirtualFile fileContent = contentEntry.getFile();
              if (sourceRoot != null && fileContent != null && VfsUtil.isAncestor(fileContent, sourceRoot, false)) {
                contentEntry.addSourceFolder(sourceRoot, false, suggestedRoot.getPackagePrefix());
              }
            }
          }
        }
        if (finishRunnable != null) {
          finishRunnable.run();
        }
      }
    };

    new SwingWorker() {
      @Override
      public Object construct() {
        searchRunnable.run();
        return null;
      }

      @Override
      public void finished() {
        addSourcesRunnable.run();
      }
    }.start();
  }

  @Override
  protected JPanel createBottomControl(Module module) {
    final JPanel innerPanel = new JPanel(new GridBagLayout());
    innerPanel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 6));
    return innerPanel;
  } */
}
