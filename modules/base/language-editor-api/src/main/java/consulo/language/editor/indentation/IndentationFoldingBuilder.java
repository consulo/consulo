package consulo.language.editor.indentation;

import consulo.annotation.UsedInPlugin;
import consulo.annotation.access.RequiredReadAction;
import consulo.application.dumb.DumbAware;
import consulo.document.Document;
import consulo.language.ast.ASTNode;
import consulo.language.ast.TokenSet;
import consulo.language.editor.folding.FoldingBuilder;
import consulo.language.editor.folding.FoldingDescriptor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * @author oleg
 */
@UsedInPlugin
public abstract class IndentationFoldingBuilder implements FoldingBuilder, DumbAware {
    private final TokenSet myTokenSet;

    public IndentationFoldingBuilder(TokenSet tokenSet) {
        myTokenSet = tokenSet;
    }

    @RequiredReadAction
    @Override
    @Nonnull
    public FoldingDescriptor[] buildFoldRegions(@Nonnull ASTNode astNode, @Nonnull Document document) {
        List<FoldingDescriptor> descriptors = new LinkedList<>();
        collectDescriptors(astNode, descriptors);
        return descriptors.toArray(new FoldingDescriptor[descriptors.size()]);
    }

    @RequiredReadAction
    private void collectDescriptors(@Nonnull ASTNode node, @Nonnull List<FoldingDescriptor> descriptors) {
        Queue<ASTNode> toProcess = new LinkedList<>();
        toProcess.add(node);
        while (!toProcess.isEmpty()) {
            ASTNode current = toProcess.remove();
            if (current.getTreeParent() != null
                && current.getTextLength() > 1
                && myTokenSet.contains(current.getElementType())) {
                descriptors.add(new FoldingDescriptor(current, current.getTextRange()));
            }
            for (ASTNode child = current.getFirstChildNode(); child != null; child = child.getTreeNext()) {
                toProcess.add(child);
            }
        }
    }

    @RequiredReadAction
    @Override
    @Nullable
    public String getPlaceholderText(@Nonnull ASTNode node) {
        StringBuilder builder = new StringBuilder();
        ASTNode child = node.getFirstChildNode();
        while (child != null) {
            String text = child.getText();
            if (text == null) {
                if (builder.length() > 0) {
                    break;
                }
            }
            else if (!text.contains("\n")) {
                builder.append(text);
            }
            else if (builder.length() > 0) {
                builder.append(text.substring(0, text.indexOf('\n')));
                break;
            }
            else {
                builder.append(getFirstNonEmptyLine(text));
                if (builder.length() > 0) {
                    break;
                }
            }
            child = child.getTreeNext();
        }
        return builder.toString();
    }

    @Nonnull
    private static String getFirstNonEmptyLine(@Nonnull String text) {
        int start = 0;
        int end;
        while ((end = text.indexOf('\n', start)) != -1 && start >= end) {
            start = end + 1;
        }
        return end == -1 ? text.substring(start) : text.substring(start, end);
    }

    @RequiredReadAction
    @Override
    public boolean isCollapsedByDefault(@Nonnull ASTNode node) {
        return false;
    }
}
