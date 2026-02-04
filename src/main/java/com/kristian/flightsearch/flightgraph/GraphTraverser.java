package com.kristian.flightsearch.flightgraph;

import java.util.ArrayList;

public class GraphTraverser {
    
    public static void depthFirstTraversal(AirportVertex origin, AirportVertex destination, ArrayList<AirportVertex> visitedVertices){
        //System.out.print(origin.getData().getCode() + " --> ");
        
        for (Edge e : origin.getEdges()){
            AirportVertex neighbour = e.getEnd();
            if (!visitedVertices.contains(neighbour)){
                visitedVertices.add(neighbour);
                depthFirstTraversal(neighbour, destination, visitedVertices);
            }
            if (e.getEnd() == destination){
                System.out.println("Success");
                return;
            } 
        }
        System.out.println();
    }

    public static void depthFirstTraversal(AirportVertex origin, AirportVertex destination, ArrayList<AirportVertex> visitedVertices, int legs, String message){

        message+= origin.getData().getCode() + " --> ";
        legs++;
        
        for (Edge e : origin.getEdges()){
            AirportVertex neighbour = e.getEnd();
            if (e.getEnd() == destination){
                System.out.println("Route Found between " + origin.getData().getCode() + " and " + destination.getData().getCode());
                System.out.println(message + destination.getData().getCode());
                System.out.println(legs + " stops");
                return;
            } 
            if (!visitedVertices.contains(neighbour)){
                visitedVertices.add(neighbour);
                depthFirstTraversal(neighbour, destination, visitedVertices, legs, message);
            }
            System.out.println();

            
        }




    }





    public static void breadthFirstSearch(AirportVertex start, ArrayList<AirportVertex> visitedVertices){
        Queue visitQueue = new Queue();
        visitQueue.enqueue(start);
        
        while (!visitQueue.isEmpty()){
            AirportVertex current = visitQueue.dequeue();
            System.out.println(current.getData());

            for (Edge e : current.getEdges()){
                AirportVertex neighbour = e.getEnd();
                if (!visitedVertices.contains(neighbour)){
                    visitedVertices.add(neighbour);
                    visitQueue.enqueue(neighbour);
                }
            }
        }
    }

    public static void main(String[] args) {
        /* TestGraph class no longer exists; needs to be rewritten before use
        TestGraph test = new TestGraph();
        AirportVertex startingVertex = test.getStartingVertex();

        System.out.println("DFS");
        ArrayList<AirportVertex> dfsVisited = new ArrayList<AirportVertex>();
        dfsVisited.add(startingVertex);
        depthFirstTraversal(startingVertex, dfsVisited);

        System.out.println("\nBFS");
        ArrayList<AirportVertex> bfsVisited = new ArrayList<AirportVertex>();
        bfsVisited.add(startingVertex);
        breadthFirstSearch(startingVertex, bfsVisited);
        */
    }
}
