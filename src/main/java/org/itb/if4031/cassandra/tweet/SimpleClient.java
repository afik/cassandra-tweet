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
import com.datastax.driver.core.utils.UUIDs;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
        System.out.printf("Connected to : %s\n", 
              metadata.getClusterName());
//        for ( Host host : metadata.getAllHosts() ) {
//           System.out.printf("Datacenter: %s; Host: %s; Rack: %s\n",
//              host.getDatacenter(), host.getAddress(), host.getRack());
//        }
        session = cluster.connect("afik");
    }
    
    public void close() {
        session.close();
        cluster.close();
    }
    
    public boolean handleCommand(String command) {
        boolean exit = false;
        String split[] = command.split(" ",2);
        
        if (split[0].equals("register") && split.length == 2) {
            String s[] = split[1].split(" ");
            if(s.length == 2){
                String username = split[1].split(" ")[0];
                String password = split[1].split(" ")[1];
                session.execute(
                        "INSERT INTO users (username, password) VALUES ('"+
                                username +"','"+password+"');"
                );
                System.out.println(username + " successfully registered");
            }
            else{
                System.out.println("Usage : register <username> <password>");
            }
        } else if (split[0].equals("login") && split.length == 2) {
            String s[] = split[1].split(" ");
            if(s.length == 2){
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
            }
            else{
                System.out.println("Usage : login <username> <password>");
            }
        } else if (split[0].equals("follow") && split.length == 2) {
            if (user != null) { 
                String tofollow = split[1];
                
                session.execute(
                        "INSERT INTO friends (username, friend, since) VALUES ('"+
                                tofollow + "','" + user + "',dateof(now()));"
                );
                session.execute(
                        "INSERT INTO followers (username, follower, since) VALUES ('"+
                                tofollow + "','" + user + "',dateof(now()));"
                );
                System.out.println("Successfully follow " + tofollow);
            } else {
                System.out.println("Please login first");
            }
        } else if (split[0].equals("tweet") && split.length == 2) {
            if (user != null) {
                String tweetBody = split[1];
                UUID timeuuid = UUIDs.timeBased();
                UUID tweetuuid = UUIDs.timeBased();
                session.execute(
                        "INSERT INTO tweets (tweet_id, username, body) VALUES ("+
                                tweetuuid + ",'" + user + "','" + tweetBody +"');"
                );
                session.execute(
                        "INSERT INTO userline (username, time, tweet_id) VALUES ('"+
                                user + "'," + timeuuid + "," + tweetuuid +");"
                );
                session.execute(
                        "INSERT INTO timeline (username, time, tweet_id) VALUES ('"+
                                user + "'," + timeuuid + "," + tweetuuid +");"
                );
                List<String> followers = new ArrayList<>();
                ResultSet results  = session.execute(
                    "SELECT username, follower FROM followers WHERE username = '"
                            + user+"';");
                for(Row row : results) {
                    followers.add(row.getString("follower"));
                }
                for (String follower : followers) {
                    session.execute(
                            "INSERT INTO timeline (username, time, tweet_id) VALUES ('"+
                                    follower +"'," + timeuuid + "," + tweetuuid+");"
                    );
                }
            } else {
                System.out.println("Please login first");
            }
        } else if (split[0].equals("showuserline") && split.length == 2) {
            if (user != null) {
                String toView = split[1];
                ResultSet tweetid = session.execute(
                        "SELECT time, tweet_id FROM userline WHERE username ='"+
                                toView+"'ORDER BY time DESC;");
                List<UUID> tweetids = new ArrayList<>();
                for (Row row : tweetid) {
                    tweetids.add(row.getUUID("tweet_id"));
                }
                for (UUID id : tweetids) {
                    ResultSet result = session.execute(
                            "SELECT username, body FROM tweets WHERE tweet_id ="+
                                    id+";");
                    Row row = result.one();
                    System.out.println(row.getString("username")+" : "+
                            row.getString("body"));
                }
            } else {
                System.out.println("Please login first");
            }
        } else if (split[0].equals("showtimeline")) {
            if (user != null) {
                ResultSet tweetid = session.execute(
                        "SELECT time, tweet_id FROM timeline WHERE username ='"+
                                user+"'ORDER BY time DESC;");
                List<UUID> tweetids = new ArrayList<>();
                for (Row row : tweetid) {
                    tweetids.add(row.getUUID("tweet_id"));
                }
                for (UUID id : tweetids) {
                    ResultSet result = session.execute(
                            "SELECT username, body FROM tweets WHERE tweet_id ="+
                                    id+";");
                    Row row = result.one();
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
