package com.product.RealestateTokenization.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    public void sendToAllUsers(String title, String body) throws Exception {

        Message message = Message.builder()
                .setTopic("all_users")
                .setNotification(
                        Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build()
                )
                .build();

        FirebaseMessaging.getInstance().send(message);
    }

    public void sendImageNotification(String title, String body, String imageUrl) throws Exception {
        Message message = Message.builder()
                .setTopic("all_users")
                .setNotification(
                        Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .setImage(imageUrl)
                                .build()
                )
                .build();

        FirebaseMessaging.getInstance().send(message);
    }

}
