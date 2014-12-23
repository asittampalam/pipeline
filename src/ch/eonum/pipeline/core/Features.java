package ch.eonum.pipeline.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.eonum.pipeline.util.Log;

/**
 * class holding a list of dimensions/features.
 * 
 * The list can be read or written to a file. non-numeric feature labels are
 * being ordered and can be accessed by an index
 * 
 * @author tim
 * 
 */
public class Features {
	private List<String> featuresByIndex;
	private Map<String, Integer> indicesByFeature;
	private Map<String, String> descriptions;

	public Features() {
		this.featuresByIndex = new ArrayList<String>();
		this.indicesByFeature = new LinkedHashMap<String, Integer>();
		this.descriptions = new HashMap<String, String>();
	}

	public Features(List<String> list) {
		this();
		for(String e : list)
			this.addFeature(e);
		this.recalculateIndex();
	}

	/**
	 * get the index from a certain dimension.
	 * 
	 * @param feature
	 * @return
	 */
	public int getIndexFromFeature(String feature) {
		return this.indicesByFeature.get(feature);
	}

	/**
	 * get the feature label from a certain index.
	 * 
	 * @param index
	 * @return
	 */
	public String getFeatureByIndex(int index) {
		return this.featuresByIndex.get(index);
	}

	/**
	 * get the description of a dimension/feature
	 * 
	 * @param dim
	 * @return
	 */
	public String getDescription(String dim) {
		return this.descriptions.containsKey(dim) ? this.descriptions.get(dim)
				: dim;
	}

	/**
	 * print all features into a file.
	 * 
	 * @param fileName
	 */
	public void writeToFile(String fileName) {
		try {
			FileWriter fstream = new FileWriter(fileName);
			BufferedWriter out = new BufferedWriter(fstream);
			for (int i = 0; i < this.featuresByIndex.size(); i++)
				out.write(i + ":" + featuresByIndex.get(i) + ":"
						+ this.getDescription(featuresByIndex.get(i)) + "\n");

			out.close();
		} catch (Exception e) {
			Log.warn(e.getMessage());
		}
	}

	/**
	 * read a features file into memory. all lines of a features file are in the
	 * format: "index:feature_label:description" Example:
	 * "23:los:Length of stay" The indices are not being read. They are asserted
	 * to be equal to the line number -1. The only purpose of the indices in the
	 * features file is better readability.
	 * 
	 * @param fileName
	 * @return
	 */
	public static Features readFromFile(String fileName) {
		Features dimensions = new Features();

		FileInputStream fstream;
		try {
			fstream = new FileInputStream(fileName);

			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line;

			while ((line = br.readLine()) != null) {
				String[] split = line.split(":");
				dimensions.addFeature(split[1]);
				if (split.length > 2)
					dimensions.addDescription(split[1], split[2]);
			}
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return dimensions;
	}

	/**
	 * add a description to a feature/dimension
	 * 
	 * @param string
	 * @param string2
	 */
	private void addDescription(String feature, String description) {
		this.descriptions.put(feature, description);
	}

	/**
	 * create a dimensions object from a list of strings, usually extracted
	 * using a FeatureExtractor
	 * 
	 * @param fileName
	 * @return
	 */
	public static Features createFromList(List<String> feats) {
		Features features = new Features();
		for (String label : feats)
			features.addFeature(label);
		features.recalculateIndex();
		return features;
	}

	public int size() {
		return this.featuresByIndex.size();
	}

	/**
	 * load descriptions of features from a separate file. format (one line):
	 * feature:description
	 * 
	 * @param fileName
	 *            file name of the descriptions file
	 */
	public void loadDescriptionFile(String fileName) {
		FileInputStream fstream;
		try {
			fstream = new FileInputStream(fileName);

			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line;

			while ((line = br.readLine()) != null) {
				String[] split = line.split(":");
				if (split.length >= 2) {
					String description = "";
					for (int i = 1; i < split.length; i++)
						description += split[i];
					this.addDescription(split[0], description);
				} else
					Log.warn("Unexpected description format in line: " + line);
			}
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void removeFeature(String feature) {
		this.indicesByFeature.remove(feature);
	}

	public void addFeature(String feature) {
		if (!this.featuresByIndex.contains(feature)) {
			this.featuresByIndex.add(feature);
			this.indicesByFeature.put(feature, this.featuresByIndex.size() - 1);
		}
	}

	/**
	 * recalculate the index. this is typically done after removing some
	 * features.
	 */
	public void recalculateIndex() {
		this.featuresByIndex = new ArrayList<String>(
				this.indicesByFeature.keySet());
		Collections.sort(featuresByIndex);
		this.indicesByFeature = new HashMap<String, Integer>();
		for (int i = 0; i < this.featuresByIndex.size(); i++)
			this.indicesByFeature.put(this.featuresByIndex.get(i), i);
	}

	/**
	 * create a data set with an instance for each feature. An instance has only
	 * one feature with the value 1.0
	 * 
	 * @return
	 */
	public DataSet<SparseInstance> createDataSet() {
		DataSet<SparseInstance> ds = new DataSet<SparseInstance>();
		for (String feature : this.featuresByIndex) {
			HashMap<String, Double> data = new HashMap<String, Double>();
			data.put(feature, 1.0);
			ds.add(new SparseInstance(feature, feature, data));
		}
		return ds;
	}

	public boolean hasFeature(String feature) {
		return this.featuresByIndex.contains(feature);
	}

	public List<String> getListOfFeaturesCopy() {
		return new ArrayList<String>(this.featuresByIndex);
	}

	@Override
	public String toString() {
		String ret = "";
		for (int i = 0; i < this.featuresByIndex.size(); i++)
			ret += i + ":" + featuresByIndex.get(i) + ":"
					+ this.getDescription(featuresByIndex.get(i)) + "\n";
		return ret;
	}

	@SafeVarargs
	public static Features createFromDataSets(DataSet<? extends Instance> ... dataSets) {
		Features features = new Features();
		for (DataSet<? extends Instance> ds : dataSets)
			for (Instance each : ds)
				for (String feature : each.features())
					features.addFeature(feature);
		features.recalculateIndex();
		return features;
	}

	public Features copy() {
		Features newF = new Features();
		for (String feature : this.featuresByIndex)
			newF.addFeature(feature);
		newF.recalculateIndex();
		return newF;
	}

	@SafeVarargs
	public static Map<String, Features> createFromDataSetsPerClass(DataSet<? extends SparseInstance> ... dataSets) {
		Map<String, Features> features = new HashMap<String, Features>();
		for (DataSet<? extends SparseInstance> ds : dataSets)
			for (SparseInstance each : ds) {
				if (!features.containsKey(each.className))
					features.put(each.className, new Features());
				for (String feature : each.features())
					features.get(each.className).addFeature(feature);
			}
		for (Features f : features.values())
			f.recalculateIndex();
		return features;
	}

	/**
	 * Remove all features which only contain constant values or are highly
	 * correlated with another feature.
	 * 
	 * @param features
	 * @param dataSet
	 * @return
	 */
	public static Features removeConstantAndPerfectlyCorrellatedFeatures(
			Features features, DataSet<? extends Instance> dataSet) {
		Features newFeatures = new Features();
		double[][] data = dataSet.asDoubleArrayMatrix(features);
		Set<String> constantFeatures = new HashSet<String>();
		for (int f = 0; f < features.size(); f++) {
			double value = data[0][f];
			boolean constant = true;
			for (double[] each : data)
				if (Math.abs(value - each[f]) > 0.00001) {
					constant = false;
					break;
				}
			if (constant) {
				String feature = features.getFeatureByIndex(f);
				Log.warn("Constant feature " + feature + " will be removed.");
				constantFeatures.add(feature);
			}
		}
		double[] means = new double[features.size()];
		for (int f = 0; f < features.size(); f++) {
			double sum = 0.0;
			for (double[] each : data)
				sum += each[f];
			means[f] = sum / data.length;
		}

		for (int f = 0; f < features.size(); f++) {
			String feature = features.getFeatureByIndex(f);
			boolean correlates = false;
			for (int f2a = 0; f2a < newFeatures.size(); f2a++) {
				String feature2 = newFeatures.getFeatureByIndex(f2a);
				int f2 = features.getIndexFromFeature(feature2);
				double cov = 0.0;
				double sd1 = 0.0;
				double sd2 = 0.0;
				for (double[] each : data) {
					double diff1 = each[f] - means[f];
					double diff2 = each[f2] - means[f2];
					cov += diff1 * diff2;
					sd1 += diff1 * diff1;
					sd2 += diff2 * diff2;
				}
				cov /= data.length;
				sd1 /= data.length;
				sd2 /= data.length;

				double corr = cov / (Math.sqrt(sd1) * Math.sqrt(sd2));

				if (Math.abs(corr) > 0.99) {
					Log.warn("Correlated features " + feature + " and "
							+ feature2 + " " + corr + "\n" + feature
							+ " will be removed.");
					correlates = true;
					break;
				}
			}
			if (!correlates) {
				newFeatures.addFeature(feature);
				newFeatures.recalculateIndex();
			}
		}
		for (String f : constantFeatures)
			newFeatures.removeFeature(f);
		newFeatures.recalculateIndex();

		return newFeatures;
	}

	public Set<String> asSet() {
		return this.indicesByFeature.keySet();
	}

	public List<String> asStringList() {
		List<String> list = new ArrayList<String>();
		for(int i = 0; i < this.size(); i++)
			list.add(this.getFeatureByIndex(i));
		return list;
	}

}
