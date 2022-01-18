package pl.edu.agh.kt;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
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
    private Table<DatapathId, DatapathId, List<NodePortTuple>> routes;
    private Map<Link, Integer> previousCosts;

    public static Map<IPv4Address, NodePortTuple> HOSTS_MAPPING = new ImmutableMap.Builder<IPv4Address, NodePortTuple>()
            .put(IPv4Address.of(10, 0, 0, 1), new NodePortTuple(DatapathId.of(1L), OFPort.of(4)))
            .put(IPv4Address.of(10, 0, 0, 2), new NodePortTuple(DatapathId.of(4L), OFPort.of(4)))
            .build();

    FamtarTopology(ITopologyService topologyService)
    {
        this.topologyService = topologyService;
        this.routes = TreeBasedTable.create();
        this.previousCosts = new HashMap<>();
    }

    public void calculatePaths(final Map<Link, Integer> linksCosts)
    {
        if (isCostMappingChanged(linksCosts)) {
            logLinksCosts(linksCosts);
            logger.debug("calculating paths...");
            //TODO: make this store previous costs and memoize the broadcast trees
            //routes.put()

            this.previousCosts = ImmutableMap.copyOf(linksCosts);
        }
    }

    public List<NodePortTuple> getPath(final DatapathId from, final DatapathId to)
    {
        logger.debug("getting route: s_{} -> s_{}", from.getLong(), to.getLong());
        return routes.get(from, to);
    }

    //TODO: wrap this with try/catch - NPE from null topology
    private void buildShortestPaths(DatapathId root, Map<Link, Integer> linksCosts)
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
