package com.kristian.flightsearch.flightgraph;

import java.util.ArrayList;

import com.kristian.flightsearch.models.Airport;

public class Graph {
    private ArrayList<AirportVertex> vertices;
    private boolean isWeighted;
    private boolean isDirected;

    public Graph (boolean isWeighted, boolean isDirected){
        this.vertices = new ArrayList<AirportVertex>();
        this.isDirected = isDirected;
        this.isWeighted = isWeighted;
    }

    public AirportVertex addVertex(Airport data){
        AirportVertex newVertex = new AirportVertex(data);
        this.vertices.add(newVertex);
        return newVertex;
    }

    public void addEdge(AirportVertex vertex1, AirportVertex vertex2, Integer weight){
        if (!this.isWeighted){
            weight = null;
        }
        vertex1.addEdge(vertex2, weight, null);
        if (!isDirected){
            vertex2.addEdge(vertex1, weight, null);
        }
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

	public AirportVertex getVertexByCode(String code) {
		for(AirportVertex v: this.vertices) { 
			if (v.getData().getCode().equals(code)) {
				return v;
			}
		}

		return null;
	}
	
	public void print() {
		for(AirportVertex v: this.vertices) {
			v.print(isWeighted);
		}
	}



}
