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
import net.floodlightcontroller.routing.Link;
import net.floodlightcontroller.statistics.IStatisticsService;
import net.floodlightcontroller.topology.ITopologyService;
import net.floodlightcontroller.topology.NodePortTuple;
import net.floodlightcontroller.topology.TopologyManager;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IpProtocol;
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
        statisticsService.collectStatistics(true); //TODO: may not be needed
//        famtarTopology.calculatePaths(initializeLinksCosts());
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

//        logger.info("************* NEW PACKET IN *************");

        //TODO: handle first buffered packet -- vide lab 5, extract the IP and send it manually
        //TODO switches 1 and 4 are our entry points, handle only them
        try {
            logger.debug("getting the current route from FamtarTopology...");
            for (Hop hop : this.famtarTopology.getPath(FamtarTopology.HOST_ONE, FamtarTopology.HOST_TWO)) {
                IOFSwitch aSwitch = switchService.getSwitch(hop.getSwitchId());
                Flows.add(aSwitch, cntx, hop.getInPort(), hop.getOutPort());
                Flows.add(aSwitch, cntx, hop.getOutPort(), hop.getInPort());
            }
            for (Hop hop : this.famtarTopology.getPath(FamtarTopology.HOST_TWO, FamtarTopology.HOST_ONE)) {
                IOFSwitch aSwitch = switchService.getSwitch(hop.getSwitchId());
                Flows.add(aSwitch, cntx, hop.getInPort(), hop.getOutPort());
                Flows.add(aSwitch, cntx, hop.getOutPort(), hop.getInPort());
            }
            logger.debug("...done");
        } catch (Exception e) {
            this.famtarTopology.calculatePaths(initializeLinksCosts());
        }

        return Command.STOP;
    }

    private boolean drop(FloodlightContext context)
    {
        Ethernet ethernetFrame = IFloodlightProviderService.bcStore.get(context, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
        if (ethernetFrame.getEtherType() == EthType.IPv6) {
            return true;
        } else if (ethernetFrame.getEtherType() == EthType.IPv4) {
            final IPv4 iPv4packet = (IPv4) ethernetFrame.getPayload();
            // this should handle issues with deserialization of D-ITG packets
            try {
                if (iPv4packet.getProtocol() == IpProtocol.UDP) {
                    final UDP udpSegment = (UDP) iPv4packet.getPayload();
                    return udpSegment.getDestinationPort().getPort() == 67 ||
                            udpSegment.getDestinationPort().getPort() == 68 ||
                            udpSegment.getSourcePort().getPort() == 67 ||
                            udpSegment.getSourcePort().getPort() == 68;
                }
            } catch (Exception e) {
                return false;
            }
        }

        return false;
    }

    private Map<Link, Integer> initializeLinksCosts()
    {
//        logger.debug("initializing link costs map...");
        Map<Link, Integer> linksCosts = new HashMap<>();
        Map<NodePortTuple, Set<Link>> switchPortLinks = ((TopologyManager) topologyService).getSwitchPortLinks();
        for (Map.Entry<NodePortTuple, Set<Link>> e : switchPortLinks.entrySet()) {
            for (Link link : e.getValue()) {
//                logger.debug("\tadding {}", link);
                linksCosts.put(link, FamtarTopology.DEFAULT_LINK_COST);
            }
        }
        return linksCosts;
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
