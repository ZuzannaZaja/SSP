package pl.edu.agh.kt;


import net.floodlightcontroller.routing.Link;
import net.floodlightcontroller.statistics.IStatisticsService;
import net.floodlightcontroller.statistics.SwitchPortBandwidth;
import net.floodlightcontroller.topology.ITopologyService;
import net.floodlightcontroller.topology.NodePortTuple;
import net.floodlightcontroller.topology.TopologyManager;
import org.projectfloodlight.openflow.types.U64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;


public class FamtarStatisticsCollector
{
    private static final Random RANDOM = new Random();
    private static final int PORT_STATISTICS_POLLING_INTERVAL = 2000; // in ms
    private static final long MAX_SPEED = (long) 10E7; // in bps
    private static final double INCREASE_THRESHOLD = 0.9; // 0.9
    private static final double DECREASE_THRESHOLD = 0.7; // 0.7
    private static FamtarStatisticsCollector singleton;
    private static final Logger logger = LoggerFactory.getLogger(FamtarStatisticsCollector.class);

    private IStatisticsService statisticsCollector;
    private ITopologyService topologyService;
    private FamtarTopology famtarTopology;

    private FamtarStatisticsCollector(IStatisticsService statisticsCollector,
                                      ITopologyService topologyService,
                                      FamtarTopology famtarTopology)
    {
        this.statisticsCollector = statisticsCollector;
        this.topologyService = topologyService;
        this.famtarTopology = famtarTopology;
        new Timer().scheduleAtFixedRate(new BandwidthMonitor(), 0, PORT_STATISTICS_POLLING_INTERVAL);
    }

    public static FamtarStatisticsCollector getInstance(IStatisticsService statisticsCollector,
                                                        ITopologyService topologyService,
                                                        FamtarTopology famtarTopology)
    {
        synchronized (FamtarStatisticsCollector.class) {
            if (singleton == null) {
                singleton = new FamtarStatisticsCollector(statisticsCollector, topologyService, famtarTopology);
            }
        }
        return singleton;
    }

    public class BandwidthMonitor extends TimerTask
    {
        private final Logger logger = LoggerFactory.getLogger(BandwidthMonitor.class);

        public void run()
        {
//            logger.debug("checking bandwidth utilization...");
            boolean changed = false;
            Map<Link, Integer> linksCosts = initializeLinksCosts();

            for (Map.Entry<NodePortTuple, SwitchPortBandwidth> measurement : statisticsCollector.getBandwidthConsumption().entrySet()) {
                int updatedCost = getCost(measurement);
                changed = updatedCost > 0;
                updateLinkCost(linksCosts, measurement, updatedCost);
            }

            if (changed) {
//                logger.debug("Calling shortest paths recalculation");
                famtarTopology.calculatePaths(linksCosts);
            }
//            logger.debug("...done");
        }

        private Map<Link, Integer> initializeLinksCosts()
        {
            Map<Link, Integer> linksCosts = new HashMap<>();
            Map<NodePortTuple, Set<Link>> switchPortLinks = ((TopologyManager) topologyService).getSwitchPortLinks();
            for (Map.Entry<NodePortTuple, Set<Link>> e : switchPortLinks.entrySet()) {
                for (Link link : e.getValue()) {
                    linksCosts.put(link, FamtarTopology.DEFAULT_LINK_COST);
                }
            }
            return linksCosts;
        }

        private int getCost(Map.Entry<NodePortTuple, SwitchPortBandwidth> bandwidthMeasurement)
        {
            if (isTxRxAbove(bandwidthMeasurement)) {
                return FamtarTopology.MAX_LINK_COST;
            } else if (isTxRxBelow(bandwidthMeasurement)) {
                return FamtarTopology.DEFAULT_LINK_COST;
            }
            return -1;
        }

        private void updateLinkCost(Map<Link, Integer> linksCosts,
                                    Map.Entry<NodePortTuple, SwitchPortBandwidth> bandwidthMeasurement,
                                    int updatedCost)
        {
            Map<NodePortTuple, Set<Link>> switchPortLinks = ((TopologyManager) topologyService).getSwitchPortLinks();
            for (Map.Entry<NodePortTuple, Set<Link>> switchPortLink : switchPortLinks.entrySet()) {
                if (isTheSameLink(bandwidthMeasurement, switchPortLink)) {
                    for (Link link : switchPortLink.getValue()) {
                        linksCosts.put(link, updatedCost);
//                        logger.debug("\t changed cost on {} to {}", link, updatedCost);
                    }
                }
            }
        }

        private boolean isTxRxBelow(final Map.Entry<NodePortTuple, SwitchPortBandwidth> bandwidthMeasurement)
        {
            return bandwidthMeasurement.getValue().getBitsPerSecondTx()
                    .compareTo(U64.of((long) (DECREASE_THRESHOLD * MAX_SPEED))) < 0 ||
                    bandwidthMeasurement.getValue().getBitsPerSecondRx()
                            .compareTo(U64.of((long) (DECREASE_THRESHOLD * MAX_SPEED))) < 0;
        }

        private boolean isTxRxAbove(Map.Entry<NodePortTuple, SwitchPortBandwidth> bandwidthMeasurement)
        {
            return bandwidthMeasurement.getValue().getBitsPerSecondTx()
                    .compareTo(U64.of((long) (INCREASE_THRESHOLD * MAX_SPEED))) > 0 ||
                    bandwidthMeasurement.getValue().getBitsPerSecondRx()
                            .compareTo(U64.of((long) (INCREASE_THRESHOLD * MAX_SPEED))) > 0;
        }

        private void logBandwidthMeasurements(Map<NodePortTuple, SwitchPortBandwidth> bandwidthMeasurements)
        {
            logger.debug("Current state of port bandwidth utilization ({} entries)", bandwidthMeasurements.size());
            for (Map.Entry<NodePortTuple, SwitchPortBandwidth> bandwidthMeasurement : bandwidthMeasurements.entrySet()) {
                if (getTxbps(bandwidthMeasurement).compareTo(BigDecimal.valueOf(100)) > 0) {
                    logger.debug("\t {}: {}", bandwidthMeasurement.getKey(), String.format(
                            "RX: %s, TX: %s", getRxbps(bandwidthMeasurement), getTxbps(bandwidthMeasurement)));
                }
            }
        }

        private void logLinksCosts(Map<Link, Integer> linksCosts)
        {
            logger.debug("Current state of link costs ({} entries)", linksCosts.size());
            for (Map.Entry<Link, Integer> entry : linksCosts.entrySet()) {
                logger.debug("\t {}: {}", entry.getKey(), entry.getValue());
            }
        }

        private void logSwitchPortLinks(Map<NodePortTuple, Set<Link>> switchPortLinks)
        {
            logger.debug("Current state of switch port links ({} entries)", switchPortLinks.size());
            for (Map.Entry<NodePortTuple, Set<Link>> entry : switchPortLinks.entrySet()) {
                logger.debug("\t {}: {}", entry.getKey().toKeyString(), entry.getValue().toArray());
            }
        }

        private BigDecimal getRxbps(Map.Entry<NodePortTuple, SwitchPortBandwidth> bandwidthMeasurement)
        {
            return new BigDecimal(bandwidthMeasurement.getValue().getBitsPerSecondRx().getBigInteger());
        }

        private BigDecimal getTxbps(Map.Entry<NodePortTuple, SwitchPortBandwidth> bandwidthMeasurement)
        {
            return new BigDecimal(bandwidthMeasurement.getValue().getBitsPerSecondTx().getBigInteger());
        }

        private boolean isTheSameLink(Map.Entry<NodePortTuple, SwitchPortBandwidth> bandwidthMeasurement,
                                      Map.Entry<NodePortTuple, Set<Link>> switchPortLink)
        {
            return switchPortLink.getKey().equals(bandwidthMeasurement.getKey());
        }
    }
}
