package pl.edu.agh.kt;


import com.google.common.util.concurrent.ListenableFuture;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.routing.Link;
import net.floodlightcontroller.statistics.IStatisticsService;
import net.floodlightcontroller.statistics.StatisticsCollector;
import net.floodlightcontroller.statistics.SwitchPortBandwidth;
import net.floodlightcontroller.topology.ITopologyService;
import net.floodlightcontroller.topology.NodePortTuple;
import net.floodlightcontroller.topology.TopologyManager;

import org.projectfloodlight.openflow.protocol.OFPortStatsEntry;
import org.projectfloodlight.openflow.protocol.OFPortStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsRequest;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.U64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.edu.agh.kt.Dijkstra.Edge;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class FamtarStatisticsCollector
{
    private static final Logger logger = LoggerFactory.getLogger(FamtarStatisticsCollector.class);
    private static final Random RANDOM = new Random();
    private long maxSpeed = (long) 10e7;
    private IStatisticsService statisticsCollector;
    
    private Map<Link, Integer> linksCosts;
    private ITopologyService topologyService;

    public class PortStatisticsPoller extends TimerTask
    {
        private final Logger logger = LoggerFactory.getLogger(PortStatisticsPoller.class);

        public void run()
        {
        	boolean changed = false;
        	Map<NodePortTuple, Set<Link>> switchPortLinks = ((TopologyManager) topologyService).getSwitchPortLinks();
            
            final Map<NodePortTuple, SwitchPortBandwidth> bandwidthMeasurements = statisticsCollector.getBandwidthConsumption(); 
            
            if(RANDOM.nextFloat()>0.7){
            	logger.debug("Current state of port bandwidth utilization ({} entries)", bandwidthMeasurements.size());
                for (Map.Entry<NodePortTuple, SwitchPortBandwidth> bandwidthMeasurement : bandwidthMeasurements.entrySet()) {
                    logger.debug("\t {}: {}", bandwidthMeasurement.getKey(), String.format(
                            "RX: %s, TX: %s", bandwidthMeasurement.getValue().getBitsPerSecondRx().getValue(), bandwidthMeasurement.getValue().getBitsPerSecondTx().getValue()));
                }
            } 	
            
            for (Map.Entry<NodePortTuple, SwitchPortBandwidth> bandwidthMeasurement : bandwidthMeasurements.entrySet()) {               
                if(bandwidthMeasurement.getValue().getBitsPerSecondTx().getValue() >= 0.9*maxSpeed){
                	for(Map.Entry<NodePortTuple, Set<Link>> switchPortLink : switchPortLinks.entrySet()){
                		if(switchPortLink.getKey().equals(bandwidthMeasurement.getKey())){
                			linksCosts.put(switchPortLink.getValue().iterator().next(), FamtarTopology.MAX_LINK_COST);
                			changed = true;
                		}
                	}
                }                    
                
                if(bandwidthMeasurement.getValue().getBitsPerSecondTx().getValue() <= 0.7*maxSpeed){
                	for(Map.Entry<NodePortTuple, Set<Link>> switchPortLink : switchPortLinks.entrySet()){
                		if(switchPortLink.getKey().equals(bandwidthMeasurement.getKey())){
                			linksCosts.put(switchPortLink.getValue().iterator().next(), FamtarTopology.DEFAULT_LINK_COST);
                			changed = true;
                		}
                	}
                }
            }
            if(changed){
            	logger.debug("Calculate shortest paths");
            }
        }
    }

    public static final int PORT_STATISTICS_POLLING_INTERVAL = 9000; // in ms
    private static FamtarStatisticsCollector singleton;

    private FamtarStatisticsCollector(IStatisticsService statisticsCollector, ITopologyService topologyService)
    {
        this.statisticsCollector = statisticsCollector;
        this.topologyService = topologyService;
        this.linksCosts = new HashMap<>();
        new Timer().scheduleAtFixedRate(new PortStatisticsPoller(), 0, PORT_STATISTICS_POLLING_INTERVAL);
    }

    public static FamtarStatisticsCollector getInstance(IStatisticsService statisticsCollector, ITopologyService topologyService)
    {
//        logger.debug("getInstance() begin");
        synchronized (FamtarStatisticsCollector.class) {
            if (singleton == null) {
//                logger.debug("Creating FamtarStatisticsCollector singleton");
                singleton = new FamtarStatisticsCollector(statisticsCollector, topologyService);
            }
        }

//        logger.debug("getInstance() end");
        return singleton;
    }
    
    public Map<Link, Integer> getLinksCosts()
    {   	
    	return this.linksCosts;
    }
}
