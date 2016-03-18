package library;

@SuppressWarnings("all")
public class SQLHelper  {

	//Adding and deleting columns
	public SQLHelper() {}

	private static FileManager mFileManager;
	private static SDatabase mDatabaseInstance;
	private static SDatabase mConfigDatabaseInstance;
	
	public static <T> SDatabase open(String databaseName) {
		if(mFileManager == null)
			throw new SQLException("FileManager is null");

		if(mConfigDatabaseInstance == null)
			mConfigDatabaseInstance = new SDatabase(mFileManager, null,"configs_database");

		if(mDatabaseInstance == null || !mDatabaseInstance.getDatabaseName().equals(databaseName) ) {
			mDatabaseInstance =  new SDatabase(mFileManager, mConfigDatabaseInstance,databaseName);
		}
		return mDatabaseInstance;
	}

	public static void registerFileManager(FileManager fileManager) {
		mFileManager = fileManager;
	}

	protected static final String SEPERATE     = "%s(?=([^']*'[^']*')*[^']*$)";
	protected static final String SELECT_TABLE = "\\{\\['%s'=.*?\\].*?\\}";

	public static int SORT_ASC  = 1; // low to high
	public static int SORT_DESC = 2; // high to low

	public static interface FileManager {
		String getFileNameWithDatabase(String databaseName);
		String readFile(String fileName);
		void writeToFile(String fileName, String data);
		void createFile(String fileName);
	}

}
