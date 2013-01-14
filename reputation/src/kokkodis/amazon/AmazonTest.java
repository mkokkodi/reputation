package kokkodis.amazon;

import java.io.File;
import java.util.HashMap;

import kokkodis.db.AmazonQueries;
import kokkodis.factory.ErrorHolder;
import kokkodis.utils.PrintToFile;
import kokkodis.utils.amazon.RegressionUtils;

public class AmazonTest {

	public static ErrorHolder errorHolder;
	private static AmazonQueries q;
	public static String evalPath;
	public static int historyThreshold;
	public static HashMap<String, Double[]> coeffs;

	private static String resultPath = "C:\\Users\\mkokkodi\\workspace\\kdd12\\results\\amazon\\";
	// private static PrintToFile resultsTxt = new PrintToFile();
	private static PrintToFile allResultsCsv = new PrintToFile();
	private static PrintToFile coeffResults = new PrintToFile();

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		AmazonTrain.print("Starting...");
		for (String model : AmazonTrain.models) {
			for (String approach : AmazonTrain.qApproach) {
				allResultsCsv.openFile(new File(resultPath + model + "\\"
						+ approach + ".csv"));
				allResultsCsv
						.writeToFile("Score-Threshold,History-Threshold,MAE-"
								+ model + ",MAE-Baseline" + ",MSE-" + model
								+ ",MSE-Baseline");
				for (float scoreTh : AmazonTrain.scoreThresholds) {
					AmazonTrain.scoreTh = scoreTh;

					// resultsTxt.openFile(new File(resultPath + Train.scoreTh
					// + "\\" + model + "_" + approach + ".csv"));

					coeffs = getCoeffs(model, approach);

					AmazonTrain.print("Running evaluation for model:" + model
							+ ", approach:" + approach);

					runEvaluation(model, approach);
					// resultsTxt.closeFile();
				}
				allResultsCsv.closeFile();
			}
		}

		coeffResults.closeFile();
		AmazonTrain.print("Completed");
	}

	private static HashMap<String, Double[]> getCoeffs(
			String model, String approach) {

		RegressionUtils ru = new RegressionUtils();

		/**
		 * Code for printing into file coeffs
		 */
		coeffResults = new PrintToFile();
		coeffResults.openFile(new File(resultPath + "Coeff.csv"));
		coeffResults.writeToFile("VoteThreshold,Approach,a11,a12,a13,a14,b1,"
				+ "a21,a22,a23,a24,b2," + "a31,a32,a33,a34,b3,"
				+ "a41,a42,a43,a44,b4");

		/**
		 * End
		 */
		String inFile = AmazonTrain.scoreTh + "\\" + model + "_" + approach
				+ "_";

		AmazonRegressions.basedOn = "_BasedOn_0_1_2_3_4";
		AmazonTrain.mPlus1 = 5;

		String regressionFileToUse = AmazonRegressions.regressionOuputPath
				+ inFile + "_";
		HashMap<String, Double[]> tmpCoeff = ru.getCoeffs(regressionFileToUse,
				true);

		// ru.printCoeffs(tmpCoeff, null);

		/**
		 * Again for printing Coeff
		 */
		String str = AmazonTrain.scoreTh + "," + approach + ",";
		for (int l = 1; l < AmazonTrain.mPlus1; l++) {

			Double[] tmp = tmpCoeff.get(l + AmazonRegressions.basedOn);
			for (int i = 0; i < AmazonTrain.mPlus1; i++) {
				str += tmp[i] + ",";
			}

		}
		coeffResults.writeToFile(str.substring(0, str.length() - 1));
		/**
		 * End
		 */

		return tmpCoeff;

	}

	private static void runEvaluation(String model, String approach) {

		q = new AmazonQueries();
		q.connect();

		// resultsTxt
		// .writeToFile("History-Threshold,MAE-Binomial,MAE-Baseline, MSE-Binomial, MSE-Baseline");
		System.out.println("History-Threshold,MAE-" + model
				+ ",MAE-Baseline, MSE-" + model + ", MSE-Baseline");

		for (int i = 5; i <= 15; i += 2) {

			errorHolder = new ErrorHolder();

			historyThreshold = i;
			q.rawDataToBinomialModel(null, approach, model, true);

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
			allResultsCsv.writeToFile(AmazonTrain.scoreTh + "," + resStr);

		}

	}

}
