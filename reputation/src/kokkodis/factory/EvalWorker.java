package kokkodis.factory;

import java.util.HashMap;

public class EvalWorker {

	private int workerId;
	public int getWorkerId() {
		return workerId;
	}

	public void setWorkerId(int workerId) {
		this.workerId = workerId;
	}

	private int technical;
	private int nonTech;
	private HashMap<Integer, ModelCategory> genericHistoryMap;
	private HashMap<Integer, ModelCategory> technicalHistoryMap;
	private HashMap<Integer, ModelCategory> nonTechHistoryMap;

	public HashMap<Integer, ModelCategory> getTechnicalHistoryMap() {
		return technicalHistoryMap;
	}

	public HashMap<Integer, ModelCategory> getNonTechHistoryMap() {
		return nonTechHistoryMap;
	}

	public HashMap<Integer, ModelCategory> getGenericHistoryMap() {
		return genericHistoryMap;
	}

	public void increaseTech() {
		technical++;
	}

	public void increaseNonTech() {
		nonTech++;
	}

	public String getWorkerType() {
		if ((technical == nonTech) && (nonTech == 0))
			return "r";
		if (technical >= nonTech)
			return "rr";
		else
			return "rl";
	}

	public EvalWorker() {
		technical = 0;
		nonTech = 0;
		genericHistoryMap = new HashMap<Integer, ModelCategory>();
		technicalHistoryMap = new HashMap<Integer, ModelCategory>();
		nonTechHistoryMap = new HashMap<Integer, ModelCategory>();
	}

}
