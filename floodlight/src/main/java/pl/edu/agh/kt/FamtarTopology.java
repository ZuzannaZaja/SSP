package pl.edu.agh.kt;

import com.google.common.collect.ImmutableMap;
import net.floodlightcontroller.routing.BroadcastTree;
import net.floodlightcontroller.routing.Link;
import net.floodlightcontroller.topology.ITopologyService;
import net.floodlightcontroller.topology.NodePortTuple;
import net.floodlightcontroller.topology.TopologyInstance;
import net.floodlightcontroller.topology.TopologyManager;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

    private static final Logger logger = LoggerFactory.getLogger(FamtarTopology.class);

    private ITopologyService topologyService;

    //switch from, switch to, [sw1:port1, sw2:port3, ...]
//    private Table<DatapathId, DatapathId, List<NodePortTuple>> routes;
    private Map<IPv4Address, List<Link>> routes;
    private Map<Link, Integer> previousCosts;

    private static final IPv4Address HOST_ONE = IPv4Address.of(10, 0, 0, 1);
    private static final IPv4Address HOST_TWO = IPv4Address.of(10, 0, 0, 2);

    public static Map<IPv4Address, NodePortTuple> HOSTS_MAPPING = new ImmutableMap.Builder<IPv4Address, NodePortTuple>()
            .put(HOST_ONE, new NodePortTuple(DatapathId.of(1L), OFPort.of(4)))
            .put(HOST_TWO, new NodePortTuple(DatapathId.of(4L), OFPort.of(4)))
            .build();

    FamtarTopology(ITopologyService topologyService)
    {
        this.topologyService = topologyService;
        this.routes = new HashMap<>();
        this.previousCosts = new HashMap<>();
    }

    public void calculatePaths(final Map<Link, Integer> linksCosts)
    {
        if (isCostMappingChanged(linksCosts)) {
            logLinksCosts(linksCosts);
            logger.debug("calculating paths...");
            final BroadcastTree fromOne = buildShortestPaths(DatapathId.of(1), linksCosts);
            List<Link> routeToSwitchFour = getPath(DatapathId.of(1), DatapathId.of(4), fromOne);
            routes.put(HOST_TWO, routeToSwitchFour);

            final BroadcastTree fromFour = buildShortestPaths(DatapathId.of(4), linksCosts);
            List<Link> routeToSwitchOne = getPath(DatapathId.of(4), DatapathId.of(1), fromFour);
            routes.put(HOST_TWO, routeToSwitchOne);

            this.previousCosts = ImmutableMap.copyOf(linksCosts);
        }
    }

    private List<Link> getPath(DatapathId src, DatapathId dst, BroadcastTree tree)
    {
        logger.debug("building path from {} to {}", src, dst);
        LinkedList<Link> hops = new LinkedList<>();
        DatapathId current = dst;
        while (!current.equals(src)) {
            logger.debug("\t current node: {}", current);
            Link link = tree.getTreeLink(current);
            logger.debug("\t adding link: {}", link);
            hops.add(link);
            current = link.getSrc();
        }
        return hops;
    }

    public List<Link> getPath(IPv4Address from, IPv4Address to)
    {
        logger.debug("getting route: {} -> {}", from, to);
        return routes.get(to);
    }

    //TODO: wrap this with try/catch - NPE from null topology
    private BroadcastTree buildShortestPaths(DatapathId root, Map<Link, Integer> linksCosts)
    {
        final TopologyManager topologyManager = (TopologyManager) this.topologyService;
        final TopologyInstance topologyInstance = topologyManager.getCurrentInstance();

        //TODO: true or false? -- we're adding flows from the end of the path
        final boolean isDstRooted = false;
        final BroadcastTree dijkstraBroadcastTree = topologyInstance.dijkstra(
                this.topologyService.getAllLinks(),
                root,
                linksCosts,
                isDstRooted);

        logger.debug(String.format(
                "Built the following broadcast tree with switch_%s as root (isDstRooted = %s)\n%s",
                root, isDstRooted, dijkstraBroadcastTree.toString()));

        return dijkstraBroadcastTree;
    }

    private boolean isCostMappingChanged(final Map<Link, Integer> linksCosts)
    {
        return !this.previousCosts.entrySet().equals(linksCosts.entrySet());
    }

    private void logLinksCosts(Map<Link, Integer> linksCosts)
    {
        logger.debug("Link costs ({} entries)", linksCosts.size());
        for (Map.Entry<Link, Integer> entry : linksCosts.entrySet()) {
            logger.debug("\t {}: {}", entry.getKey(), entry.getValue());
        }
    }
}
