package consulo.language.editor.impl.internal.generation;

import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.dataContext.DataContext;
import consulo.language.editor.generation.PatternDescriptor;
import consulo.language.editor.impl.internal.template.TemplateEditorUtil;
import consulo.language.editor.internal.TemplateEx;
import consulo.language.editor.template.Template;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.Splitter;
import consulo.ui.ex.awt.tree.SimpleTree;
import consulo.ui.ex.awt.tree.Tree;
import consulo.util.collection.MultiMap;

import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.Collection;

/**
 * @author Dmitry Avdeev
 */
public class GenerateByPatternDialog extends DialogWrapper {

  private JPanel myPanel;
  private Splitter mySplitter;
  private Tree myTree = new Tree();
  private Editor myEditor;

  private MultiMap<String,PatternDescriptor> myMap;

  public GenerateByPatternDialog(Project project, PatternDescriptor[] descriptors, DataContext context) {
    super(project);
    setTitle("Generate by Pattern");
    setOKButtonText("Generate");

    myMap = new MultiMap<String, PatternDescriptor>();
    for (PatternDescriptor descriptor : descriptors) {
      myMap.putValue(descriptor.getParentId(), descriptor);
    }
    DefaultMutableTreeNode root = createNode(null);

    myTree = new SimpleTree() {

    };
    myTree.setRootVisible(false);
    myTree.setCellRenderer(new DefaultTreeCellRenderer() {
      @Override
      public Component getTreeCellRendererComponent(JTree tree,
                                                    Object value,
                                                    boolean sel,
                                                    boolean expanded,
                                                    boolean leaf,
                                                    int row,
                                                    boolean hasFocus) {
        Component component = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row,

                                                                           hasFocus);
        Object object = ((DefaultMutableTreeNode)value).getUserObject();
        if (object instanceof PatternDescriptor) {
          setText(((PatternDescriptor)object).getName());
          setIcon(((PatternDescriptor)object).getIcon());
        }
        return component;
      }
    });

    myTree.setModel(new DefaultTreeModel(root));
    myTree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
      @Override
      public void valueChanged(TreeSelectionEvent e) {
        update();
      }
    });
    myEditor = TemplateEditorUtil.createEditor(true, "");

    mySplitter.setFirstComponent(ScrollPaneFactory.createScrollPane(myTree));
    JPanel details = new JPanel(new BorderLayout());
    details.add(myEditor.getComponent(), BorderLayout.CENTER);
    mySplitter.setSecondComponent(details);
    mySplitter.setHonorComponentsMinimumSize(true);
    mySplitter.setShowDividerControls(true);

    myTree.setSelectionRow(0);

    init();
  }

  private void update() {
    DefaultMutableTreeNode node = (DefaultMutableTreeNode)myTree.getSelectionModel().getSelectionPath().getLastPathComponent();
    getOKAction().setEnabled(node != null && node.isLeaf());

    PatternDescriptor descriptor = getSelectedDescriptor();
    if (descriptor != null) {
      updateDetails(descriptor);
    }
  }

  PatternDescriptor getSelectedDescriptor() {
    Object o = myTree.getSelectionModel().getSelectionPath().getLastPathComponent();
    if (o instanceof DefaultMutableTreeNode) {
      Object object = ((DefaultMutableTreeNode)o).getUserObject();
      if (object instanceof PatternDescriptor) {
        return (PatternDescriptor)object;
      }
    }
    return null;
  }

  @RequiredUIAccess
  private void updateDetails(final PatternDescriptor descriptor) {
    ApplicationManager.getApplication().runWriteAction(() -> {
      final Template template = descriptor.getTemplate();
      if (template instanceof TemplateEx) {
        String text = ((TemplateEx)template).getString();
        myEditor.getDocument().replaceString(0, myEditor.getDocument().getTextLength(), text);
        TemplateEditorUtil.setHighlighter(myEditor, ((TemplateEx)template).getTemplateContext());
      } else {
        myEditor.getDocument().replaceString(0, myEditor.getDocument().getTextLength(), "");
      }
    });
  }

  private DefaultMutableTreeNode createNode(@Nullable PatternDescriptor descriptor) {
    DefaultMutableTreeNode root = new DefaultMutableTreeNode(descriptor) {
      @Override
      public String toString() {
        Object object = getUserObject();
        return object == null ? "" : ((PatternDescriptor)object).getName();
      }
    };
    String id = descriptor == null ? PatternDescriptor.ROOT : descriptor.getId();
    Collection<PatternDescriptor> collection = myMap.get(id);
    for (PatternDescriptor childDescriptor : collection) {
      root.add(createNode(childDescriptor));
    }
    return root;
  }

  @Override
  protected JComponent createCenterPanel() {
    return myPanel;
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myTree;
  }

  @Override
  protected String getDimensionServiceKey() {
    return "generate.patterns.dialog";
  }

  @Override
  protected void dispose() {
    super.dispose();
    EditorFactory.getInstance().releaseEditor(myEditor);
  }

  private void createUIComponents() {
    mySplitter = new Splitter(false, 0.3f);
  }
}
