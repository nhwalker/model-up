package io.github.nhwalker.modelup.container;

/**
 * Indicates a problem occured while dispatching a change event
 */
public class ChangeDispatchException extends RuntimeException {

  private static final long serialVersionUID = -9034153833751190227L;

  public ChangeDispatchException() {
    super();
  }

  public ChangeDispatchException(String message, Throwable cause) {
    super(message, cause);
  }

  public ChangeDispatchException(String message) {
    super(message);
  }

  public ChangeDispatchException(Throwable cause) {
    super(cause);
  }

}
