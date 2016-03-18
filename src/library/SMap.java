package library;
import java.lang.reflect.Array;
import java.util.Arrays;

@SuppressWarnings("all")
public class SMap<T> {
	Class<T> mObjectInstance;
	T[] mMap;
	
	public SMap(Class<T> object) {
		mObjectInstance= object;
		mMap = (T[]) Array.newInstance(mObjectInstance, 0);
	}

	public void add(int columnIndex, T value) {
		if(columnIndex >= mMap.length) {
			T[] newMap = (T[]) Array.newInstance(mObjectInstance, columnIndex + 1);
			for(int i = 0; i < mMap.length; i++)
				newMap[i] = mMap[i];
			mMap = newMap;
		}
		
		mMap[columnIndex] = value;
	}
	
	public T get(int columnIndex) {
		if(columnIndex >= mMap.length)
			return null;
		return mMap[columnIndex];
	}
	
	public int size() {
		return mMap.length;
	}
	
	public boolean contains(int columnIndex) {
		try {
			return mMap[columnIndex] != null;
		}
		catch(ArrayIndexOutOfBoundsException e) {
			return false;
		}
	}

	@Override
	public String toString() {
		return Arrays.toString(mMap);
	}
}
