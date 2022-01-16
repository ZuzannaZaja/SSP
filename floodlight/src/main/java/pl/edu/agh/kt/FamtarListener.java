package pl.edu.agh.kt;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.routing.BroadcastTree;
import net.floodlightcontroller.routing.Link;
import net.floodlightcontroller.statistics.IStatisticsService;
import net.floodlightcontroller.topology.ITopologyService;
import net.floodlightcontroller.topology.NodePortTuple;
import net.floodlightcontroller.topology.TopologyManager;

import net.floodlightcontroller.topology.TopologyInstance;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class FamtarListener implements IFloodlightModule, IOFMessageListener
{
    protected IFloodlightProviderService floodlightProvider;
    protected ITopologyService topologyService;
    protected IOFSwitchService switchService;
    protected IStatisticsService statisticsService;
    protected FamtarStatisticsCollector famtarStatisticsCollector;
    protected static Logger logger;

    @Override
    public String getName()
    {
        return FamtarListener.class.getSimpleName();
    }

    @Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException
    {
        floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
        topologyService = context.getServiceImpl(ITopologyService.class);
        switchService = context.getServiceImpl(IOFSwitchService.class);
        statisticsService = context.getServiceImpl(IStatisticsService.class);
        famtarStatisticsCollector = FamtarStatisticsCollector.getInstance(statisticsService, topologyService);
        logger = LoggerFactory.getLogger(FamtarListener.class);
    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException
    {
        floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
        topologyService.addListener(new FamtarTopologyListener());
        statisticsService.collectStatistics(true); //TODO: may not be needed
        logger.info("******************* START **************************");
    }

    @Override
    public boolean isCallbackOrderingPrereq(OFType type, String name)
    {
        return false;
    }

    @Override
    public boolean isCallbackOrderingPostreq(OFType type, String name)
    {
        return false;
    }

    @Override
    public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg,
                                                                   FloodlightContext cntx)
    {

//		logger.info("************* NEW PACKET IN *************");
        // TODO make packet extractor extract the 5-tuple to identify the flow
        PacketExtractor extractor = new PacketExtractor();

        OFPacketIn packetIn = (OFPacketIn) msg;
        OFPort outPort = OFPort.of(0);
        if (packetIn.getInPort() == OFPort.of(1)) {
            outPort = OFPort.of(2);
        } else {
            outPort = OFPort.of(1);
        }
        Flows.simpleAdd(sw, packetIn, cntx, outPort);

        // TODO adding routes
        final FamtarTopology famtarTopology = FamtarTopology.getInstance();
        final DatapathId destinationDatapathId = FamtarTopology.ipDatapathIdMapping.get(extractor.getDestinationIP(cntx));
        final List<NodePortTuple> path = famtarTopology.getPath(sw.getId(), destinationDatapathId);
        Flows.addPath(path);

//		logger.info("........looking for TCP 500!");
//		if (extractor.isTCP500(cntx)) {
//			logger.info("........matched TCP 500!");
//			Flows.enqueue(sw, pin, cntx, outPort, 1);
//		} else {
//			Flows.simpleAdd(sw, pin, cntx, outPort);
//		}


        if (new Random().nextBoolean()) {
            buildShortestPaths(DatapathId.of(1));
            buildShortestPaths(DatapathId.of(7));
        }

        return Command.STOP;
    }

    //TODO: wrap this with try/catch - NPE from null topology
    private void buildShortestPaths(final DatapathId root)
    {
        final TopologyManager topologyManager = (TopologyManager) this.topologyService;
        final TopologyInstance topologyInstance = topologyManager.getCurrentInstance();

        //TODO: implement this to use updated weights
        final Map<DatapathId, Set<Link>> allLinks = topologyManager.getAllLinks();
        final HashMap<Link, Integer> linkCost = new HashMap<>();
        for (Set<Link> linkSet : allLinks.values()) {
            for (Link link : linkSet) {
                linkCost.put(link, FamtarTopology.DEFAULT_LINK_COST);
            }
        }

        //TODO: check this variable
        final boolean isDstRooted = false;
        final BroadcastTree dijkstraBroadcastTree = topologyInstance.dijkstra(
                this.topologyService.getAllLinks(),
                root,
                linkCost,
                isDstRooted);

        logger.debug(String.format(
                "Built the following broadcast tree with switch_%s as root (isDstRooted = %s)\n%s",
                root, isDstRooted, dijkstraBroadcastTree.toString()));
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices()
    {
        return null;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls()
    {
        return null;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies()
    {
        Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
        l.add(IFloodlightProviderService.class);
        l.add(IOFSwitchService.class);
        l.add(IStatisticsService.class);
        return l;
    }
}
