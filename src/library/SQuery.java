package library;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//Language for SDatabase
public class SQuery {

    private String mQuery;
    private SDatabase mDatabase;
    private SDatabase mConfigDatabase;
    private OnFinishCallback mCallback;

    protected SQuery(SDatabase configDatabase, SDatabase database, String query, OnFinishCallback callback) {
        mConfigDatabase = configDatabase;
        mDatabase = database;
        mQuery = query;
        mCallback = callback;

        Matcher matcher = Pattern.compile(String.format(SQLHelper.SEPERATE , "[a-z]")).matcher(mQuery);
        while(matcher.find()) {
            String data = mQuery.substring(matcher.start(), matcher.end());
            data = data.toUpperCase();
            if(data.charAt(0) == ((char)(304)))
                data = "I";
            if(data.charAt(0) == ((char)(305)))
                data = "i";
            mQuery = mQuery.substring(0, matcher.start()) + data + mQuery.substring(matcher.end() , mQuery.length());
        }
    }

    public void run() {
        System.out.println(mQuery);
        if(mQuery.indexOf("SELECT") == 0) {
            select();
        }
        else if(mQuery.indexOf("INSERT") == 0) {
            insert();
        }
        else if(mQuery.indexOf("UPDATE") == 0) {
            update();
        }
        else if(mQuery.indexOf("DELETE") == 0) {
            delete();
        }
    }

    public void select() {
        String[] parts = mQuery.split(String.format(SQLHelper.SEPERATE, "(WHERE|LIMIT)"));

        String[] partsForSelect = parts[0].split(" ");

        String tableName = partsForSelect[partsForSelect.length-1].trim();
        tableName = tableName.substring(1,tableName.length()-1);

        String[] selects = partsForSelect[1].split(",");
        for(int i = 0; i < selects.length; i++)
            if(selects[i].length() > 2)
                selects[i] = selects[i].substring(1,selects[i].length()-1);
        partsForSelect = null;

        STable table = mDatabase.get(tableName);
        SCompiler compiler = table.getCompiler();

        SMap<String> whereMap = new SMap<>(String.class);
        int limit = -1;
        if(Pattern.compile(String.format(SQLHelper.SEPERATE, "(WHERE)")).matcher(mQuery).find()) {
            String[] partsForWhere = parts[1].split(String.format(SQLHelper.SEPERATE , "AND"));

            for(int i = 0; i < partsForWhere.length; i++) {
                String[] partsOfWhere = partsForWhere[i].split("=");

                partsOfWhere[0] = partsOfWhere[0].trim().substring(1,partsOfWhere[0].trim().length()-1);
                partsOfWhere[1] = partsOfWhere[1].trim().substring(1,partsOfWhere[1].trim().length()-1);

                whereMap.add(table.getColumnIndexWithName(partsOfWhere[0]),partsOfWhere[1]);
                partsOfWhere = null;
            }
            partsForWhere = null;
        }


        if(Pattern.compile(String.format(SQLHelper.SEPERATE, "(LIMIT)")).matcher(mQuery).find()) {
            String item = parts[parts.length-1].trim();
            try {
                limit = Integer.parseInt(item);
            }
            catch (Exception e) {
                e.printStackTrace();
                limit = -1;
            }
        }

        compiler.select(selects);
        for(int i = 0; i< whereMap.size(); i++) {
            if(whereMap.contains(i)) {
                compiler.where(i,whereMap.get(i));
            }
        }
        if(limit > -1)
            compiler.limit(limit);

        if(mCallback != null)
            mCallback.onFinish(compiler.getNoObject());
    }

    public void insert() {
        String[] parts = mQuery.split(String.format(SQLHelper.SEPERATE, "(INSERT|VALUES)"));

        if(parts.length == 3) {
            String tableName = parts[1].substring(0,parts[1].indexOf("(")).trim();
            tableName = tableName.split(" ")[tableName.split(" ").length-1];
            tableName = tableName.trim().substring(1,tableName.trim().length()-1);

            STable table = mDatabase.get(tableName);
            SCompiler compiler = table.getCompiler();

            parts[1] = parts[1].substring(parts[1].indexOf("("),parts[1].length());
            parts[1] = parts[1].trim().substring(1,parts[1].trim().length()-1);


            String[] columns = parts[1].split(",");
            for(int i = 0; i < columns.length; i++)
                columns[i] = columns[i].trim().substring(1,columns[i].trim().length()-1);


            parts[2] = parts[2].trim().substring(1,parts[2].trim().length()-1);

            String[] items = parts[2].split(",");
            for(int i = 0; i < items.length; i++)
                items[i] = items[i].trim().substring(1,items[i].trim().length()-1);

            SQLObject object = new SQLObject();
            if(columns.length == items.length) {
                for(int i = 0; i < columns.length; i++) {
                    object.add(table.getColumnIndexWithName(columns[i]), items[i]);
                }
            }
            compiler.insert(object).run();

            if(mCallback != null)
                mCallback.onFinish();
        }
    }

    public void delete() {
        String[] parts = mQuery.split(String.format(SQLHelper.SEPERATE, "(WHERE)"));

        String[] partsForDelete = parts[0].split(" ");

        String tableName = partsForDelete[partsForDelete.length-1].trim();
        tableName = tableName.substring(1,tableName.length()-1);
        partsForDelete = null;

        STable table = mDatabase.get(tableName);
        SCompiler compiler = table.getCompiler();

        SMap<String> whereMap = new SMap<>(String.class);
        if(Pattern.compile(String.format(SQLHelper.SEPERATE, "(WHERE)")).matcher(mQuery).find()) {
            String[] partsForWhere = parts[1].split(String.format(SQLHelper.SEPERATE , "AND"));

            for(int i = 0; i < partsForWhere.length; i++) {
                String[] partsOfWhere = partsForWhere[i].split("=");

                partsOfWhere[0] = partsOfWhere[0].trim().substring(1,partsOfWhere[0].trim().length()-1);
                partsOfWhere[1] = partsOfWhere[1].trim().substring(1,partsOfWhere[1].trim().length()-1);

                whereMap.add(table.getColumnIndexWithName(partsOfWhere[0]),partsOfWhere[1]);
                partsOfWhere = null;
            }
            partsForWhere = null;
        }

        compiler.delete();
        for(int i = 0; i< whereMap.size(); i++) {
            if(whereMap.contains(i)) {
                compiler.where(i,whereMap.get(i));
            }
        }

        compiler.run();
        if(mCallback != null)
            mCallback.onFinish();
    }

    public void update() {
        String[] parts = mQuery.split(String.format(SQLHelper.SEPERATE, "(SET|WHERE)"));

        String[] partsForUpdate = parts[0].split(" ");

        String tableName = partsForUpdate[partsForUpdate.length-1].trim();
        tableName = tableName.substring(1,tableName.length()-1);
        partsForUpdate = null;

        STable table = mDatabase.get(tableName);
        SCompiler compiler = table.getCompiler();

        SMap<String> setMap = new SMap<>(String.class);
        if(Pattern.compile(String.format(SQLHelper.SEPERATE, "(SET)")).matcher(mQuery).find()) {
            String[] partsForSet = parts[1].split(String.format(SQLHelper.SEPERATE , ","));

            for(int i = 0; i < partsForSet.length; i++) {
                String[] partsOfSet = partsForSet[i].trim().split("=");

                partsOfSet[0] = partsOfSet[0].trim().substring(1,partsOfSet[0].trim().length()-1);
                partsOfSet[1] = partsOfSet[1].trim().substring(1,partsOfSet[1].trim().length()-1);

                setMap.add(table.getColumnIndexWithName(partsOfSet[0]),partsOfSet[1]);
                partsOfSet = null;
            }
            partsForSet = null;
        }

        SMap<String> whereMap = new SMap<>(String.class);
        if(Pattern.compile(String.format(SQLHelper.SEPERATE, "(WHERE)")).matcher(mQuery).find()) {
            String[] partsForWhere = parts[2].split(String.format(SQLHelper.SEPERATE , "AND"));

            for(int i = 0; i < partsForWhere.length; i++) {
                String[] partsOfWhere = partsForWhere[i].split("=");

                partsOfWhere[0] = partsOfWhere[0].trim().substring(1,partsOfWhere[0].trim().length()-1);
                partsOfWhere[1] = partsOfWhere[1].trim().substring(1,partsOfWhere[1].trim().length()-1);

                whereMap.add(table.getColumnIndexWithName(partsOfWhere[0]),partsOfWhere[1]);
                partsOfWhere = null;
            }
            partsForWhere = null;
        }

        compiler.update(setMap);
        for(int i = 0; i< whereMap.size(); i++) {
            if(whereMap.contains(i)) {
                compiler.where(i,whereMap.get(i));
            }
        }

        compiler.run();
        if(mCallback != null)
            mCallback.onFinish();
    }

    public static interface OnFinishCallback {
        void onFinish();

        void onFinish(ArrayList<ArrayList<String>> list);
    }
}
