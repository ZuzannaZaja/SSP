package pl.edu.agh.kt;

import org.projectfloodlight.openflow.types.DatapathId;
import org.sdnplatform.sync.internal.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Routing
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

    private static final Logger logger = LoggerFactory.getLogger(Routing.class);
    private static Routing singleton;

    // (from, to), link info
    private Set<DatapathId> nodes;
    private Map<Pair<DatapathId, DatapathId>, String> links;
    private Map<Pair<DatapathId, DatapathId>, List<Pair<DatapathId, DatapathId>>> paths;

    private Routing()
    {
        nodes = new HashSet<>();
        links = new HashMap<>();
        paths = new HashMap<>();
    }

    public void addLink(DatapathId from, DatapathId to, String linkInformation)
    {
        nodes.add(from);
        nodes.add(to);
        links.put(new Pair<>(from, to), linkInformation);
//        logAllLinks();
    }

    private void calculatePaths()
    {

    }

    private void logAllLinks()
    {
        logger.debug("Current state of links map");
        for (Map.Entry<Pair<DatapathId, DatapathId>, String> entry : links.entrySet()) {
            logger.debug("\t {}: {}", entry.getKey(), entry.getValue());
        }
    }

    public static Routing getInstance()
    {
        logger.debug("getInstance() begin");
        synchronized (Routing.class) {
            if (singleton == null) {
                logger.debug("Creating Routing singleton");
                singleton = new Routing();
            }
        }
        logger.debug("getInstance() end");
        return singleton;
    }
}
