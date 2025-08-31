package consulo.language.editor.codeFragment;

/**
* @author oleg
*/
public class CannotCreateCodeFragmentException extends RuntimeException {
   public CannotCreateCodeFragmentException(String reason) {
     super(reason);
   }
 }
