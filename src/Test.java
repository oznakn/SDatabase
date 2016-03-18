import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

import library.*;

public class Test {

	public Test() {
		
	}
	
	@SColumn(name = "id",ai = true)
	public int id;
	
	@SColumn(name = "name") 
	public String name;
	
	public int notColumn;

	@SuppressWarnings("unchecked")
	public static void main(String args[]) {
		SQLHelper.registerFileManager(new SQLHelper.FileManager() {
			@Override
			public String getFileNameWithDatabase(String databaseName) {
				return databaseName + ".dat";
			}

			@Override
			public String readFile(String fileName) {
				FileReader in = null;

				try {
					in = new FileReader("E:\\" + fileName);
					String read = "";

					int c;
					while ((c = in.read()) != -1) {
						read += ((char) c);
					}

					in.close();
					return read;
				}
				catch (Exception e) {
					e.printStackTrace();
					createFile(fileName);
					return readFile(fileName);
				}
			}

			@Override
			public void writeToFile(String fileName, String data) {
				FileWriter out = null;

				try {
					out = new FileWriter("E:\\" + fileName);

					out.write(data);
					out.flush();
					out.close();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}

			@Override
			public void createFile(String fileName) {
				File f = new File(fileName);
				try {
					f.createNewFile();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		SQLHelper.open("test").get("peoples", Test.class)
				.getCompiler()
				.select("*")
			.callback(new SCompiler.GetCallback<Test>() {

				@Override
				public void onCallback(ArrayList<Test> list) {
					System.out.println(((Test)list.get(0)).name);
				}
				
			});
	}
}



