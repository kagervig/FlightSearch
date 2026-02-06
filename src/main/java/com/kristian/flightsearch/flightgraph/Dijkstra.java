package com.kristian.flightsearch.flightgraph;

import com.kristian.flightsearch.models.Airport;
import java.time.Duration;
import java.util.*;

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
    public static Map[] searchByPrice(FlightGraph g, AirportVertex startingVertex){
        // distances: stores the shortest known distance from start to each vertex
        // Key = airport code (String), Value = distance (Integer)
        // Example: {"A": 0, "B": 3, "C": 100}
        Map<Airport, Integer> flightPrices = new HashMap<>();

        // previous: stores which vertex you came FROM to reach each vertex (for reconstructing the path)
        // Key = airport code (String), Value = the previous vertex (AirportVertex object)
        // Example: {"B": A, "C": D} means you reach B from A, C from D
        Map<Airport, AirportVertex> previous = new HashMap<>();
        
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
                flightPrices.put(v.getData(), Integer.MAX_VALUE);
            }
            // Set the "previous vertex" for all vertices to null
            // This will be updated as we find the shortest paths
            previous.put(v.getData(), null);
        }

        // Set the distance to the starting vertex from itself to 0 (we're already there)
        flightPrices.put(startingVertex.getData(), 0);

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
                Integer alternativePrice = flightPrices.get(current.getData()) + e.getPrice();
                
                // Get the neighbor vertex's airport that this edge points to
                // e.getEnd() returns the AirportVertex that this edge connects TO
                Airport neighbourValue = e.getEnd().getData();
                
                // Check if this new path is SHORTER than what we previously knew
                // distances.get(neighbourValue) returns the current best-known distance to the neighbor
                // If alternative is smaller, we found a better path!
                if (alternativePrice < flightPrices.get(neighbourValue)){
                    // Update the shortest distance to this neighbor
                    flightPrices.put(neighbourValue, alternativePrice);
                    
                    // Remember that we reach this neighbor FROM the current vertex
                    // This lets us reconstruct the actual path later if needed
                    previous.put(neighbourValue, current);
                    
                    // Add the neighbor to the queue with its new (updated) distance
                    // PriorityQueue will sort it by distance, so closer vertices are processed first
                    // distances.get(neighbourValue) gets the distance we just updated
                    queue.add(new QueueObject(e.getEnd(), flightPrices.get(neighbourValue)));
                }
            }
        }
        
        // Return both maps as an array
        // [0] = distances map (shortest distance to each vertex)
        // [1] = previous map (how to reach each vertex)
        return new Map[] {flightPrices, previous};
    }

    public static Map[] searchByDuration(FlightGraph g, AirportVertex startingVertex){
        Map<Airport, Duration> flightDurations = new HashMap<>();
        Map<Airport, AirportVertex> previous = new HashMap<>();

        PriorityQueue<QueueObject> queue = new PriorityQueue<>(
            Comparator.comparing(q -> (Duration) flightDurations.get(q.vertex.getData()))
        );

        for (AirportVertex v : g.getVertices()){
            if (v != startingVertex){
                flightDurations.put(v.getData(), Duration.ofHours(99));
            }
            previous.put(v.getData(), null);
        }

        flightDurations.put(startingVertex.getData(), Duration.ZERO);

        queue.add(new QueueObject(startingVertex, 0)); // The int is ignored in this context

        while (!queue.isEmpty()){
            AirportVertex current = queue.poll().vertex;
            for (Edge e : current.getEdges()){
                Duration alternativeDuration = flightDurations.get(current.getData()).plus(e.getDuration());
                Airport neighbourValue = e.getEnd().getData();

                if (alternativeDuration.compareTo(flightDurations.get(neighbourValue)) < 0){
                    flightDurations.put(neighbourValue, alternativeDuration);
                    previous.put(neighbourValue, current);
                    queue.add(new QueueObject(e.getEnd(), 0)); // The int is ignored
                }
            }
        }
        return new Map[] {flightDurations, previous};
    }

    /**
     * Prints the results of Dijkstra's algorithm in a readable format
     *
     * @param results: Array of two maps [distances, previous]
     */
    @SuppressWarnings("unchecked")
    public static void printSearchResult(Map[] results){
        Map<Airport, ?> distances = results[0];
        Map<Airport, AirportVertex> previous = (Map<Airport, AirportVertex>) results[1];

        System.out.println("Prices:\n");
        for (Map.Entry<Airport, ?> entry : distances.entrySet()) {
            System.out.println(entry.getKey().getCode() + ": $p" + entry.getValue());
        }

        System.out.println("\nPrevious:\n");
        for (Map.Entry<Airport, AirportVertex> entry : previous.entrySet()) {
            AirportVertex prevVertex = entry.getValue();
            String prevAirport = (prevVertex != null) ? prevVertex.getData().getCode() : "none";
            System.out.println(entry.getKey() + ": " + prevAirport);
        }
    }


    public static void main(String[] args) {
        // Note: This main method needs to be updated to work with AirportVertex
        // which requires Airport objects instead of String names
        // For now, this is left as a template


    }
    
}
