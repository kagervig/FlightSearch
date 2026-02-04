package com.kristian.flightsearch.flightgraph;

import java.util.*;

import com.kristian.flightsearch.models.Airport;

public class Dijkstra {

    /**
     * Dijkstra's Algorithm: Finds the shortest path from a starting vertex to all other vertices in a weighted graph.
     * 
     * How it works:
     * 1. Start at the beginning vertex with distance 0
     * 2. Visit the nearest unvisited vertex
     * 3. Update distances to its neighbors if a shorter path is found
     * 4. Repeat until all vertices are visited
     * 
     * Returns two dictionaries: distances (how far each vertex is) and previous (how to get there)
     */
    public static Dictionary[] dijkstra(Graph g, AirportVertex startingVertex){
        // distances: stores the shortest known distance from start to each vertex
        // Key = airport code (String), Value = distance (Integer)
        // Example: {"A": 0, "B": 3, "C": 100}
        Dictionary<Airport, Integer> distances = new Hashtable<>();
        
        // previous: stores which vertex you came FROM to reach each vertex (for reconstructing the path)
        // Key = airport code (String), Value = the previous vertex (AirportVertex object)
        // Example: {"B": A, "C": D} means you reach B from A, C from D
        Dictionary<Airport, AirportVertex> previous = new Hashtable<>();
        
        // PriorityQueue: automatically sorts vertices by their distance (closest first)
        // We use QueueObject which pairs a Vertex with its distance value for sorting
        // This ensures we always process the nearest unvisited vertex next
        PriorityQueue<QueueObject> queue = new PriorityQueue<QueueObject>();

        // Add the starting vertex to the queue with distance 0 (we start here)
        // QueueObject wraps the vertex and its distance so PriorityQueue can sort by distance
        queue.add(new QueueObject(startingVertex, 0));

        // Initialize all vertices: set their distances to infinity (unknown) and previous to null
        // Get all vertices from the graph using g.getVertices()
        for (AirportVertex v : g.getVertices()){
            // If this isn't the starting vertex, set its distance to MAX_VALUE (infinity)
            // This represents "we don't know how to reach it yet"
            if (v != startingVertex){
                distances.put(v.getData(), Integer.MAX_VALUE);
            }
            // Set the "previous vertex" for all vertices to null
            // This will be updated as we find the shortest paths
            previous.put(v.getData(), null);
        }

        // Set the distance to the starting vertex from itself to 0 (we're already there)
        distances.put(startingVertex.getData(), 0);

        // Main loop: Keep processing vertices until the queue is empty
        // The queue is empty when we've found the shortest path to all reachable vertices
        while (queue.size() != 0){
            // Remove and process the vertex with the SMALLEST distance from the queue
            // This is guaranteed to be the nearest unvisited vertex
            // poll() retrieves the head of the PriorityQueue and removes it
            // .vertex extracts just the AirportVertex from the QueueObject wrapper
            AirportVertex current = queue.poll().vertex;
            
            // Look at all edges (connections) FROM the current vertex
            // current.getEdges() returns an ArrayList of all edges starting from 'current'
            for (Edge e : current.getEdges()){
                // Calculate an alternative path distance to the neighbor
                // = (distance to current vertex) + (weight of this edge)
                // distances.get(current.getData()) gets how far we traveled to reach 'current'
                // e.getWeight() gets the weight of the edge to the neighbor
                // This is the total distance if we use this path
                Integer alternative = distances.get(current.getData()) + e.getWeight();
                
                // Get the neighbor vertex's airport that this edge points to
                // e.getEnd() returns the AirportVertex that this edge connects TO
                Airport neighbourValue = e.getEnd().getData();
                
                // Check if this new path is SHORTER than what we previously knew
                // distances.get(neighbourValue) returns the current best-known distance to the neighbor
                // If alternative is smaller, we found a better path!
                if (alternative < distances.get(neighbourValue)){
                    // Update the shortest distance to this neighbor
                    distances.put(neighbourValue, alternative);
                    
                    // Remember that we reach this neighbor FROM the current vertex
                    // This lets us reconstruct the actual path later if needed
                    previous.put(neighbourValue, current);
                    
                    // Add the neighbor to the queue with its new (updated) distance
                    // PriorityQueue will sort it by distance, so closer vertices are processed first
                    // distances.get(neighbourValue) gets the distance we just updated
                    queue.add(new QueueObject(e.getEnd(), distances.get(neighbourValue)));
                }
            }
        }
        
        // Return both dictionaries as an array
        // [0] = distances dictionary (shortest distance to each vertex)
        // [1] = previous dictionary (how to reach each vertex)
        return new Dictionary[] {distances, previous};
    }



    /**
     * Prints the results of Dijkstra's algorithm in a readable format
     * 
     * @param d: Array of two dictionaries [distances, previous]
     */
    public static void dijkstraResultPrinter(Dictionary[] d){
        System.out.println("Distances:\n");
        // Iterate through all keys (vertex names) in the distances dictionary (d[0])
        // keys() returns an Enumeration of all the vertex name strings
        for (Enumeration keys = d[0].keys(); keys.hasMoreElements();){
            // Get the next vertex name
            String nextKey = keys.nextElement().toString();
            // Print the vertex name and its shortest distance from the start
            System.out.println(nextKey + ": " + d[0].get(nextKey));
        }
        
        System.out.println("\nPrevious:\n");
        // Iterate through all keys (vertex names) in the previous dictionary (d[1])
        for (Enumeration keys = d[1].keys(); keys.hasMoreElements();) {
            // Get the next vertex name
            String nextKey = keys.nextElement().toString();
            // Get the Vertex that comes before this vertex in the shortest path
            AirportVertex nextVertex = (AirportVertex) d[1].get(nextKey);
            // Print which vertex leads to this one
            System.out.println(nextKey + ": " + nextVertex.getData());
        }
    }


    public static void main(String[] args) {
        // Note: This main method needs to be updated to work with AirportVertex
        // which requires Airport objects instead of String names
        // For now, this is left as a template
    }
    
}
