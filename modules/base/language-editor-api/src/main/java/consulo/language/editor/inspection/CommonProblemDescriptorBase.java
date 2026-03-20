package consulo.language.editor.inspection;

import consulo.localize.LocalizeValue;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;

import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * @author anna
 * @since 2006-01-04
 */
public class CommonProblemDescriptorBase implements CommonProblemDescriptor {
    private final QuickFix[] myFixes;
    
    private final LocalizeValue myDescriptionTemplate;

    public CommonProblemDescriptorBase(QuickFix[] fixes, LocalizeValue descriptionTemplate) {
        if (fixes == null) {
            myFixes = null;
        }
        else if (fixes.length == 0) {
            myFixes = QuickFix.EMPTY_ARRAY;
        }
        else {
            // no copy in most cases
            myFixes = ArrayUtil.contains(null, fixes) ? ContainerUtil.mapNotNull(fixes, Function.identity(), QuickFix.EMPTY_ARRAY) : fixes;
        }
        myDescriptionTemplate = descriptionTemplate;
    }

    
    @Override
    public LocalizeValue getDescriptionTemplate() {
        return myDescriptionTemplate;
    }

    @Override
    public @Nullable QuickFix[] getFixes() {
        return myFixes;
    }

    @Override
    public String toString() {
        return myDescriptionTemplate.get();
    }
}
