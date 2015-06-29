package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.net.Uri;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.net.UnknownHostException;
import java.util.ArrayList;

import java.util.Collections;
import java.util.Iterator;


/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String [] REMOTE_PORT = {"11108","11112","11116","11120","11124"};
    static final int SERVER_PORT = 10000;
    static private int keyCount = 0;
    private ArrayList<MessageObject> msgList = new ArrayList<MessageObject>();
    private int proposed = 1;
    private int aliveNodes = 5;
    private int msgCount = 1;
    private int procNum = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        for (int i=0 ;i<5;i++) {
            if (Integer.parseInt(myPort) == Integer.parseInt(REMOTE_PORT[i])){
                procNum = i;
            }
        }
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);

        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        //display the messages
        final EditText editText = (EditText) findViewById(R.id.editText1);
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        /*
         * registers and implements an OnClickListener for the "Send" button.
         * gets the message from the input box (EditText)
         * and sends it to other AVDs.
         */
        Button sendButton = (Button) findViewById(R.id.button4);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String msg = editText.getText().toString() + "\n";
                TextView tv1 = (TextView) findViewById(R.id.textView1);
                editText.setText("");
                tv1.append("\t" + msg);
                //formatting msg string to desired
                msg = "M::"+ proposed +"::"+procNum+"::"+ msgCount +"::"+procNum+"::N::"+ msg;
                msgCount++;
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            Socket clientSocket = null;
            BufferedReader inMsg;
            try {
                while(true) {
                    clientSocket = serverSocket.accept();
                    inMsg = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    String rMsg = inMsg.readLine();
                    // interpreting the incoming message
                    String [] rMsgParts = rMsg.split("::");
                    MessageObject incomingMsg = new MessageObject();
                    incomingMsg.mType = rMsgParts[0];
                    incomingMsg.mPriority = Integer.parseInt(rMsgParts[1]);
                    incomingMsg.mProcNum = Integer.parseInt(rMsgParts[2]);
                    incomingMsg.mMsgId = Integer.parseInt(rMsgParts[3]);
                    incomingMsg.mComingFromProcess = Integer.parseInt(rMsgParts[4]);
                    incomingMsg.deliverable = rMsgParts[5];
                    incomingMsg.mMsg = rMsgParts[6];
//                    Log.d("Received","received :"+incomingMsg.toString());

                    if(incomingMsg.mType.equals("M")) {

                        //add msg to queue & send back the proposed priority
                        incomingMsg.mType = "P";
                        proposed++;
                        incomingMsg.mPriority = proposed;
                        if (incomingMsg.mComingFromProcess != procNum) {
                            incomingMsg.mComingFromProcess = procNum;
                            msgList.add(incomingMsg);
//                            Log.d("Multicast","Added to msgList "+incomingMsg.toString());

                        }
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(REMOTE_PORT[incomingMsg.mProcNum]));
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        out.println(incomingMsg.toString() + "\r\n");
                        socket.close();

                    } else if (incomingMsg.mType.equals("P")) {

                        //check priority queue for the message and update its priority
//                        Log.d("Proposed","inside P evaluation "+incomingMsg.toString());
                        for(MessageObject qMsg : msgList) {
//                            Log.d("Proposed","qMsg Id "+qMsg.mMsgId+" qMsg procnum "+qMsg.mProcNum);
                            if ((qMsg.mMsgId == incomingMsg.mMsgId) &&
                                    (qMsg.mProcNum == incomingMsg.mProcNum)) {
                                qMsg.receipt += 1;
//                                Log.d("Proposed", "receipt for "+incomingMsg.mProcNum+
//                                        " process's message"+incomingMsg.mMsgId +"is "+qMsg.receipt);
//                                if(incomingMsg.mProcNum != procNum) {
                                    qMsg.mPriority = (qMsg.mPriority > incomingMsg.mPriority) ?
                                            qMsg.mPriority : incomingMsg.mPriority;
//                                }
                                if(qMsg.receipt == aliveNodes) {
                                    //multicast the message and final priority and send out R msg
                                    qMsg.mType = "R";
                                    Log.d("Proposed","receipt matched for "+qMsg.toString());
                                    qMsg.receipt = 0;
                                    for(int k = 0; k < 5; k++) {
                                        Socket socket = new Socket(InetAddress.
                                                getByAddress(new byte[]{10, 0, 2, 2}),
                                                Integer.parseInt(REMOTE_PORT[k]));
                                        PrintWriter out = new PrintWriter
                                                (socket.getOutputStream(), true);
                                        out.println(qMsg.toString() + "\r\n");
                                    }
                                }
                                break;
                            }
                        }
                    }
                    else if (incomingMsg.mType.equals("R")) {

                        //check queue for the msg and update its priority with agreed.
//                        Log.d("Delivery",  "Received agreed priority for "+incomingMsg.toString());
                        proposed = (incomingMsg.mPriority > proposed)?
                                incomingMsg.mPriority:proposed;
                        for(MessageObject qMsg : msgList) {
                            if ((qMsg.mMsgId == incomingMsg.mMsgId) &&
                                    (qMsg.mProcNum == incomingMsg.mProcNum)) {
                                Log.d("Delivery","MsgList = "+msgList.toString());
//                                Log.d("Delivery","Marked "+incomingMsg.toString());
                                qMsg.mPriority = incomingMsg.mPriority;
                                qMsg.deliverable = "Y";
                                Collections.sort(msgList, MessageObject.priorityComparator);
                                Log.d("Delivery","After sort MsgList = "+msgList.toString());
                                break;
                            }
                        }

                    }
                    Iterator<MessageObject> iter = msgList.iterator();
                    while (iter.hasNext()) {
                        MessageObject checkmsg = iter.next();
                        if (checkmsg.deliverable.equals("Y")) {
//                            Log.d("Delivery","Delivered "+ checkmsg.toString());
                            publishProgress(checkmsg.mMsg);
                            iter.remove();
                        } else {
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Exception in Server Task");
            }
            return null;
        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
//            Log.d("Delivery","Progress Update");
            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");
            //insert into db
            String key = Integer.toString(keyCount);
            ContentValues values = new ContentValues();
            values.put("key",key);
            values.put("value",strReceived);
            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.authority("edu.buffalo.cse.cse486586.groupmessenger2.provider");
            uriBuilder.scheme("content");
            Uri iUri = uriBuilder.build();
            Uri newUri = getApplicationContext().getContentResolver().insert(iUri,values);
            keyCount++;
//            Log.e(TAG, "Storing in AVD's internal storage");
            return;
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {

            String msgToSend = msgs[0];
            MessageObject sendingObj = new MessageObject();
            String[] rMsgParts = msgToSend.split("::");
            sendingObj.mType = rMsgParts[0];
            sendingObj.mPriority = Integer.parseInt(rMsgParts[1]);
            sendingObj.mProcNum = Integer.parseInt(rMsgParts[2]);
            sendingObj.mMsgId = Integer.parseInt(rMsgParts[3]);
            sendingObj.mComingFromProcess = Integer.parseInt(rMsgParts[4]);
            sendingObj.deliverable = rMsgParts[5];
            sendingObj.mMsg = rMsgParts[6];
            msgList.add(sendingObj);

            sendMessageToClient(REMOTE_PORT[0],msgToSend);
            sendMessageToClient(REMOTE_PORT[1],msgToSend);
            sendMessageToClient(REMOTE_PORT[2],msgToSend);
            sendMessageToClient(REMOTE_PORT[3],msgToSend);
            sendMessageToClient(REMOTE_PORT[4],msgToSend);
            return null;
        }

        private void sendMessageToClient(String remotePort,String msgToSend) {

            try {

                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(remotePort));
                PrintWriter out = new PrintWriter(socket.getOutputStream(),true);
                out.println(msgToSend + "\r\n");
//                Log.e(TAG, "Client sending msg: "+msgToSend);
                socket.close();
            }catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");

            }catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }
        }
    }
}
