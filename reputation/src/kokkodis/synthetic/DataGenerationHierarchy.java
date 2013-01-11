package kokkodis.synthetic;

import java.util.HashMap;
import java.util.Map.Entry;

import kokkodis.factory.PropertiesFactory;
import kokkodis.utils.PrintToFile;
import flanagan.math.PsRandom;

public class DataGenerationHierarchy {

	public static int noOfClusters = 2;
	public static int noOfCatsInClaster = 4;

	/**
	 * @param args
	 */
	public static String rawData;

	
	public static HashMap<Integer, Integer> categoryToCluster;

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		System.out.println("Starting...");
		rawData = PropertiesFactory.getInstance().getProps()
				.getProperty("rawPath");

		categoryToCluster = new HashMap<Integer, Integer>();
		int catIndex=0;
		for (int k = 0; k < noOfClusters; k++) {
			for (int i = 0; i < noOfCatsInClaster; i++) {

				
				System.out.println("Cat:" + catIndex + " cluster:" + k);
				categoryToCluster.put(catIndex, k);
				catIndex++;
			}
		}
		System.out.println("Starting...");
		for (int categories = 8; categories < 9; categories += 2) {
			PrintToFile trainFile = new PrintToFile();
			trainFile
					.openFile(rawData + "syn_cluster_train.csv");
			PrintToFile testFile = new PrintToFile();
			testFile.openFile(rawData + "syn_cluster_test"  + ".csv");

			double[] categoriesDistribution = new double[categories];

			HashMap<Integer, double[][]> clustersTransitionMatrix = new HashMap<Integer, double[][]>();

			for (int cluster = 0; cluster < noOfClusters; cluster++) {

				double[][] probs = new double[noOfCatsInClaster][noOfCatsInClaster];
				for (int i = 0; i < noOfCatsInClaster; i++) {
					probs[i][i] = Math.random()/2  +0.4;
					double sum = probs[i][i];
					for (int j = 0; j < noOfCatsInClaster - 1; j++) {
						if (i != j) {
							probs[i][j] = (1 - sum) * Math.random();
							sum += probs[i][j];
						}
					}
					if (i != noOfCatsInClaster - 1)
						probs[i][noOfCatsInClaster - 1] = 1 - sum;
					else {
						probs[i][i] += 1 - sum;
					}

				}
				clustersTransitionMatrix.put(cluster, probs);

			}

			for(int j=0; j<categories; j++){
				categoriesDistribution[j] = 1.0/(double)categories;
			}

			trainFile.writeToFile("##");
			trainFile.writeToFile("#Transition Probabilities for " + categories
					+ " categories.");

			for (Entry<Integer, double[][]> e : clustersTransitionMatrix
					.entrySet()) {
				trainFile.writeToFile("# cluster:" + e.getKey());
				for (int i = 0; i < noOfCatsInClaster; i++) {
					trainFile.writeNoLN_ToFile("# ");
					for (int j = 0; j < noOfCatsInClaster; j++)
						trainFile.writeNoLN_ToFile(" " + e.getValue()[i][j]);
					trainFile.writeToFile("");
				}
			}
			trainFile.writeToFile("##");
			trainFile
					.writeNoLN_ToFile("# Categories distribution -> used to choose the initial category. ");
			trainFile.writeToFile("#");
			for (int i = 0; i < categories; i++)
				trainFile.writeToFile("# Prob(category =" + (i + 1) + ")="
						+ categoriesDistribution[i]);

			trainFile.writeToFile("###############");
			trainFile.writeToFile("id,cat,quality");
			testFile.writeToFile("id,cat,quality");
			for (int i = 0; i < 15000; i++) {
				int initCat = getInitialCat(categoriesDistribution);
			//	System.out.println(initCat);
				double[] userQualities = getUserQualities(initCat, categories);
				double[] userQualitiesDeviations = getDeviations(categories);
				int curCat = initCat;
				PsRandom psr = new PsRandom();
				int numberOfReviews = 30 + (int) Math.floor(Math.random() * 25);
				boolean test = (Math.random() > 0.8) ? true : false;
				for (int l = 0; l < numberOfReviews; l++) {
					double res = 2;
					while (res > 1)
						res = psr.nextGaussian(userQualities[initCat],
								userQualitiesDeviations[initCat]);

					if (test)
						testFile.writeToFile(i + "," + (curCat + 1)
								+ "," + res);
					else {
						trainFile.writeToFile(i  + "," + (curCat + 1)
								+ "," + res);
					}
					int curCluster = categoryToCluster.get(curCat);
					//System.out.println(curCluster);
					if (Math.random() > 0.9) {
						curCat = getNextCatFromOtherClusters(curCluster);
					} else{
//initCat starts from zero.
						curCat = getNextCat(
								clustersTransitionMatrix.get(curCluster),
								(curCat)%noOfCatsInClaster, curCluster);
					}
				}
			}
			trainFile.closeFile();
			testFile.closeFile();
		}
		System.out.println("Data generation completed. ");
	}

	private static int getNextCatFromOtherClusters(int curCluster) {
		if (curCluster == 1) {
			return (int) (Math.round(Math.random() * (noOfCatsInClaster-1))) ;// 0 or 1 or 2 or 3.
					
		} else {
			return (int) (noOfCatsInClaster+Math.round(Math.random() * (noOfCatsInClaster-1))) ;//4,5,6,7
		/*	
			if (Math.random() > 0.5)
				return (int) (Math.round(Math.random() * 2));
			else
				return (int) (5 + Math.round(Math.random() * 3));
		} else
			return (int) (Math.round(Math.random() * 5));
			*/
		}

	}

	private static int getNextCat(double[][] transitionProbabilities, int curCat,int cluster) {
		//System.out.println(curCat);

		double random = Math.random();

	
		double sum = 0;
		for (int i = 0; i < transitionProbabilities[curCat].length; i++) {
			sum += transitionProbabilities[curCat][i];
		//	System.out.println("sum:" + sum);
			if (random <= sum)
				return i + cluster * noOfCatsInClaster;
		}
	//	System.out.println("Random:" + random + " , sum:" + sum);
		return -1;
	}

	private static double[] getDeviations(int categories) {
		double[] deviations = new double[categories];
		for (int i = 0; i < categories; i++)
			deviations[i] = (double) Math.random() / 5;
		return deviations;
	}

	private static double[] getUserQualities(int initCat, int categories) {
		double[] qualities = new double[categories];
		qualities[initCat] = Math.random() / 2 + 0.5;
		for (int i = 0; i < categories; i++) {
			if (i != initCat)
				qualities[i] = Math.random();

		}
		return qualities;
	}

	private static int getInitialCat(double[] categoriesDistribution) {
		double random = Math.random();
		double sum = 0;
		for (int j = 0; j < categoriesDistribution.length; j++) {
			sum += categoriesDistribution[j];
			if (random <= sum)
				return j;
		}
		return -1;
	}
}
