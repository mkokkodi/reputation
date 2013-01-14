package kokkodis.synthetic;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

public class CreateHierarchies {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			BufferedReader input = new BufferedReader(
					new FileReader(
							"/Users/mkokkodi/git/kdd12/kdd12/results/synthetic/temp/transitionProbsCat9.csv"));
			String line;
			line = input.readLine();
			HashMap<Integer, TreeMap<Double, Integer>> hm = new HashMap<Integer, TreeMap<Double, Integer>>();
			int cat=1;
			while ((line = input.readLine()) != null) {
				String[] tmpAr = line.split("\\s+");
				TreeMap<Double,Integer > tm = new TreeMap<Double, Integer>(new Comparator<Double>() {

					@Override
					public int compare(Double o1, Double o2) {
						// TODO Auto-generated method stub
						return -o1.compareTo(o2);
					}
				});
				int innerCat = 1;
				for (String s : tmpAr) {
					tm.put(Double.parseDouble(s.trim()),innerCat);
					innerCat++;
				}
				hm.put(cat,tm);
				cat++;
			}
			for(Entry<Integer, TreeMap<Double, Integer>> e: hm.entrySet()){
				System.out.println("Cat "+ e.getKey());
				for(Entry<Double,Integer> innerEntry: e.getValue().entrySet()){
					System.out.println(innerEntry.getKey()+","+innerEntry.getValue());
				}
				System.out.println("--------------------------");
			}
		} catch (IOException e) {
		}

	}

}
