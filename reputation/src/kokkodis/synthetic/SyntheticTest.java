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

public class SyntheticTest {

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

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Reputation.print("Starting...");
		for (int cat = 9; cat < 10; cat += 2) {
			SyntheticTrain.categories = cat;
			for (String model : SyntheticTrain.models) {
				for (String approach : SyntheticTrain.qApproach) {
					allResultsCsv.openFile(new File(resultPath + model + "/"
							+ approach + cat+".csv"));
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

							coeffs = getAllModelCoeffs(model, approach, cat);

							Reputation.print("Running evaluation for model:"
									+ model + ", approach:" + approach);

							runEvaluation(model, approach);
							// resultsTxt.closeFile();
						}
					} else {
						coeffs = getAllModelCoeffs(model, approach, cat);

						Reputation.print("Running evaluation for model:"
								+ model + ", approach:" + approach);

						runEvaluation(model, approach);

					}
					allResultsCsv.closeFile();
				}
			}
		}
		for (PrintToFile pf : coeffResults.values())
			pf.closeFile();
		Reputation.print("Completed");
	}

	private static HashMap<String, Double[]> getAllModelCoeffs(String model,
			String approach, int cat) {

		RegressionUtils ru = new RegressionUtils();

		PrintToFile pf = coeffResults.get(model+cat+Reputation.scoreTh );
		if (pf == null) {
			pf = new PrintToFile();
			coeffResults.put(model, pf);
			pf.openFile(new File(resultPath + model + "//" + "Coeff"+cat+	Reputation.scoreTh +".csv"));
			String str = "VoteThreshold,Approach";
			for (int i = 1; i <= SyntheticTrain.categories; i++) {
				for (int j = 1; j <= SyntheticTrain.categories; j++)
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
			inFile = model + "_" + approach + "_" + cat + "_"
					+ Reputation.scoreTh;
		else
			inFile = model + "_" + approach + cat + "_";

		ODeskRegressions.basedOn = "_BasedOn";
		for (int i = 0; i < cat + 1; i++) {
			ODeskRegressions.basedOn += "_" + i;
		}
		// ODeskTrain.mPlus1 = SyntheticTrain.categories + 1;
		Reputation.mPlus1 = cat + 1;
		ODeskRegressions.regressionOuputPath = SyntheticRegressions.regressionOuputPath;

		String regressionFileToUse = ODeskRegressions.regressionOuputPath
				+ inFile + "_";
		HashMap<String, Double[]> tmpCoeff = ru.getCoeffs(regressionFileToUse,
				true);

		// ru.printCoeffs(tmpCoeff, null);

		/**
		 * Again for printing Coeff
		 */
		String str = Reputation.scoreTh + "," + approach + ",";
		for (int l = 1; l < Reputation.mPlus1; l++) {

			Double[] tmp = tmpCoeff.get(l + ODeskRegressions.basedOn);
			if (tmp != null) {
				for (int i = 0; i < Reputation.mPlus1; i++) {
					str += tmp[i] + ",";
				}
			}
		}
		pf.writeToFile(str.substring(0, str.length() - 1));
		/**
		 * End
		 */
		return tmpCoeff;

	}

	private static void runEvaluation(String model, String approach) {

		Utils u = new Utils();
		// resultsTxt
		// .writeToFile("History-Threshold,MAE-Binomial,MAE-Baseline, MSE-Binomial, MSE-Baseline");
		System.out.println("History-Threshold,MAE-" + model
				+ ",MAE-Baseline, MSE-" + model + ", MSE-Baseline");

		for (int i = 5; i <= 15; i += 2) {

			errorHolder = new ErrorHolder();

			ODeskTest.historyThreshold = i;

			historyThreshold = i;
			u.rawDataToBinomialModel(approach, model,
					SyntheticTrain.categories, true);

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
