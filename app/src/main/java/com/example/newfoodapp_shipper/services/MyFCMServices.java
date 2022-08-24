package com.example.newfoodapp_shipper.services;

import com.example.newfoodapp_shipper.Common.Common;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;
import java.util.Random;

import androidx.annotation.NonNull;

public class MyFCMServices extends FirebaseMessagingService {




    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {

        Map<String,String> dataRecv = remoteMessage.getData();
        if (dataRecv!=null)
        {
            Common.showNotification(this,new Random().nextInt(),
                    dataRecv.get(Common.NOTI_TITLE),
                    dataRecv.get(Common.NOTI_CONTENT),
                    null);


        }




    }

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        Common.updateToken(this,s,false,true);  // Right now we are on Shipper App so we put isShipper=true


    }







}
