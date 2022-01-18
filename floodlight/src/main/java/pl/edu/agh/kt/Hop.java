package pl.edu.agh.kt;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;

public class Hop
{
    private OFPort inPort;
    private DatapathId switchId;
    private OFPort outPort;

    Hop(final OFPort inPort, final DatapathId switchId, final OFPort outPort)
    {
        this.inPort = inPort;
        this.switchId = switchId;
        this.outPort = outPort;
    }

    public OFPort getInPort()
    {
        return inPort;
    }

    public DatapathId getSwitchId()
    {
        return switchId;
    }

    public OFPort getOutPort()
    {
        return outPort;
    }

    @Override
    public String toString()
    {
        return String.format("s_%s/%s -> s_%s/%s",
                switchId.getLong(), inPort.toString(),
                switchId.getLong(), outPort.toString());
    }
}
