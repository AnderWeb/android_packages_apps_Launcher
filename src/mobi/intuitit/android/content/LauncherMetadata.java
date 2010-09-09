package mobi.intuitit.android.content;

public final class LauncherMetadata {
	
	/*
	 * Contains the current "Android AppWidget Extension API Version" 
	 */
	public static final int CurrentAPIVersion = 2;
	
	private static final String PNAME = "LauncherMetadata.";
	
	public static final class Requirements {

		private static final String REQPNAME = PNAME + "Requirements.";
		
		/*
		 * Metadata name for the required API Version
		 */
		public static final String APIVersion = REQPNAME + "APIVersion";
		
		/*
		 * Metadata name for "requires scrollable widgets"
		 */
		public static final String Scrollable = REQPNAME + "Scrollable";
		
		/*
		 * Metadata name for "requires animated widgets"
		 */
		public static final String Animation = REQPNAME + "Animation";
		
	}
}
