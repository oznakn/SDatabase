package library;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class STable<T> {

    private Class<T> mClassInstance;
    private SDatabase mConfigDatabase;
    private SDatabase mDatabaseInstance;
    private String mTableName;
    private String mTableData;

    protected STable(SDatabase configDatabase, SDatabase databaseInstance, String tableName,String tableData, Class<T> object) {
        mConfigDatabase = configDatabase;
        mClassInstance = object;
        mDatabaseInstance = databaseInstance;
        mTableName = tableName;
        mTableData = tableData;

        checkDatabaseForConfigs();
    }

    private SCompiler mCompilerInstance;

    public <T> SCompiler getCompiler() {
        if(mCompilerInstance == null || !mCompilerInstance.getDatabaseName().equals(mDatabaseInstance.getDatabaseName()) || !mCompilerInstance.getTableName().equals(mTableName)) {
            mCompilerInstance =  new SCompiler<>(mConfigDatabase, mDatabaseInstance, this,  mClassInstance);
        }
        return mCompilerInstance;
    }

    private void checkDatabaseForConfigs() {
        String[] list = getAIColumns();
        for(String s : list) {
            Object a = mConfigDatabase.get("auto_increment").getCompiler().select("*")
                    .where("database", getDatabaseName())
                    .where("table", getTableName())
                    .where("column",s)
                    .oneNoObject();
            if(a == null) {
                SQLObject obj = new SQLObject();
                obj.add(getColumnIndexWithName("database"),getDatabaseName());
                obj.add(getColumnIndexWithName("table"),getTableName());
                obj.add(getColumnIndexWithName("column"),s);
                obj.add(getColumnIndexWithName("value"),Integer.toString(1));

                mConfigDatabase.get("auto_increment").getCompiler().insert(obj).run();
            }
        }
    }

    protected String getSavedDatabaseName() {
        if(mCompilerInstance == null)
            return "";
        return mCompilerInstance.getDatabaseName();
    }

    protected String getDatabaseName() {
        return mDatabaseInstance.getDatabaseName();
    }

    public String getTableData() {
        return mTableData;
    }

    public String getTableName() {
        return mTableName;
    }

    protected void setTableData(String tableData) {
        mTableData = tableData;
    }

    protected void appendTableData(String appendTableData) {
        mTableData += appendTableData;
    }

    protected int getColumnIndexWithName(String name) {
        String tempList[] = getTableData().split(String.format(SQLHelper.SEPERATE, "[=|\\]]"))[1].split(String.format(SQLHelper.SEPERATE, ",") );
        for(int i = 0; i < tempList.length; i++) {
            if(tempList[i].split("'")[1].equals(name))
                return i;
        }
        return -1;
    }

    public int getColumnIndex(String name) {
        return getColumnIndexWithName(name);
    }

    protected int getColumnCount() {
        return getTableData().split(String.format(SQLHelper.SEPERATE, "[=|\\]]"))[1].split(String.format(SQLHelper.SEPERATE, ",") ).length;
    }

    protected String[] getColumns() {
        String tempList[] = getTableData().split(String.format(SQLHelper.SEPERATE, "[=|\\]]"))[1].split(String.format(SQLHelper.SEPERATE, ",") );
        for(int i = 0; i < tempList.length; i++) {
            tempList[i] = tempList[i].split("'")[1];
        }
        return tempList;
    }

    protected String[] getAIColumns() {
        String tempList[] = getTableData().split(String.format(SQLHelper.SEPERATE, "[=|\\]]"))[1].split(String.format(SQLHelper.SEPERATE, ",") );
        for(int i = 0; i < tempList.length; i++) {
            tempList[i] = tempList[i].split("'")[1];
            if(!(tempList[i].split("'").length == 3 && tempList[i].split("'")[2].contains(":ai")))
                tempList[i] = null;
        }
        int c = 0;
        for(int i = 0; i < tempList.length;i++)
            if(tempList[i] != null)
                c++;
        String newList[] = new String[c];
        c = 0;
        for(int i = 0; i < tempList.length;i++)
            if(tempList[i] != null) {
                newList[c] = tempList[i];
                c++;
            }
        return newList;
    }

/*
    public void addTableColumn(String columnName) {
        addTableColumn(columnName, false);
    }

    public void addTableColumn(String columnName, boolean isAi) {
        updateTableColumn("", columnName, isAi);
    }

    public void removeTableColumn(String columnName) {
        updateTableColumn(columnName, "" , false,false);
    }
*/
    public void updateTableColumn(String oldName, String newName) {
        updateTableColumn(oldName,newName,false,false);
    }

    public void updateTableColumn(String name, boolean isAi) {
        updateTableColumn(name,name,isAi,true);
    }

    public void updateTableColumn(String oldName, String newName,  boolean isAi) {
        updateTableColumn(oldName,newName,isAi,true);
    }

    private void updateTableColumn(String oldName, String newName,  boolean isAi, boolean makeAi) {
        SQLObject obj = new SQLObject();
        obj.add(mConfigDatabase.get("auto_increment").getColumnIndexWithName("column"),newName);
        System.out.println(obj.toString());

        boolean update = !oldName.equals("") && !newName.equals("");
        boolean remove = newName.equals("");
        boolean add = oldName.equals("");
        if(update) {
            mConfigDatabase.get("auto_increment")
                    .getCompiler()
                    .update(obj)
                    .where("column", oldName)
                    .run();
        }
        //Remove
        else if(remove) {
            mConfigDatabase.get("auto_increment")
                    .getCompiler()
                    .delete()
                    .where("column", oldName)
                    .run();
        }
        //Add
        else if(add && makeAi && isAi) {
            SQLObject obj2 = new SQLObject();
            obj2.add(mConfigDatabase.get("auto_increment").getColumnIndexWithName("database"),getDatabaseName());
            obj2.add(mConfigDatabase.get("auto_increment").getColumnIndexWithName("table"),getTableName());
            obj2.add(mConfigDatabase.get("auto_increment").getColumnIndexWithName("column"),newName);
            obj2.add(mConfigDatabase.get("auto_increment").getColumnIndexWithName("value"),Integer.toString(1));

            mConfigDatabase.get("auto_increment")
                    .getCompiler()
                    .insert(obj2).run();
        }

        if(update) {
            if(makeAi && !isAi) {
                mConfigDatabase.get("auto_increment")
                        .getCompiler()
                        .delete()
                        .where("column", oldName)
                        .run();
            }
            else if(makeAi) {
                @SuppressWarnings("all")
                ArrayList<String> item = mConfigDatabase.get("auto_increment")
                        .getCompiler()
                        .select("*")
                        .where("column", oldName)
                        .oneNoObject();

                if(item == null) {
                    SQLObject obj2 = new SQLObject();
                    obj2.add(mConfigDatabase.get("auto_increment").getColumnIndexWithName("database"),getDatabaseName());
                    obj2.add(mConfigDatabase.get("auto_increment").getColumnIndexWithName("table"),getTableName());
                    obj2.add(mConfigDatabase.get("auto_increment").getColumnIndexWithName("column"),newName);
                    obj2.add(mConfigDatabase.get("auto_increment").getColumnIndexWithName("value"),Integer.toString(1));

                    mConfigDatabase.get("auto_increment")
                            .getCompiler()
                            .insert(obj2).run();
                }
            }
        }


        String result = "['" + mTableName + "'=";
        String tempList[] = getTableData().split(String.format(SQLHelper.SEPERATE, "[=|\\]]"))[1].split(String.format(SQLHelper.SEPERATE, ",") );
        for(int i = 0; i < tempList.length; i++) {
            String item = tempList[i].split("'")[1];
            if(remove) {
                if(item.equals(oldName))
                    continue;
            }
            if(item.equals(oldName)) {
                item = newName;
                item = "'" + item + "'";
                if(makeAi && isAi)
                    item += ":ai";
                else if(!makeAi) {
                    if(tempList[i].split("'").length > 2 && !tempList[i].split("'")[2].equals(""))
                        item += ":ai";
                }
            }
            else {
                item = "'" + item + "'";
                if(tempList[i].split("'").length > 2 && !tempList[i].split("'")[2].equals(""))
                    item += ":ai";
            }
            result += item;
            if(i < tempList.length - 1)
                result += ",";
        }
        if(add) {
            result += ",'" + newName + "'";
            if(makeAi && isAi)
                result += ":ai";
        }
        result += "]" + getTableData().split(String.format(SQLHelper.SEPERATE, "[=|\\]]"))[2];

        mTableData = result;

        result = mDatabaseInstance.getDatabaseData().replaceAll(String.format(SQLHelper.SELECT_TABLE,getTableName()), "{" + getTableData() + "}");
        mDatabaseInstance.saveData(result);
    }

    public void changeTableName(String name) {
        SQLObject obj = new SQLObject();
        obj.add(mConfigDatabase.get("auto_increment").getColumnIndexWithName("table"),name);
        mConfigDatabase.get("auto_increment")
                .getCompiler()
                .update(obj)
                .where("table",getTableName())
                .run();

        String tempList[] = getTableData().split(String.format(SQLHelper.SEPERATE, "[=|\\]]"));
        String result = "['" + name + "'=" + tempList[1] + "]" + tempList[2];

        mTableData = result;

        result = mDatabaseInstance.getDatabaseData().replaceAll(String.format(SQLHelper.SELECT_TABLE,getTableName()), "{" + getTableData() + "}");
        mDatabaseInstance.saveData(result);
    }

    public void clearTable() {
        String tempList[] = getTableData().split(String.format(SQLHelper.SEPERATE, ";"));
        mTableData = tempList[0]+";";
        String result = mDatabaseInstance.getDatabaseData().replaceAll(String.format(SQLHelper.SELECT_TABLE,getTableName()), "{" + getTableData() + "}");
        mDatabaseInstance.saveData(result);
    }
}
