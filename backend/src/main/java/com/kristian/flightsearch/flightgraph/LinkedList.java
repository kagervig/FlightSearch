package com.kristian.flightsearch.flightgraph;
public class LinkedList {
    public Node head;
    private Node tail;

    public static class Node {
        public AirportVertex data;
        public Node next;

        public Node(AirportVertex data) {
            this.data = data;
        }
    }

    public void addToTail(AirportVertex data) {
        Node newNode = new Node(data);
        if (head == null) {
            head = tail = newNode;
        } else {
            tail.next = newNode;
            tail = newNode;
        }
    }

    public AirportVertex removeHead() {
        if (head == null) {
            throw new IllegalStateException("LinkedList is empty");
        }
        AirportVertex data = head.data;
        head = head.next;
        if (head == null) {
            tail = null;
        }
        return data;
    }
}
