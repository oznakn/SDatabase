package library;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("all")
public class SCompiler<T> {

	private static final int SELECT = 1;
	private static final int INSERT = 2;
	private static final int UPDATE = 3;
	private static final int DELETE = 4;

	private final int ALL=1;
	private final int ONE=2;

	private boolean mHasInstantiateObject = true;
	private int mReqType = -1;
	private int mLimitValue = -1;
	private SList<Integer> mSelectMap;
	private SMap<String> mWhereMap;
	private SList<String> mValuesMap;
	private SMap<String> mSortMap;

	private SDatabase mConfigDatabase;
	private SDatabase mDatabaseInstance;
	private STable mTableInstance;
	private Class<T> mObjectType;

	protected SCompiler(SDatabase configDatabase, SDatabase databaseInstance , STable tableInstance, Class<T> object) {
		mConfigDatabase = configDatabase;
		mDatabaseInstance = databaseInstance;
		mTableInstance = tableInstance;
		mObjectType = object;

		if(object == null)
			mHasInstantiateObject = false;
	}

	public String getDatabaseName() {
		return mDatabaseInstance.getDatabaseName();
	}

	public String getTableName() {
		return mTableInstance.getTableName();
	}

	public String getData() {
		return mTableInstance.getTableData();
	}

	// * for everything
	public SCompiler select(String... selects) {
		isDefined(SELECT);
		mReqType = SELECT;
		mSortMap = new SMap<>(String.class);
		mWhereMap = new SMap<>(String.class);
		if(mSelectMap == null)
			mSelectMap = new SList<>();
		else
			mSelectMap.clear();
		
		if(selects[0].trim().equals("*")) {
			for(int i = 0; i < mTableInstance.getColumnCount(); i++) {
				mSelectMap.add(i);
			}
		}
		else {
			for(int i = 0; i < selects.length; i++) {
				mSelectMap.add(mTableInstance.getColumnIndexWithName(selects[i]));
			}
		}
		return this;
	}
	
	public SCompiler where(String name , String value) {
		return where(mTableInstance.getColumnIndexWithName(name), value);
	}

	public SCompiler where(int index , String value) {
		if(mWhereMap == null)
			mWhereMap = new SMap(String.class);

		mWhereMap.add(index, value);

		return this;
	}

	public SCompiler limit(int value) {
		isDefined(SELECT, true);
		mReqType = SELECT;

		mLimitValue = value;
		return this;
	}

	public SCompiler sort(String column , int sortType) {
		isDefined(SELECT, true);
		mReqType = SELECT;
		
		if(mSortMap == null)
			mSortMap = new SMap(String.class);
		
		mSortMap.add(mTableInstance.getColumnIndexWithName(column), Integer.toString(sortType));
		return this;
	}
	
	public SCompiler insert(SMap map) {
		isDefined(INSERT);
		mReqType = INSERT;

		if(mValuesMap == null)
			mValuesMap = new SList<>();
		else
			mValuesMap.clear();

		for(int i = 0; i < mTableInstance.getColumnCount(); i++) {
			if(map.contains(i)) {
				mValuesMap.add((String) map.get(i));
			}
			else {
				if(isAi(i))
					mValuesMap.add(Integer.toString(getLastInsertId(i)));
				else 
					throw new SQLException("ANAN");
			}
		}
		return this;
	}
	
	public SCompiler update(SMap map) {
		isDefined(UPDATE);
		mReqType = UPDATE;

		mWhereMap = new SMap<>(String.class);
		if(mValuesMap == null)
			mValuesMap = new SList<>();
		else
			mValuesMap.clear();
		
		for(int i = 0; i < mTableInstance.getColumnCount(); i++) {
			if(map.contains(i)) 
				mValuesMap.add((String) map.get(i));
			else {
				mValuesMap.add(null);
			}
		}
		return this;
	}
	
	public SCompiler delete() {
		isDefined(DELETE);
		mReqType = DELETE;

		mWhereMap = new SMap<>(String.class);
		return this;
	}
	
	public boolean run() {
		if(mReqType == INSERT) {
			if(mValuesMap == null || mValuesMap.size() != mTableInstance.getColumnCount())
				throw new SQLException("anan in " + getTableName() + " - " + getDatabaseName() + " " + mValuesMap.toString());
			
			String text = "";
			for(int i = 0; i < mValuesMap.size(); i++) {
				text += "'%s'";
				if(i < mValuesMap.size() - 1)
					text += ",";
			}
			text = "(" + String.format(text,mValuesMap.toArray()) + ");";
			mTableInstance.appendTableData(text);
			
			save();
			return true;
		}
		else if(mReqType == DELETE) {
			int count = mTableInstance.getColumnCount();
			String regex = "";
			for(int i = 0; i < count; i++) {
				if(mWhereMap != null && mWhereMap.contains(i)) {
					regex += "'" + mWhereMap.get(i) +"'";
				}
				else 
					regex += "'.*?'";
				
				
				if(i < count - 1) 
					regex += ",";
			}
			
			mTableInstance.setTableData(mTableInstance.getTableData().replaceAll("\\(" + regex + "\\);", ""));
			
			save();
		}
		else if(mReqType == UPDATE) {
			if(mValuesMap == null)
				throw new SQLException("ANAN");
			
			int count = mTableInstance.getColumnCount();

			String regex = "";
			for(int i = 0; i < count; i++) {
				if(mWhereMap != null && mWhereMap.contains(i)) {
					regex += "'" + mWhereMap.get(i) +"'";
				}
				else 
					regex += "'.*?'";
				
				if(i < count - 1) 
					regex += ",";
			}

			Matcher matcher = Pattern.compile("\\(" + regex + "\\);").matcher(mTableInstance.getTableData());
			while(matcher.find()) {
				String text = "";
				String[] parts = mTableInstance.getTableData().substring(matcher.start() + 1, matcher.end() - 2).split("[,]");
				for(int i = 0; i < count; i++) {
					if(mValuesMap.exists(i) && mValuesMap.get(i) != null) {
						parts[i] = "'" + mValuesMap.get(i) + "'";
					}
					
					text += parts[i];
					if(i < count - 1)
						text += ",";
				}
				text = "(" + text + ");";
				
				mTableInstance.setTableData(mTableInstance.getTableData().substring(0 , matcher.start()) + text + mTableInstance.getTableData().substring(matcher.end()  , mTableInstance.getTableData().length()));
			}
			
			save();
		}
		return false;
	}
	
	public void callback(GetCallback<T> getCallback) {
		getCallback.onCallback(get());
	}
	
	public ArrayList<T> get() {
		if(mHasInstantiateObject)
			return get(ALL);
		else
			throw new SQLException("Class object is in wrong format");
	}
	
	public T one() {
		if(mHasInstantiateObject) {
			ArrayList<T> list = get(ONE);
			if(list.size() == 0)
				return null;

			return list.get(0);
		}
		else
			throw new SQLException("Class object is in wrong format");
	}

	public ArrayList<ArrayList<String>> getNoObject() {
		return getNoObject(ALL);
	}

	public ArrayList<String> oneNoObject() {
		ArrayList<ArrayList<String>> list = getNoObject(ONE);
		if(list.size() == 0)
			return null;

		return list.get(0);
	}
	
	private ArrayList<T> get(int type) {
		isDefined(SELECT , true);

		ArrayList<T> list = new ArrayList();
		int count = mTableInstance.getColumnCount();
		String regex = "";
		for(int i = 0; i < count; i++) {
			if(mWhereMap != null && mWhereMap.contains(i)) {
				regex += "'" + mWhereMap.get(i) +"'";
			}
			else
				regex += "'.*?'";


			if(i < count - 1)
				regex += ",";
		}

		Field[] list1 = getFields(mObjectType);
		int counter = 0;
		Matcher matcher = Pattern.compile("\\(" + regex + "\\);").matcher(mTableInstance.getTableData());
		while(matcher.find()) {
			T t = newClassInstance();
			String[] values = mTableInstance.getTableData().substring(matcher.start() - 1, matcher.end() - 2).split(",");

			for(int i = 0; i < list1.length; i++) {
				if(list1[i].isAnnotationPresent(SColumn.class)) {
					int columnIndex = mTableInstance.getColumnIndexWithName(((SColumn) list1[i].getAnnotation(SColumn.class)).name());
					String value = values[i].split("'")[1];
					try {
						if(mSelectMap.contains(new Integer(columnIndex)) ) {
							if(list1[i].getType() == int.class)
								list1[i].setInt(t, Integer.parseInt(value));
							else
								list1[i].set(t, value);
						}
						else {
							if(list1[i].getType() == int.class)
								list1[i].setInt(t, 0);
							else
								list1[i].set(t, null);
						}
						
					} catch (Exception e) {
						e.printStackTrace();
					} 
				}
			}
			list.add(t);

			counter++;
			if(type == ONE) 
				break;
			if(counter == mLimitValue)
				break;
		}
		
		mReqType = -1;
		return sort(list);
	}
	
	private ArrayList<T> sort(ArrayList<T> list) {
		if(mSortMap == null || mSortMap.size() == 0) {
			return list;
		}
		
		Field[] targetFields = new Field[mSortMap.size()];
		int[] sortMaps = new int[mSortMap.size()];
		
		Field[] tempFieldList = getFields(mObjectType);
		for(int i = 0; i < tempFieldList.length; i++) {
			for(int j = 0; j< mSortMap.size(); j++) {
				if(tempFieldList[i].isAnnotationPresent(SColumn.class) ) {
					int columnIndex = mTableInstance.getColumnIndexWithName(((SColumn)tempFieldList[i].getAnnotation(SColumn.class)).name());
					if(mSortMap.get(columnIndex) != null) {
						targetFields[j] = tempFieldList[i];
						sortMaps[j] = Integer.parseInt(mSortMap.get(columnIndex ));						
					}
				}
			}
		}
		tempFieldList = null;
			
		Collections.sort( list, new Comparator<T>() {

			@Override
			public int compare(T o1, T o2) {
				int compareCount = 0;
				try {
					for(int i = 0; i < targetFields.length; i++) {
						if(compareCount == 0 && targetFields[i].getType() == int.class) {
							if(targetFields[i].getInt(o1) > targetFields[i].getInt(o2)) {
								if(sortMaps[i] == SQLHelper.SORT_ASC)
									compareCount++;
								else if(sortMaps[i] == SQLHelper.SORT_DESC)
									compareCount--;
							}
							else if(targetFields[i].getInt(o1) < targetFields[i].getInt(o2)) {
								if(sortMaps[i] == SQLHelper.SORT_ASC)
									compareCount--;
								else if(sortMaps[i] == SQLHelper.SORT_DESC)
									compareCount++;
							}
						}
						else
							break;
					}
				}
				catch(Exception e) {
					e.printStackTrace();
				}
				return compareCount;
			}
		});
		return list;
	}

	private ArrayList<ArrayList<String>> getNoObject(int type) {
		isDefined(SELECT , true);

		ArrayList<ArrayList<String>> list = new ArrayList();
		int count = mTableInstance.getColumnCount();
		String regex = "";
		for(int i = 0; i < count; i++) {
			if(mWhereMap != null && mWhereMap.contains(i)) {
				regex += "'" + mWhereMap.get(i) +"'";
			}
			else
				regex += "'.*?'";


			if(i < count - 1)
				regex += ",";
		}

		int counter = 0;
		Matcher matcher = Pattern.compile("\\(" + regex + "\\);").matcher(mTableInstance.getTableData());
		while(matcher.find()) {
			String[] values = mTableInstance.getTableData().substring(matcher.start() - 1, matcher.end() - 2).split(",");
			ArrayList<String> resultValues = new ArrayList<String>();
			for(int i = 0; i < count; i++) {
				if(mSelectMap.contains(new Integer(i)) ) {
					resultValues.add(values[i].split("'")[1]);
				}
				else {
					resultValues.add("");
				}
			}
			list.add(resultValues);
			counter++;

			if(type == ONE)
				break;
			if(counter == mLimitValue)
				break;
		}

		mReqType = -1;
		return sortNoObject(list);
	}

	private ArrayList<ArrayList<String>> sortNoObject(ArrayList<ArrayList<String>> list) {
		if(mSortMap == null || mSortMap.size() == 0) {
			return list;
		}

		int[] sortMaps = new int[mSortMap.size()];
		ArrayList<Integer> targetFields = new ArrayList<>();

		int count = mTableInstance.getColumnCount();
		for(int i = 0; i < count; i++) {
			for(int j = 0; j< mSortMap.size(); j++) {
				if(mSortMap.get(i) != null) {
					sortMaps[j] = Integer.parseInt(mSortMap.get(i));
					targetFields.add(i);
				}
			}
		}

		Collections.sort( list, new Comparator<ArrayList<String>>() {

			@Override
			public int compare(ArrayList<String> o1, ArrayList<String> o2) {
				int compareCount = 0;
				for(int i = 0; i < targetFields.size(); i++) {
					try {
						if (Integer.parseInt(o1.get(i)) > Integer.parseInt(o2.get(i))) {
							if (sortMaps[i] == SQLHelper.SORT_ASC)
								compareCount++;
							else if (sortMaps[i] == SQLHelper.SORT_DESC)
								compareCount--;
						} else if (Integer.parseInt(o1.get(i)) < Integer.parseInt(o2.get(i))) {
							if (sortMaps[i] == SQLHelper.SORT_ASC)
								compareCount--;
							else if (sortMaps[i] == SQLHelper.SORT_DESC)
								compareCount++;
						}
					}
					catch (NumberFormatException e) {
						e.printStackTrace();
						break;
					}
				}
				return compareCount;
			}
		});
		return list;
	}
	
	/*
	 * For arrays
	 * */
	private void save() {
		mReqType = -1;
		String result = mDatabaseInstance.getDatabaseData().replaceAll(String.format(SQLHelper.SELECT_TABLE,getTableName()), "{" + mTableInstance.getTableData() + "}");
		mDatabaseInstance.saveData(result);
	}

	private boolean isAi(int columnIndex) {
		String tempList[] = mTableInstance.getTableData().split(String.format(SQLHelper.SEPERATE, "[=|\\]]"))[1].split(String.format(SQLHelper.SEPERATE, ",") )[columnIndex].split(String.format(SQLHelper.SEPERATE, ":"));
		if(tempList.length > 1 && tempList[1].equals("ai"))
			return true;
		else
			return false;
	}
	
	private int getLastInsertId(int columnIndex) {
		int pos = 0;

		STable table = mConfigDatabase.get("auto_increment");
		ArrayList<String> item = table.getCompiler()
				.select("*")
				.where("database", getDatabaseName())
				.where("table",getTableName())
				.where("column",mTableInstance.getColumns()[columnIndex])
				.oneNoObject();

		if(item == null) {
			SQLObject map = new SQLObject();
			map.add(table.getColumnIndexWithName("database"), getDatabaseName());
			map.add(table.getColumnIndexWithName("table"), getTableName());
			map.add(table.getColumnIndexWithName("column"), mTableInstance.getColumns()[columnIndex]);
			map.add(table.getColumnIndexWithName("value"), Integer.toString(1));

			table.getCompiler().insert(map).run();
			pos = 1;

			System.out.println("ConfigObjectItem is null");
		}
		else {
			try {
				System.out.println("ConfigObjectItem is not null " +table.getColumns()[columnIndex]);
				pos = Integer.parseInt(item.get(3));
				pos++;

				SQLObject map = new SQLObject();
				map.add(table.getColumnIndex("value"), Integer.toString(pos));
				table.getCompiler()
						.update(map)
						.where("database", getDatabaseName())
						.where("table",getTableName())
						.where("column",item.get(2))
						.run();
			}
			catch (NumberFormatException e) {
				e.printStackTrace();
				throw new SQLException("Wrong Format on auto_increment value");
			}
		}
		return pos;
	}

	
	private void isDefined(int type) {
		isDefined(type , false);
	}
	
	private void isDefined(int type, boolean helper) {
		if(!helper && mReqType >=0 && mReqType != type) {
			throw new SQLException("defined");
		}
		else if(helper && mReqType != type) 
			throw new SQLException("defined");
	}

	private T newClassInstance() {
		final Constructor[] ctors = mObjectType.getDeclaredConstructors();
        Constructor ctor = null;
        for (Constructor ct : ctors) {
            ctor = ct;
            if (ctor.getGenericParameterTypes().length == 0)
                break;
        }
        if (ctor == null)
            throw new IllegalStateException("No default constructor found for " + mObjectType.getName());
        ctor.setAccessible(true);

        try {
            return (T) ctor.newInstance();
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException("Failed to instantiate " + mObjectType.getName() + ": " + t.getLocalizedMessage());
        }
	}

	private Field[] getFields(Class<?> object) {
		return object.getDeclaredFields();
	}

	public interface GetCallback<T> {
		void onCallback(ArrayList<T> list);
	}
}