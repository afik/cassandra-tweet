/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.itb.if4031.cassandra.tweet;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.Session;


public class SimpleClient {
    private Cluster cluster;
    private Session session;
    
    public Session getSession() {
        return this.session;
    }
    
    public void connect(String node) {
        cluster = Cluster.builder()
         .addContactPoint(node)
         .build();
        Metadata metadata = cluster.getMetadata();
        System.out.printf("Connected to cluster: %s\n", 
              metadata.getClusterName());
        for ( Host host : metadata.getAllHosts() ) {
           System.out.printf("Datacenter: %s; Host: %s; Rack: %s\n",
              host.getDatacenter(), host.getAddress(), host.getRack());
        }
        session = cluster.connect();
    }
    
    public void close() {
        session.close();
        cluster.close();
    }
    
    public static void main (String[] args) {
        SimpleClient client = new SimpleClient();
        client.connect("127.0.0.1");
        client.close();
    }
}
