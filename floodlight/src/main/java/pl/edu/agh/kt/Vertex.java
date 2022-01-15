package pl.edu.agh.kt;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;

import java.util.Map;

//inspired by https://www.vogella.com/tutorials/JavaAlgorithmsDijkstra/article.html
public class Vertex
{
    private DatapathId datapathId;
    private Map<DatapathId, OFPort> neighbors;

    public Vertex(final DatapathId datapathId, final Map<DatapathId, OFPort> neighbors)
    {
        this.datapathId = datapathId;
        this.neighbors = neighbors;
    }

    public DatapathId getDatapathId()
    {
        return datapathId;
    }

    public Map<DatapathId, OFPort> getNeighbors()
    {
        return neighbors;
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
        Vertex other = (Vertex) obj;
        return this.datapathId.equals(other.datapathId);
    }
}
