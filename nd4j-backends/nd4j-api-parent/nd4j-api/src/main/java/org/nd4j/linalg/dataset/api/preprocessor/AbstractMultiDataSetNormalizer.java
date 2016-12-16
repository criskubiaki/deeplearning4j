package org.nd4j.linalg.dataset.api.preprocessor;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Setter;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.stats.NormalizerStats;
import org.nd4j.linalg.dataset.api.MultiDataSet;
import org.nd4j.linalg.dataset.api.MultiDataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.MultiDataSetIterator;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for normalizers that act upon {@link MultiDataSet} instances or iterators
 *
 * @author Ede Meijer
 */
@EqualsAndHashCode(callSuper = false)
abstract class AbstractMultiDataSetNormalizer<S extends NormalizerStats> extends AbstractNormalizer<S> implements MultiDataSetPreProcessor {
    @Setter private List<S> featureStats;
    @Setter private List<S> labelStats;
    private boolean fitLabels = false;

    protected AbstractMultiDataSetNormalizer(NormalizerStrategy<S> strategy) {
        super(strategy);
    }

    /**
     * Flag to specify if the labels/outputs in the dataset should be also normalized
     * default value is false
     *
     * @param fitLabels
     */
    public void fitLabel(boolean fitLabels) {
        this.fitLabels = fitLabels;
    }

    /**
     * Whether normalization for the labels is also enabled. Most commonly used for regression, not classification.
     *
     * @return True if labels will be
     */
    public boolean isFitLabel() {
        return this.fitLabels;
    }

    @Override
    protected boolean isFit() {
        return featureStats != null;
    }

    protected S getFeatureStats(int input) {
        return getFeatureStats().get(input);
    }

    protected List<S> getFeatureStats() {
        assertIsFit();
        return featureStats;
    }

    protected S getLabelStats(int output) {
        return getLabelStats().get(output);
    }

    protected List<S> getLabelStats() {
        assertIsFit();
        return labelStats;
    }

    /**
     * Fit a MultiDataSet (only compute based on the statistics from this dataset)
     *
     * @param dataSet the dataset to compute on
     */
    public void fit(@NonNull MultiDataSet dataSet) {
        List<S.Builder> featureNormBuilders = new ArrayList<>();
        List<S.Builder> labelNormBuilders = new ArrayList<>();

        fitPartial(dataSet, featureNormBuilders, labelNormBuilders);

        featureStats = buildList(featureNormBuilders);
        if (isFitLabel()) {
            labelStats = buildList(labelNormBuilders);
        }
    }

    /**
     * Fit an iterator
     *
     * @param iterator for the data to iterate over
     */
    public void fit(@NonNull MultiDataSetIterator iterator) {
        List<S.Builder> featureNormBuilders = new ArrayList<>();
        List<S.Builder> labelNormBuilders = new ArrayList<>();

        iterator.reset();
        while (iterator.hasNext()) {
            MultiDataSet next = iterator.next();
            fitPartial(next, featureNormBuilders, labelNormBuilders);
        }

        featureStats = buildList(featureNormBuilders);
        if (isFitLabel()) {
            labelStats = buildList(labelNormBuilders);
        }
    }

    private List<S> buildList(@NonNull List<S.Builder> builders) {
        List<S> result = new ArrayList<>(builders.size());
        for (S.Builder builder : builders) {
            result.add((S) builder.build());
        }
        return result;
    }

    private void fitPartial(MultiDataSet dataSet, List<S.Builder> featureStatsBuilders,
                            List<S.Builder> labelStatsBuilders) {
        int numInputs = dataSet.numFeatureArrays();
        int numOutputs = dataSet.numLabelsArrays();

        ensureStatsBuilders(featureStatsBuilders, numInputs);
        ensureStatsBuilders(labelStatsBuilders, numOutputs);

        for (int i = 0; i < numInputs; i++) {
            featureStatsBuilders.get(i).add(dataSet.getFeatures(i), dataSet.getFeaturesMaskArray(i));
        }

        if (isFitLabel()) {
            for (int i = 0; i < numOutputs; i++) {
                labelStatsBuilders.get(i).add(dataSet.getLabels(i), dataSet.getLabelsMaskArray(i));
            }
        }
    }

    private void ensureStatsBuilders(List<S.Builder> builders, int amount) {
        if (builders.isEmpty()) {
            for (int i = 0; i < amount; i++) {
                builders.add(newBuilder());
            }
        }
    }

    protected abstract S.Builder newBuilder();

    /**
     * Pre process a MultiDataSet
     *
     * @param toPreProcess the data set to pre process
     */
    @Override
    public void preProcess(@NonNull MultiDataSet toPreProcess) {
        int numFeatures = toPreProcess.numFeatureArrays();
        int numLabels = toPreProcess.numLabelsArrays();

        for (int i = 0; i < numFeatures; i++) {
            strategy.preProcess(toPreProcess.getFeatures(i), toPreProcess.getFeaturesMaskArray(i), getFeatureStats(i));
        }
        if (isFitLabel()) {
            for (int i = 0; i < numLabels; i++) {
                strategy.preProcess(toPreProcess.getLabels(i), toPreProcess.getLabelsMaskArray(i), getLabelStats(i));
            }
        }
    }

    /**
     * Revert the data to what it was before transform
     *
     * @param data the dataset to revert back
     */
    public void revert(@NonNull MultiDataSet data) {
        revertFeatures(data.getFeatures(), data.getFeaturesMaskArrays());
        revertLabels(data.getLabels(), data.getLabelsMaskArrays());
    }

    /**
     * Undo (revert) the normalization applied by this normalizer to the features arrays
     *
     * @param features Features to revert the normalization on
     */
    public void revertFeatures(@NonNull INDArray[] features, INDArray[] maskArrays) {
        for (int i = 0; i < features.length; i++) {
            INDArray mask = (maskArrays == null ? null : maskArrays[i]);
            revertFeatures(features[i], mask, i);
        }
    }

    /**
     * Undo (revert) the normalization applied by this normalizer to a specific features array.
     * If labels normalization is disabled (i.e., {@link #isFitLabel()} == false) then this is a no-op.
     * Can also be used to undo normalization for network output arrays, in the case of regression.
     *
     * @param features features arrays to revert the normalization on
     * @param input    the index of the array to revert
     */
    public void revertFeatures(@NonNull INDArray features, INDArray mask, int input) {
        strategy.revert(features, mask, getFeatureStats(input));
    }

    /**
     * Undo (revert) the normalization applied by this normalizer to the labels arrays.
     * If labels normalization is disabled (i.e., {@link #isFitLabel()} == false) then this is a no-op.
     * Can also be used to undo normalization for network output arrays, in the case of regression.
     *
     * @param labels Labels arrays to revert the normalization on
     */
    public void revertLabels(@NonNull INDArray[] labels, INDArray[] labelsMask) {
        for (int i = 0; i < labels.length; i++) {
            INDArray mask = (labelsMask == null ? null : labelsMask[i]);
            revertLabels(labels[i], mask, i);
        }
    }

    /**
     * Undo (revert) the normalization applied by this normalizer to a specific labels array.
     * If labels normalization is disabled (i.e., {@link #isFitLabel()} == false) then this is a no-op.
     * Can also be used to undo normalization for network output arrays, in the case of regression.
     *
     * @param labels Labels arrays to revert the normalization on
     * @param output the index of the array to revert
     */
    public void revertLabels(@NonNull INDArray labels, INDArray mask, int output) {
        if (isFitLabel()) {
            strategy.revert(labels, mask, getLabelStats(output));
        }
    }

    /**
     * Get the number of inputs
     */
    public int numInputs() {
        return getFeatureStats().size();
    }

    /**
     * Get the number of outputs
     */
    public int numOutputs() {
        return getLabelStats().size();
    }
}
