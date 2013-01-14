package kokkodis.odesk.cv;

import kokkodis.odesk.ODeskRegressions;
import kokkodis.odesk.Reputation;
import kokkodis.utils.odesk.RegressionUtils;

public class Regressions {

	/**
	 * @param args
	 */
	private static RegressionUtils ru;
	public static String regressionsCVOutPath = "/home/mkokkodi/workspace/git/kdd12/cv_data/regressions/";

	public static void main(String[] args) {
		Reputation.print("Starting...");
		ODeskRegressions.regressionOuputPath = regressionsCVOutPath;
		ru = new RegressionUtils();
		Reputation.trainingOutPath = Train.cvDataPath;

		System.out.println("Starting...");

		for (int i = 1; i <= 10; i++)
			runRegressions(i);

		Reputation.print("Completed");

	}

	private static void runRegressions(int i) {
		for (String model : Reputation.models) {

			for (String approach : Reputation.approaches) {

				for (String level : Reputation.hierarchyLevel) {
					if (model.equals("Binomial")) {
						for (float scoreTh : Reputation.scoreThresholds) {
							Reputation.scoreTh = scoreTh;
							String inFile = Reputation.scoreTh + "/" + model
									+ "_" + approach + "_" + level+i;
							ru.createRegressionFiles(inFile, level);
							String tmpPath = ODeskRegressions.regressionOuputPath
									+ inFile + "_";
							ru.getCoeffs(tmpPath, true);
						}
					} else {
						String inFile = model + "_" + approach + "_" + level+i;
						ru.createRegressionFiles(inFile, level);
						String tmpPath = ODeskRegressions.regressionOuputPath
								+ inFile + "_";
						ru.getCoeffs(tmpPath, true);
					}
				}

			}

		}
	}

}
