package pl.edu.agh.kt;

import net.floodlightcontroller.linkdiscovery.ILinkDiscovery;
import net.floodlightcontroller.topology.NodePortTuple;
import org.projectfloodlight.openflow.types.DatapathId;
import org.sdnplatform.sync.internal.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.edu.agh.kt.Dijkstra.Edge;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class FamtarTopology
{
    //TODO keep the current topology image -- the nodes, links, their costs and capacities (12 links -> 24 entries)
    //TODO make it a singleton -- done
    //TODO use a map of routes between all nodes (7 nodes -> 21 pairs, not a problem)
    //TODO storing IP to port mappings (we can deal with fixed entries and just differentiate flows on UDP ports)

    /*
    TODO Methods
    1. accept a node to the topology -- check with the LLDP
    2. set link cost -- called from FamtarListener
    any topology change (new node, node removal or cost change) triggers Dijkstra recalculation
    * */

    private static final Logger logger = LoggerFactory.getLogger(FamtarTopology.class);
    private static final int DEFAULT_LINK_COST = 1;
    private static FamtarTopology singleton;

    private Set<DatapathId> nodes;
    //    private Map<Pair<DatapathId, DatapathId>, String> links;
    // (switch from, switch to) -> [sw1:port1, sw2:port3, ...]
    private Map<Pair<DatapathId, DatapathId>, List<NodePortTuple>> paths;
    private ConcurrentMap<Edge, Integer> links;

    private FamtarTopology()
    {
        nodes = new HashSet<>();
        links = new ConcurrentHashMap<>();
        paths = new HashMap<>();
    }

    public void calculatePaths()
    {

    }

    public Map<Pair<DatapathId, DatapathId>, List<NodePortTuple>> getPaths()
    {
        return paths;
    }

    public Map<Edge, Integer> getLinks()
    {
        return links;
    }

    public static FamtarTopology getInstance()
    {
        logger.debug("getInstance() begin");
        synchronized (FamtarTopology.class) {
            if (singleton == null) {
                logger.debug("Creating Routing singleton");
                singleton = new FamtarTopology();
            }
        }
        logger.debug("getInstance() end");
        return singleton;
    }

    public void addLink(final ILinkDiscovery.LDUpdate linkUpdate)
    {
        addLink(linkUpdate, DEFAULT_LINK_COST);
    }

    public void addLink(final ILinkDiscovery.LDUpdate linkUpdate, int cost)
    {
        nodes.add(linkUpdate.getSrc());
        nodes.add(linkUpdate.getDst());
        links.putIfAbsent(Edge.from(linkUpdate), cost);
        logAllLinks();
    }

    public void removeLink(final ILinkDiscovery.LDUpdate linkUpdate)
    {
        links.remove(Edge.from(linkUpdate));
        logAllLinks();
    }

    private void logAllLinks()
    {
        logger.debug("Current state of links map ({} entries)", links.size());
        for (Map.Entry<Edge, Integer> entry : links.entrySet()) {
            logger.debug("\t {}: {}", entry.getKey(), entry.getValue());
        }
    }
}
