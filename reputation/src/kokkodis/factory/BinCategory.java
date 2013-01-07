package kokkodis.factory;

/**
 * 
 * @author mkokkodi
 * 
 */
public class BinCategory extends ModelCategory {

	private double x; // # successes ( helpful reviews)
	private double n; // total reviews

	public BinCategory() {
		x = n = 0;
	}

	/**
	 * 
	 * @return succesful number of tasks in this cat
	 */
	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	/**
	 * 
	 * @return total number of tasks in this category
	 */
	public double getN() {
		return n;
	}

	public void setN(double n) {
		this.n = n;
	}

}
