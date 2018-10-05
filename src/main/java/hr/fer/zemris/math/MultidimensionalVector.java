package hr.fer.zemris.math;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Class which represents a classic multidimensional vector filled with double
 * values.
 * 
 * @author Dinz
 *
 */
public class MultidimensionalVector {

	/**
	 * Values in the vector.
	 */
	private List<Double> values;

	/**
	 * Constructs a new vector with given list of values.
	 * 
	 * @param values
	 *            Values.
	 */
	public MultidimensionalVector(List<Double> values) {
		super();
		Objects.requireNonNull(values);
		this.values = values;
	}

	/**
	 * Gets the vector values.
	 * 
	 * @return Values.
	 */
	public List<Double> getValues() {
		return values;
	}

	/**
	 * Gets the length of the vector.
	 * 
	 * @return Vector length.
	 */
	public int getLength() {
		return values.size();
	}

	/**
	 * Gets the norm of the vector.
	 * 
	 * @return Norm of the vector.
	 */
	public double norm() {
		double sum = 0;
		for (Double val : values) {
			sum += Math.pow(val.doubleValue(), 2);
		}

		return Math.sqrt(sum);
	}

	/**
	 * Returns the dot product of two vectors.
	 * 
	 * @param mv
	 *            Another multidimensional vector.
	 * @return Dot product.
	 */
	public double dotProduct(MultidimensionalVector mv) {
		if (this.getLength() != mv.getLength()) {
			throw new IllegalArgumentException("Vector lengths have to match!");
		}
		double sum = 0;
		for (int i = 0; i < this.getLength(); i++) {
			sum += this.getValues().get(i).doubleValue() * mv.getValues().get(i).doubleValue();
		}
		return sum;
	}

	/**
	 * Calculates the similarity between two vectors.
	 * 
	 * @param mv
	 *            Another vectors.
	 * @return Similarity.
	 */
	public double similarity(MultidimensionalVector mv) {
		double dotP = dotProduct(mv);
		double normP = this.norm() * mv.norm();

		return dotP / normP;
	}

	/**
	 * Calculate the inbetween product of the values of the vector.
	 * 
	 * @param mv
	 *            Another vector.
	 * @return Newly created vector.
	 */
	public MultidimensionalVector crossProduct(MultidimensionalVector mv) {
		List<Double> results = new ArrayList<>();
		if (this.getLength() != mv.getLength()) {
			throw new IllegalArgumentException("Vector lengths have to match!");
		}

		for (int i = 0; i < this.getLength(); i++) {
			results.add(this.getValues().get(i).doubleValue() * mv.getValues().get(i).doubleValue());
		}

		return new MultidimensionalVector(results);
	}

}
