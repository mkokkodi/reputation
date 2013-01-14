/**
 * Author Marios Kokkodis
 * Last update 01/17/2012
 * 
 * Rutines for creating clean regression files.
 * Output files at bigFiles/odesk/regressions...
 * 
 */

package kokkodis.odesk;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import kokkodis.utils.PrintToFile;
import kokkodis.utils.odesk.RegressionUtils;

public class ODeskRegressions {

	public static String basedOn;

	public static String regressionOuputPath = "/Users/mkokkodi/git/odeskdev-shadchan/" +
			"odesk-bestmatch/odesk-bestmatch-model/data/results/utility/regressions/";
	private static RegressionUtils ru;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Reputation.print("Starting...");
		ru = new RegressionUtils();

		System.out.println("Starting...");
		for (String model : Reputation.models) {

			for (String approach : Reputation.approaches) {

				for (String level : Reputation.hierarchyLevel) {
					if (model.equals("Binomial")) {
						for (float scoreTh : Reputation.scoreThresholds) {
							Reputation.scoreTh = scoreTh;
							String inFile = Reputation.scoreTh + "/" + model
									+ "_" + approach + "_" + level;
							
							ru.createRegressionFiles(inFile, level);
							String tmpPath = regressionOuputPath + inFile + "_";
							ru.getCoeffs(tmpPath, true);
						}
					} else {
						String inFile =  model + "_"
								+ approach + "_" + level;
						ru.createRegressionFiles(inFile, level);
						String tmpPath = regressionOuputPath + inFile + "_";
						ru.getCoeffs(tmpPath, true);
					}
				}

			}
		}

		Reputation.print("Completed");

	}

	

}
