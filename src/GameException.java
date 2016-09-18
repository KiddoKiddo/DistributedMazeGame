public class GameException extends Exception {
	
	private static final long serialVersionUID = 1902020590272838028L;

	public GameException() {
		super();
	}

	public GameException(String message) {
		super(message);
	}

	public GameException(String message, Throwable cause) {
		super(message, cause);
	}

	public GameException(Throwable cause) {
		super(cause);
	}
}
