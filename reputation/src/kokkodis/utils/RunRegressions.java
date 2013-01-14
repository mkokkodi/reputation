package kokkodis.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import flanagan.analysis.Regression;

import kokkodis.odesk.Reputation;
import kokkodis.utils.GlobalVariables;
import kokkodis.factory.PropertiesFactory;

public class RunRegressions {

	private static String basedOn;
	private static String regressionOutPath;
	private static GlobalVariables globalVariables;
	private static Properties props;
	private static ArrayList<Integer> curCatIds;
	private static HashMap<String,ArrayList<String>> regressionData;

	public static void createRegressionFiles() {

		initializeVars();

		String inFile = props.getProperty("trainingOutPath")
				+ Utils.createFileName();

		System.out.println(GlobalVariables.line);
		System.out.println("Output files at " + regressionOutPath);
		System.out.println("Input File:" + inFile);

		regressionData = loadRegressionData(inFile);

	}

	private static void initializeVars() {
		globalVariables = GlobalVariables.getInstance();
		basedOn = globalVariables.getClusterToBasedOn().get(
				GlobalVariables.curCluster);
		props = PropertiesFactory.getInstance().getProps();
		regressionOutPath = props.getProperty("regressionOutPath");
		curCatIds = Utils.getCurCatIds();

	}

	private static HashMap<String, ArrayList<String>> loadRegressionData(String inFile) {
		try {
			BufferedReader input = new BufferedReader(new FileReader(inFile));
			String line = input.readLine();

			//HashMap<String, PrintToFile> outFilesHolder = initializeFiles();

			HashMap<String,ArrayList<String>> regressionData  = new HashMap<String, ArrayList<String>>();
			/**
			 * id,logit(q),logit(overall),logit(writing),
			 * logit(administrative),logit(sales-and-marketing),
			 * cat,logit(q_cat(t+1))
			 */

			while ((line = input.readLine()) != null)

			{
				String[] tmpAr = line.split(",");
				int baseCat = Integer.parseInt(tmpAr[tmpAr.length - 2].trim());
				String key = baseCat + basedOn;
				String newLine = tmpAr[tmpAr.length - 1] + ",";
				if (!line.contains("NH")) {
					for (int i = 1; i < tmpAr.length - 2; i++) {
						newLine += tmpAr[i] + ",";
					}
					newLine = newLine.substring(0, newLine.length() - 1);
					ArrayList<String> curData = regressionData.get(key);
					if(curData==null){
						curData = new ArrayList<String>();
						regressionData.put(key, curData);
					}
					curData.add(newLine);
					/*
					PrintToFile tmp = outFilesHolder.get(key);
					if (tmp != null) {
						tmp.writeToFile(newLine);
					}
					*/

				}
			}
			input.close();
			return regressionData;
			//closeFiles(outFilesHolder);

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static void closeFiles(HashMap<String, PrintToFile> outFilesHolder) {
		/*
		 * Closing files
		 */
		Set<Entry<String, PrintToFile>> set = outFilesHolder.entrySet();
		Iterator<Entry<String, PrintToFile>> setIt = set.iterator();
		while (setIt.hasNext()) {
			Entry<String, PrintToFile> me = setIt.next();
			me.getValue().closeFile();

		}

	}

	private static HashMap<String, PrintToFile> initializeFiles() {
		HashMap<String, PrintToFile> hm = new HashMap<String, PrintToFile>();

		PrintToFile pf = new PrintToFile();

		for (Integer catId : curCatIds) {
			if (catId != 0) {
				pf = createRegressionFile(catId, curCatIds);
				hm.put(catId + basedOn, pf);
			}
		}
		return hm;
	}

	

	private static String createRegressionFileName(int catId,
			ArrayList<Integer> curCatIds) {
		String outFile = regressionOutPath
				+ Utils.createFileName().replace(".csv", "") + "_" + catId
				+ basedOn + ".csv";
		return outFile;

	}

	private static PrintToFile createRegressionFile(int catId,
			ArrayList<Integer> curCatIds) {

		String outFile = createRegressionFileName(catId, curCatIds);
		System.out.println("Creating Regression file:" + outFile);
		PrintToFile pf = new PrintToFile();
		pf.openFile(outFile);
		String line = "logit(q_" + catId + "(t+1))";
		for (Integer i : curCatIds) {
			line += ",logit(q_" + i + "(t))";
		}
		pf.writeToFile(line);
		return pf;
	}

	public static HashMap<String, HashMap<Integer,Double>> getCoeffs(boolean printCoeffs) {
			initializeVars();
		//Reputation.print("Calculating coefficients for:"
			//	+ Utils.createFileName());
		HashMap<String, HashMap<Integer,Double>> coeffs = new HashMap<String, HashMap<Integer,Double>>();

		
		for (int baseCat : curCatIds) {
			if (baseCat != 0) {
				
				String key = baseCat + basedOn;
				//System.out.println("key:" + key);
				//String fileName = createRegressionFileName(baseCat, curCatIds);
				
				//HashMap<Integer, ArrayList<Double>> vars = readVarsForRegression(fileName);

				HashMap<Integer, ArrayList<Double>> vars = readVarsForRegression(regressionData.get(key));

				int rowSize = vars.get(0).size();
				//System.out.println("rowsize:" + rowSize);
				if (rowSize > 10) {
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
					try {
						reg.linearGeneral();
					} catch (Exception e) {
						e.printStackTrace();
					}

					double[] coeff = reg.getCoeff();
					Double[] tmp = new Double[coeff.length];
					for (int i = 0; i < coeff.length; i++) {
						tmp[i] = coeff[i];
					}
					int i=0;
					HashMap<Integer,Double> tmpHm = new HashMap<Integer, Double>();
					for(int catId:Utils.getCurCatIds()){
						tmpHm.put(catId, tmp[i]);
						i++;
					}
					
					coeffs.put(key, tmpHm);
				}
			}
		}
		printCeffsToFile(coeffs);
		if (printCoeffs)
			printCoeffs(coeffs, null);

		return coeffs;

	}

	
	private static HashMap<Integer, ArrayList<Double>> readVarsForRegression(
			ArrayList<String> data) {

			
			int size = data.get(0).split(",").length;
			HashMap<Integer, ArrayList<Double>> vars = new HashMap<Integer, ArrayList<Double>>();
			for (int i = 0; i < size; i++) {
				ArrayList<Double> al = new ArrayList<Double>();
				vars.put(i, al);
			}
			for(String line:data)
			 {
				// System.out.println(line);
				String[] tmpAr = line.split(",");
				for (int i = 0; i < size; i++) {
					vars.get(i).add(Double.parseDouble(tmpAr[i].trim()));
				}

			}
			return vars;


	}

	private static HashMap<Integer, ArrayList<Double>> readVarsForRegression(
			String f) {
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
				// System.out.println(line);
				String[] tmpAr = line.split(",");
				for (int i = 0; i < size; i++) {
					vars.get(i).add(Double.parseDouble(tmpAr[i].trim()));
				}

			}
			input.close();
			return vars;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;

	}
	
	private static void printCeffsToFile(
			HashMap<String, HashMap<Integer, Double>> coeffs) {
		
		PrintToFile coeffsFile = new PrintToFile();
		String fileName = PropertiesFactory.getInstance().getProps().getProperty("regressionOutPath")+getCoeffsFile();
		coeffsFile.openFile(fileName);
		System.out.println("Printing coefficients at:"+fileName);
		/**
		 * key,cats
		 */
		
		for (int curCat : curCatIds) {
			if (curCat != 0) {
				String str = curCat +basedOn+",";
				HashMap<Integer,Double> tmp = coeffs.get(curCat +basedOn);
				for(int catId:Utils.getCurCatIds()){
					str+=tmp.get(catId) + ",";
				}
				System.out.println();
				coeffsFile.writeToFile(str);
			/*	str += tmp[tmp.length - 1];
				if (pf != null)
					pf.writeToFile(str);
					*/

			}
		}
coeffsFile.closeFile();
		
	}


	private static String getCoeffsFile() {
		return "coeffs_"+GlobalVariables.curModel+"_"+GlobalVariables.curApproach+"_"+ GlobalVariables.curCluster+"_"+
	(GlobalVariables.curModel.equals("Binomial")?GlobalVariables.currentBinomialThreshold+"":"")+
	(Reputation.crossValidation?GlobalVariables.currentFold:"")+".csv";
	}

	public static void printCoeffs(HashMap<String, HashMap<Integer, Double>> coeffs,
			PrintToFile pf) {
		for (int curCat : curCatIds) {
			if (curCat != 0) {

				String str = "";
				HashMap<Integer,Double> tmp = coeffs.get(curCat +basedOn);
				for(int catId:Utils.getCurCatIds()){
					System.out.print(tmp.get(catId) + "\t");
				}
				System.out.println();
			/*	str += tmp[tmp.length - 1];
				if (pf != null)
					pf.writeToFile(str);
					*/

			}
		}
	}

	public static HashMap<String, HashMap<Integer, Double>> readCoeffs() {
		//	+ Utils.createFileName());
	HashMap<String, HashMap<Integer,Double>> coeffs = new HashMap<String, HashMap<Integer,Double>>();

	String fileName = PropertiesFactory.getInstance().getProps().getProperty("regressionOutPath")+getCoeffsFile();
	System.out.println("Reading coefficients from file:"+fileName);
	try{
			BufferedReader input = new BufferedReader(new FileReader(fileName));
			String line;
			/**
			 * key, coefficients....
			 */
			while ((line = input.readLine()) != null) {
				String tmpAr[] = line.split(",");
				String key = tmpAr[0];
				HashMap<Integer,Double> curCoeffs = new HashMap<Integer, Double>();
				int i=1;
				for(int catId:Utils.getCurCatIds()){
					curCoeffs.put(catId,Double.parseDouble(tmpAr[i]));
					
					i++;
				}
				coeffs.put(key,curCoeffs);
			}
			input.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	
	return coeffs;
	}

}
