package pl.edu.agh.kt;


import net.floodlightcontroller.routing.Link;
import net.floodlightcontroller.statistics.IStatisticsService;
import net.floodlightcontroller.statistics.SwitchPortBandwidth;
import net.floodlightcontroller.topology.ITopologyService;
import net.floodlightcontroller.topology.NodePortTuple;
import net.floodlightcontroller.topology.TopologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;


public class FamtarStatisticsCollector
{

    private static final Random RANDOM = new Random();
    private static final int PORT_STATISTICS_POLLING_INTERVAL = 9000; // in ms
    private static final long MAX_SPEED = (long) 10E7; // in bps
    private static FamtarStatisticsCollector singleton;
    private static final Logger logger = LoggerFactory.getLogger(FamtarStatisticsCollector.class);

    private IStatisticsService statisticsCollector;
    private ITopologyService topologyService;
    private Map<Link, Integer> linksCosts;

    private FamtarStatisticsCollector(IStatisticsService statisticsCollector, ITopologyService topologyService)
    {
        this.statisticsCollector = statisticsCollector;
        this.topologyService = topologyService;
        this.linksCosts = new HashMap<>();
        new Timer().scheduleAtFixedRate(new BandwidthMonitor(), 0, PORT_STATISTICS_POLLING_INTERVAL);
    }

    public static FamtarStatisticsCollector getInstance(IStatisticsService statisticsCollector, ITopologyService topologyService)
    {
        synchronized (FamtarStatisticsCollector.class) {
            if (singleton == null) {
                singleton = new FamtarStatisticsCollector(statisticsCollector, topologyService);
            }
        }
        return singleton;
    }

    public Map<Link, Integer> getLinksCosts()
    {
        return this.linksCosts;
    }

    public class BandwidthMonitor extends TimerTask
    {
        private final Logger logger = LoggerFactory.getLogger(BandwidthMonitor.class);

        public void run()
        {
            boolean changed = false;
            final Map<Link, Integer> previousLinkCosts = getLinksCosts();
            Map<NodePortTuple, Set<Link>> switchPortLinks = ((TopologyManager) topologyService).getSwitchPortLinks();

            final Map<NodePortTuple, SwitchPortBandwidth> bandwidthMeasurements = statisticsCollector.getBandwidthConsumption();

            if (RANDOM.nextFloat() > 0.7) {
                logBandwidthMeasurements(bandwidthMeasurements);
            }

            for (Map.Entry<NodePortTuple, SwitchPortBandwidth> bandwidthMeasurement : bandwidthMeasurements.entrySet()) {
                if (getTxbps(bandwidthMeasurement) >= 0.9 * MAX_SPEED) {
                    for (Map.Entry<NodePortTuple, Set<Link>> switchPortLink : switchPortLinks.entrySet()) {
                        if (isTheSameLink(bandwidthMeasurement, switchPortLink)) {
                            final Link link = unpackLink(switchPortLink);
                            linksCosts.put(link, FamtarTopology.MAX_LINK_COST);
                            changed = previousLinkCosts.get(link) != FamtarTopology.MAX_LINK_COST;
                        }
                    }
                }

                if (getTxbps(bandwidthMeasurement) <= 0.7 * MAX_SPEED) {
                    for (Map.Entry<NodePortTuple, Set<Link>> switchPortLink : switchPortLinks.entrySet()) {
                        if (isTheSameLink(bandwidthMeasurement, switchPortLink)) {
                            final Link link = unpackLink(switchPortLink);
                            linksCosts.put(link, FamtarTopology.DEFAULT_LINK_COST);
                            changed = previousLinkCosts.get(link) != FamtarTopology.DEFAULT_LINK_COST;
                        }
                    }
                }
            }
            if (changed) {
                logger.debug("Calculate shortest paths");
            }
        }

        private void logBandwidthMeasurements(final Map<NodePortTuple, SwitchPortBandwidth> bandwidthMeasurements)
        {
            logger.debug("Current state of port bandwidth utilization ({} entries)", bandwidthMeasurements.size());
            for (Map.Entry<NodePortTuple, SwitchPortBandwidth> bandwidthMeasurement : bandwidthMeasurements.entrySet()) {
                logger.debug("\t {}: {}", bandwidthMeasurement.getKey(), String.format(
                        "RX: %s, TX: %s", getRxbps(bandwidthMeasurement), getTxbps(bandwidthMeasurement)));
            }
        }

        private Link unpackLink(final Map.Entry<NodePortTuple, Set<Link>> switchPortLink)
        {
            return switchPortLink.getValue().iterator().next();
        }

        private long getRxbps(final Map.Entry<NodePortTuple, SwitchPortBandwidth> bandwidthMeasurement)
        {
            return bandwidthMeasurement.getValue().getBitsPerSecondRx().getValue();
        }

        private long getTxbps(final Map.Entry<NodePortTuple, SwitchPortBandwidth> bandwidthMeasurement)
        {
            return bandwidthMeasurement.getValue().getBitsPerSecondTx().getValue();
        }

        private boolean isTheSameLink(final Map.Entry<NodePortTuple, SwitchPortBandwidth> bandwidthMeasurement, final Map.Entry<NodePortTuple, Set<Link>> switchPortLink)
        {
            return switchPortLink.getKey().equals(bandwidthMeasurement.getKey());
        }
    }
}
