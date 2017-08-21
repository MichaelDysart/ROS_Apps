package com.michaeldysart.RosTopicEcho;

/**
 * Created by Michael Dysart on 2016-10-27.
 */

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import org.jboss.netty.buffer.ChannelBuffer;
import org.ros.internal.message.Message;
import org.ros.math.Unsigned;
import org.ros.message.Time;
import org.ros.message.Duration;
import org.ros.master.client.MasterStateClient;
import org.ros.master.client.TopicType;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeMain;

import org.ros.node.topic.Subscriber;
import org.ros.message.MessageListener;
import org.ros.node.Node;

import java.math.BigInteger;
import java.net.URI;
import java.util.List;


public class Reader extends TextView implements NodeMain {

    private String topicName;
    private String messageType;
    private ConnectedNode connectedNode;
    private Subscriber<Message> subscriber;
    private boolean paused;

    public Reader(Context context) {
        super(context);
        paused = false;
        topicName = "";
        messageType = "";
    }

    public Reader(Context context, AttributeSet attrs) {
        super(context, attrs);
        paused = false;
    }

    public Reader(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        paused = false;
    }

    private void unsubscribe(){
        if (subscriber != null){
            subscriber.shutdown();
            subscriber = null;
        }
    }

    private void subscribe() {
        if (connectedNode == null) {
            return;
        }
        //Make sure the subscriber is not subscribed to anything
        this.unsubscribe();
        //Try catch block to handle situation where subscriber
        //tries to subscribe to a topic that it lacks the message type for
        try {
            subscriber = connectedNode.newSubscriber(topicName, messageType);
            subscriber.addMessageListener(new MessageListener<Message>() {
                @Override
                public void onNewMessage(final Message message) {

                    post(new Runnable() {
                        @Override
                        public void run() {
                            if (!paused) {
                                setText(parseMessage(message));
                            }
                        }
                    });

                    postInvalidate();
                }
            });
        } catch (Exception e) {
            setText("Error Subscribing to " + topicName + ": " + e);
            this.unsubscribe();
        }
    }

    private void closeNode(){
        unsubscribe();
        connectedNode = null;
    }

    public synchronized void rostopicPause() {
        paused = !paused;

        if(paused){
            unsubscribe();
        } else {
            subscribe();
        }
    }

    public synchronized void rostopicList (URI thisURI) {

        if(this.connectedNode == null){
            setText("Not Connected To Master");
            return;
        }

        MasterStateClient client = new MasterStateClient(connectedNode, thisURI);

        this.unsubscribe();
        String message = "";
        for(TopicType topic : client.getTopicTypes()) {
            message += topic.getName() + " : " + topic.getMessageType() + "\n";
        }

        setText(message);
        postInvalidate();
    }

    public String parseMessage (final Message message) {

        String parsedMessage = message.toRawMessage().getName()+"\n\n";

        for (org.ros.internal.message.field.Field field : message.toRawMessage().getFields()) {
            parsedMessage += field.getName() + " : ";

            //Primitive types are returned as arrays
            //objects are returned as ArrayLists
            //byte arrays are returned as ChannelBuffers
            if (!field.getValue().getClass().isArray() && !(field.getValue() instanceof List)
                    && !(field.getValue() instanceof ChannelBuffer)){

                switch(field.getType().getName()){
                    case "bool": case "int8": case "int16": case "int32": case "int64": case "float32":
                    case "float64": case "string": case "byte": case "char":
                                parsedMessage += field.getValue().toString();
                                break;
                    case "uint8": parsedMessage += Unsigned.byteToShort(message.toRawMessage().getInt8(field.getName()));
                                break;
                    case "uint16": parsedMessage += Unsigned.shortToInt(message.toRawMessage().getInt16(field.getName()));
                                break;
                    case "uint32": parsedMessage += Unsigned.intToLong(message.toRawMessage().getInt32(field.getName()));
                                break;
                    case "uint64":
                                if (message.toRawMessage().getInt64(field.getName()) >= 0) {
                                    parsedMessage += message.toRawMessage().getInt64(field.getName());
                                } else {
                                    BigInteger toUnsigned = new BigInteger(Long.toString(message.toRawMessage().getInt64(field.getName())));
                                    parsedMessage += toUnsigned.add(BigInteger.ONE.shiftLeft(64)).toString();
                                }
                                break;
                    case "time":
                                parsedMessage += "\n\tsecs: "+message.toRawMessage().getTime(field.getName()).secs;
                                parsedMessage += "\n\tnsecs: "+message.toRawMessage().getTime(field.getName()).nsecs;
                                break;
                    case "duration":
                                parsedMessage += "\n\tsecs: "+message.toRawMessage().getDuration(field.getName()).secs;
                                parsedMessage += "\n\tnsecs: "+message.toRawMessage().getDuration(field.getName()).nsecs;
                                break;
                    default:
                        parsedMessage += "\n" + parseMessage(message.toRawMessage().getMessage(field.getName()));
                        break;

                }

            }
            else {

                //A buffer for handling byte arrays
                ChannelBuffer buffer;

                parsedMessage += "[";
                switch(field.getType().getName()){
                    case "bool":
                                for (boolean val : message.toRawMessage().getBoolArray(field.getName())) {
                                    parsedMessage += val + ", ";
                                }
                                break;
                    case "int8": case "byte":
                                buffer = message.toRawMessage().getChannelBuffer(field.getName());
                                for (int i = 0; i < buffer.capacity(); i++) {
                                    byte val = buffer.getByte(i);
                                    parsedMessage += val + ", ";
                                }
                                break;
                    case "uint8":
                                buffer = message.toRawMessage().getChannelBuffer(field.getName());
                                for (int i = 0; i < buffer.capacity(); i++) {
                                    byte val = buffer.getByte(i);
                                    parsedMessage += Unsigned.byteToShort(val) + ", ";
                                }
                                break;
                    case "int16":
                                for (short val : message.toRawMessage().getInt16Array(field.getName())) {
                                    parsedMessage += val + ", ";
                                }
                                break;
                    case "int32":
                                for (int val : message.toRawMessage().getInt32Array(field.getName())) {
                                   parsedMessage += val + ", ";
                                }
                                break;
                    case "int64":
                                for (long val : message.toRawMessage().getInt64Array(field.getName())) {
                                    parsedMessage += val + ", ";
                                }
                                break;
                    case "float32":
                                for (float val : message.toRawMessage().getFloat32Array(field.getName())) {
                                    parsedMessage += val + ", ";
                                }
                                break;
                    case "float64":
                                for (double val : message.toRawMessage().getFloat64Array(field.getName())) {
                                    parsedMessage += val + ", ";
                                }
                                break;
                    case "uint16":
                                for (short val : message.toRawMessage().getInt16Array(field.getName())) {
                                    parsedMessage += Unsigned.shortToInt(val) + ", ";
                                }
                                break;
                    case "uint32":
                                for (int val : message.toRawMessage().getInt32Array(field.getName())) {
                                    parsedMessage += Unsigned.intToLong(val) + ", ";
                                }
                                break;
                    case "uint64":
                                 for (long val : message.toRawMessage().getInt64Array(field.getName())) {
                                    if (val >= 0) {
                                        parsedMessage += val;
                                    } else {
                                        BigInteger toUnsigned = new BigInteger(Long.toString(val));
                                        parsedMessage += toUnsigned.add(BigInteger.ONE.shiftLeft(64)).toString();
                                    }
                                    parsedMessage += ", ";
                                }
                                break;
                    case "string":
                                for (String val : message.toRawMessage().getStringList(field.getName())) {
                                    parsedMessage += val + ", ";
                                }
                                break;
                    case "time":
                                for (Time val : message.toRawMessage().getTimeList(field.getName())) {
                                    parsedMessage += "\n\tsecs: "+val.secs + "\n\tnsecs: "+val.nsecs+", ";
                                }
                                break;
                    case "duration":
                                for (Duration val : message.toRawMessage().getDurationList(field.getName())) {
                                    parsedMessage += "\n\tsecs: "+val.secs + "\n\tnsecs: "+val.nsecs+", ";
                                }
                                break;
                    case "char":
                                for (short val : message.toRawMessage().getCharArray(field.getName())) {
                                    parsedMessage += val + ", ";
                                }
                                break;
                    default:
                        for (Message listMessage : message.toRawMessage().getMessageList(field.getName())) {
                            parsedMessage += "\n"+parseMessage(listMessage) +", ";
                        }
                        break;

                }

                if(!parsedMessage.endsWith("[")) {
                    parsedMessage = parsedMessage.substring(0, parsedMessage.length() - 2) + "]";
                } else {
                    parsedMessage += "]";
                }
            }

            parsedMessage += "\n";
        }

        return parsedMessage;
    }

    public synchronized void rostopicEcho (String topicName, URI thisURI) {

        if (this.connectedNode == null) {
            setText("Not Connected To Master");
            return;
        }

        setText("");
        postInvalidate();

        MasterStateClient client = new MasterStateClient(connectedNode, thisURI);

        String messageType = "";

        for (TopicType topic : client.getTopicTypes()) {
            if (topicName.equals(topic.getName())) {
                messageType = topic.getMessageType();
            }
        }

        if (messageType.equals("")) {
            setText("This topic does not exist");
            return;
        }

        this.topicName = topicName;
        this.messageType = messageType;

        subscribe();
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("rostopic/echo");
    }

    @Override
    public synchronized void onStart(final ConnectedNode connectedNode) {
        this.connectedNode = connectedNode;
    }

    @Override
    public synchronized void onShutdown(Node node) {closeNode();}

    @Override
    public synchronized void onShutdownComplete(Node node) {}

    @Override
    public synchronized void onError(Node node, Throwable throwable) {closeNode();}
}