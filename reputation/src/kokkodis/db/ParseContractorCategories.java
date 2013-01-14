package kokkodis.db;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.Map.Entry;

import kokkodis.utils.Counter;

public class ParseContractorCategories {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			String f = "/Users/mkokkodi/git/reputationResults/raw/contractor_categories";
			BufferedReader input = new BufferedReader(new FileReader(f));
			String line;
			line = input.readLine();

			HashMap<String, HashSet<String>> hm = new HashMap<String, HashSet<String>>();
			Counter<String> c = new Counter<String>();
			while ((line = input.readLine()) != null) {
				String[] tmpAr = line.split(",");
				if (tmpAr[1].length() > 1) {
					HashSet<String> l = hm.get(tmpAr[0]);
					if (l == null) {
						l = new HashSet<String>();
						hm.put(tmpAr[0], l);
					}

					l.add(tmpAr[1]);
				}
			}
			input.close();
			for (Entry<String, HashSet<String>> e : hm.entrySet()) {
				Object[] o = e.getValue().toArray();
				String key = "";
				for (int i = 0; i < o.length; i++) {
					for (int j = i + 1; j < o.length; j++) {
						for (int k = j + 1; k < o.length; k++) {
							key = o[i].toString() + "_" + o[j].toString() + "_"
									+ o[k].toString();
							c.incrementCount(key, 1);
							key = "";
						}
					}

				}

				// for(String s:e.getValue() )
				// key+="_"+s;

			}
			TreeMap<Double,String> ts = new TreeMap<Double, String>();
			
			for (Entry<String, Double> e : c.getEntrySet())
				ts.put(e.getValue()+Math.random(),e.getKey());
			
			
			for(Entry<Double,String> e: ts.entrySet())
				System.out.println(e.getKey() + " " + e.getValue());
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
