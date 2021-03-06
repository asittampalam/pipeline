package ch.eonum.pipeline.classification.tree;

import java.util.List;
import java.util.Random;

import ch.eonum.pipeline.core.DataSet;
import ch.eonum.pipeline.core.Features;
import ch.eonum.pipeline.core.Instance;

/**
 * Node in a decision tree for regression.
 * 
 * @author tim
 *
 */
public class SplitNode<E extends Instance> {

	protected DataSet<E> trainSet;
	/** depth of this node within the tree. */
	protected int depth;
	/** decision tree where this node belongs to. */
	protected DecisionTree<E> parent;
	/** split feature. null if this is a terminal node. */
	protected String splitFeature;
	/** split value. */
	protected double splitValue;
	/** value (average outcome) of this node. */
	private double value;
	/** left Node. null if this is a terminal node. */
	protected SplitNode<E> left;
	/** right Node. null if this is a terminal node. */
	protected SplitNode<E> right;

	public SplitNode(DecisionTree<E> parent, int depth, DataSet<E> trainSet) {
		this.parent = parent;
		this.depth = depth;
		this.trainSet = trainSet;
	}
	
	public boolean isTerminal(){
		return right == null && left == null;
	}

	public void train() {
		this.calculateValue();
		/** check for termination. */
		int minSize = (int)parent.getDoubleParameter("minSize");
		if (depth >= parent.getDoubleParameter("maxDepth")
				|| trainSet.size() <= minSize) {
			return;
		}
		/** select features. */
		Features fs = parent.getFeatures();
		double k = parent.getDoubleParameter("k");
		if(k < 1.0){
			Random rand = parent.getRandom();
			fs = new Features();
			for(int f = 0; f < parent.getFeatures().size(); f++)
				if(rand.nextDouble() < k)
					fs.addFeature(parent.getFeatures().getFeatureByIndex(f));
			fs.recalculateIndex();
		}
		pickSplitFeature(minSize, fs);
	}

	/**
	 * select a split feature from the provided feature set and a value by
	 * minimizing the squared error.
	 * 
	 * @param minSize
	 * @param features
	 */
	protected void pickSplitFeature(int minSize, Features features) {
		double minSquareError = Double.POSITIVE_INFINITY;
		for(int f = 0; f < features.size(); f++){
			String feature = features.getFeatureByIndex(f);
			List<Double> splits = parent.getSplitsForFeature(feature);
			for(Double sv : splits){
				double ltAvg = 0.0;
				int ltNum = 0;
				double geAvg = 0.0;
				int geNum = 0;
				for(Instance each : trainSet)
					if(each.get(feature) >= sv){
						geAvg += each.outcome;
						geNum ++;
					} else {
						ltAvg += each.outcome;
						ltNum ++;
					}
				geAvg /= geNum;
				ltAvg /= ltNum;
				double squareError = 0.0;
				for(Instance each : trainSet)
					if(each.get(feature) >= sv){
						squareError += Math.pow(each.outcome - geAvg, 2);
					} else {
						squareError += Math.pow(each.outcome - ltAvg, 2);
					}
				if(squareError < minSquareError && ltNum > minSize && geNum > minSize){
					minSquareError = squareError;
					splitFeature = feature;
					splitValue = sv;
				}
			}
		}
		if(minSquareError == Double.POSITIVE_INFINITY)
			return;
		DataSet<E> leftTrain = new DataSet<E>();
		DataSet<E> rightTrain = new DataSet<E>();
		for(E each : trainSet)
			if(each.get(splitFeature) >= splitValue){
				leftTrain.add(each);
			} else {
				rightTrain.add(each);
			}
		left = new SplitNode<E>(parent, depth + 1, leftTrain);
		right = new SplitNode<E>(parent, depth + 1, rightTrain);
		left.train(); // #TODO parallelize
		right.train();
	}

	/**
	 * Get the mean outcome in the training set.
	 */
	protected void calculateValue() {
		this.value = 0.0;
		for(Instance each : trainSet)
			value += each.outcome;
		value /= (double)trainSet.size();
	}

	public void test(Instance each) {
		if (isTerminal())
			each.putResult("result", value);
		else {
			if (each.get(splitFeature) >= splitValue) {
				left.test(each);
			} else {
				right.test(each);
			}
		}
	}
	
	public String toString(){
		String tree = "{ 'value' : " + value + ", 'num' : " + trainSet.size();
		if(splitFeature != null)
			tree += ", 'splitOn' : '" + splitFeature + "', 'splitValue' : " + splitValue;
		if(right != null)
			tree += ", \n'right' : " + indent(right.toString());
		if(left != null)
			tree += ", 'left' : " + indent(left.toString());
		tree += "}";
		return tree;
	}

	protected String indent(String string) {
		String[] lines = string.split("\n");
		String ret = "";
		for(String line : lines)
			ret += "  " + line + "\n";
		return ret;
	}
}
