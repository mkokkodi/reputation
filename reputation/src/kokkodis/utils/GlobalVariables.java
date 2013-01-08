package kokkodis.utils;

import java.util.HashMap;
import java.util.Properties;

import kokkodis.factory.PropertiesFactory;

/*
 * Global Variables for Train, Regressions and Test
 */

public class GlobalVariables {

	private static GlobalVariables globaleVars = null;

	public static String curCluster;
	public static String curApproach;
	public static String curModel;
	public static String line = "-----------------------------------------------------";
	public static double K;
	public static float currentBinomialThreshold;
	public static int rsTrials;
	public static PrintToFile allResultsFile;
	public static PrintToFile predictions;
	public static boolean printFiles = false;
	public static boolean hierarchicalFlag = false;
	public static boolean evaluateOnTrain=false;
	public static boolean outputPredictions = false;
	

	/**
	 * Cluster -> cat_id based_x on cats -> (cat in cats) coeffs
	 */
	private static HashMap<String, HashMap<String, HashMap<Integer, Double>>> curCoeffs;

	public static double[] qualities;
	public static double[] binomialPrior;
	public static double[] multinomialPrior;
	public static double multinomialPriorTotal;

	private int historyThr;
	private String[] approaches;
	private String[] hierarchyStructure;
	private String[] models;
	private float[] scoreThresholds;

	private PrintToFile outputFile;
	private static Properties props;
	private HashMap<String, String[]> clusterToCategories;
	private HashMap<String, String> categoriesToClusters;
	private HashMap<String, Integer> catNameToInt;
	private HashMap<String, String> clusterToRealName;
	private HashMap<String, String> clusterToBasedOn;
	private HashMap<Integer,Integer> categoriesToRoot;
	public static int folds=-1;
	public static Integer currentFold = null;
	

	public HashMap<Integer, Integer> getCategoriesToRoot() {
		return categoriesToRoot;
	}

	public static HashMap<String, HashMap<String, HashMap<Integer, Double>>> getCurCoeffs() {

		return curCoeffs;
	}

	public HashMap<String, String> getClusterToBasedOn() {
		return clusterToBasedOn;
	}

	public HashMap<String, String> getClusterToRealName() {
		return clusterToRealName;
	}

	public HashMap<String, String> getCategoriesToClusters() {
		return categoriesToClusters;
	}

	public HashMap<String, Integer> getCatNameToInt() {
		return catNameToInt;
	}

	public HashMap<Integer, String> getCatIntToName() {
		return catIntToName;
	}

	private HashMap<Integer, String> catIntToName;

	public HashMap<String, String[]> getClusterCategories() {
		return clusterToCategories;
	}

	public void setClusterCategories(HashMap<String, String[]> clusterCategories) {
		this.clusterToCategories = clusterCategories;
	}

	private GlobalVariables() {

		curCoeffs = new HashMap<String, HashMap<String, HashMap<Integer, Double>>>();
		outputFile = new PrintToFile();
		props = PropertiesFactory.getInstance().getProps();
		rsTrials = Integer.parseInt(props.getProperty("RS-trials"));
		models = props.getProperty("models").split(",");
		approaches = props.getProperty("approaches").split(",");
		hierarchyStructure = props.getProperty("hierarchyStructure").split(",");
		if (hierarchyStructure.length > 1)
			hierarchicalFlag = true;

		historyThr = Integer.parseInt(props.getProperty("historyThr").trim());
		K = Integer.parseInt(props.getProperty("K").trim());
		String[] tmpAr = props.getProperty("scoreThresholds").split(",");
		scoreThresholds = new float[tmpAr.length];
		for (int i = 0; i < tmpAr.length; i++)
			scoreThresholds[i] = Float.parseFloat(tmpAr[i]);

		clusterToCategories = new HashMap<String, String[]>();
		categoriesToClusters = new HashMap<String, String>();
		for (String cluster : hierarchyStructure) {
			String[] t = props.getProperty(cluster).split(",");
			clusterToCategories.put(cluster, t);
			for (String catName : t)
				categoriesToClusters.put(catName, cluster);

		}
		catNameToInt = new HashMap<String, Integer>();
		catIntToName = new HashMap<Integer, String>();
		String[] catMapping = props.getProperty("category-mapping").split(",");
		for (String s : catMapping) {
			String[] t = s.split(":");
			catNameToInt.put(t[1], Integer.parseInt(t[0]));
			catIntToName.put(Integer.parseInt(t[0]), t[1]);
		}
		
		categoriesToRoot = new HashMap<Integer, Integer>();
		for (String s : props.getProperty("category-to-root").split(",")) {
			String[] t = s.split(":");
			categoriesToRoot.put(Integer.parseInt(t[0].trim()),Integer.parseInt(t[1]));
		}
	
		if (hierarchicalFlag) {
			clusterToRealName = new HashMap<String, String>();
			for (String s : props.getProperty("r-realMap").split(",")) {
				String[] t = s.split(":");
				clusterToRealName.put(t[1], t[0]);
			}
		}
		clusterToBasedOn = new HashMap<String, String>();
		for (String s : props.getProperty("basedon").split(",")) {
			String[] t = s.split(":");
			clusterToBasedOn.put(t[0], t[1]);
		}

		String[] bptmp = props.getProperty("binomialPrior").split(",");
		binomialPrior = new double[bptmp.length];
		for (int i = 0; i < bptmp.length; i++)
			binomialPrior[i] = Double.parseDouble(bptmp[i]);

		String[] mptmp = props.getProperty("multinomialPrior").split(",");
		multinomialPrior = new double[(int) K];
		for (int i = 0; i < (int) K; i++)
			multinomialPrior[i] = 0;
		multinomialPriorTotal = 0;
		for (int i = 0; i < mptmp.length; i++) {
			String[] tmpAr3 = mptmp[i].split(":");
			int priorInd = Integer.parseInt(tmpAr3[0]);
			multinomialPrior[priorInd] = Double.parseDouble(tmpAr3[1]);
			multinomialPriorTotal += multinomialPrior[priorInd];
		}
		qualities = new double[(int) K];
		double q = 0;
		for (int i = 0; i < K; i++) {
			q += 1.0 / K;
			// System.out.println(i+" - "+q);
			qualities[i] = q;
		}

	}

	public static GlobalVariables getInstance() {
		if (globaleVars == null)
			globaleVars = new GlobalVariables();

		return globaleVars;
	}

	public static GlobalVariables getGlobaleVars() {
		return globaleVars;
	}

	public int getHistoryThr() {
		return historyThr;
	}

	public String[] getApproaches() {
		return approaches;
	}

	public String[] getHierarchyStracture() {
		return hierarchyStructure;
	}

	public String[] getModels() {
		return models;
	}

	public float[] getScoreThresholds() {
		return scoreThresholds;
	}

	public PrintToFile getOutputFile() {
		return outputFile;
	}

	public void openFile(String str) {
		this.outputFile.openFile(str);
	}

}
