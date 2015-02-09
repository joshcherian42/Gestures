import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

public class extract_features {
	final static File old_folder = new File("data/Raw Data");
	final static File new_folder = new File("data/Raw Data Cleaned");

	public static void main (String [] args) {
		if (!(new_folder.exists())) {
			new_folder.mkdir();
			clean_files();
			generate_features();
		} else {
			generate_features();
		}
	}

	public static void clean_files() {
		for (File fileEntry : old_folder.listFiles()) {

			CSVReader reader;
			try {
				reader = new CSVReader(new FileReader(fileEntry), ',' , '"' , 0);

				String[] nextLine=reader.readNext();
				String new_path = new_folder.getPath() + "/" + fileEntry.getName();
				
				/*Not Cleaned*/
				if (nextLine.length == 5) {
					CSVWriter writer = new CSVWriter(new FileWriter(new_path));

					/*Header*/
					String[] header = "Time(s), X, Y, Z".split(",");
					writer.writeNext(header);

					/*First Entry*/
					String milliseconds = nextLine[1];
					if (Integer.parseInt(milliseconds) < 10) {
						milliseconds = ".00" + milliseconds;
					} else if (Integer.parseInt(milliseconds) < 100) {
						milliseconds = ".0" + milliseconds;
					} else {
						milliseconds = "." + milliseconds;
					}
					milliseconds = nextLine[0] + milliseconds;
					String[] entry = (milliseconds + "," + nextLine[2] + "," + nextLine[3] + "," + nextLine[4]).split(",");
					writer.writeNext(entry);

					while ((nextLine = reader.readNext()) != null) {
						if (nextLine != null) {
							milliseconds = nextLine[1];
							if (Integer.parseInt(milliseconds) < 10) {
								milliseconds = ".00" + milliseconds;
							} else if (Integer.parseInt(milliseconds) < 100) {
								milliseconds = ".0" + milliseconds;
							} else {
								milliseconds = "." + milliseconds;
							}
							milliseconds = nextLine[0] + milliseconds;
							entry = (milliseconds + "," + nextLine[2] + "," + nextLine[3] + "," + nextLine[4]).split(",");
							writer.writeNext(entry);
						}
					}
					writer.close();
				} 
				
				/*Cleaned*/
				else {
					String old_path = old_folder.getPath() + "/" + fileEntry.getName();
					File new_file = new File (new_path);
					new_file.createNewFile();
					FileInputStream fis = null;
					FileOutputStream fos = null;
					FileChannel cis = null;
					FileChannel cos = null;

					long len = 0, pos = 0;

					try {
						fis = new FileInputStream(old_path);
						cis = fis.getChannel();
						fos = new FileOutputStream(new_path);
						cos = fos.getChannel();
						len = cis.size();
						while (pos < len) {
							pos += cis.transferTo(pos, (1024 * 1024 * 10), cos);
						}
						fos.flush();
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						if (cos != null) { try { cos.close(); } catch (Exception e) { } }
						if (fos != null) { try { fos.close(); } catch (Exception e) { } }
						if (cis != null) { try { cis.close(); } catch (Exception e) { } }
						if (fis != null) { try { fis.close(); } catch (Exception e) { } }
					}
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	@SuppressWarnings("resource")
	public static void generate_features () {
		try {
			String new_path = "data/features.csv";
			CSVWriter writer = new CSVWriter(new FileWriter(new_path));
			
			/*Header*/
			String[] header = "FileName, Time, Average X, Average Y, Average Z, Number of Peaks X, Number of Peaks Y, Number of Peaks Z, Average Jerk X, Average Jerk Y, Average Jerk Z, Average XY, Average XZ, Average YZ ".split(",");
			writer.writeNext(header);
			
			String[] nextLine;
			for (File fileEntry : new_folder.listFiles()) {

				CSVReader reader = new CSVReader(new FileReader(fileEntry), ',' , '"' , 1);
				ArrayList<Double> time = new ArrayList<Double>();
				int cur_length = 0;
				Double last_time = 0.0;

				while ((nextLine = reader.readNext()) != null) {
					ArrayList<Integer> x = new ArrayList<Integer>();
					ArrayList<Integer> y = new ArrayList<Integer>();
					ArrayList<Integer> z = new ArrayList<Integer>();
					Double cur_time = Double.parseDouble(nextLine[0]);
					Double prev_time;
					Double total_time = 0.0;
					
					//break into 2 second intervals
					while (total_time < 2.0) {
						
						if (nextLine != null) {
							cur_time = Double.parseDouble(nextLine[0]);

							time.add(Double.parseDouble(nextLine[0]));

							//first time or not
							if (total_time == 0 && cur_length == 0) {
								prev_time = time.get(cur_length);

							} else if (total_time == 0 && last_time != 0.0){
								prev_time = last_time;
							}
							else {
								prev_time = time.get(cur_length - 1);
							}
							total_time = total_time + (cur_time - prev_time);

							x.add(Integer.parseInt(nextLine[1]));
							y.add(Integer.parseInt(nextLine[2]));
							z.add(Integer.parseInt(nextLine[3]));

							if (total_time >= 2.0) {
								last_time = time.get(cur_length);
							}
							cur_length++;
						} else {
							break;
						}
						if (total_time < 2.0) {
							nextLine = reader.readNext();
						}
					}

					String[] cur_features = (String.valueOf(fileEntry)
							+ "," + String.valueOf(total_time) 
							+ "," + String.valueOf(average(x, cur_length)) 
							+ "," + String.valueOf(average(y, cur_length))
							+ "," + String.valueOf(average(z, cur_length))
							+ "," + String.valueOf(num_peaks(x, time, cur_length))
							+ "," + String.valueOf(num_peaks(y, time, cur_length))
							+ "," + String.valueOf(num_peaks(z, time, cur_length))
							+ "," + String.valueOf(avg_jerk(x, time, cur_length))
							+ "," + String.valueOf(avg_jerk(y, time, cur_length))
							+ "," + String.valueOf(avg_jerk(z, time, cur_length))
							+ "," + String.valueOf(avg_diff(x, y, cur_length))
							+ "," + String.valueOf(avg_diff(x, z, cur_length))
							+ "," + String.valueOf(avg_diff(y, z, cur_length))).split(",");
					writer.writeNext(cur_features);
					total_time = 0.0;
				}
			}
			writer.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*Generate Average*/
	public static Double average(ArrayList<Integer> x, int cur_length) {
		Double avg = 0.0;
		for (int cnt = 0; cnt < x.size(); cnt++) {
			avg += x.get(cnt);
		}
		return avg/cur_length;
	}
	
	/*Find Number of Peaks*/
	public static int num_peaks(ArrayList<Integer> x, ArrayList<Double> time, int cur_length) {
		int peaks = 0;

		try {
			Double prev_slope = (((double)x.get(1) - (double)x.get(0)))/(time.get(1) - time.get(0));

			Double cur_slope;
			for (int cnt = 1; cnt < x.size(); cnt++) {
				cur_slope = ((x.get(cnt) - x.get(cnt-1)))/(time.get(cnt) - time.get(cnt-1));

				if (prev_slope > 0 && cur_slope < 0) {
					peaks++;
				}
				prev_slope = cur_slope;
			}
		} catch (IndexOutOfBoundsException e) {
			//no peaks if only point available
		}

		return peaks;
	}

	/*Find Average Jerk*/
	public static Double avg_jerk(ArrayList<Integer> x, ArrayList<Double> time, int cur_length) {
		Double jerk = 0.0;
		int cnt;
		
		for (cnt = 1; cnt < x.size(); cnt++) {
			jerk += (((double)x.get(cnt) - (double)x.get(cnt-1)))/(time.get(cnt) - time.get(cnt-1));
		}
		if (cnt == 1) {
			return jerk/cnt;
		} else {
			return jerk/(cnt-1);

		}
	}

	/*Find Average Distance Between Each Value*/
	public static Double avg_diff(ArrayList<Integer> x, ArrayList<Integer> y, int cur_length) {
		Double diff = 0.0;
		for (int cnt = 0; cnt < x.size(); cnt++) {
			diff += x.get(cnt) - y.get(cnt);
		}
		return diff/cur_length;
	}
}
