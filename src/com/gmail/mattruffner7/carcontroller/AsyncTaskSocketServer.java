package com.gmail.mattruffner7.carcontroller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

import android.os.AsyncTask;
import android.util.Log;

public class AsyncTaskSocketServer extends AsyncTask<Integer, String, Integer> {

       private int id = 829347;
       private String TAG = "AsyncTaskSocketServer";

       AsyncTaskSocketServer() {
           super();
           Random generator = new Random();
           id = generator.nextInt();
           Log.d(TAG, "created with id: " + id);
       }

       @Override
       protected Integer doInBackground(Integer... ports) {

           int port = ports[0];
           Log.v(TAG, "Trying to start on port: " + port + " with id: " + id);

           try {
               ServerSocket serverSocket = new ServerSocket(port);

               while (!isCancelled()) {
                   Socket client = serverSocket.accept();
                   try {
                       Log.v(TAG, "Listening on port: "
                               + port);
                       BufferedReader in = new BufferedReader(
                               new InputStreamReader(client.getInputStream()));
                       String str = in.readLine();
                       publishProgress(str);

                   } catch (Exception e) {
                       e.printStackTrace();
                       Log.v(TAG, "Exception while socket.accept"+ id);
                   } finally {
                       client.close();
                   }
                   client.close();
               }
           } catch (Exception e) {
               e.printStackTrace();
               Log.v(TAG, "Exception in SocketServer creation" + id);
           }
           return port;
       }

       @Override
       protected void onProgressUpdate(String... values) {
           super.onProgressUpdate(values);
           String message = values[0];
           try {
               //NetworkQueue.MESSAGE_IN_QUEUE.put(message);
               Log.v(TAG, "received: " + message);
           } catch (Exception e) {
               Log.e("Car Controller","AsyncTaskSocketServer: Exception while writing to IN_QUEUE");
           }
       }
}