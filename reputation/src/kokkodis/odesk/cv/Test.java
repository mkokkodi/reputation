package kokkodis.odesk.cv;

import java.io.File;
import java.util.HashMap;

import kokkodis.db.MySQLoDeskQueries;
import kokkodis.factory.ErrorHolder;
import kokkodis.odesk.ODeskRegressions;
import kokkodis.odesk.ODeskTest;
import kokkodis.odesk.Reputation;
import kokkodis.utils.PrintToFile;
import kokkodis.utils.odesk.RegressionUtils;

public class Test {

	private static String resultPath = "/home/mkokkodi/workspace/git/kdd12/cv_data/results/";

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Reputation.print("Starting...");
		
		for(int fold=1; fold < 11; fold++)
			runTests(fold);
	
		Reputation.print("Completed");
	}

	private static void runTests(int fold) {
		for (String model : Reputation.models) {
			for (String approach : Reputation.approaches) {
				ODeskTest.allResultsCsv.openFile(new File(resultPath + model
						+ "/" + approach +fold+ ".csv"));
				ODeskTest.allResultsCsv
						.writeToFile("Score-Threshold,History-Threshold,MAE-"
								+ model + ",MAE-Baseline" + ",MSE-" + model
								+ ",MSE-Baseline");
				Reputation.print("Running evaluation for model:"
						+ model + ", approach:" + approach+fold);

				if (model.equals("Binomial")) {
					for (float scoreTh : Reputation.scoreThresholds) {
						Reputation.scoreTh = scoreTh;

						// resultsTxt.openFile(new File(resultPath +
						// Train.scoreTh
						// + "/" + model + "_" + approach + ".csv"));

						ODeskTest.allModelCoeffs = getAllModelCoeffs(model,
								approach,fold);

					
						runEvaluation(model, approach,fold);
						// resultsTxt.closeFile();
					}
				} else {
					ODeskTest.allModelCoeffs = getAllModelCoeffs(model,
							approach,fold);

					System.out.println("Printing Keys...");
				for(String key: ODeskTest.allModelCoeffs.keySet())
					System.out.println(key);
					runEvaluation(model, approach,fold);

				}
				ODeskTest.allResultsCsv.closeFile();
			}
		}

		for (PrintToFile pf : ODeskTest.coeffResults.values())
			pf.closeFile();
		
	}

	private static HashMap<String, HashMap<String, Double[]>> getAllModelCoeffs(
			String model, String approach, int fold) {

		HashMap<String, HashMap<String, Double[]>> allcoeffs = new HashMap<String, HashMap<String, Double[]>>();
		RegressionUtils ru = new RegressionUtils();

		for (String level : Reputation.hierarchyLevel) {
			/**
			 * Code for printing into file coeffs
			 */
			PrintToFile pf = ODeskTest.coeffResults.get(model + level+fold);
			if (pf == null) {
				pf = new PrintToFile();
				ODeskTest.coeffResults.put(model + level+fold, pf);
				pf.openFile(new File(resultPath + model + "/" + level
						+ "Coeff.csv"));
				if (level.equals("Technical") || level.equals("Non-technical"))
					pf.writeToFile("VoteThreshold,Approach,a11,a12,a13,b1,a21,a22,a23,b2,a31,a32,a33,b3");
				else
					pf.writeToFile("VoteThreshold,Approach,a11,a12,b1,a21,a22,b2");

			}
			/**
			 * End
			 */
			String inFile;
			if (model.equals("Binomial"))
				inFile = Reputation.scoreTh + "/" + model + "_" + approach
						+ "_" + level+fold;
			else
				inFile = model + "_" + approach + "_" + level+fold;

			if (level.equals("Technical") || level.equals("Non-technical")) {
				ODeskRegressions.basedOn = "_BasedOn_0_1_2_3";
				Reputation.mPlus1 = 4;
			} else {
				ODeskRegressions.basedOn = "_BasedOn_0_1_2";
				Reputation.mPlus1 = 3;
			}
			String regressionFileToUse = Regressions.regressionsCVOutPath
					+ inFile + "_";
			HashMap<String, Double[]> tmpCoeff = ru.getCoeffs(
					regressionFileToUse, true);

			// ru.printCoeffs(tmpCoeff, null);
			allcoeffs.put(level+fold, tmpCoeff);

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

	private static void runEvaluation(String model, String approach, int fold) {

		ODeskTest.q = new MySQLoDeskQueries();
		ODeskTest.q.connect();
		Train.loadSets();

		// resultsTxt
		// .writeToFile("History-Threshold,MAE-Binomial,MAE-Baseline, MSE-Binomial, MSE-Baseline");
		System.out.println("History-Threshold,MAE-" + model
				+ ",MAE-Baseline, MSE-" + model + ", MSE-Baseline");

		for (int i = 5; i <= 15; i += 2) {

			ODeskTest.errorHolder = new ErrorHolder();

			ODeskTest.historyThreshold = i;
			ODeskTest.q.rawDataToBinomialModelCV("hierarchicalModel", approach,
					model, true,fold);

			double maeBaseline = ODeskTest.errorHolder.getBaselineMAESum()
					/ ODeskTest.errorHolder.getTotalEvaluations();
			double maeBinomialModel = ODeskTest.errorHolder
					.getBinomialModelMAESum()
					/ ODeskTest.errorHolder.getTotalEvaluations();

			double mseBinomialModel = ODeskTest.errorHolder
					.getBinomialModelMSESum()
					/ ODeskTest.errorHolder.getTotalEvaluations();

			double mseBaseline = ODeskTest.errorHolder.getBaselineMSESum()
					/ ODeskTest.errorHolder.getTotalEvaluations();

			String resStr = ODeskTest.historyThreshold + "," + maeBinomialModel
					+ "," + maeBaseline + "," + mseBinomialModel + ","
					+ mseBaseline;
			System.out.println(resStr);
			// resultsTxt.writeToFile(resStr);
			ODeskTest.allResultsCsv.writeToFile(Reputation.scoreTh + ","
					+ resStr);

		}

	}

}
