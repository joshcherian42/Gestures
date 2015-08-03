import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

public class extract_features {
	final static File old_folder = new File("data/Testing");
	final static File new_folder = new File("data/Testing");
	final static double window_size = 2;
	final static double overlap_size = 1;

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
			String new_path = "data/Testing/sliding_features_" + window_size + "_testing_" + overlap_size + ".csv";
			CSVWriter writer = new CSVWriter(new FileWriter(new_path));

			/*Header*/
			String[] header = "FileName, Time, Number of Points, Average X, Average Y, Average Z, Number of Peaks X, Number of Peaks Y, Number of Peaks Z, Average Jerk X, Average Jerk Y, Average Jerk Z, Average XY, Average XZ, Average YZ, Standard Deviation X, Standard Deviation Y, Standard Deviation Z, Zero-Crossings X, Zero-Crossings Y, Zero-Crossings Z, Correlation XY, Correlation XZ, Correlation YZ, Energy X, Energy Y, Energy Z, Entropy X, Entropy Y, Entropy Z".split(",");
			writer.writeNext(header);

			String[] nextLine;
			int dyn_init = 0; //
			for (File fileEntry : new_folder.listFiles()) {
				String cur_gesture = "";
				String prev_gesture = "Inactive";
				if (fileEntry.getName().endsWith(".csv")) {
					CSVReader reader = new CSVReader(new FileReader(fileEntry), ',' , '"' , 1);
					ArrayList<Double> time = new ArrayList<Double>();
					int cur_length = 0;
					Double init_time = 0.0;
					Double last_time = 0.0;
					int shift_mult = 1;
					System.out.println(fileEntry);
					ArrayList<Double> x = new ArrayList<Double>();
					ArrayList<Double> y = new ArrayList<Double>();
					ArrayList<Double> z = new ArrayList<Double>();
					//ArrayList<String> gestures = new ArrayList<String>();
					nextLine = reader.readNext();

					while (nextLine != null) {
						Double cur_time = Double.parseDouble(nextLine[0]);
						Double prev_time;
						Double total_time = 0.0;

						//first window_size
						while (total_time < window_size && dyn_init == 0) {
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
								
								x.add(Double.parseDouble(nextLine[1]));
								y.add(Double.parseDouble(nextLine[2]));
								z.add(Double.parseDouble(nextLine[3]));
								//gestures.add(nextLine[4]);


								if (total_time >= window_size) {
									last_time = time.get(cur_length);
									nextLine = reader.readNext();
								}
								cur_length++;
							} else {
								break;
							}
							if (total_time < window_size) {
								nextLine = reader.readNext();
							}
						} //end initial second

						if (dyn_init == 1) {

							if (nextLine != null) {
								while (time.get(0) < init_time + (overlap_size*shift_mult)) {
									time.remove(0);
									x.remove(0);
									y.remove(0);
									z.remove(0);
									//gestures.remove(0);
									if (x.size() == 0 ) {
										time.add(Double.parseDouble(nextLine[0]));
										x.add(Double.parseDouble(nextLine[1]));
										y.add(Double.parseDouble(nextLine[2]));
										z.add(Double.parseDouble(nextLine[3]));
										//gestures.add(nextLine[4]);
										nextLine = reader.readNext();
										while (init_time + (overlap_size*shift_mult) < cur_time) {
											shift_mult++;
										}
										shift_mult--;
										break;
									}
								}

								while (cur_time <= init_time + (overlap_size*shift_mult) + window_size && nextLine != null) {
									time.add(Double.parseDouble(nextLine[0]));
									x.add(Double.parseDouble(nextLine[1]));
									y.add(Double.parseDouble(nextLine[2]));
									z.add(Double.parseDouble(nextLine[3]));
									//gestures.add(nextLine[4]);
									nextLine = reader.readNext();
									if (nextLine != null) {
										cur_time = Double.parseDouble(nextLine[0]);
									}
								}
								shift_mult++;
							}
						} else {
							init_time = time.get(0);
						}
						dyn_init = 1;

						//cur_gesture = win_gesture(gestures);
						String[] cur_features = (String.valueOf(fileEntry)
								+ "," + String.valueOf(time.get(time.size() - 1) - time.get(0))
								+ "," + String.valueOf(time.size())
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
								+ "," + String.valueOf(avg_diff(y, z, cur_length))
								+ "," + String.valueOf(stdev(x))
								+ "," + String.valueOf(stdev(y))
								+ "," + String.valueOf(stdev(z))
								+ "," + String.valueOf(z_crossings(x))
								+ "," + String.valueOf(z_crossings(y))
								+ "," + String.valueOf(z_crossings(z))
								+ "," + String.valueOf(sig_corr(x,y))
								+ "," + String.valueOf(sig_corr(x,z))
								+ "," + String.valueOf(sig_corr(y,z))
								+ "," + String.valueOf(energy(x))
								+ "," + String.valueOf(energy(y))
								+ "," + String.valueOf(energy(z))
								+ "," + String.valueOf(entropy(x))
								+ "," + String.valueOf(entropy(y))
								+ "," + String.valueOf(entropy(z))).split(",");
								//+ "," + prev_gesture
								//+ "," + cur_gesture).split(",");
						//prev_gesture = cur_gesture;
						writer.writeNext(cur_features);
						total_time = 0.0;
					}
					dyn_init = 0;
				}
			}
			writer.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	/*Find most common Gesture in window*/
	public static String win_gesture (ArrayList<String> gestures) {
		String gesture = "Inactive";
		HashMap<String, Integer> gestures_cnt = new HashMap<>();
		for (int c = 0; c < gestures.size(); c++) {
			if (gestures_cnt.containsKey(gestures.get(c))) {
				gestures_cnt.put(gestures.get(c),gestures_cnt.get(gestures.get(c))+1);
			} else {
				gestures_cnt.put(gestures.get(c), 1);
			}
		}
		for (String key : gestures_cnt.keySet()){
			int max_value = 0;
			int cur_value = gestures_cnt.get(key);
			if (cur_value > max_value) {
				max_value = cur_value;
				gesture = key;
			}
		}
		return gesture;
	}



	/**************************************************** 
	 ** 					Features					**
	 ****************************************************/

	/*
	 * Average
	 * Number of Peaks
	 * Average Jerk
	 * Average Distance Between Values
	 * Energy
	 * Frequency-domain entropy
	 * Zero-Crossings
	 * Signal Correlation
	 * Standard Deviation
	 */


	//Run this shit for both filtered and unfiltered data

	/*Generate Average*/
	public static Double average(ArrayList<Double> x, int cur_length) {
		Double avg = 0.0;
		for (int cnt = 0; cnt < x.size(); cnt++) {
			avg += x.get(cnt);
		}
		return avg/cur_length;
	}

	/*Find Number of Peaks*/
	public static int num_peaks(ArrayList<Double> x, ArrayList<Double> time, int cur_length) {
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
	public static Double avg_jerk(ArrayList<Double> x, ArrayList<Double> time, int cur_length) {
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
	public static Double avg_diff(ArrayList<Double> x, ArrayList<Double> y, int cur_length) {
		Double diff = 0.0;
		for (int cnt = 0; cnt < x.size(); cnt++) {
			diff += x.get(cnt) - y.get(cnt);
		}
		return diff/cur_length;
	}

	/*Find Energy*/
	public static Double energy(ArrayList<Double> x) {
		
		int N = x.size();
		double angle;
		double energy = 0;
		for (int k = 0; k < N; k++){
			double ak = 0;
			double bk = 0;
			for (int i = 0; i < N; i++) {
				angle = 2*Math.PI*i*k/N;
				ak += x.get(i)*Math.cos(angle);
				bk += -x.get(i)*Math.sin(angle);
			}
			energy += (Math.pow(ak, 2)+Math.pow(bk, 2))/N;
	    }
		return energy;

	}

	/*Find Entropy*/
	public static Double entropy(ArrayList<Double> x) {
		int N = x.size();
		double angle;
		double spectralentropy = 0;
		for (int j = 0; j < N; j++){
			double ak = 0;
			double bk = 0;
			double aj = 0;
			double bj = 0;
			double mag_j = 0;
			double mag_k = 0;
			double cj = 0;
			
			for (int i = 0; i < N; i++) {
				angle = 2*Math.PI*i*j/N;
				ak = x.get(i)*Math.cos(angle); //Real
				bk = -x.get(i)*Math.sin(angle); //Imaginary
				aj+=ak;
				bj+=bk;
				
				mag_k += Math.sqrt(Math.pow(ak, 2)+Math.pow(bk, 2));
			}
			mag_j = Math.sqrt(Math.pow(aj, 2)+Math.pow(bj, 2));
			
			cj = mag_j/mag_k;
			
			spectralentropy += cj*Math.log(cj)/Math.log(2);
	    }
		return -spectralentropy;

	}

	/*Find Zero Crossings*/
	public static int z_crossings(ArrayList<Double> x) {
		int cur_sign;
		int prev_sign = 0; 
		int sign;
		int cnt = 0;
		int crossings = 0;
		while (prev_sign == 0 && cnt < x.size()-1) {
			prev_sign = Long.signum(x.get(cnt).longValue());
			cnt++;
		}
		if (prev_sign == 0) {
			return crossings;
		}
		while (cnt < x.size()) {
			cur_sign = Long.signum(x.get(cnt).longValue());
			while (cur_sign == 0 && cnt < x.size()-1) {
				cnt++;
				cur_sign = Long.signum(x.get(cnt).longValue());
			}
			if (cur_sign == 0) { //the last value was zero, so no more crossings will occur
				break;
			}
			sign = cur_sign - prev_sign;
			switch (sign) {
			case 2: //1-(-1)
				crossings++;
				break;
			case 0: //1-(+1), -1-(-1)
				break;
			case -2: //-1-(+1)
				crossings++;
				break;
			}
			prev_sign = cur_sign;
			cnt++;
		}
		
		return crossings;

	}


	/*Find Signal Correlation*/
	public static Double sig_corr(ArrayList<Double> x, ArrayList<Double> y) {
		double correlation = 0;
		int N = x.size();
		for (int cnt = 0; cnt < N; cnt++) {
			correlation += x.get(cnt) * y.get(cnt);
		}
		return correlation/N;

	}

	/*Find Standard Deviation*/
	public static Double stdev(ArrayList<Double> x) {
		int N = x.size();
		double avg = average(x, N);
		double std = 0;
		for (int i = 0; i < N; i++) {
			std +=Math.pow((x.get(i) - avg),2);
		}
		return std/N;

	}
}