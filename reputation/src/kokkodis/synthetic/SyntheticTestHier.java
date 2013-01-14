package kokkodis.synthetic;

import java.io.File;
import java.util.HashMap;

import kokkodis.db.MySQLoDeskQueries;
import kokkodis.factory.ErrorHolder;
import kokkodis.odesk.ODeskRegressions;
import kokkodis.odesk.ODeskTest;
import kokkodis.odesk.Reputation;
import kokkodis.utils.PrintToFile;
import kokkodis.utils.odesk.RegressionUtils;

public class SyntheticTestHier {
	public static ErrorHolder errorHolder;
	private static MySQLoDeskQueries q;
	public static String evalPath;
	public static int historyThreshold;
	public static HashMap<String, Double[]> coeffs;

	private static String resultPath = // "C:\\Users\\mkokkodi\\"
	// + "Documents\\My Dropbox\\workspace\\java\\kdd12\\results\\odesk\\";
	"/Users/mkokkodi/git/kdd12/kdd12/results/synthetic/";
	// private static PrintToFile resultsTxt = new PrintToFile();
	private static PrintToFile allResultsCsv = new PrintToFile();
	private static HashMap<String, PrintToFile> coeffResults = new HashMap<String, PrintToFile>();
	public static int globalCategories;
	public static HashMap<Integer, HashMap<String, Double[]>> allModelCoeffs;


	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Reputation.print("Starting...");

		globalCategories = DataGenerationHierarchy.noOfCatsInClaster
				* DataGenerationHierarchy.noOfClusters;
		SyntheticTrain.categories = DataGenerationHierarchy.noOfCatsInClaster;
		int cat = globalCategories;
			for (String model : SyntheticTrainHierarchical.models) {
				for (String approach : SyntheticTrainHierarchical.qApproach) {
					allResultsCsv.openFile(new File(resultPath + model + "/"
							+ approach + cat+"cluster.csv"));
					allResultsCsv
							.writeToFile("Score-Threshold,History-Threshold,MAE-"
									+ model
									+ ",MAE-Baseline"
									+ ",MSE-"
									+ model
									+ ",MSE-Baseline");
					if (model.equals("Binomial")) {
						for (float scoreTh : SyntheticTrain.scoreThresholds) {
							Reputation.scoreTh = scoreTh;

							// resultsTxt.openFile(new File(resultPath +
							// Train.scoreTh
							// + "\\" + model + "_" + approach + ".csv"));

							allModelCoeffs = getAllModelCoeffs(model, approach);

							Reputation.print("Running evaluation for model:"
									+ model + ", approach:" + approach);

							runEvaluation(model, approach);
							// resultsTxt.closeFile();
						}
			}
		}
			}
		for (PrintToFile pf : coeffResults.values())
			pf.closeFile();
		Reputation.print("Completed");
		

						


	}

	private static HashMap<Integer, HashMap<String, Double[]>> getAllModelCoeffs(String model,
			String approach) {

		HashMap<Integer, HashMap<String, Double[]>> allcoeffs = new HashMap<Integer, HashMap<String, Double[]>>();
		RegressionUtils ru = new RegressionUtils();

		
		for (int cluster : SyntheticTrainHierarchical.hierarchyLevel) {
			/**
			 * Code for printing into file coeffs
			 */
			PrintToFile pf = coeffResults.get(model+cluster);
			if (pf == null) {
				pf = new PrintToFile();
				coeffResults.put(model+cluster, pf);
				pf.openFile(new File(resultPath + model+"//"+ cluster + "Coeff.csv"));
				String str = "VoteThreshold,Approach";
				for (int i = 1; i <= SyntheticTrainHierarchical.categories; i++) {
					for (int j = 1; j <= SyntheticTrainHierarchical.categories; j++)
						str += ",a" + i + j;
					str += ",b" + i;
				}
				pf.writeToFile(str);
			}
			/**
			 * End
			 */
			String inFile;
			if (model.equals("Binomial"))
				inFile = model + "_" + approach + "_" + SyntheticTrain.categories + "_"
						+ Reputation.scoreTh+cluster;
			else
				inFile = model + "_" + approach + "_" + cluster;

				ODeskRegressions.basedOn = "_BasedOn_0_1_2_3";
				Reputation.mPlus1 = 4;

				ODeskRegressions.regressionOuputPath = SyntheticRegressions.regressionOuputPath;
				String regressionFileToUse = ODeskRegressions.regressionOuputPath
					+ inFile + "_";
			HashMap<String, Double[]> tmpCoeff = ru.getCoeffs(
					regressionFileToUse, true);

			// ru.printCoeffs(tmpCoeff, null);
			allcoeffs.put(cluster, tmpCoeff);

			/**
			 * Again for printing Coeff
			 */
			String str = Reputation.scoreTh + "," + approach + ",";
			for (int l = 1; l < Reputation.mPlus1; l++) {

				Double[] tmp = tmpCoeff.get(l + ODeskRegressions.basedOn);
				for (int i = 0; i < Reputation.mPlus1; i++) {
					str += tmp[i] + ",";
				}

			}
			pf.writeToFile(str.substring(0, str.length() - 1));
			/**
			 * End
			 */
		}
		return allcoeffs;

	
		
	

	}

	private static void runEvaluation(String model, String approach) {

		Utils u = new Utils();
		// resultsTxt
		// .writeToFile("History-Threshold,MAE-Binomial,MAE-Baseline, MSE-Binomial, MSE-Baseline");
		System.out.println("History-Threshold,MAE-" + model
				+ ",MAE-Baseline, MSE-" + model + ", MSE-Baseline");

		for (int i = 5; i <= 15; i += 2) {

			errorHolder = new ErrorHolder();
			ODeskTest.errorHolder = errorHolder;
			ODeskTest.historyThreshold = i;

			historyThreshold = i;
			u.rawDataToBinomialModelHier(approach, model,
					SyntheticTrain.categories, true, -1);

			double maeBaseline = errorHolder.getBaselineMAESum()
					/ errorHolder.getTotalEvaluations();
			double maeBinomialModel = errorHolder.getBinomialModelMAESum()
					/ errorHolder.getTotalEvaluations();

			double mseBinomialModel = errorHolder.getBinomialModelMSESum()
					/ errorHolder.getTotalEvaluations();

			double mseBaseline = errorHolder.getBaselineMSESum()
					/ errorHolder.getTotalEvaluations();

			String resStr = historyThreshold + "," + maeBinomialModel + ","
					+ maeBaseline + "," + mseBinomialModel + "," + mseBaseline;
			System.out.println(resStr);
			// resultsTxt.writeToFile(resStr);
			allResultsCsv.writeToFile(Reputation.scoreTh + "," + resStr);

		}
		
		


	}
}
