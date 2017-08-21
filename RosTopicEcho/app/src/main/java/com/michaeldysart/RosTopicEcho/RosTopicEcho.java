package com.michaeldysart.RosTopicEcho;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.common.base.Preconditions;

import org.ros.android.NodeMainExecutorService;
import org.ros.android.RosActivity;
import org.ros.exception.RosRuntimeException;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;


import java.lang.String;
import java.net.URI;
import java.net.URISyntaxException;

public class RosTopicEcho extends RosActivity {

    private Reader reader;

    public RosTopicEcho() {
        super("Rostopic Echo", "Rostopic Echo");
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_echo);
        reader = (Reader) findViewById(R.id.text);

        //Check permissions (necessary to allow shutdown)
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, 101);
    }

    @Override
    protected void onPause(){
        shutdownNode();
        super.onPause();
    }

    @Override
    protected void onStop(){
        shutdownNode();
        super.onStop();
    }

    private void shutdownNode(){
        if (nodeMainExecutorService != null) {
            nodeMainExecutorService.shutdownNodeMain(reader);
            nodeMainExecutorService.setMasterUri(null);
        }
    }

    public void topicList (View view) {
        reader.rostopicList(getMasterUri());
    }

    public void topicEcho (View view) {
        EditText editText = (EditText) findViewById(R.id.edit_message);
        String topicName = editText.getText().toString();
        reader.rostopicEcho(topicName, getMasterUri());
    }

    public void topicPause(View view) {
        Button button = (Button) view;
        if (button.getText().equals(getResources().getString(R.string.button_pause))){
            button.setText(getResources().getString(R.string.button_resume));
        } else {
            button.setText(getResources().getString(R.string.button_pause));
        }
        reader.rostopicPause();
    }

    public void connectToMaster(View view) {
        shutdownNode();
        startMasterChooser();
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(getRosHostname());
        nodeConfiguration.setMasterUri(getMasterUri());
        nodeMainExecutor.execute(reader, nodeConfiguration);
    }
}
