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
package consulo.ui.ex.awt;

import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.IdeaFileChooser;
import consulo.project.Project;
import consulo.ui.ex.UIBundle;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author oleg
 * This component is used to configure list of folders with add/remove buttons.
 */
public class PathsChooserComponent implements ComponentWithEmptyText {
  private JPanel myContentPane;
  private JBList myList;
  private final DefaultListModel myListModel;

  private List<String> myWorkingCollection;
  private final List<String> myInitialCollection;
  @Nullable private final Project myProject;

  public PathsChooserComponent(@Nonnull List<String> collection, @Nonnull PathProcessor processor) {
    this(collection, processor, null);
  }

  public PathsChooserComponent(@Nonnull List<String> collection,
                               @Nonnull final PathProcessor processor,
                               @Nullable Project project) {
    myList = new JBList();
    myList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    myInitialCollection = collection;
    myProject = project;
    myWorkingCollection = new ArrayList<String>(myInitialCollection);
    myListModel = new DefaultListModel();
    myList.setModel(myListModel);

    myContentPane = ToolbarDecorator.createDecorator(myList).disableUpDownActions().setAddAction(new AnActionButtonRunnable() {
      @Override
      public void run(AnActionButton button) {
        FileChooserDescriptor dirChooser = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        dirChooser.setShowFileSystemRoots(true);
        dirChooser.setHideIgnored(true);
        dirChooser.setTitle(UIBundle.message("file.chooser.default.title"));
        IdeaFileChooser.chooseFiles(dirChooser, myProject, null, new Consumer<List<VirtualFile>>() {
          @Override
          public void accept(List<VirtualFile> files) {
            for (VirtualFile file : files) {
              // adding to the end
              String path = file.getPath();
              if (processor.addPath(myWorkingCollection, path)) {
                myListModel.addElement(path);
              }
            }
          }
        });
      }
    }).setRemoveAction(new AnActionButtonRunnable() {
      @Override
      public void run(AnActionButton button) {
        int selected = myList.getSelectedIndex();
        if (selected != -1) {
          // removing index
          String path = (String) myListModel.get(selected);
          if (processor.removePath(myWorkingCollection, path)){
            myListModel.remove(selected);
          }
        }
      }
    }).createPanel();

    // fill list
    reset();
  }

  @Nonnull
  @Override
  public StatusText getEmptyText() {
    return myList.getEmptyText();
  }

  public JPanel getContentPane() {
    return myContentPane;
  }

  public List<String> getValues(){
    return myWorkingCollection;
  }

  public void reset() {
    myListModel.clear();
    myWorkingCollection = new ArrayList<String>(myInitialCollection);
    for (String path : myWorkingCollection) {
      myListModel.addElement(path);
    }
  }

  public boolean isModified() {
    return !myWorkingCollection.equals(myInitialCollection);
  }

  public void apply() {
    myInitialCollection.clear();
    myInitialCollection.addAll(myWorkingCollection);
  }

  public interface PathProcessor {
    boolean addPath(List<String> paths, String path);
    boolean removePath(List<String> paths, String path);
  }
}
