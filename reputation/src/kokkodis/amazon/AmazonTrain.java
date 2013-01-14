/**
 * Author Marios Kokkodis
 * Last update 01/17/2012
 * -------------------------------------------------------
 * Categories ategories:
 * 
 * Technical Level: (m=3+1), '1 - web-development', '2 - Software-development', '3 - design and multimedia'
 * Non - Technical Level: (m=3+1), '1 - writing', '2 - administrative', '3 - Sales & marketing'
 * Generic: (m=2+1), '1 - technical' , '2 - non tech'
 * 
 * --------------------------------------------------------
 *  

 * 
 * Binomial model with history and score (helpfulness) thresholds. 
 * Point Estimate (PE) and  Random Sampling  (RS) aproaches.
 * 
 * Training stream. Outputs tuples ready for regressions
 * Output dir: ~/outFiles/training/$scoreTh/$model_$approach_$level 
 * 
 */

package kokkodis.amazon;

import java.io.File;

import kokkodis.db.AmazonQueries;
import kokkodis.utils.PrintToFile;

public class AmazonTrain {

	/*
	 * Global Variables for Train, Regressions and Test
	 */
	public static double K=5; // number of buckets!
	public static int votesTh=5;
	public static int mPlus1; // m+1
	public static float scoreTh;
	public static int historyThr = 5;
	public static String trainingOutPath = "C:\\Users\\mkokkodi\\Desktop\\bigFiles\\kdd\\amazon\\training\\";
	public static String[] qApproach = {
		"PE",
		"RS" };
	public static String[] models = { 
		"Binomial",
		"Multinomial" };
	public static float[] scoreThresholds = {0.6f, 0.7f, 0.8f, 0.9f };
	public static PrintToFile outputFile;


	private static AmazonQueries q;



	public static void main(String[] args) {

		System.out.println("Starting...");
		for (String model : models) {
			for (float tmpTh : scoreThresholds) {
				scoreTh = tmpTh;  
				for (String approach : qApproach) {
						runLevel( approach, model);

				}
			}
		}
		System.out.println("Completed.");

	}

	private static void runLevel( String approach,String model) {

		initialize();
		
		print(trainingOutPath + scoreTh + "\\" + model + "_" + approach + "_"
				+  ".csv");
		outputFile.openFile(new File(trainingOutPath + scoreTh + "\\" + model
				+ "_" + approach + "_" +  ".csv"));

			
			outputFile
					.writeToFile("id,logit(q), lo logit(q_books), logit(q_movies)," +
							" logit(q_music), logit(q_vgames),cat, logit(q_cat(t+1))");
			mPlus1 = 5;

		
		q.rawDataToBinomialModel(null,  approach, model,false);
		outputFile.closeFile();
	}

	private static void initialize() {
		System.out.println("Initializing...");
		q = new AmazonQueries();
		q.connect();
		outputFile = new PrintToFile();
	}

	public static void print(String str) {
		System.out.println(str);
	}
}
