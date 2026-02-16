package com.kristian.flightsearch.flightgraph;

import java.time.Duration;
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





    public static void depthFirstTraversal(AirportVertex origin, AirportVertex destination, ArrayList<AirportVertex> visitedVertices, int legs, String message, Duration totalDuration){

        message+= origin.getData().getCode() + " --> ";
        legs++;
        if (legs > 5){
            return;
        }

        
        
        for (Edge e : origin.getEdges()){
            AirportVertex neighbour = e.getEnd();
            if (e.getEnd() == destination){
                String originCode = message.substring(0,3); //fixes bug where wrong origin was printed
                System.out.println("Possible route Found between " + originCode + " and " + destination.getData().getCode());
                System.out.println(message + destination.getData().getCode());
                System.out.println(legs-1 + " stops");
                System.out.println();
                return;
            } 
            if (!visitedVertices.contains(neighbour)){
                visitedVertices.add(neighbour);
                depthFirstTraversal(neighbour, destination, visitedVertices, legs, message, totalDuration);
            }
            
        }
    }

    /*
    Duration total = duration1.plus(duration2);
    You can also chain them:
        Duration total = duration1.plus(duration2).plus(duration3);
    Or use plusHours(), plusMinutes(), etc. for adding specific units:  
        Duration extended = duration1.plusHours(2).plusMinutes(30); 
    */

    public static void breadthFirstSearch(AirportVertex origin, AirportVertex destination, ArrayList<AirportVertex> visitedVertices){
        Queue visitQueue = new Queue();
        visitQueue.enqueue(origin);
        String message = origin.getData().getCode() + " --> ";
        int legs = 0;
        
        while (!visitQueue.isEmpty()){
            AirportVertex current = visitQueue.dequeue();
            System.out.println(current.getData().getCode());

            for (Edge e : current.getEdges()){
                message+= current.getData().getCode() + " --> ";
                legs++;

                AirportVertex neighbour = e.getEnd();
                if (e.getEnd() == destination){     //found target airport
                    System.out.println("BFS Success");
                }
                if (!visitedVertices.contains(neighbour)){
                    visitedVertices.add(neighbour);
                    visitQueue.enqueue(neighbour);
                }
                System.out.println(message);
                System.out.println(legs + " stops");
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
