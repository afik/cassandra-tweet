/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.itb.if4031.cassandra.tweet;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.UUID;


public class SimpleClient {
    private Cluster cluster;
    private Session session;
    public String user;
    
   
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
        session = cluster.connect("afik");
    }
    
    public void close() {
        session.close();
        cluster.close();
    }
    
    public boolean handleCommand(String command) {
        boolean exit = false;
        String split[] = command.split(" ",2);
        
        if (split[0].equals("register")) {
            String username = split[1].split(" ")[0];
            String password = split[1].split(" ")[1];
            session.execute(
                    "INSERT INTO users (username, password) VALUES ('"+
                            username +"','"+password+"');"
            );
            System.out.println(username + " successfully registered");
        } else if (split[0].equals("login")) {
            String username = split[1].split(" ")[0];
            String password = split[1].split(" ")[1];
            ResultSet results  = session.execute(
                    "SELECT username, password FROM users WHERE username = '" + username+"';");
            if(results.one().getString("password").equals(password)) {
                user = username;
                System.out.println("Hello, " + user + " !");
            } else {
                System.out.println("Wrong password");
            }
        } else if (split[0].equals("follow")) {
            if (user != null) { 
                String tofollow = split[1];
                Date now = new Date();
                SimpleDateFormat format = new SimpleDateFormat("yyyy-mm-dd HH:mm");
                String date = format.format(now);
                
                session.execute(
                        "INSERT INTO friends (username, friend, since) VALUES ('"+
                                user + "','" + tofollow + "','" + date +"');"
                );
                session.execute(
                        "INSERT INTO followers (username, follower, since) VALUES ('"+
                                user + "','" + tofollow + "','" + date +"');"
                );
                System.out.println("Successfully follow " + tofollow);
            } else {
                System.out.println("Please login first");
            }
        } else if (split[0].equals("tweet")) {
            //TODO : cek lagi kayanya salah deh
            if (user != null) {
                String tweetBody = split[1];
                UUID timeuuid = UUID.fromString(new Date().toString());
                //TODO : bedain timeuuid & uuid
                UUID tweetuuid = UUID.fromString("");
                session.execute(
                        "INSERT INTO tweets (tweet_id, username, body) VALUES ('"+
                                tweetuuid + "','" + user + "','" + tweetBody +"');"
                );
                session.execute(
                        "INSERT INTO userline (username, time, tweet_id) VALUES ('"+
                                user + "','" + timeuuid + "','" + tweetuuid +"');"
                );
                session.execute(
                        "INSERT INTO timeline (username, time, tweet_id) VALUES ('"+
                                user + "','" + timeuuid + "','" + tweetuuid +"');"
                );
            } else {
                System.out.println("Please login first");
            }
        } else if (split[0].equals("showuserline")) {
            //TODO : cek lagi kayanya salah deh
            if (user != null) {
                String toView = split[1];
                ResultSet results  = session.execute(
                    "SELECT username, body FROM tweets WHERE username = "
                            + toView+";");
                for(Row row : results) {
                    System.out.println(row.getString("username")+" : "+
                            row.getString("body"));
                }
            } else {
                System.out.println("Please login first");
            }
        } else if (split[0].equals("showtimeline")) {
            //TODO : cek lagi kayanya salah deh
            if (user != null) {
                ResultSet results  = session.execute(
                    "SELECT username, body FROM tweets t JOIN timeline u ON"
                            + "t.tweet_id=u.tweet_id WHERE t.username = "
                            + user+";");
                for(Row row : results) {
                    System.out.println(row.getString("username")+" : "+
                            row.getString("body"));
                }
            } else {
                System.out.println("Please login first");
            }
        } else if (split[0].equalsIgnoreCase("exit")){
            exit = true;
        } else {
            printUsage();
        }
        return exit;
    }
    
    public void printUsage() {
        System.out.println("Available command : ");
        System.out.println("- register <username> <password>");
        System.out.println("- login <username> <password>");
        System.out.println("- follow <username>");
        System.out.println("- tweet <tweet>");
        System.out.println("- showuserline <username>");
        System.out.println("- showtimeline");
        System.out.println("- exit");
    }
    
    public static void main (String[] args) {
        SimpleClient client = new SimpleClient();
        client.connect("167.205.35.19");
        client.user = null;
        boolean exit = false;
        client.printUsage();
        Scanner in = new Scanner(System.in);
        do {
            System.out.print("> ");
            String command = in.nextLine();
            exit = client.handleCommand(command);
        } while (!exit);
        client.close();
    }
}
