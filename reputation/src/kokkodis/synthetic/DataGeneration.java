package kokkodis.synthetic;


import kokkodis.factory.PropertiesFactory;
import kokkodis.utils.PrintToFile;
import flanagan.math.PsRandom;

public class DataGeneration {

	/**
	 * @param args
	 */

	public static String rawData;

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		System.out.println("Starting...");
		rawData = PropertiesFactory.getInstance().getProps()
				.getProperty("rawPath");
		for (int categories = 7; categories < 8; categories += 2) {
			PrintToFile trainFile = new PrintToFile();
			trainFile.openFile(rawData + "syn_train_cat" + categories + ".csv"); 
			PrintToFile testFile = new PrintToFile();
			testFile.openFile(rawData + "syn_test_cat" + categories + ".csv");

			double[][] transitionProbabilities = new double[categories][categories];
			double[] categoriesDistribution = new double[categories];

			for (int i = 0; i < categories; i++) {
				transitionProbabilities[i][i] = Math.random() / 2 + 1
						/ (double) categories;
				double sum = transitionProbabilities[i][i];
				for (int j = 0; j < categories - 1; j++) {
					if (i != j) {
						transitionProbabilities[i][j] = (1 - sum)
								* Math.random();
						sum += transitionProbabilities[i][j];
					}
				}
				if (i != categories - 1)
					transitionProbabilities[i][categories - 1] = 1 - sum;
				else
					transitionProbabilities[i][i] += 1-sum; 
					

			}
			double sum = 0;
			for (int j = 0; j < categories - 1; j++) {
				categoriesDistribution[j] = (1 - sum) * Math.random();
				sum += categoriesDistribution[j];
			}

			categoriesDistribution[categories - 1] = 1 - sum;

			trainFile.writeToFile("##");
			trainFile.writeToFile("#Transition Probabilities for " + categories
					+ " categories.");

			for (int i = 0; i < categories; i++) {
				trainFile.writeNoLN_ToFile("# ");
				for (int j = 0; j < categories; j++)
					trainFile.writeNoLN_ToFile(" "
							+ transitionProbabilities[i][j]);
				trainFile.writeToFile("");
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
			for (int i = 0; i < 10000; i++) {
				int initCat = getInitialCat(categoriesDistribution);
				double[] userQualities = getUserQualities(initCat, categories);
				double[] userQualitiesDeviations = getDeviations(categories);
				int curCat = initCat;
				PsRandom psr = new PsRandom();
				int numberOfReviews = (int) Math.floor(Math.random() * 50);
				boolean test = (Math.random() > 0.8) ? true : false;
				for (int l = 0; l < numberOfReviews; l++) {
					double res = 2;
					while (res > 1)
						res = psr.nextGaussian(userQualities[initCat],
								userQualitiesDeviations[initCat]);

					if (test)
						testFile.writeToFile(i +  "," + (curCat + 1)
								+ "," + res);
					else {
						trainFile.writeToFile(i + "," + (curCat + 1)
								+ "," + res);
					}
					curCat = getNextCat(transitionProbabilities, initCat); // maybe
																			// this
																			// should
																			// be
																			// based
																			// on
																			// the
																			// initial
																			// cat1!!!!!!
				}
			}
			trainFile.closeFile();
			testFile.closeFile();
		}
		System.out.println("Data generation completed. ");
	}

	private static int getNextCat(double[][] transitionProbabilities, int curCat) {

		double random = Math.random();

		double sum =0;
		for (int i = 0; i < transitionProbabilities[curCat].length; i++) {
			sum += transitionProbabilities[curCat][i];
			if (random <= sum)
				return i;
		}
		System.err.println("Returning -1 - sum:"+sum+" random:"+random);
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
		if (random < categoriesDistribution[0])
			return 0;
		double sum = categoriesDistribution[0];
		for (int j = 1; j < categoriesDistribution.length; j++) {
			sum += categoriesDistribution[j];
			if (random < sum)
				return j;
		}
		return -1;
	}
}
