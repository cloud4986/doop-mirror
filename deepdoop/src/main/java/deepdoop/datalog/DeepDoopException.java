package deepdoop.datalog;

@SuppressWarnings("serial")
public class DeepDoopException extends RuntimeException {

	public DeepDoopException(String message) {
		super("ERROR: " + message);
	}

	@Override
	public Throwable fillInStackTrace() {
		return null;
	}
}