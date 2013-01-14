/**
 * Author Marios Kokkodis
 * Last update 01/17/2012
 * 
 * Rutines for creating clean regression files.
 * Output files at bigFiles/odesk/regressions...
 * 
 */

package kokkodis.amazon;

import kokkodis.utils.amazon.RegressionUtils;

public class AmazonRegressions {

	public static String basedOn;

	public static String regressionOuputPath = "C:\\Users\\mkokkodi\\Desktop\\bigFiles\\kdd\\amazon\\regressions\\";
	private static RegressionUtils ru;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		AmazonTrain.print("Starting...");
		ru = new RegressionUtils();

		System.out.println("Starting...");
		for (String model : AmazonTrain.models) {
			for (float scoreTh : AmazonTrain.scoreThresholds) {
				AmazonTrain.scoreTh = scoreTh;
				for (String approach : AmazonTrain.qApproach) {
						String inFile = AmazonTrain.scoreTh + "\\" + model + "_"
								+ approach + "_"; 
						ru.createRegressionFiles(inFile);
						String tmpPath = regressionOuputPath+ inFile+"_";
						ru.getCoeffs(tmpPath,true);

				}
			}
		}

		AmazonTrain.print("Completed");

	}

}
