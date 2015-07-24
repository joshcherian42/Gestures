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

public class extract_features_dynamic_sliding {
	final static File old_folder = new File("data/Raw Data");
	final static File new_folder = new File("data/Raw Data Cleaned");
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
			String new_path = "data/dynamic_features_" + window_size + "_" + overlap_size + ".csv";
			CSVWriter writer = new CSVWriter(new FileWriter(new_path));

			/*Header*/
			String[] header = "FileName, Time, Number of Points, Average X, Average Y, Average Z, Number of Peaks X, Number of Peaks Y, Number of Peaks Z, Average Jerk X, Average Jerk Y, Average Jerk Z, Average XY, Average XZ, Average YZ, Previous Gesture, Gesture".split(",");
			writer.writeNext(header);

			String[] nextLine;
			int dyn_init = 0;
			
			//Running Averages
			HashMap<String, Double> inactive = new HashMap<>();
			HashMap<String, Double> raise_hand = new HashMap<>();
			HashMap<String, Double> lower_hand = new HashMap<>();
			HashMap<String, Double> brush_teeth = new HashMap<>();
			HashMap<String, Double> comb_hair = new HashMap<>();
			HashMap<String, Double> scratch_chin = new HashMap<>();
			HashMap<String, Double> draw = new HashMap<>();
			inactive = init_hashmap(inactive);
			raise_hand = init_hashmap(raise_hand);
			lower_hand = init_hashmap(lower_hand);
			brush_teeth = init_hashmap(brush_teeth);
			comb_hair = init_hashmap(comb_hair);
			scratch_chin = init_hashmap(scratch_chin);
			draw = init_hashmap(draw);
			
			HashMap<String, HashMap<String, Double>> gesture_averages = new HashMap<>();
			gesture_averages.put("inactive", inactive);
			gesture_averages.put("raise_hand", raise_hand);
			gesture_averages.put("lower_hand", lower_hand);
			gesture_averages.put("brush_teeth", brush_teeth);
			gesture_averages.put("comb_hair", comb_hair);
			gesture_averages.put("scratch_chin", scratch_chin);
			gesture_averages.put("draw", draw);


			for (File fileEntry : new_folder.listFiles()) {
				String cur_gesture = "";
				String prev_gesture = "Inactive";
				if (fileEntry.getName().endsWith(".csv")) {
					CSVReader reader = new CSVReader(new FileReader(fileEntry), ',' , '"' , 1);
					System.out.println(fileEntry);

					ArrayList<Double> time = new ArrayList<Double>();
					ArrayList<Integer> x = new ArrayList<Integer>();
					ArrayList<Integer> y = new ArrayList<Integer>();
					ArrayList<Integer> z = new ArrayList<Integer>();
					ArrayList<String> gestures = new ArrayList<String>();
					
//					ArrayList<Double> all_time = new ArrayList<Double>();
//					ArrayList<Integer> all_x = new ArrayList<Integer>();
//					ArrayList<Integer> all_y = new ArrayList<Integer>();
//					ArrayList<Integer> all_z = new ArrayList<Integer>();
//					ArrayList<String> all_gestures = new ArrayList<String>();
					
					int cur_length = 0;
					Double init_time = 0.0;
					Double last_time = 0.0;
					int shift_mult = 1;
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

								x.add(Integer.parseInt(nextLine[1]));
								y.add(Integer.parseInt(nextLine[2]));
								z.add(Integer.parseInt(nextLine[3]));
								gestures.add(nextLine[4]);

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
									gestures.remove(0);
									if (x.size() == 0 ) {
										time.add(Double.parseDouble(nextLine[0]));
										x.add(Integer.parseInt(nextLine[1]));
										y.add(Integer.parseInt(nextLine[2]));
										z.add(Integer.parseInt(nextLine[3]));
										gestures.add(nextLine[4]);
										nextLine = reader.readNext();
										while (init_time + (overlap_size*shift_mult) < cur_time) {
											shift_mult++;
										}
										shift_mult--;
										break;
									}
								}
								//int added = 0;
								while (cur_time <= init_time + (overlap_size*shift_mult) + window_size && nextLine != null) {
									time.add(Double.parseDouble(nextLine[0]));
									x.add(Integer.parseInt(nextLine[1]));
									y.add(Integer.parseInt(nextLine[2]));
									z.add(Integer.parseInt(nextLine[3]));
									gestures.add(nextLine[4]);
									nextLine = reader.readNext();
									if (nextLine != null) {
										cur_time = Double.parseDouble(nextLine[0]);
									}
									//added++;
								}
								shift_mult++;
								//check averages
								//if averages are within 10% add to current window if gesture is the same
								//if averages
								
//								int num_averages;
//								HashMap<String, Double> cur_gesturemap = new HashMap<>();
//								cur_gesturemap = gesture_averages.get(win_gesture(gestures));
//								Double average_x = average(x, cur_length);
//								Double average_y;
//								Double average_z;
//								Double num_peaks_x;
								
								//if (average_x*.9 <= )
								for (int c = 0; c < 11; c++) {
									
								}
							}
						} else {
							init_time = time.get(0);
//							all_x = x;
//							all_y = y;
//							all_z = z;
//							all_gestures = gestures;
//							all_time = time;
							gesture_averages.get(win_gesture(gestures)).put("Average X", average(x, cur_length));
							gesture_averages.get(win_gesture(gestures)).put("Average Y", average(y, cur_length));
							gesture_averages.get(win_gesture(gestures)).put("Average Z", average(z, cur_length));
							gesture_averages.get(win_gesture(gestures)).put("Number of Peaks X", (double) num_peaks(x, time, cur_length));
							gesture_averages.get(win_gesture(gestures)).put("Number of Peaks Y", (double) num_peaks(y, time, cur_length));
							gesture_averages.get(win_gesture(gestures)).put("Number of Peaks Z", (double) num_peaks(z, time, cur_length));
							gesture_averages.get(win_gesture(gestures)).put("Average Jerk X", avg_jerk(x, time, cur_length));
							gesture_averages.get(win_gesture(gestures)).put("Average Jerk Y", avg_jerk(y, time, cur_length));
							gesture_averages.get(win_gesture(gestures)).put("Average Jerk Z", avg_jerk(z, time, cur_length));
							gesture_averages.get(win_gesture(gestures)).put("Average XY", avg_diff(x, y, cur_length));
							gesture_averages.get(win_gesture(gestures)).put("Average XZ", avg_diff(x, z, cur_length));
							gesture_averages.get(win_gesture(gestures)).put("Average YZ", avg_diff(y, z, cur_length));
						}
						dyn_init = 1;

						cur_gesture = win_gesture(gestures);
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
								+ "," + prev_gesture
								+ "," + cur_gesture).split(",");
						prev_gesture = cur_gesture;
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

	/*Initialize gesture HashMap*/
	public static HashMap<String, Double> init_hashmap(HashMap<String, Double> gesture) {
		gesture.put("Average X", 0.0);
		gesture.put("Average Y", 0.0);
		gesture.put("Average Z", 0.0);
		gesture.put("Number of Peaks X", 0.0);
		gesture.put("Number of Peaks Y", 0.0);
		gesture.put("Number of Peaks Z", 0.0);
		gesture.put("Average Jerk X", 0.0);
		gesture.put("Average Jerk Y", 0.0);
		gesture.put("Average Jerk Z", 0.0);
		gesture.put("Average XY", 0.0);
		gesture.put("Average XZ", 0.0);
		gesture.put("Average YZ", 0.0);
		return gesture;
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
}