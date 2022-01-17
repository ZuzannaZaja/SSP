package pl.edu.agh.kt;

import com.google.common.collect.ImmutableMap;
import net.floodlightcontroller.topology.NodePortTuple;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.OFPort;
import org.sdnplatform.sync.internal.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.edu.agh.kt.Dijkstra.Edge;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    2. set link cost -- called from FamtarStatsListener
    any topology change (new node, node removal or cost change) triggers Dijkstra recalculation
    * */

    public static final int DEFAULT_LINK_COST = 1;
    public static final int MAX_LINK_COST = 10;
    private static FamtarTopology singleton;

    private static final Logger logger = LoggerFactory.getLogger(FamtarTopology.class);

    // (switch from, switch to) -> [sw1:port1, sw2:port3, ...]
    private Map<Pair<DatapathId, DatapathId>, List<NodePortTuple>> paths;
    private ConcurrentMap<Edge, Integer> links;

    public static Map<IPv4Address, NodePortTuple> HOSTS_MAPPING = new ImmutableMap.Builder<IPv4Address, NodePortTuple>()
            .put(IPv4Address.of(10, 0, 0, 1), new NodePortTuple(DatapathId.of(1L), OFPort.of(4)))
            .put(IPv4Address.of(10, 0, 0, 2), new NodePortTuple(DatapathId.of(4L), OFPort.of(4)))
            .build();

    private FamtarTopology()
    {
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

    public List<NodePortTuple> getPath(final DatapathId from, final DatapathId to)
    {
        return paths.get(new Pair<>(from, to));
    }

    public Map<Edge, Integer> getLinks()
    {
        return links;
    }

    public static FamtarTopology getInstance()
    {
        synchronized (FamtarTopology.class) {
            if (singleton == null) {
                singleton = new FamtarTopology();
            }
        }
        return singleton;
    }
}
