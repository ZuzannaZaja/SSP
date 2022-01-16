package pl.edu.agh.kt;

import com.google.common.collect.Iterators;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.TCP;
import net.floodlightcontroller.packet.UDP;
import net.floodlightcontroller.topology.NodePortTuple;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.VlanVid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Flows
{
    //TODO set timeouts on new flows
    private static final Logger logger = LoggerFactory.getLogger(Flows.class);

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

    public static void simpleAdd(IOFSwitch sw, OFPacketIn pin, FloodlightContext cntx, OFPort outPort)
    {
        // FlowModBuilder
        OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
        // match
        Match m = getFamtarMatch(sw, pin.getInPort(), cntx);

        // actions
        OFActionOutput.Builder aob = sw.getOFFactory().actions().buildOutput();
        List<OFAction> actions = new ArrayList<OFAction>();
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

    public static Match getFamtarMatch(IOFSwitch sw, OFPort inPort, FloodlightContext cntx)
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
            if (iPv4packet.getProtocol() == IpProtocol.UDP) {
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

        final Match build = matchBuilder.build();
        logger.debug("returning match: {}\n{}", build, Iterators.toArray(build.getMatchFields().iterator(),
                MatchField.class));

        return build;
    }

    public static Match createMatchFromPacket(IOFSwitch sw, OFPort inPort, FloodlightContext cntx)
    {
        // The packet in match will only contain the port number.
        // We need to add in specifics for the hosts we're routing between.
        Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
        VlanVid vlan = VlanVid.ofVlan(eth.getVlanID());
        MacAddress srcMac = eth.getSourceMACAddress();
        MacAddress dstMac = eth.getDestinationMACAddress();

        Match.Builder mb = sw.getOFFactory().buildMatch();
        mb.setExact(MatchField.IN_PORT, inPort);

        if (FLOWMOD_DEFAULT_MATCH_MAC) {
            mb.setExact(MatchField.ETH_SRC, srcMac).setExact(MatchField.ETH_DST, dstMac);
        }

        if (FLOWMOD_DEFAULT_MATCH_VLAN) {
            if (!vlan.equals(VlanVid.ZERO)) {
                mb.setExact(MatchField.VLAN_VID, OFVlanVidMatch.ofVlanVid(vlan));
            }
        }

        // TODO Detect switch type and match to create hardware-implemented flow
        if (eth.getEtherType() == EthType.IPv4) { /*
         * shallow check for equality is okay for EthType
         */
            IPv4 ip = (IPv4) eth.getPayload();
            IPv4Address srcIp = ip.getSourceAddress();
            IPv4Address dstIp = ip.getDestinationAddress();

            if (FLOWMOD_DEFAULT_MATCH_IP_ADDR) {
                mb.setExact(MatchField.ETH_TYPE, EthType.IPv4).setExact(MatchField.IPV4_SRC, srcIp)
                        .setExact(MatchField.IPV4_DST, dstIp);
            }

            if (FLOWMOD_DEFAULT_MATCH_TRANSPORT) {
                /*
                 * Take care of the ethertype if not included earlier, since it's a prerequisite
                 * for transport ports.
                 */
                if (!FLOWMOD_DEFAULT_MATCH_IP_ADDR) {
                    mb.setExact(MatchField.ETH_TYPE, EthType.IPv4);
                }

                if (ip.getProtocol().equals(IpProtocol.TCP)) {
                    TCP tcp = (TCP) ip.getPayload();
                    mb.setExact(MatchField.IP_PROTO, IpProtocol.TCP)
                            .setExact(MatchField.TCP_SRC, tcp.getSourcePort())
                            .setExact(MatchField.TCP_DST, tcp.getDestinationPort());
                } else if (ip.getProtocol().equals(IpProtocol.UDP)) {
                    UDP udp = (UDP) ip.getPayload();
                    mb.setExact(MatchField.IP_PROTO, IpProtocol.UDP).setExact(MatchField.UDP_SRC, udp.getSourcePort())
                            .setExact(MatchField.UDP_DST, udp.getDestinationPort());
                }
            }
        } else if (eth.getEtherType() == EthType.ARP) { /*
         * shallow check for equality is okay for EthType
         */
            mb.setExact(MatchField.ETH_TYPE, EthType.ARP);
        }

        return mb.build();
    }

//    public static void enqueue(IOFSwitch sw, OFPacketIn pin,
//                               FloodlightContext cntx, OFPort outPort, long queueId)
//    {
//        // FlowModBuilder
//        OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
//// match
//        Match m = createMatchFromPacket(sw, pin.getInPort(), cntx);
//        List<OFAction> actions = new ArrayList<OFAction>();
//        OFActionEnqueue enqueue = sw.getOFFactory().actions().buildEnqueue()
//                .setPort(outPort).setQueueId(queueId).build();
//        actions.add(enqueue);
//        fmb.setMatch(m).setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT)
//                .setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
//                .setBufferId(pin.getBufferId()).setOutPort(outPort)
//                .setPriority(FLOWMOD_DEFAULT_PRIORITY);
//        fmb.setActions(actions);
//// write flow to switch
//        try {
//            sw.write(fmb.build());
//            logger.info(
//                    "Flow from port {} forwarded to port {}; match: {}",
//                    new Object[]{pin.getInPort().getPortNumber(),
//                            outPort.getPortNumber(), m.toString()});
//        } catch (Exception e) {
//            logger.error("error {}", e);
//        }
//    }
}
