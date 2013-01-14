package kokkodis.utils.amazon;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import flanagan.analysis.Regression;

import kokkodis.amazon.AmazonRegressions;
import kokkodis.amazon.AmazonTrain;
import kokkodis.utils.PrintToFile;
import kokkodis.utils.Utils;

public class RegressionUtils extends Utils {

	public RegressionUtils() {
	}

	/**
	 * 
	 * @param regressionOuputPath
	 *            : output path (regressions)
	 * @param inFile
	 *            : raw training file
	 * @param level
	 *            : hierarchy
	 *            Creates regression files with full history, for each
	 *            category!!
	 */
	public void createRegressionFiles(String fileInitialString) {

	
		AmazonRegressions.basedOn = "_BasedOn_0_1_2_3_4";
		
		AmazonTrain.print("Output files at:"
				+ AmazonRegressions.regressionOuputPath + fileInitialString
				+ "_");

		String inFile = AmazonTrain.trainingOutPath + fileInitialString + ".csv";
		AmazonTrain.print("Input File:" + inFile);
		String basedOn = AmazonRegressions.basedOn;

		try {
			HashMap<String, PrintToFile> outFilesHolder = new HashMap<String, PrintToFile>();
			BufferedReader input = new BufferedReader(new FileReader(inFile));
			input.readLine();
			PrintToFile pf = new PrintToFile();

			ArrayList<Integer> cats = new ArrayList<Integer>();
			for (int j = 0; j < AmazonTrain.mPlus1; j++)
				cats.add(j);
			for (int i = 1; i < AmazonTrain.mPlus1; i++) {

				pf = createFile(AmazonRegressions.regressionOuputPath
						+ fileInitialString + "_", i + basedOn, i, cats);
				outFilesHolder.put(i + basedOn, pf);
			}

			String line;
			while ((line = input.readLine()) != null)

			{
				String[] tmpAr = line.split(",");

				boolean notComplete = false;
				int baseCat = Integer.parseInt(tmpAr[tmpAr.length - 2].trim());
				String key = baseCat + basedOn;
				String newLine = tmpAr[tmpAr.length - 1] + ",";
				for (int i = 1; i < tmpAr.length - 2; i++) {
					if ((tmpAr[i].trim().equals("NH"))) {
						notComplete = true;
						break;
					} else {
						newLine += tmpAr[i] + ",";

					}
				}
				if (!notComplete) {

					newLine = newLine.substring(0, newLine.length() - 1);
					PrintToFile tmp = outFilesHolder.get(key);
					if (tmp != null) {
						tmp.writeToFile(newLine);
					}
				}
			}

			/*
			 * Closing files
			 */
			Set<Entry<String, PrintToFile>> set = outFilesHolder.entrySet();
			Iterator<Entry<String, PrintToFile>> setIt = set.iterator();
			while (setIt.hasNext()) {
				Entry<String, PrintToFile> me = setIt.next();
				me.getValue().closeFile();

			}
		} catch (IOException e) {
		}
	}

	private PrintToFile createFile(String path, String file, int baseCat,
			ArrayList<Integer> cats) {
		System.out.println("Creating file:" + path + file + ".csv");
		PrintToFile pf = new PrintToFile();
		pf.openFile(new File(path + file + ".csv"));
		String line = "logit(q_" + baseCat + "(t+1))";
		for (Integer i : cats) {
			line += ",logit(q_" + i + "(t))";
		}
		pf.writeToFile(line);
		return pf;
	}

	public HashMap<String, Double[]> getCoeffs(String tmpPath, boolean print) {

		AmazonTrain.print("Calculating coefficients for:" + tmpPath);
		HashMap<String, Double[]> coeffs = new HashMap<String, Double[]>();

		int numOfRegressions = 0;
		for (int baseCat = 1; baseCat < AmazonTrain.mPlus1; baseCat++) {
			String key = baseCat + AmazonRegressions.basedOn;
			System.out.println("key:" + key);
			HashMap<Integer, ArrayList<Double>> vars = readVarsForRegression(tmpPath
					+ key + ".csv");

			int rowSize = vars.get(0).size();
			System.out.println("rowsize:" + rowSize);
			if (rowSize > 10) {
				numOfRegressions++;
				ArrayList<Double> tmpList = vars.get(0);
				double[] yArray = new double[rowSize];
				for (int i = 0; i < rowSize; i++)
					yArray[i] = tmpList.get(i);

				double[][] x = new double[vars.size() - 1][rowSize];
				for (int i = 1; i < vars.size(); i++) {
					tmpList = vars.get(i);
					for (int j = 0; j < rowSize; j++)
						x[i - 1][j] = tmpList.get(j);
				}

				Regression reg = new Regression(x, yArray);
				reg.linearGeneral();
				double[] coeff = reg.getCoeff();
				Double[] tmp = new Double[coeff.length];
				for (int i = 0; i < coeff.length; i++) {
					tmp[i] = coeff[i];
				}
				coeffs.put(key, tmp);
			}
		}
		if (print)
			printCoeffs(coeffs, null);

		return coeffs;

	}

	public void printCoeffs(HashMap<String, Double[]> coeffs, PrintToFile pf) {

		if (pf != null) {
			for (int l = 1; l < AmazonTrain.mPlus1; l++) {
				String str = "";
				Double[] tmp = coeffs.get(l + AmazonRegressions.basedOn);
				for (int i = 0; i < AmazonTrain.mPlus1 - 1; i++) {
					str += tmp[i] + ",";
					System.out.print(tmp[i] + ",");
				}
				System.out.println(tmp[AmazonTrain.mPlus1 - 1]);
				str += tmp[AmazonTrain.mPlus1 - 1];
				pf.writeToFile(str);

			}
		} else {
			for (int l = 1; l < AmazonTrain.mPlus1; l++) {
				Double[] tmp = coeffs.get(l + AmazonRegressions.basedOn);
				for (int i = 0; i < AmazonTrain.mPlus1 - 1; i++)
					System.out.print(tmp[i] + ",");

				System.out.println(tmp[AmazonTrain.mPlus1 - 1]);

			}
		}
	}

	public HashMap<Integer, ArrayList<Double>> readVarsForRegression(String f) {

		try {

			BufferedReader input = new BufferedReader(new FileReader(f));
			String line;
			line = input.readLine();
			int size = line.split(",").length;
			HashMap<Integer, ArrayList<Double>> vars = new HashMap<Integer, ArrayList<Double>>();
			for (int i = 0; i < size; i++) {
				ArrayList<Double> al = new ArrayList<Double>();
				vars.put(i, al);
			}
			while ((line = input.readLine()) != null) {

				String[] tmpAr = line.split(",");
				for (int i = 0; i < size; i++) {
					vars.get(i).add(Double.parseDouble(tmpAr[i].trim()));
				}

			}
			return vars;
		} catch (IOException e) {
		}
		return null;

	}
}
