package pl.edu.agh.kt;

import java.util.Collection;

//inspired by https://www.vogella.com/tutorials/JavaAlgorithmsDijkstra/article.html
public class Graph
{
    private final Collection<Vertex> vertexes;
    private final Collection<Edge> edges;

    public Graph(Collection<Vertex> vertexes, Collection<Edge> edges)
    {
        this.vertexes = vertexes;
        this.edges = edges;
    }

    public Collection<Vertex> getVertexes()
    {
        return vertexes;
    }

    public Collection<Edge> getEdges()
    {
        return edges;
    }
}
