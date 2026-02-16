package com.kristian.flightsearch.flightgraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.time.Duration;

import com.kristian.flightsearch.models.Airport;

public class FlightGraph {
    private ArrayList<AirportVertex> vertices;
    private HashMap<String, AirportVertex> vertexIndex;
    private boolean isWeighted;
    private boolean isDirected;

    public FlightGraph (boolean isWeighted, boolean isDirected){
        this.vertices = new ArrayList<AirportVertex>();
        this.vertexIndex = new HashMap<>();
        this.isDirected = isDirected;
        this.isWeighted = isWeighted;
    }

    public AirportVertex addVertex(Airport airport){
        AirportVertex newVertex = new AirportVertex(airport);
        this.vertices.add(newVertex);
        this.vertexIndex.put(airport.getCode(), newVertex);
        return newVertex;
    }

    public AirportVertex getVertex(String airportCode){
        return this.vertexIndex.get(airportCode);
    }

    public void addEdge(AirportVertex vertex1, AirportVertex vertex2, Integer price, Duration duration, String flightNumber){
        if (!this.isWeighted){
            price = null;
        }
        vertex1.addEdge(vertex2, price, duration, flightNumber);

    }

    public void removeEdge(AirportVertex vertex1, AirportVertex vertex2){
        vertex1.removeEdge(vertex2);
        if (!this.isDirected){
            vertex2.removeEdge(vertex1);
        }
    }

    public void removeVertex(AirportVertex vertex){
        this.vertices.remove(vertex);

    }

    public ArrayList<AirportVertex> getVertices() {
		return this.vertices;
	}

	public boolean isWeighted() {
		return this.isWeighted;
	}

	public boolean isDirected() {
		return this.isDirected;
	}

    public void print() {
        //int count = 0;
		for(AirportVertex v: this.vertices) {
			v.print(isWeighted);
            //count++;
		}
        //System.out.print(count);
	}



    public static void main(String[] args) {
        FlightGraph flightNetwork = new FlightGraph(true, true);

        Airport JFK = new Airport("JFK", "John F. Kennedy International Airport", 40.6398, -73.7789, "America/New_York", 4423);
        Airport ATL = new Airport("ATL", "Hartsfield-Jackson Atlanta International Airport", 33.6404, -84.4199, "America/New_York", 3962);
        Airport LAX = new Airport("LAX", "Los Angeles International Airport", 33.9428, -118.4100, "America/Los_Angeles", 3939);

        AirportVertex jfkVertex = flightNetwork.addVertex(JFK);
        AirportVertex atlVertex = flightNetwork.addVertex(ATL);
        AirportVertex laxVertex = flightNetwork.addVertex(LAX);

        flightNetwork.addEdge(jfkVertex, laxVertex, 300, Duration.ofHours(5).plusMinutes(30), "UA 12");
        flightNetwork.addEdge(atlVertex, laxVertex, 333, Duration.ofHours(4).plusMinutes(15), "UA 97");
        flightNetwork.addEdge(atlVertex, jfkVertex, 700, Duration.ofHours(2).plusMinutes(10), "UA 82");
        flightNetwork.addEdge(jfkVertex, laxVertex, 328, Duration.ofHours(5).plusMinutes(45), "UA 25");
        flightNetwork.addEdge(atlVertex, laxVertex, 535, Duration.ofHours(4).plusMinutes(30), "UA 333");
        flightNetwork.addEdge(laxVertex, atlVertex, 100, Duration.ofHours(4).plusMinutes(0), "UA 99");
        flightNetwork.addEdge(jfkVertex, laxVertex, 200, Duration.ofHours(5).plusMinutes(15), "UA 656");

        flightNetwork.print();

    }
}



/* 
vertex = airport
edge = route
edge weight = price (or time)

populate vertices with airports
then populate list of edges.

a graph can have multiple edges between vertices


to build a graph of all available flights, first need to populate list of vertices
    for (Airport a : airports)
    


*/


/*
        Graph busNetwork = new Graph(true, true);
        Vertex cliftonStation = busNetwork.addVertex("Clifton");
        Vertex capeMayStation = busNetwork.addVertex("Cape May");

        busNetwork.addEdge(cliftonStation, capeMayStation, 1000);
        busNetwork.addEdge(capeMayStation, cliftonStation, 50);
        busNetwork.addEdge(cliftonStation, capeMayStation, 300);
        busNetwork.addEdge(capeMayStation, cliftonStation, 500);
        busNetwork.addEdge(cliftonStation, capeMayStation, 330);

        busNetwork.print();
 */