package com.kristian.flightsearch.flightgraph;
public class Node {
    Integer data;
    Node left;
    Node right;


    public Node(Integer data){
        this.data = data;
    }
    Integer getData(){
        return this.data;
    }
    Node getLeft(){
        return this.left;
    }
    Node getRight(){
        return this.right;
    }
    void setLeft(Node left){
        this.left = left;
    }
    void setRight(Node right){
        this.right = right;
    }
}