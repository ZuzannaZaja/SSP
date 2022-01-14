package pl.edu.agh.kt;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.packet.ARP;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.TCP;
import net.floodlightcontroller.packet.UDP;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketExtractor
{
    private static final Logger logger = LoggerFactory.getLogger(PacketExtractor.class);
    private FloodlightContext cntx;
    protected IFloodlightProviderService floodlightProvider;
    private Ethernet eth;
    private IPv4 ipv4;
    private ARP arp;
    private TCP tcp;
    private UDP udp;
    private OFMessage msg;

    public PacketExtractor(FloodlightContext cntx, OFMessage msg)
    {
        this.cntx = cntx;
        this.msg = msg;
        logger.info("PacketExtractor: Constructor method called");
    }

    public PacketExtractor()
    {
        logger.info("PacketExtractor: Constructor method called");
    }

    //TODO: implement this and similar methods for the five tuple Nat2
    public IPv4Address getDestinationIP(FloodlightContext cntx)
    {
        return IPv4Address.of(10, 0, 0, 1);
    }

    public void packetExtract(FloodlightContext cntx)
    {
        this.cntx = cntx;
        extractEth();
    }

    public boolean isTCP500(FloodlightContext cntx)
    {
        this.cntx = cntx;
        eth = IFloodlightProviderService.bcStore.get(this.cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);

        if (eth.getEtherType() == EthType.IPv4) {
            ipv4 = (IPv4) eth.getPayload();
            if (ipv4.getProtocol() == IpProtocol.TCP) {
                tcp = (TCP) ipv4.getPayload();
                return tcp.getDestinationPort().getPort() == 500;
            }
        }
        return false;
    }

    public void extractEth()
    {
        eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
        logger.info("Frame: src mac {}", eth.getSourceMACAddress());
        logger.info("Frame: dst mac {}", eth.getDestinationMACAddress());
        logger.info("Frame: ether_type {}", eth.getEtherType());

        if (eth.getEtherType() == EthType.ARP) {
            arp = (ARP) eth.getPayload();
            //extractArp();
        }
        if (eth.getEtherType() == EthType.IPv4) {
            ipv4 = (IPv4) eth.getPayload();
            //extractIp();
        }

    }

    public void extractArp()
    {
        logger.info("ARP extractor");
    }

    public void extractIp()
    {
        logger.info("IP extractor");
    }

}
