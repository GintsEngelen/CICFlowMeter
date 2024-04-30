package dev.sequana.cyber;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleSupplier;
import java.util.stream.Stream;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import cic.cs.unb.ca.jnetpcap.BasicFlow;
import cic.cs.unb.ca.jnetpcap.BasicPacketInfo;
import cic.cs.unb.ca.jnetpcap.FlowFeature;

public class CostMeasuredSummaryStatistics extends SummaryStatistics implements CostMeasuredDataStructure {
    private Map<FlowFeature, CostMeasurements> featureCostMeasurements;

    // TODO: Something about current context
    private long startTime = 0L;
    private long duration = 0L;
    private long operationTimestamp;
    private boolean noActiveMeasurements = true;

    public long previousStartTime;

    public CostMeasuredSummaryStatistics(
            BasicFlow flow,
            Map<FlowFeature, CostMeasurements> featureCosts,
            FlowFeature... associatedFeatures
    ) {
        this.featureCostMeasurements = new HashMap<>(associatedFeatures.length);

        for (FlowFeature feature : associatedFeatures) {
            CostMeasurements measurements = new CostMeasurements(flow);

            // Locally track this cost measurement.
            featureCostMeasurements.put(feature, measurements);

            // Allow the outside world to also have a reference to this measurement.
            featureCosts.put(feature, measurements);
        }
    }

    public CostMeasuredSummaryStatistics() {}

    public void startMeasuring(BasicPacketInfo packet) {
        this.operationTimestamp = packet.getTimeStamp();
        this.startMeasuring();
    }

    public void startMeasuring() {
        this.noActiveMeasurements = false;
        startTime = System.nanoTime();
    }

    public void stopMeasuring(String operationName, FlowFeature... onlyAppliesToFeatures) {
        duration = System.nanoTime() - startTime;
        this.previousStartTime = startTime;

        if (this.noActiveMeasurements) {
            return;
        }

        this.noActiveMeasurements = true;

        Stream<FlowFeature> featuresToUpdate;
        if (onlyAppliesToFeatures.length == 0) {
            // Measurement applies to all features
            featuresToUpdate = this.featureCostMeasurements.keySet().stream();
        } else {
            featuresToUpdate = Arrays.asList(onlyAppliesToFeatures).stream();
        }

        featuresToUpdate.forEach(
            (feature) -> featureCostMeasurements.get(feature)
                .addMeasurement(
                    operationName,
                    this.operationTimestamp,
                    this.duration,
                    0L
                )
        );

        this.startTime = 0L;
        this.operationTimestamp = -1;
    }

    public void addValue(BasicPacketInfo packet, String operationName, DoubleSupplier sourceOperation) {
        startMeasuring(packet);
        super.addValue(sourceOperation.getAsDouble());
        stopMeasuring(operationName);
    }

    public long getN(FlowFeature... onlyAppliesToFeatures) {
        startMeasuring();
        long n = super.getN();
        stopMeasuring("getN", onlyAppliesToFeatures);
        return n;
    }

    @Override
    public Long getStartTime() {
        return this.startTime;
    }

    // /**
    //  * Finish taking measurements for the features associated with this measurement
    //  * and record them to a file for later analysis.
    //  * @param flowId
    //  */
    // public void finalMeasurements(String flowId) {
    //     CSVFormat
    // }
}
