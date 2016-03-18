package library;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SDatabase {

    private static SQLHelper.FileManager mFileManager;
    private static SDatabase mConfigDatabase;
    private String mDatabaseName;
    private String mDatabaseData;

    protected SDatabase(SQLHelper.FileManager fileManager, SDatabase configDatabase, String databaseName) {
        mFileManager = fileManager;
        mConfigDatabase = configDatabase;
        mDatabaseName = databaseName;
        mDatabaseData = readData();

        if(configDatabase != null && (!mDatabaseName.equals("configs_database")))
            initDatabaseForConfigs();
    }

    private STable mTableInstance;

    public <T> STable get(String tableName) {
        return get(tableName, null);
    }

    public <T> STable get(String tableName, Class<T> object) {
        if(mTableInstance == null || !mTableInstance.getSavedDatabaseName().equals(mDatabaseName) || !mTableInstance.getTableName().equals(tableName)) {
            String tableData = parseTableData(tableName);
            if(tableData == null)
                addTable(object, false);
            tableData = parseTableData(tableName);
            mTableInstance =  new STable<>(mConfigDatabase, this, tableName,tableData, object);
        }
        return mTableInstance;
    }

    private void initDatabaseForConfigs() {
        String tableData = mConfigDatabase.parseTableData("auto_increment");
        if(tableData == null) {
            SNewTable table = new SNewTable("auto_increment");
            table.addColumn(new SNewTable.SNewColumn("database"));
            table.addColumn(new SNewTable.SNewColumn("table"));
            table.addColumn(new SNewTable.SNewColumn("column"));
            table.addColumn(new SNewTable.SNewColumn("value"));

            mConfigDatabase.addTable(table);
        }
    }

    public void execQuery(String query) {
        execQuery(query, null);
    }

    public void execQuery(String query, SQuery.OnFinishCallback callback) {
        SQuery queryObject = new SQuery(mConfigDatabase, this, query, callback);
        queryObject.run();
    }

    private String readData() {
        /*
        if(mDatabaseName.equals("configs_database"))
            return "{['auto_increment'='database','table','column','value'];('merhaba','peoples','id','2');};";;

        return "{['peoples'='id':ai,'name','surname'];('1','Ozan','Akin');('2','Ozan2','Akin2');};{['lessons'='id','name'];('1','Math');('2','Turkish');};";
        */
        return mFileManager.readFile(mFileManager.getFileNameWithDatabase(mDatabaseName));
    }

    protected void saveData(String s) {
        mDatabaseData = s;
        System.out.println(s);
        mFileManager.writeToFile(mFileManager.getFileNameWithDatabase(mDatabaseName), s);
    }

    public String getData() {
        return mDatabaseData;
    }

    public String getConfigData() {
        if(mDatabaseName.equals("configs_database"))
            return getData();
        return mConfigDatabase.getData();
    }

    protected String getDatabaseData() {
        return mDatabaseData;
    }

    public String getDatabaseName() {
        return mDatabaseName;
    }

    public <T> void addTable(Class<T> object, boolean hasIndex) {
        addTable(object.getSimpleName().toLowerCase(), object, hasIndex);
    }

    public <T> void addTable(String tableName, Class<T> object, boolean hasIndex) {
        String result = "{['" + tableName + "'=";

        ArrayList<String> items = new ArrayList<>();
        Field[] fileds = object.getDeclaredFields();

        for(int i = 0; i < fileds.length; i++) {
            if(fileds[i].isAnnotationPresent(SColumn.class)) {
                String item = "'" + fileds[i].getName() + "'";
                if(fileds[i].getAnnotation(SColumn.class).ai())
                    item += ":ai";
                if(hasIndex) {
                    if(fileds[i].getAnnotation(SColumn.class).index() <= 0) {
                        throw new SQLException("No Index Decleare: Decleare index or not use hasIndex");
                    }
                    items.set(fileds[i].getAnnotation(SColumn.class).index(), item);
                }
                else {
                    items.add(item);
                }
            }
        }

        for(int i = 0; i < items.size(); i++) {
            result += items.get(i);
            if(i < items.size() - 1)
                result += ",";
        }

        items = null;

        result += "];};";

        mDatabaseData += result;

        saveData(mDatabaseData);
    }

    public void addTable(SNewTable table) {
        if(table.getTableName() == null || table.getColumns().size() == 0) {
            throw new SQLException("SNewTable is in wrong form");
        }

        String result = "{['" + table.getTableName() + "'=";

        ArrayList<String> items = new ArrayList<>();

        for(int i = 0; i < table.getColumns().size(); i++) {
            String item = "'" + table.getColumns().get(i).mName + "'";
            if(table.getColumns().get(i).mIsAi)
                item += ":ai";
            if(table.getHasIndex()) {
                if(table.getColumns().get(i).mIndex <= 0) {
                    throw new SQLException("No Index Decleare: Decleare index or not use hasIndex");
                }
                items.set(table.getColumns().get(i).mIndex, item);
            }
            else {
                items.add(item);
            }
        }

        for(int i = 0; i < items.size(); i++) {
            result += items.get(i);
            if(i < items.size() - 1)
                result += ",";
        }

        items = null;

        result += "];};";

        mDatabaseData += result;

        saveData(mDatabaseData);
    }

    private String parseTableData(String tableName) {
        String data = getDatabaseData();
        Matcher matcher = Pattern.compile(String.format(SQLHelper.SELECT_TABLE , tableName)).matcher(data);
        if(matcher.find())
            data = data.substring(matcher.start() + 1, matcher.end() - 1);
        else {
            data = null;
            //throw new SQLException("No Such Table");
        }
        return data;
    }

    public boolean ifTableExist(String tableName) {
        return parseTableData(tableName) != null;
    }
}
