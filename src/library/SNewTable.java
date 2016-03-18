package library;

import java.util.ArrayList;

public class SNewTable {

    private boolean mHasIndex = false;
    private String mTableName = null;
    private ArrayList<SNewColumn> mColumns = new ArrayList<>();

    public SNewTable() {}

    public SNewTable(String tableName) {
        this(tableName, false);
    }


    public SNewTable(String tableName , boolean hasIndex) {
        mTableName = tableName;
        mHasIndex = hasIndex;
    }

    public void setTableName(String tableName) {
        mTableName = tableName;
    }

    public String getTableName() {
        return mTableName;
    }

    protected boolean getHasIndex() {
        return mHasIndex;
    }

    protected ArrayList<SNewColumn> getColumns() {
        return mColumns;
    }

    public void addColumn(SNewColumn column) {
        mColumns.add(column);
    }

    public static class SNewColumn {
        protected int mIndex = 0;
        protected String mName;
        protected boolean mIsAi = false;

        public SNewColumn(String name) {
            this(0,name,false);
        }

        public SNewColumn(int index, String name) {
            this(index,name,false);
        }

        public SNewColumn(String name, boolean isAi) {
            this(0,name,isAi);
        }

        public SNewColumn(int index, String name, boolean isAi) {
            mIndex = index;
            mName = name;
            mIsAi = isAi;
        }

        public SNewColumn index(int index) {
            mIndex = index;
            return this;
        }

        public SNewColumn name(String name) {
            mName = name;
            return this;
        }

        public SNewColumn isAi(boolean isAi) {
            mIsAi = isAi;
            return this;
        }
    }
}
