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
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.UDP;
import net.floodlightcontroller.routing.BroadcastTree;
import net.floodlightcontroller.routing.Link;
import net.floodlightcontroller.statistics.IStatisticsService;
import net.floodlightcontroller.topology.ITopologyService;
import net.floodlightcontroller.topology.TopologyInstance;
import net.floodlightcontroller.topology.TopologyManager;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FamtarListener implements IFloodlightModule, IOFMessageListener
{
    protected IFloodlightProviderService floodlightProvider;
    protected ITopologyService topologyService;
    protected IOFSwitchService switchService;
    protected IStatisticsService statisticsService;
    protected FamtarStatisticsCollector famtarStatisticsCollector;
    protected FamtarTopology famtarTopology;
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
        famtarTopology = new FamtarTopology(topologyService);
        famtarStatisticsCollector = FamtarStatisticsCollector.getInstance(statisticsService, topologyService, famtarTopology);
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
    public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx)
    {
        if (drop(cntx)) {
            return Command.STOP;
        }

        logger.info("************* NEW PACKET IN *************");

        //TODO: handle first buffered packet -- vide lab 5, extract the IP and send it manually
        OFPacketIn packetIn = (OFPacketIn) msg;

        if (sw.getId().getLong() == 1) {
            logger.debug("switch {}", sw.getId().getLong());
            Flows.add(switchService.getSwitch(DatapathId.of(4)), cntx, OFPort.of(3), OFPort.of(4));
            Flows.add(switchService.getSwitch(DatapathId.of(7)), cntx, OFPort.of(1), OFPort.of(6));
            Flows.add(switchService.getSwitch(DatapathId.of(1)), cntx, OFPort.of(4), OFPort.of(3));
        } else if (sw.getId().getLong() == 4) {
            logger.debug("switch {}", sw.getId().getLong());
            Flows.add(switchService.getSwitch(DatapathId.of(1)), cntx, OFPort.of(3), OFPort.of(4));
            Flows.add(switchService.getSwitch(DatapathId.of(7)), cntx, OFPort.of(6), OFPort.of(1));
            Flows.add(switchService.getSwitch(DatapathId.of(4)), cntx, OFPort.of(4), OFPort.of(3));
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

        //TODO: true or false? -- we're adding flows from the end of the path
        final boolean isDstRooted = false;
        final BroadcastTree dijkstraBroadcastTree = topologyInstance.dijkstra(
                this.topologyService.getAllLinks(),
                root,
                famtarStatisticsCollector.getLinksCosts(),
                isDstRooted);

        logger.debug(String.format(
                "Built the following broadcast tree with switch_%s as root (isDstRooted = %s)\n%s",
                root, isDstRooted, dijkstraBroadcastTree.toString()));
    }

    private boolean drop(FloodlightContext context)
    {
        Ethernet ethernetFrame = IFloodlightProviderService.bcStore.get(context, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
        if (ethernetFrame.getEtherType() == EthType.IPv6) {
            return true;
        } else if (ethernetFrame.getEtherType() == EthType.IPv4) {
            final IPv4 iPv4packet = (IPv4) ethernetFrame.getPayload();
            if (iPv4packet.getProtocol() == IpProtocol.UDP) {
                final UDP udpSegment = (UDP) iPv4packet.getPayload();
                return udpSegment.getDestinationPort().getPort() == 67 ||
                        udpSegment.getDestinationPort().getPort() == 68 ||
                        udpSegment.getSourcePort().getPort() == 67 ||
                        udpSegment.getSourcePort().getPort() == 68;
            }
        }

        return false;
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
