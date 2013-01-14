package kokkodis.synthetic;

import java.io.File;

import kokkodis.odesk.Reputation;
import kokkodis.utils.PrintToFile;

public class SyntheticTrainHierarchical {
	/*
	 * Global Variables for Train, Regressions and Test
	 */

	public static int mPlus1; // m+1
	public static float scoreTh;
	public static int historyThr = 5;
	public static String trainingOutPath = "/Users/mkokkodi/Desktop/bigFiles/kdd/synthetic/logits/";
	// "C:\\Users\\mkokkodi\\Desktop\\bigFiles\\kdd\\odesk\\synthetic\\logits\\";
	public static String[] qApproach = { "RS"
	// ,
	};

	public static int[] hierarchyLevel = { 
		0,
		1, 
		2
		, 
		3 
		}; // 3 = generic /
															// highest
															// level.!!!!!

	public static String[] models = { "Binomial",
	// "Multinomial"
	};
	public static int globalCategories;

	public static int categories = 5;
	public static float[] scoreThresholds = { 0.5f, 0.6f
	// , 0.7f
	// , 0.8f, 0.9f
	};

	/*
	 * Some tmp additions.
	 */
	public static PrintToFile outputFile;

	public static void main(String[] args) {

		globalCategories = DataGenerationHierarchy.noOfCatsInClaster
				* DataGenerationHierarchy.noOfClusters;
		System.out.println("Starting...");
		// categories = 5;
		categories = DataGenerationHierarchy.noOfCatsInClaster;// noOfCategories
																// in cluster.
		for (String model : models) {
			for (String approach : qApproach) {
				if (model.equals("Binomial")) {
					for (float tmpTh : scoreThresholds) {
						scoreTh = tmpTh;
						for (int cluster : hierarchyLevel) {
							runLevel(approach, model, categories, cluster);
						}

					}
				} else {
					for (int cluster : hierarchyLevel) {
						runLevel(approach, model, categories, cluster);
					}
				}

			}
		}
		print("Completed.");

	}

	private static void runLevel(String approach, String model, int categories,
			int cluster) {

		initialize();

		if (model.equals("Binomial")) {
			print(trainingOutPath + model + "_" + approach + "_" + categories
					+ "_" + scoreTh + cluster + ".csv");
			outputFile.openFile(new File(trainingOutPath + model + "_"
					+ approach + "_" + categories + "_" + scoreTh + cluster
					+ ".csv"));
		} else {
			print(trainingOutPath + model + "_" + approach + "_" + categories
					+ ".csv");
			outputFile.openFile(new File(trainingOutPath + model + "_"
					+ approach + "_" + categories + cluster + ".csv"));
		}

		String str = "id,logit(q)";
		for (int j = 1; j < categories + 1; j++)
			str += ",logit(cat" + j + ")";

		str += ",cat, logit(q_cat(t+1))";
		outputFile.writeToFile(str);
		mPlus1 = categories + 1;

		Reputation.mPlus1 = mPlus1;
		Reputation.historyThr = historyThr;
		Reputation.models = models;
		Reputation.outputFile = outputFile;
		Reputation.scoreTh = scoreTh;
		Reputation.scoreThresholds = scoreThresholds;
		Reputation.trainingOutPath = trainingOutPath;

		Utils u = new Utils();
		u.rawDataToBinomialModelHier(approach, model, categories, false,
				cluster);
		outputFile.closeFile();

	}

	private static void initialize() {
		System.out.println("Initializing...");
		outputFile = new PrintToFile();
	}

	public static void print(String str) {
		System.out.println(str);
	}
}
