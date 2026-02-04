package com.kristian.flightsearch.flightgraph;

import java.util.ArrayList;

import com.kristian.flightsearch.utils.AirportPrinter;
import com.kristian.flightsearch.utils.FlightPrinter;
import com.kristian.flightsearch.models.Airport;

public class AirportVertex {
    private Airport data;
    private ArrayList<Edge> edges;

    public AirportVertex(Airport inputData){
        this.data = inputData;
        this.edges = new ArrayList<Edge>();
    }

    public void addEdge(AirportVertex endVertex, Integer weight, String flightNumber){
        this.edges.add(new Edge(this, endVertex, weight, flightNumber));
    }

    public void removeEdge(AirportVertex endVertex){
        this.edges.removeIf(edge -> edge.getEnd().equals(endVertex));
    } 

    public Airport getData(){
        return this.data;
    }

    public ArrayList<Edge> getEdges(){
        return this.edges;
    }
    

    public void print(boolean showWeight) {
		String message = "";
        AirportPrinter ap = new AirportPrinter();
        FlightPrinter fp = new FlightPrinter();
        boolean printedSomething = false;

        if (this.edges.isEmpty() && printedSomething){
            System.out.println("Error: No routes found from this airport.");
            return;
        }
		
        // Loop through all edges (connections) from this vertex
		for(int i = 0; i < this.edges.size(); i++) {
            //each edge represents a flight from origin to destination
            //search for flights between origin and destination
            //print flight number and price
                //option to print all resulting flights using flightprinter

            //============

            // On the first edge only, add the starting vertex name and arrow
			// Example: "MIA -->  " (we only print the source vertex once)
            if (i == 0) {
				message += this.edges.get(i).getStart().getData().getCode() + " -->  ";
			}
            printedSomething = true;
 
			// Add the destination vertex name for this edge
			// edges.get(i).getEnd() returns the Vertex this edge points to
			// .data gets the name of that vertex
			// Example: adds "B" to the message
			message += this.edges.get(i).getEnd().getData().getCode();
			
			// If showWeight is true, add the edge weight in parentheses
			// edges.get(i).getWeight() returns the Integer weight of this edge
			// Example: adds " (3)" if the edge weight is 3
			if (showWeight) {
				//message += " [" + this.edges.get(i).getFlightNum() + "  $" + this.edges.get(i).getWeight() + "]";
                message += " [$" + this.edges.get(i).getWeight() + "]";
			}
			
			// If this isn't the last edge, add a comma separator
			// This creates the format "A --> B, C, D" instead of "A --> BCD"
			if (i != this.edges.size() - 1) {
				message += ", ";
			}
		}
        //once the for loop is complete we can print the output (message)
        System.out.println(message);
	}
    
}
