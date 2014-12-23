package ch.eonum.pipeline.distance;

import ch.eonum.pipeline.core.DataSet;
import ch.eonum.pipeline.core.Sequence;
import ch.eonum.pipeline.core.SparseInstance;

public class DynamicTimeWarping<E extends Sequence> extends Distance<E> {

	/**
	 * Cost function to measure the transformation cost from one point in a
	 * sequence to another point in a sequence.
	 */
	private Distance<SparseInstance> costFunction;

	/**
	 * 
	 * @param zi
	 *            zero instance
	 * @param costFunction
	 *            Cost function to measure the transformation cost from one
	 *            point in a sequence to another point in a sequence.
	 */
	public DynamicTimeWarping(E zi, Distance<SparseInstance> costFunction) {
		super(zi);
		this.costFunction = costFunction;
	}

	/**
	 * Simple dynamic time warping distance without any optimization. See
	 * http://en.wikipedia.org/wiki/Dynamic_time_warping Setting different
	 * weights for different features is done by scaling these features.
	 */
	@Override
	public double distance(E s, E t) {
		int n = s.getSequenceLength() + 1;
		int m = t.getSequenceLength() + 1;
		DataSet<SparseInstance> sPoints = s.getDataSetFromTimePoints();
		DataSet<SparseInstance> tPoints = t.getDataSetFromTimePoints();

		double[][] DTW = new double[n][m];

		for (int i = 0; i < n; i++)
			DTW[i][0] = Double.POSITIVE_INFINITY;
		for (int i = 0; i < m; i++)
			DTW[0][i] = Double.POSITIVE_INFINITY;
		DTW[0][0] = 0;

		for (int i = 1; i < n; i++) {
			for (int j = 1; j < m; j++) {
				double cost = this.costFunction.distance(sPoints.get(i - 1),
						tPoints.get(j - 1));
				DTW[i][j] = cost + Math.min(DTW[i - 1][j], Math.min( // insertion
						DTW[i][j - 1], // deletion
						DTW[i - 1][j - 1])); // match
			}
		}

		return DTW[n - 1][m - 1];
	}

}
