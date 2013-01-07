package kokkodis.factory;

public class ErrorHolder {

	private double baselineMAESum;
	private double binomialModelMAESum;
	private double EMModelMAESum;

	private double baselineMSESum;
	private double binomialModelMSESum;
	public double totalEvaluations;

	public ErrorHolder() {
		super();

		baselineMAESum = 0;
		totalEvaluations = 0;
		binomialModelMAESum = 0;
		
		EMModelMAESum = 0;
	}

	
	
	public double getEMModelMAESum() {
		return EMModelMAESum;
	}



	public void setEMModelMAESum(double eMModelMAESum) {
		EMModelMAESum = eMModelMAESum;
	}



	public double getBaselineMSESum() {
		return baselineMSESum;
	}



	public void setBaselineMSESum(double baselineMSESum) {
		this.baselineMSESum = baselineMSESum;
	}



	public double getBinomialModelMSESum() {
		return binomialModelMSESum;
	}



	public void setBinomialModelMSESum(double binomialModelMSESum) {
		this.binomialModelMSESum = binomialModelMSESum;
	}



	public double getBinomialModelMAESum() {
		return binomialModelMAESum;
	}

	public void setBinomialModelMAESum(double abstractModelErrorSum) {
		this.binomialModelMAESum = abstractModelErrorSum;
	}

	public double getBaselineMAESum() {
		return baselineMAESum;
	}

	public void setBaselineMAESum(double noModelErrorSum) {
		this.baselineMAESum = noModelErrorSum;
	}

	public double getTotalEvaluations() {
		return totalEvaluations;
	}

	public void setTotalEvaluations(double totalEvaluations) {
		this.totalEvaluations = totalEvaluations;
	}

}
