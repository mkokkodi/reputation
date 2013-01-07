package kokkodis.factory;

import kokkodis.utils.odesk.GlobalVariables;

public class MultCategory extends ModelCategory {

	private double[] bucketSuccesses;
	private double n; /*total trials*/
	private double [] q_ijk; /* probability distro across buckets!*/

	public double[] getQ_ijk() {
		return q_ijk;
	}

	public MultCategory() {
		bucketSuccesses = new double[(int) GlobalVariables.K];
		for(int i=0; i<bucketSuccesses.length; i++)
			bucketSuccesses[i]=0;
		q_ijk = new double[(int) GlobalVariables.K];
		n=0;
		calculateNewDistroQijk();
	}

	public double[] getBucketSuccesses() {
		return bucketSuccesses;
	}

	public void increaseTotalTrials(){
		n++;
		calculateNewDistroQijk();
	}

	private void calculateNewDistroQijk() {
		
		for(int i=0; i<q_ijk.length; i++)
			q_ijk[i]=(bucketSuccesses[i]+GlobalVariables.multinomialPrior[i])/(n+GlobalVariables.multinomialPriorTotal);
	}

	public double getN() {
		return n;
	}


}
