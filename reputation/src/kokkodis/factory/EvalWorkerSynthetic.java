package kokkodis.factory;

import java.util.HashMap;

public class EvalWorkerSynthetic {
	private int cluster1;
	private int cluster2;
	private int cluster0;
	private HashMap<Integer, ModelCategory> cluster3HistoryMap;
	private HashMap<Integer, ModelCategory> cluster0HistoryMap;
	private HashMap<Integer, ModelCategory> cluster1HistoryMap;
	private HashMap<Integer, ModelCategory> cluster2HistoryMap;

	public HashMap<Integer, ModelCategory> getCluster2HistoryMap() {
		return cluster2HistoryMap;
	}

	public HashMap<Integer, ModelCategory> getCluster0HistoryMap() {
		return cluster0HistoryMap;
	}

	public HashMap<Integer, ModelCategory> getCluster1HistoryMap() {
		return cluster1HistoryMap;
	}

	public HashMap<Integer, ModelCategory> getCluster3HistoryMap() {
		return cluster3HistoryMap;
	}

	public void increaseCluster0() {
		cluster0++;
	}

	public void increaseCluster1() {
		cluster1++;
	}

	public void increaseCluster2() {
		cluster2++;
	}

	public String getWorkerType() {
		if ((cluster1 == cluster2) && (cluster0 == 0) && (cluster2 == 0))
			return "unknown";
		if (cluster1 >= cluster2 && cluster1 >= cluster0)
			return "1";
		else if (cluster2 >= cluster1 && cluster2 >= cluster0)
			return "2";
		else
			return "0";
	}

	public EvalWorkerSynthetic() {
		cluster0 = 0;
		cluster1 = 0;
		cluster2 = 0;

		cluster2HistoryMap = new HashMap<Integer, ModelCategory>();

		cluster3HistoryMap = new HashMap<Integer, ModelCategory>();
		cluster0HistoryMap = new HashMap<Integer, ModelCategory>();
		cluster1HistoryMap = new HashMap<Integer, ModelCategory>();
	}
}
