package kokkodis.synthetic;

import kokkodis.odesk.ODeskRegressions;
import kokkodis.odesk.Reputation;
import kokkodis.utils.odesk.RegressionUtils;

public class SyntheticRegressionsHier {
	
	public static String basedOn;

	public static String regressionOuputPath = // "C:\\Users\\mkokkodi\\Desktop\\bigFiles\\kdd\\odesk\\regressions\\";
	"/Users/mkokkodi/Desktop/bigFiles/kdd/synthetic/regressions/";

	private static RegressionUtils ru;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Reputation.print("Starting...");
		ODeskRegressions.regressionOuputPath = regressionOuputPath;
		Reputation.trainingOutPath = "/Users/mkokkodi/Desktop/bigFiles/kdd/synthetic/logits/";
		ru = new RegressionUtils();

		System.out.println("Starting...");
		for (int cat = 9; cat < 10; cat += 2) {
			Reputation.mPlus1 = DataGenerationHierarchy.noOfCatsInClaster+1;//noOfCats inclusters +1 cat + 1;
			for (String model : SyntheticTrain.models) {

				for (String approach : SyntheticTrainHierarchical.qApproach) {
					for (int cluster : SyntheticTrainHierarchical.hierarchyLevel) {
					if (model.equals("Binomial")) {
						for (float scoreTh : SyntheticTrain.scoreThresholds) {
							Reputation.scoreTh = scoreTh;
							String inFile = model + "_" + approach + "_"+DataGenerationHierarchy.noOfCatsInClaster+"_"
									+ scoreTh+cluster;
							ru.createRegressionFilesHier(inFile,cluster);
							String tmpPath = regressionOuputPath + inFile + "_";
							ru.getCoeffs(tmpPath, true);
						}
					} else {
						String inFile = model + "_" + approach+"_"+cat;
						ru.createRegressionFiles(inFile,cat);
						String tmpPath = regressionOuputPath + inFile + "_";
						ru.getCoeffs(tmpPath, true);
					}
				}
				}

			}
		}
		Reputation.print("Completed");

	}

	
}
