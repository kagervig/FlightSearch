package com.kristian.flightsearch.flightgraph;
public class Queue {

    public LinkedList queue;
    public int size;
  
    public Queue() {
      this.queue = new LinkedList();
      this.size = 0;
    }
  
    public boolean isEmpty() {
      return this.size == 0;
    }
  
    public void enqueue(AirportVertex data) {
      this.queue.addToTail(data);
      this.size++;
    }
  
    public AirportVertex peek() {
      if (this.isEmpty()) {
        return null;
      } else {
        return this.queue.head.data;
      }    
    }
  
    public AirportVertex dequeue() {
      if (!this.isEmpty()) {
        AirportVertex data = this.queue.removeHead();
        this.size--;
        return data;
      } else {
        throw new Error("Queue is empty!");
      }
    }
  
}