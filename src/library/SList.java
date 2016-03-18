package library;
import java.util.ArrayList;
import java.util.Arrays;

@SuppressWarnings("all")
public class SList<T> extends ArrayList<T> {

	public boolean exists(int i) {
		try {
			if(get(i) != null)
				return true;
		}
		catch(Exception e) {
			
		}
		return false;
	}

	@Override
	public String toString() {
		return Arrays.toString(this.toArray());
	}
}