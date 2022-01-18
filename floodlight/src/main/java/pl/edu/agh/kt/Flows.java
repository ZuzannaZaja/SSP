package pl.edu.agh.kt;

import com.google.common.collect.ImmutableList;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.TCP;
import net.floodlightcontroller.packet.UDP;
import net.floodlightcontroller.topology.NodePortTuple;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Flows
{
    private static final Logger logger = LoggerFactory.getLogger(Flows.class);

    //TODO set timeouts on new flows
    public static short FLOWMOD_DEFAULT_IDLE_TIMEOUT = 0; // in seconds
    public static short FLOWMOD_DEFAULT_HARD_TIMEOUT = 0; // infinite
    public static short FLOWMOD_DEFAULT_PRIORITY = 100;

    protected static boolean FLOWMOD_DEFAULT_MATCH_VLAN = true;
    protected static boolean FLOWMOD_DEFAULT_MATCH_MAC = true;
    protected static boolean FLOWMOD_DEFAULT_MATCH_IP_ADDR = true;
    protected static boolean FLOWMOD_DEFAULT_MATCH_TRANSPORT = true;

    public Flows()
    {
        logger.info("Flows() begin/end");
    }

    //TODO: implement (probably add some other parameters)
    public static void addPath(List<NodePortTuple> path)
    {
//        Lists.reverse(path)
//        logger.debug("Adding path {}...", path);
    }

    public static void add(IOFSwitch ofSwitch, FloodlightContext cntx, OFPort inPort, OFPort outPort)
    {
        if (ofSwitch == null) {
            logger.debug("got null switch, unable to add flow");
        } else {
            final Match match = buildMatch(ofSwitch, cntx, inPort);
            final OFActionOutput actionOutput = ofSwitch.getOFFactory().actions().buildOutput()
                    .setPort(outPort)
                    .setMaxLen(Integer.MAX_VALUE)
                    .build();

            final OFFlowAdd ofFlowAdd = ofSwitch.getOFFactory().buildFlowAdd()
                    .setMatch(match)
                    .setActions(ImmutableList.<OFAction>of(actionOutput))
                    .setOutPort(outPort)
                    .setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT)
                    .setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
                    .setBufferId(OFBufferId.NO_BUFFER)
                    .build();
            try {
                ofSwitch.write(ofFlowAdd);
                logger.info("Flow from s_{}/{} forwarded to port s_{}/{}; match: {}",
                        new Object[]{ofSwitch.getId().getLong(), inPort.getPortNumber(),
                                ofSwitch.getId().getLong(), outPort.getPortNumber(), match.toString()});
            } catch (Exception e) {
                logger.error("Unable to add flow to {}", ofSwitch.getId().getLong(), e);
            }
        }
    }

    public static void simpleAdd(IOFSwitch sw, OFPacketIn pin, FloodlightContext cntx, OFPort outPort)
    {
        // FlowModBuilder
        OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
        // match
        Match m = buildMatch(sw, cntx, pin.getInPort());

        // actions
        OFActionOutput.Builder aob = sw.getOFFactory().actions().buildOutput();
        List<OFAction> actions = new ArrayList<>();
        aob.setPort(outPort);
        aob.setMaxLen(Integer.MAX_VALUE);
        actions.add(aob.build());
        fmb.setMatch(m)
                .setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT)
                .setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
                .setBufferId(pin.getBufferId())
                .setOutPort(outPort)
                .setPriority(FLOWMOD_DEFAULT_PRIORITY);
        fmb.setActions(actions);
        // write flow to switch
        try {
            sw.write(fmb.build());
            logger.info("Flow from port {} forwarded to port {}; match: {}",
                    new Object[]{pin.getInPort().getPortNumber(), outPort.getPortNumber(), m.toString()});
        } catch (Exception e) {
            logger.error("error {}", e);
        }
    }

    private static Match buildMatch(IOFSwitch sw, FloodlightContext cntx, OFPort inPort)
    {
        Match.Builder matchBuilder = sw.getOFFactory().buildMatch();

        //match flows on specific inPort
        matchBuilder.setExact(MatchField.IN_PORT, inPort);

        // unpacking the payload -- purposefully skipping src and dst MAC matching
        Ethernet ethernetFrame = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
        logger.debug("\tchecking ether type {}...", ethernetFrame.getEtherType().toString());
        if (ethernetFrame.getEtherType() == EthType.IPv4) {
            logger.debug("\t...got IPv4");
            final IPv4 iPv4packet = (IPv4) ethernetFrame.getPayload();
            logger.debug("\tgot addresses {} -> {}",
                    iPv4packet.getSourceAddress().toString(),
                    iPv4packet.getDestinationAddress().toString());
            matchBuilder.setExact(MatchField.ETH_TYPE, EthType.IPv4);
            matchBuilder.setExact(MatchField.IPV4_SRC, iPv4packet.getSourceAddress());
            matchBuilder.setExact(MatchField.IPV4_DST, iPv4packet.getDestinationAddress());

            logger.debug("\tchecking ip proto...");

            if (iPv4packet.getProtocol() == IpProtocol.ICMP) {
                logger.debug("\t...got icmp");
                matchBuilder.setExact(MatchField.IP_PROTO, IpProtocol.ICMP);
            } else if (iPv4packet.getProtocol() == IpProtocol.TCP) {
                logger.debug("\t...got tcp");
                final TCP tcpSegment = (TCP) iPv4packet.getPayload();
                logger.debug("\tgot ports {} -> {}",
                        tcpSegment.getSourcePort().toString(),
                        tcpSegment.getDestinationPort().toString());
                matchBuilder.setExact(MatchField.IP_PROTO, IpProtocol.TCP);
                matchBuilder.setExact(MatchField.TCP_DST, tcpSegment.getDestinationPort());
                matchBuilder.setExact(MatchField.TCP_SRC, tcpSegment.getSourcePort());
            } else if (iPv4packet.getProtocol() == IpProtocol.UDP) {
                logger.debug("\t...got udp");
                final UDP udpSegment = (UDP) iPv4packet.getPayload();
                logger.debug("\tgot ports {} -> {}",
                        udpSegment.getSourcePort().toString(),
                        udpSegment.getDestinationPort().toString());
                matchBuilder.setExact(MatchField.IP_PROTO, IpProtocol.UDP);
                matchBuilder.setExact(MatchField.UDP_DST, udpSegment.getDestinationPort());
                matchBuilder.setExact(MatchField.UDP_SRC, udpSegment.getSourcePort());
            }
        }

        if (ethernetFrame.getEtherType() == EthType.ARP) { /*
         * shallow check for equality is okay for EthType
         */
            matchBuilder.setExact(MatchField.ETH_TYPE, EthType.ARP);
        }

        if (ethernetFrame.getEtherType() == EthType.IPv6) {
            logger.debug("got IPv6 -- using a single flow entry");
            matchBuilder.setExact(MatchField.ETH_TYPE, EthType.IPv6);
        }

        final Match build = matchBuilder.build();
        logger.debug("returning match: {}", build);

        return build;
    }
}
