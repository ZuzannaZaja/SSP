package pl.edu.agh.kt;

import net.floodlightcontroller.linkdiscovery.ILinkDiscovery;
import net.floodlightcontroller.topology.NodePortTuple;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Dijkstra
{
    //TODO: implement this as a static method to avoid dealing with keeping state here
    private static final Logger logger = LoggerFactory.getLogger(Dijkstra.class);

    public static void calculateShortesPaths(Map<Edge, Integer> links)
    {
        final ConcurrentHashMap<DatapathId, Vertex> verticesMap = getVerticesMap(links);
        final List<pl.edu.agh.kt.Edge> edgesList = getEdgesList(links, verticesMap);

        final Graph graph = new Graph(verticesMap.values(), edgesList);
        DijkstraAlgorithm dijkstra = new DijkstraAlgorithm(graph);
        dijkstra.execute(new Vertex(DatapathId.of(1)));
        final List<Vertex> path = dijkstra.getPath(new Vertex(DatapathId.of(4)));
        logger.debug("Dijkstra done, path from 1 -> 4");
        for (Vertex v : path) {
            logger.debug("\t{}", v);
        }
    }

    private static List<pl.edu.agh.kt.Edge> getEdgesList(final Map<Edge, Integer> links, final ConcurrentHashMap<DatapathId, Vertex> verticesMap)
    {
        final List<pl.edu.agh.kt.Edge> edges = new ArrayList<>();
        for (Map.Entry<Edge, Integer> entry : links.entrySet()) {
            final pl.edu.agh.kt.Edge edge = new pl.edu.agh.kt.Edge(
                    verticesMap.get(entry.getKey().getFrom().getNodeId()),
                    verticesMap.get(entry.getKey().getTo().getNodeId()),
                    entry.getValue());
            edges.add(edge);
        }
        return edges;
    }

    private static ConcurrentHashMap<DatapathId, Vertex> getVerticesMap(final Map<Edge, Integer> links)
    {
        ConcurrentHashMap<DatapathId, Vertex> vertices = new ConcurrentHashMap<>();
        for (Edge edge : links.keySet()) {
            final DatapathId fromDatapathId = edge.getFrom().getNodeId();
            final OFPort fromPortId = edge.getFrom().getPortId();
            final DatapathId toDatapathId = edge.getTo().getNodeId();
            final OFPort toPortId = edge.getTo().getPortId();

            if (vertices.containsKey(fromDatapathId)) {
                vertices.get(fromDatapathId).addNeighbor(toDatapathId, fromPortId);
            } else {
                final Vertex vertex = new Vertex(fromDatapathId);
                vertex.addNeighbor(toDatapathId, fromPortId);
                vertices.put(fromDatapathId, vertex);
            }

            if (vertices.containsKey(toDatapathId)) {
                vertices.get(toDatapathId).addNeighbor(fromDatapathId, toPortId);
            } else {
                final Vertex vertex = new Vertex(toDatapathId);
                vertex.addNeighbor(fromDatapathId, toPortId);
                vertices.put(toDatapathId, vertex);
            }
        }
        return vertices;
    }

    public static List<NodePortTuple> getShortestPath(Map<Edge, Integer> links, DatapathId from, DatapathId to)
    {
//        logger.debug("building path s_{} -> s_{}", from.getLong(), to.getLong());
        //TODO: shortest path with weights
        return new LinkedList<>();
    }

    //TODO: probably should be extracted
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
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Edge other = (Edge) obj;
            if (!from.equals(other.from))
                return false;
            return to.equals(other.to);
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
