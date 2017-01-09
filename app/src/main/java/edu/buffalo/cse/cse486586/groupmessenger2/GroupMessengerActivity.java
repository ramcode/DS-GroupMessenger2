package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;



import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 */
public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final int SERVER_PORT = 10000;
    static final String PROVIDER_NAME = "edu.buffalo.cse.cse486586.groupmessenger2.provider";
    static final String URL = "content://"+PROVIDER_NAME;
    private static final Uri CONTENT_URI = Uri.parse(URL);
    private static final String TABLE_COL_KEY = "key";
    private static final String TABLE_COL_VAL = "value";
    static Integer messageCounter = -1;
    static Map<String, String> avdMap = new HashMap<String, String>();
    static String myPort = null;
    static int[] myVectorTimeStamp = new int[] {0,0,0,0,0};
    static int mySequencer = 0;
    static Map<String, List<Message>> messageBuffer = new HashMap<String, List<Message>>();
    PriorityBlockingQueue<Message> messageQueue = new PriorityBlockingQueue<Message>(25, new MessageComparator());
    static Map<String, Boolean> avdTracker = new HashMap<String, Boolean>();
    // Variables for Message Identification
    static final String MESSAGE = "MSG";
    static final String PROPOSED_MSG = "PRP";
    static final String AGREED_MSG = "AGR";
    static final String CLEAN_UP_MSG = "CLN";
    static Integer myAgreedSequence = 0;
    Map<String, List<String>> portTracker = new HashMap<String,List<String>>();
    Map<String, List<String>> proposalTracker = new HashMap<String, List<String>>();
    Map<String, List<Double>> proposalValues = new HashMap<String, List<Double>>();
    Map<String, List<Message>> messageTracker = new HashMap<String, List<Message>>();
    Map<String, Boolean> receivedMessages = new HashMap<String, Boolean>();
    Set<String> failedPeers = new HashSet<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.local_text_display);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Calculate the port number that this AVD listens on.
         * It is just a hack that I came up with to get around the networking limitations of AVDs.
         * The explanation is provided in the PA1 spec.
         */
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        /*
         * Initialize avd map with client ports
         */
        avdMap.put(REMOTE_PORT0,"avd0");
        avdMap.put(REMOTE_PORT1, "avd1");
        avdMap.put(REMOTE_PORT2, "avd2");
        avdMap.put(REMOTE_PORT3, "avd3");
        avdMap.put(REMOTE_PORT4, "avd4");
        avdTracker.put(REMOTE_PORT0,true);
        avdTracker.put(REMOTE_PORT1, true);
        avdTracker.put(REMOTE_PORT2, true);
        avdTracker.put(REMOTE_PORT3, true);
        avdTracker.put(REMOTE_PORT4, true);
        try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
             * port.
             *
             * AsyncTask is a simplified thread construct that Android provides. Please make sure
             * you know how it works by reading
             * http://developer.android.com/reference/android/os/AsyncTask.html
             */
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            /*
             * Log is a good way to debug your code. LogCat prints out all the messages that
             * Log class writes.
             *
             * Please read http://developer.android.com/tools/debugging/debugging-projects.html
             * and http://developer.android.com/tools/debugging/debugging-log.html
             * for more information on debugging.
             */
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));


        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */


        try {
            Button sendButton = (Button) findViewById(R.id.button4);
            sendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final EditText editText = (EditText) findViewById(R.id.editText1);
                    String message = editText.getText().toString();
                    editText.setText("");
                    if (message != null && message.length()>0) {
                        TextView localTextView = (TextView) findViewById(R.id.local_text_display);
                        localTextView.append("\t"+avdMap.get(myPort) + ": " + "\n");
                        localTextView.append(message + "\n");
                        char[] spaces = new char[localTextView.getText().length()];
                        Arrays.fill(spaces, ' ');
                        TextView remoteTextView = (TextView) findViewById(R.id.remote_text_display);
                        remoteTextView.append(new String(spaces)+"\n");
                        new ClientMultiCastTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message);
                    }
                }

                ;
            });
        } catch (Exception ex) {
            Log.e(TAG, "Exception in Send message");
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }



    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     */
    private class ClientMultiCastTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            synchronized (ClientMultiCastTask.class) {
                mySequencer++;
            }
            String messageId = avdMap.get(myPort) + "-" + mySequencer;
            String msgToSend = msgs[0];
            Message multicastMessage = new Message(msgToSend);
            multicastMessage.setAvd(avdMap.get(myPort));
            multicastMessage.setFromPort(myPort);
            multicastMessage.setMessageType(MESSAGE);
            multicastMessage.setMessageId(messageId);
            multicastMessage.setOriginalSender(myPort);
            messageTracker.put(messageId, new ArrayList<Message>());
            proposalTracker.put(messageId, new ArrayList<String>());
            proposalValues.put(messageId, new ArrayList<Double>());
            portTracker.put(messageId, new ArrayList<String>());
            Iterator<Map.Entry<String, String>> it = avdMap.entrySet().iterator();
            while(it.hasNext()) {
                String clientPort = it.next().getKey();
                Socket socket = null;
                try {
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(clientPort));
                    socket.setSoTimeout(500);
                    OutputStream os = socket.getOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(os);
                    oos.writeObject(multicastMessage);
                    oos.flush();
                    oos.close();
                } catch (SocketTimeoutException ex) {
                    Log.e(TAG, "Client: " + clientPort + " is dead");
                } catch (UnknownHostException ex) {
                    Log.e(TAG, "Client: " + clientPort + " is not up yet");
                } catch (IOException ex) {
                    Log.e(TAG, "Exception in connecting to client: " + clientPort);
                } catch (Exception ex) {
                    Log.e(TAG, "Exception in connecting to client: " + clientPort);
                } finally {
                    try {
                        socket.close();
                    } catch (Exception ex) {
                        Log.e(TAG, "Exception in closing socket");
                    }
                }

            }

            return null;
        }
    }

    /***
     * ServerTask is an AsyncTask that should handle incoming messages. It is created by
     * ServerTask.executeOnExecutor() call in SimpleMessengerActivity.
     * <p>
     * Please make sure you understand how AsyncTask works by reading
     * http://developer.android.com/reference/android/os/AsyncTask.html
     *
     * @author stevko
     */
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
                Socket socket = null;

            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */
            try {
                while (true) {
                    socket = serverSocket.accept();
                    InputStream in = socket.getInputStream();
                    ObjectInputStream ois = new ObjectInputStream(in);
                    Message multiCastMessage = (Message) ois.readObject();
                    String myAvd = avdMap.get(myPort);
                    String msgId = multiCastMessage.getMessageId();
                    if (multiCastMessage != null) {
                        String messageType = multiCastMessage.getMessageType();
                        switch (messageType) {
                            case MESSAGE:
                                multiCastMessage.setAgreementReceived(false);
                                Integer myProposed = null;
                                synchronized (ServerTask.class) {
                                    myProposed = (mySequencer > myAgreedSequence ? mySequencer : myAgreedSequence) + 1;
                                    mySequencer = myProposed;
                                }
                                Double myProposedWithPort = Double.parseDouble(myProposed.toString() + "." + myPort);
                                Log.d(TAG, "I am proposing: " + myProposedWithPort);
                                multiCastMessage.setSequencer(myProposedWithPort);
                                messageQueue.add(multiCastMessage);
                                multiCastMessage.setMessageType(PROPOSED_MSG);
                                multiCastMessage.setFromPort(myPort);
                                multiCastMessage.setAvd(avdMap.get(myPort));
                                String senderPort = multiCastMessage.getOriginalSender();
                                //receivedMessages.put(messageQueue.getMessageId(), false);
                                Socket proposalSocket = null;
                                try {
                                    proposalSocket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                            Integer.parseInt(senderPort));
                                    //Timer tracker = new Timer();
                                    //tracker.schedule(new CheckAgreedMessage(proposalMessage.getMessageId(), proposalMessage.getOriginalSender()), 2000);
                                    proposalSocket.setSoTimeout(500);
                                    OutputStream os = proposalSocket.getOutputStream();
                                    ObjectOutputStream oos = new ObjectOutputStream(os);
                                    oos.writeObject(multiCastMessage);
                                    oos.flush();
                                    oos.close();
                                } catch (SocketTimeoutException ex) {
                                    Log.e(TAG, "Sender: " + senderPort + " is dead");
                                    //avdTracker.remove(senderPort);
                                    removeFailedClientMessage(senderPort);
                                } catch (UnknownHostException ex) {
                                    Log.e(TAG, "Sender: " + senderPort + " is not up yet");
                                    //avdTracker.remove(senderPort);
                                    removeFailedClientMessage(senderPort);
                                } catch (IOException ex) {
                                    Log.e(TAG, "Exception in connecting to sender: " + senderPort);
                                    //avdTracker.remove(senderPort);
                                    removeFailedClientMessage(senderPort);
                                } catch (Exception ex) {
                                    Log.e(TAG, "Exception in connecting to sender: " + senderPort);
                                    //avdTracker.remove(senderPort);
                                    removeFailedClientMessage(senderPort);
                                }
                                finally{
                                    try {
                                        proposalSocket.close();
                                    } catch (Exception ex) {
                                        Log.e(TAG, "Exception in closing socket");
                                    }
                                }
                                //createClientTask(multiCastMessage, messageType);
                                //Timer timer = new Timer();
                                //timer.schedule(new CheckAgreedMessage(msgId, multiCastMessage), 5000);
                                break;

                            case PROPOSED_MSG:
                                Log.d(TAG, multiCastMessage.getAvd() + " proposed " + multiCastMessage.getSequencer());
                                if (messageTracker.containsKey(msgId)) {
                                    messageTracker.get(msgId).add(multiCastMessage);
                                    proposalTracker.get(msgId).add(multiCastMessage.getFromPort());
                                    proposalValues.get(msgId).add(multiCastMessage.getSequencer());
                                }
                                if ((proposalTracker.get(msgId).size() + 1) == avdTracker.size()) {
                                    Timer tracker = new Timer();
                                    tracker.schedule(new CheckPeerStatus(msgId, multiCastMessage), 1000);
                                }
                                break;

                            case AGREED_MSG:
                                Log.d(TAG, "Final agreed sequence: " + multiCastMessage.getSequencer());
                                multiCastMessage.setAgreementReceived(true);
                                Double maxAgreed = multiCastMessage.getSequencer();
                                multiCastMessage.setIsDeliverable(true);
                                synchronized (this) {
                                    myAgreedSequence = Math.max(maxAgreed.intValue(), myAgreedSequence);
                                }
                                Log.d(TAG, "printing queue before ordering...");
                                printQueue(messageQueue);
                                Iterator<Message> it = messageQueue.iterator();
                                while (it.hasNext()) {
                                    Message msg = it.next();
                                    if (msg.getMessageId().equalsIgnoreCase(msgId)) {
                                        Log.d(TAG, "Removing message with msgId: " + msg.getMessageId() + " and updating new sequence to: " + multiCastMessage.getSequencer());
                                        it.remove();
                                        messageQueue.add(multiCastMessage);
                                    }
                                }
                                Log.d(TAG, "printing queue after removing and ordering...");
                                printQueue(messageQueue);
                                checkAndDeliverMessages(messageQueue);
                                break;
                            case CLEAN_UP_MSG:
                                Log.d(TAG, "In cleanup messages....");
                                    if (messageQueue.contains(multiCastMessage)) {
                                        messageQueue.remove(multiCastMessage);
                                        checkAndDeliverMessages(messageQueue);
                                    }
                        }

                    }
                }

            } catch (Exception ex) {
                Log.e(TAG, "Exception in Receiving messages " + ex);

            }
            return null;
        }

        private class CheckAgreedMessage extends TimerTask{
            Message message;
            String msgId;
            CheckAgreedMessage(String msgId, Message message){
                this.message = message;
                this.msgId = msgId;
            }
            @Override
            public void run(){
                if(message.getMessageId().equals(msgId)){
                    if(!message.isAgreementReceived()){
                        Log.d(TAG, "Entered inside timer...");
                        Log.d(TAG, "MsgId: "+msgId);
                        removeFailedClientMessage(message.getOriginalSender());

                    }
                }
                this.cancel();
            }
        }


        private class SendProposalTask extends AsyncTask<Message, Void, Void> {

            @Override
            protected Void doInBackground(Message... msgs) {
                Message proposalMessage = msgs[0];
                Integer myProposed = null;
                synchronized (GroupMessengerActivity.class) {
                    myProposed = (mySequencer > myAgreedSequence ? mySequencer : myAgreedSequence) + 1;
                    mySequencer = myProposed;
                }
                Double myProposedWithPort = Double.parseDouble(myProposed.toString() + "." + myPort);
                Log.d(TAG, "I am proposing: " + myProposedWithPort);
                proposalMessage.setSequencer(myProposedWithPort);
                synchronized (GroupMessengerActivity.class) {
                    messageQueue.add(proposalMessage);
                }
                proposalMessage.setMessageType(PROPOSED_MSG);
                proposalMessage.setFromPort(myPort);
                proposalMessage.setAvd(avdMap.get(myPort));
                String senderPort = proposalMessage.getOriginalSender();
                receivedMessages.put(proposalMessage.getMessageId(), false);
                Socket proposalSocket = null;
                try {
                    proposalSocket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(senderPort));
                    //Timer tracker = new Timer();
                    //tracker.schedule(new CheckAgreedMessage(proposalMessage.getMessageId(), proposalMessage.getOriginalSender()), 2000);
                    proposalSocket.setSoTimeout(500);
                    OutputStream os = proposalSocket.getOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(os);
                    oos.writeObject(proposalMessage);
                    oos.flush();
                    oos.close();
                } catch (SocketTimeoutException ex) {
                    Log.e(TAG, "Sender: " + senderPort + " is dead");
                    //avdTracker.remove(senderPort);
                    removeFailedClientMessage(senderPort);
                } catch (UnknownHostException ex) {
                    Log.e(TAG, "Sender: " + senderPort + " is not up yet");
                    //avdTracker.remove(senderPort);
                    removeFailedClientMessage(senderPort);
                } catch (IOException ex) {
                    Log.e(TAG, "Exception in connecting to sender: " + senderPort);
                    //avdTracker.remove(senderPort);
                    removeFailedClientMessage(senderPort);
                } catch (Exception ex) {
                    Log.e(TAG, "Exception in connecting to sender: " + senderPort);
                    //avdTracker.remove(senderPort);
                    removeFailedClientMessage(senderPort);
                }
                finally{
                    try {
                        proposalSocket.close();
                    } catch (Exception ex) {
                        Log.e(TAG, "Exception in closing socket");
                    }
                }
                return null;
            }
        }

        private class SendCleanUpTask extends AsyncTask<Message, Void, Void> {

            @Override
            protected Void doInBackground(Message... msgs) {
                Message proposalMessage = msgs[0];
                proposalMessage.setMessageType(CLEAN_UP_MSG);
                proposalMessage.setFromPort(myPort);
                proposalMessage.setAvd(avdMap.get(myPort));
                Socket proposalSocket = null;
                try {
                    proposalSocket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(myPort));
                    proposalSocket.setSoTimeout(500);
                    OutputStream os = proposalSocket.getOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(os);
                    oos.writeObject(proposalMessage);
                    oos.flush();
                    oos.close();
                } catch (SocketTimeoutException ex) {
                    Log.e(TAG, "Sender: " + myPort + " is dead");
                    removeFailedClientMessage(myPort);
                } catch (UnknownHostException ex) {
                    Log.e(TAG, "Sender: " + myPort + " is not up yet");
                    removeFailedClientMessage(myPort);
                } catch (IOException ex) {
                    Log.e(TAG, "Exception in connecting to sender: " + myPort);
                    removeFailedClientMessage(myPort);
                } catch (Exception ex) {
                    Log.e(TAG, "Exception in connecting to sender: " + myPort);
                    removeFailedClientMessage(myPort);
                }
                finally{
                    try {
                        proposalSocket.close();
                    } catch (Exception ex) {
                        Log.e(TAG, "Exception in closing socket");
                    }
                }
                return null;
            }
        }


        private class SendAgreedMessageTask extends AsyncTask<Message, Void, Void> {
            @Override
            protected Void doInBackground(Message... msgs) {
                Message agreedMessage = msgs[0];
                String msgId = agreedMessage.getMessageId();
           /* Log.d(TAG, agreedMessage.getAvd() + " proposed " + agreedMessage.getSequencer());
            if (messageTracker.containsKey(msgId)) {
                messageTracker.get(msgId).add(agreedMessage);
                proposalTracker.get(msgId).add(agreedMessage.getFromPort());
                proposalValues.get(msgId).add(agreedMessage.getSequencer());
            }

            if (!checkProposalTracker(proposalTracker.get(msgId), agreedMessage)) {
                Log.d(TAG, "Not received all proposed msgs");
            } else {*/
                Double maxPeerProposed = Collections.max(proposalValues.get(msgId));
                Log.d(TAG, "Max Sequencer: " + maxPeerProposed);
                agreedMessage.setAvd(avdMap.get(myPort));
                agreedMessage.setSequencer(maxPeerProposed);
                agreedMessage.setFromPort(myPort);
                agreedMessage.setMessageType(AGREED_MSG);
                Iterator<Map.Entry<String, String>> it = avdMap.entrySet().iterator();
                while(it.hasNext()) {
                    String peerPort = it.next().getKey();
                    Socket peerSocket = null;
                    try {
                        peerSocket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(peerPort));
                        peerSocket.setSoTimeout(500);
                        OutputStream os = peerSocket.getOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(os);
                        oos.writeObject(agreedMessage);
                        oos.flush();
                        oos.close();
                    } catch (SocketTimeoutException ex) {
                        Log.e(TAG, "Sender: " + peerPort + " is dead");
                        removeFailedClientMessage(peerPort);
                    } catch (UnknownHostException ex) {
                        Log.e(TAG, "Sender: " + peerPort + " is not up yet");
                        removeFailedClientMessage(peerPort);
                    } catch (IOException ex) {
                        Log.e(TAG, "Exception in connecting to sender: " + peerPort);
                        removeFailedClientMessage(peerPort);
                    } catch (Exception ex) {
                        Log.e(TAG, "Exception in connecting to sender: " + peerPort);
                        removeFailedClientMessage(peerPort);
                    }
                }
                return null;
            }
        }

        protected synchronized void createClientTask(Message message, String messageType) {
            switch (messageType) {
                case MESSAGE:
                    new SendProposalTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message);
                    break;
                case PROPOSED_MSG:
                    new SendAgreedMessageTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message);
                    break;
                case CLEAN_UP_MSG:
                    new SendCleanUpTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message);
            }
        }


        private synchronized void removeFailedClientMessage(String failedClientPort) {
            failedPeers.add(failedClientPort);
            PriorityBlockingQueue<Message> dupHoldBackQueue = new PriorityBlockingQueue<Message>(messageQueue);
            for (Message tempMsg; (tempMsg = dupHoldBackQueue.poll()) != null; ) {
                if (tempMsg.getOriginalSender().equals(failedClientPort)) {
                    Log.d(TAG, "removing "+tempMsg.getMessageId());
                    Log.d(TAG, "size before removing " + messageQueue.size());
                    messageQueue.remove(tempMsg);
                    Log.d(TAG, "size after removing " + messageQueue.size());
                    checkAndDeliverMessages(messageQueue);
                }
            }
        }



        private class CheckPeerStatus extends TimerTask{
            String msgId;
            Message message;
            CheckPeerStatus(String msgId, Message message){
                this.msgId = msgId;
                this.message = message;
            }
            @Override
            public void run(){
                if((avdTracker.size()==proposalTracker.get(msgId).size()) || (avdTracker.size()==proposalTracker.get(msgId).size()+1)) {
                    if(msgId.equals(message.getMessageId())){
                        new SendAgreedMessageTask().execute(message);
                    }
                }
                this.cancel();
            }
        }




        private void printQueue(PriorityBlockingQueue<Message> messageQueue) {
            Iterator<Message> it = messageQueue.iterator();
            while (it.hasNext()) {
                Message msg = it.next();
                Log.d(TAG, "Message Id: " + msg.getMessageId() + " Message Sequence: " + msg.getSequencer()+"Message Deliverable: "+msg.isDeliverable());
            }
        }

        private void checkAndDeliverMessages(PriorityBlockingQueue<Message> messageQueue) {
            if (messageQueue.size() > 0) {
                Message msg = messageQueue.peek();
                if (msg.isDeliverable) {
                    if(failedPeers.size()>0) {
                        for (String failedPeer : failedPeers) {
                            if (!msg.getOriginalSender().equals(failedPeer)) {
                                Log.d(TAG, "Message: " + msg.getMessageId() + " is deliverable");
                                Log.d(TAG, "Delivering message...");
                                messageCounter++;
                                deliverMessage(messageQueue.poll(), messageCounter);
                                checkAndDeliverMessages(messageQueue);
                            } else {
                                messageQueue.poll();
                                checkAndDeliverMessages(messageQueue);
                            }
                        }
                    }
                    else{
                        Log.d(TAG, "Message: " + msg.getMessageId() + " is deliverable");
                        Log.d(TAG, "Delivering message...");
                        messageCounter++;
                        deliverMessage(messageQueue.poll(), messageCounter);
                        checkAndDeliverMessages(messageQueue);
                    }
                } else {
                    return;
                }
            }
        }





        private void deliverMessage(Message multiCastMessage, Integer messageCounter){
            String fromAvd = multiCastMessage.getAvd();
            String message = multiCastMessage.getMessage();
            ContentValues values = new ContentValues();
            values.put(TABLE_COL_KEY, messageCounter);
            values.put(TABLE_COL_VAL, message);
            Log.d(TAG, "inserting data into db..");
            Log.d(TAG, "Key : "+messageCounter+" Value: "+message);
            getContentResolver().insert(CONTENT_URI, values);
            publishProgress(new String[]{fromAvd, message});
        }

        protected void onProgressUpdate(String... strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[1];
            String fromAvd = strings[0];
            TextView remoteTextView = (TextView) findViewById(R.id.remote_text_display);
            remoteTextView.append(fromAvd+": \t\n");
            remoteTextView.append(strReceived+"\t\n");
            char[] spaces = new char[remoteTextView.getText().length()];
            Arrays.fill(spaces, ' ');
            TextView localTextView = (TextView) findViewById(R.id.local_text_display);
            localTextView.append(new String(spaces)+"\n");


            /*
             * The following code creates a file in the AVD's internal storage and stores a file.
             *
             * For more information on file I/O on Android, please take a look at
             * http://developer.android.com/training/basics/data-storage/files.html
             */

            String filename = "SimpleMessengerOutput";
            String string = strReceived + "\n";
            FileOutputStream outputStream;

            try {
                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                outputStream.write(string.getBytes());
                outputStream.close();
            } catch (Exception e) {
                Log.e(TAG, "File write failed");
            }

            return;
        }
    }
    }

