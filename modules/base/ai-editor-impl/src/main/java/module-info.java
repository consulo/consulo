import org.jspecify.annotations.NullMarked;

/**
 * The parts of the AI chats which work with the editor, documents and diffs - kept out of ai-impl, which stays free of
 * them.
 *
 * @author VISTALL
 * @since 2026-11-13
 */
@NullMarked
module consulo.ai.editor.impl {
    requires consulo.ai.api;
    requires consulo.diff.api;
    requires consulo.review.api;
    requires consulo.language.editor.ui.api;
    requires consulo.local.history.api;

    opens consulo.ai.editor.impl.internal.review.state to consulo.util.xml.serializer;
}
