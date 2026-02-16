package com.kristian.flightsearch.flightgraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.time.Duration;

import com.kristian.flightsearch.models.Airport;

public class NewFlightGraph {
    private ArrayList<AirportVertex> vertices;
    private HashMap<String, AirportVertex> vertexIndex;
    private boolean isWeighted;
    private boolean isDirected;

    public NewFlightGraph (boolean isWeighted, boolean isDirected){
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



        

        flightNetwork.print();

    }
}

