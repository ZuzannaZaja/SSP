package pl.edu.agh.kt;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import net.floodlightcontroller.linkdiscovery.ILinkDiscovery;
import net.floodlightcontroller.topology.NodePortTuple;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.IPv4Address;
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

    private static final Logger logger = LoggerFactory.getLogger(FamtarTopology.class);
    public static final int DEFAULT_LINK_COST = 1;
    public static final int MAX_LINK_COST = 10;
    private static FamtarTopology singleton;

    // (switch from, switch to) -> [sw1:port1, sw2:port3, ...]
    private Map<Pair<DatapathId, DatapathId>, List<NodePortTuple>> paths;
    private ConcurrentMap<Edge, Integer> links;

    // leaving this static for now
    public static Map<IPv4Address, DatapathId> ipDatapathIdMapping = new ImmutableMap.Builder<IPv4Address, DatapathId>()
            .put(IPv4Address.of(10, 0, 0, 1), DatapathId.of(1L))
            .put(IPv4Address.of(10, 0, 0, 2), DatapathId.of(4L))
            .build();

    private FamtarTopology()
    {
        links = new ConcurrentHashMap<>();
        paths = new HashMap<>();
    }

    public void calculatePaths()
    {
        final Sets.SetView<DatapathId> nodes = getNodes();
        for (List<DatapathId> pair : Sets.cartesianProduct(nodes, nodes)) {
            final DatapathId from = pair.get(0);
            final DatapathId to = pair.get(1);
            if (!from.equals(to)) {
                paths.put(new Pair<>(from, to), Dijkstra.getShortestPath(links, from, to));
            }
        }
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
//        logger.debug("getInstance() begin");
        synchronized (FamtarTopology.class) {
            if (singleton == null) {
//                logger.debug("Creating Routing singleton");
                singleton = new FamtarTopology();
            }
        }
//        logger.debug("getInstance() end");
        return singleton;
    }

    public void addLink(final ILinkDiscovery.LDUpdate linkUpdate)
    {
        addLink(linkUpdate, DEFAULT_LINK_COST);
    }

    public void addLink(final ILinkDiscovery.LDUpdate linkUpdate, int cost)
    {
        links.putIfAbsent(Edge.from(linkUpdate), cost);
        logAllLinks();
    }

    public void removeLink(final ILinkDiscovery.LDUpdate linkUpdate)
    {
        links.remove(Edge.from(linkUpdate));
        logAllLinks();
    }

    public void updateLinkCost(Edge edge, int cost)
    {
        logger.debug("previous cost on link {} was {}...", edge, links.get(edge));
        links.replace(edge, cost);
        logger.debug("...now the it is {}: {}", edge, links.get(edge));
    }

    private Sets.SetView<DatapathId> getNodes()
    {
        return Sets.union(
                FluentIterable.from(links.keySet()).transform(new Function<Edge, DatapathId>()
                {
                    @Override
                    public DatapathId apply(final Edge edge)
                    {
                        return edge.getFrom().getNodeId();
                    }
                }).toImmutableSet(),
                FluentIterable.from(links.keySet()).transform(new Function<Edge, DatapathId>()
                {
                    @Override
                    public DatapathId apply(final Edge edge)
                    {
                        return edge.getTo().getNodeId();
                    }
                }).toImmutableSet());
    }

    private void logAllLinks()
    {
        logger.debug("Current state of links map ({} entries)", links.size());
        for (Map.Entry<Edge, Integer> entry : links.entrySet()) {
            logger.debug("\t {}: {}", entry.getKey(), entry.getValue());
        }
    }
}
