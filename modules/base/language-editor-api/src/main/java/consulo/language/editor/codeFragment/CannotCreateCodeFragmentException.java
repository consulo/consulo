package consulo.language.editor.codeFragment;

/**
* @author oleg
*/
public class CannotCreateCodeFragmentException extends RuntimeException {
   public CannotCreateCodeFragmentException(final String reason) {
     super(reason);
   }
 }
