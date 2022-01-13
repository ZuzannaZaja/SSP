package pl.edu.agh.kt;

import net.floodlightcontroller.linkdiscovery.ILinkDiscovery;
import net.floodlightcontroller.topology.NodePortTuple;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Dijkstra
{
    //TODO use this to calculate SPF (https://www.baeldung.com/java-dijkstra)
    // implement this as s static method to avoid dealing with keeping state here

    public static List<NodePortTuple> getShortestPath(Map<Edge, Integer> links, DatapathId from, DatapathId to)
    {
        //TODO: shortest path with weights
        return new LinkedList<>();
    }

    public static class Edge
    {
        private NodePortTuple from;
        private NodePortTuple to;

        public Edge(final NodePortTuple from, final NodePortTuple to)
        {
            this.from = from;
            this.to = to;
        }

        public Edge(DatapathId fromNodeId, OFPort fromPortId,
                    DatapathId toNodeId, OFPort toPortId)
        {
            this.from = new NodePortTuple(fromNodeId, fromPortId);
            this.to = new NodePortTuple(toNodeId, toPortId);
        }

        public NodePortTuple getFrom()
        {
            return from;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Edge other = (Edge) obj;
            if (!from.equals(other.from))
                return false;
            if (!to.equals(other.to))
                return false;
            return true;
        }

        @Override
        public String toString()
        {
            return String.format("switch_%s/port_%s -> switch_%s/port_%s",
                    from.getNodeId().getLong(), from.getPortId().toString(),
                    to.getNodeId().getLong(), to.getPortId().toString());
        }

        @Override
        public int hashCode()
        {
            return from.hashCode() + to.hashCode();
        }

        public NodePortTuple getTo()
        {
            return to;
        }

        public static Edge from(ILinkDiscovery.LDUpdate linkUpdate)
        {
            return new Edge(linkUpdate.getSrc(), linkUpdate.getSrcPort(), linkUpdate.getDst(),
                    linkUpdate.getDstPort());
        }
    }
}
