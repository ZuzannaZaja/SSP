package pl.edu.agh.kt;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.topology.ITopologyService;
import net.floodlightcontroller.topology.NodePortTuple;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class FamtarListener implements IFloodlightModule, IOFMessageListener
{
    protected IFloodlightProviderService floodlightProvider;
    protected ITopologyService topologyService;
    protected IOFSwitchService switchService;
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
        logger = LoggerFactory.getLogger(FamtarListener.class);
    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException
    {
        floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
        topologyService.addListener(new FamtarTopologyListener());
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
        //TODO
        final FamtarTopology famtarTopology = FamtarTopology.getInstance();
        FamtarStatisticsCollector.getInstance(sw);

//		logger.info("************* NEW PACKET IN *************");
        PacketExtractor extractor = new PacketExtractor();

        final IPv4Address destinationIP = extractor.getDestinationIP(cntx);
        OFPacketIn packetIn = (OFPacketIn) msg;
        // TODO adding routes with
        final DatapathId destinationDatapathId = FamtarTopology.ipDatapathIdMapping.get(destinationIP);
        final List<NodePortTuple> path = famtarTopology.getPath(sw.getId(), destinationDatapathId);

        final List<NodePortTuple> oneToTwo = ImmutableList.of(
                new NodePortTuple(DatapathId.of(1), OFPort.of(3)),
                new NodePortTuple(DatapathId.of(7), OFPort.of(6)),
                new NodePortTuple(DatapathId.of(4), OFPort.of(4)));
        final List<NodePortTuple> twoToOne = ImmutableList.of(
                new NodePortTuple(DatapathId.of(4), OFPort.of(3)),
                new NodePortTuple(DatapathId.of(7), OFPort.of(1)),
                new NodePortTuple(DatapathId.of(1), OFPort.of(4))
        );
        addFlowOnPath(cntx, packetIn, oneToTwo);
        addFlowOnPath(cntx, packetIn, twoToOne);

        return Command.STOP;
    }

    private void addFlowOnPath(final FloodlightContext cntx, final OFPacketIn packetIn, final List<NodePortTuple> oneToTwo)
    {
        for (NodePortTuple hop : Lists.reverse(oneToTwo)) {
            final DatapathId nodeId = hop.getNodeId();
            final OFPort portId = hop.getPortId();
            final IOFSwitch iofSwitch = this.switchService.getSwitch(nodeId);
            if (iofSwitch != null) {
                Flows.simpleAdd(iofSwitch, packetIn, cntx, portId);
            } else {
                logger.debug("can't add flow for now, switch_{} is not registered", nodeId.getLong());
            }
        }
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
        return l;
    }
}
