package consulo.task.impl.internal.setting;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.ConcurrentFactoryMap;
import consulo.configurable.*;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.task.TaskManager;
import consulo.task.TaskRepository;
import consulo.task.TaskRepositorySubtype;
import consulo.task.TaskRepositoryType;
import consulo.task.impl.internal.RecentTaskRepositories;
import consulo.task.impl.internal.TaskManagerImpl;
import consulo.task.ui.TaskRepositoryEditor;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.util.collection.ContainerUtil;
import jakarta.inject.Inject;
import org.jetbrains.annotations.Nls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

/**
 * @author Dmitry Avdeev
 */
@ExtensionImpl
public class TaskRepositoriesConfigurable implements Configurable.NoScroll, ProjectConfigurable, NonDefaultProjectConfigurable {

  private static final String EMPTY_PANEL = "empty.panel";
  private JPanel myPanel;
  private JPanel myServersPanel;
  private final JBList myRepositoriesList;
  @SuppressWarnings({"UnusedDeclaration"})
  private JPanel myToolbarPanel;
  private JPanel myRepositoryEditor;
  private JBLabel myServersLabel;
  private Splitter mySplitter;
  private JPanel myEmptyPanel;

  private final List<TaskRepository> myRepositories = new ArrayList<TaskRepository>();
  private final List<TaskRepositoryEditor> myEditors = new ArrayList<TaskRepositoryEditor>();
  private final Project myProject;

  private final Consumer<TaskRepository> myChangeListener;
  private int count;
  private final Map<TaskRepository, String> myRepoNames = ConcurrentFactoryMap.createMap(repository -> Integer.toString(count++));

  private final TaskManagerImpl myManager;

  @Inject
  @SuppressWarnings("unchecked")
  public TaskRepositoriesConfigurable(final Project project) {

    myProject = project;
    myManager = (TaskManagerImpl)TaskManager.getManager(project);

    myRepositoriesList = new JBList();
    myRepositoriesList.getEmptyText().setText("No servers");

    myServersLabel.setLabelFor(myRepositoriesList);

    List<TaskRepositoryType> groups = TaskRepositoryType.getRepositoryTypes();

    final List<AnAction> createActions = new ArrayList<AnAction>();
    for (final TaskRepositoryType repositoryType : groups) {
      for (final TaskRepositorySubtype subtype : (List<TaskRepositorySubtype>)repositoryType.getAvailableSubtypes()) {
        String description = "New " + subtype.getPresentableName() + " server";
        createActions.add(new AnAction(subtype.getPresentableName().get(), description, subtype.getIcon()) {
          @Override
          public void actionPerformed(AnActionEvent e) {
            TaskRepository repository = repositoryType.createRepository(subtype);
            addRepository(repository);
          }
        });
      }
    }

    ToolbarDecorator toolbarDecorator = ToolbarDecorator.createDecorator(myRepositoriesList).disableUpDownActions();

    toolbarDecorator.setAddAction(new AnActionButtonRunnable() {
      @Override
      public void run(AnActionButton anActionButton) {
        DefaultActionGroup group = new DefaultActionGroup();
        for (AnAction aMyAdditional : createActions) {
          group.add(aMyAdditional);
        }
        Set<TaskRepository> repositories = RecentTaskRepositories.getInstance().getRepositories();
        repositories.removeAll(myRepositories);
        if (!repositories.isEmpty()) {
          group.add(AnSeparator.getInstance());
          for (final TaskRepository repository : repositories) {
            group.add(new AnAction(repository.getUrl(), repository.getUrl(), repository.getIcon()) {
              @Override
              public void actionPerformed(AnActionEvent e) {
                addRepository(repository);
              }
            });
          }
        }

        JBPopupFactory.getInstance()
                .createActionGroupPopup("Add server", group, DataManager.getInstance().getDataContext(anActionButton.getContextComponent()), JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false)
                .show(anActionButton.getPreferredPopupPoint());
      }
    });

    toolbarDecorator.setRemoveAction(new AnActionButtonRunnable() {
      @Override
      public void run(AnActionButton anActionButton) {
        TaskRepository repository = getSelectedRepository();
        if (repository != null) {

          CollectionListModel model = (CollectionListModel)myRepositoriesList.getModel();
          model.remove(repository);
          myRepositories.remove(repository);

          if (model.getSize() > 0) {
            myRepositoriesList.setSelectedValue(model.getElementAt(0), true);
          }
          else {
            myRepositoryEditor.removeAll();
            myRepositoryEditor.repaint();
          }
        }
      }
    });

    myServersPanel.add(toolbarDecorator.createPanel(), BorderLayout.CENTER);

    myRepositoriesList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        TaskRepository repository = getSelectedRepository();
        if (repository != null) {
          String name = myRepoNames.get(repository);
          assert name != null;
          ((CardLayout)myRepositoryEditor.getLayout()).show(myRepositoryEditor, name);
          mySplitter.doLayout();
          mySplitter.repaint();
        }
      }
    });

    myRepositoriesList.setCellRenderer(new ColoredListCellRenderer() {
      @Override
      protected void customizeCellRenderer(JList list, Object value, int index, boolean selected, boolean hasFocus) {
        TaskRepository repository = (TaskRepository)value;
        setIcon(repository.getIcon());
        append(repository.getPresentableName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
      }
    });

    myChangeListener = repository -> ((CollectionListModel)myRepositoriesList.getModel()).contentsChanged(repository);
  }

  private void addRepository(TaskRepository repository) {
    myRepositories.add(repository);
    ((CollectionListModel)myRepositoriesList.getModel()).add(repository);
    addRepositoryEditor(repository, true);
    myRepositoriesList.setSelectedIndex(myRepositoriesList.getModel().getSize() - 1);
  }

  private void addRepositoryEditor(TaskRepository repository, boolean requestFocus) {
    TaskRepositoryEditor editor = repository.getRepositoryType().createEditor(repository, myProject, myChangeListener);
    myEditors.add(editor);
    JComponent component = editor.createComponent();
    String name = myRepoNames.get(repository);
    myRepositoryEditor.add(component, name);
    myRepositoryEditor.doLayout();
    JComponent preferred = editor.getPreferredFocusedComponent();
    if (preferred != null && requestFocus) {
      //      IdeFocusManager.getInstance(myProject).requestFocus(preferred, false);
    }
  }

  @Nullable
  private TaskRepository getSelectedRepository() {
    return (TaskRepository)myRepositoriesList.getSelectedValue();
  }

  @Override
  @Nls
  public String getDisplayName() {
    return "Servers";
  }

  @Nonnull
  @Override
  public String getId() {
    return "tasks.servers";
  }

  @Nullable
  @Override
  public String getParentId() {
    return StandardConfigurableIds.TASKS_GROUP;
  }

  @Override
  @RequiredUIAccess
  public JComponent createComponent() {
    return myPanel;
  }

  @RequiredUIAccess
  @Override
  public JComponent getPreferredFocusedComponent() {
    return myRepositoriesList;
  }

  @Override
  @RequiredUIAccess
  public boolean isModified() {
    return !myRepositories.equals(getReps());
  }

  @Override
  @RequiredUIAccess
  public void apply() throws ConfigurationException {
    List<TaskRepository> newRepositories = ContainerUtil.map(myRepositories, taskRepository -> taskRepository.clone());
    myManager.setRepositories(newRepositories);
    myManager.updateIssues(null);
    RecentTaskRepositories.getInstance().addRepositories(myRepositories);
  }

  @Override
  @RequiredUIAccess
  public void reset() {
    myRepoNames.clear();
    myRepositoryEditor.removeAll();
    myRepositoryEditor.add(myEmptyPanel, EMPTY_PANEL);
    //    ((CardLayout)myRepositoryEditor.getLayout()).show(myRepositoryEditor, );
    myRepositories.clear();

    CollectionListModel listModel = new CollectionListModel(new ArrayList());
    for (TaskRepository repository : myManager.getAllRepositories()) {
      TaskRepository clone = repository.clone();
      assert clone.equals(repository) : repository.getClass().getName();
      myRepositories.add(clone);
      listModel.add(clone);
    }

    myRepositoriesList.setModel(listModel);

    for (TaskRepository clone : myRepositories) {
      addRepositoryEditor(clone, false);
    }

    if (!myRepositories.isEmpty()) {
      myRepositoriesList.setSelectedValue(myRepositories.get(0), true);
    }
  }

  private List<TaskRepository> getReps() {
    return Arrays.asList(myManager.getAllRepositories());
  }

  @Override
  @RequiredUIAccess
  public void disposeUIResources() {
    for (TaskRepositoryEditor editor : myEditors) {
      if (!Disposer.isDisposed(editor)) {
        editor.disposeWithTree();
      }
    }
    myEditors.clear();
  }
}
