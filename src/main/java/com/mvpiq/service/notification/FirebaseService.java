package com.mvpiq.service.notification;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import com.mvpiq.model.DeviceToken;
import com.mvpiq.repositories.DeviceTokenRepository;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
@Slf4j
public class FirebaseService {

    @Inject
    DeviceTokenRepository deviceTokenRepository;

    private boolean initialized = false;

    public void initializeFirebase(String firebaseConfigJson) {
        if (!initialized) {
            try {
                GoogleCredentials credentials = GoogleCredentials.fromStream(
                        new ByteArrayInputStream(firebaseConfigJson.getBytes(StandardCharsets.UTF_8)));
                
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .build();

                FirebaseApp.initializeApp(options);
                initialized = true;
                log.info("Firebase initialized successfully");
            } catch (IOException e) {
                log.error("Failed to initialize Firebase", e);
                throw new RuntimeException("Firebase initialization failed", e);
            }
        }
    }

    public void sendNotification(UUID userId, String title, String message, Map<String, String> data) {
        try {
            List<DeviceToken> tokens = deviceTokenRepository.findByUserId(userId);
            
            if (tokens.isEmpty()) {
                log.warn("No device tokens found for user: {}", userId);
                return;
            }

            List<String> activeTokens = tokens.stream()
                    .map(DeviceToken::getToken)
                    .collect(Collectors.toList());

            // Build notification message
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(message)
                    .build();

            // Build message
            MulticastMessage.Builder messageBuilder = MulticastMessage.builder()
                    .setNotification(notification)
                    .addAllTokens(activeTokens);

            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            MulticastMessage multicastMessage = messageBuilder.build();

            // Send message
            BatchResponse response = FirebaseMessaging.getInstance().sendMulticast(multicastMessage);

            log.info("Notification sent to user: {}, success: {}, failure: {}", 
                    userId, response.getSuccessCount(), response.getFailureCount());

            // Handle failed tokens
            if (response.getFailureCount() > 0) {
                handleFailedTokens(response, tokens);
            }

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send notification to user: " + userId, e);
        }
    }

    public void sendNotificationToToken(String token, String title, String message, Map<String, String> data) {
        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(message)
                    .build();

            Message.Builder messageBuilder = Message.builder()
                    .setNotification(notification)
                    .setToken(token);

            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            Message firebaseMessage = messageBuilder.build();

            String response = FirebaseMessaging.getInstance().send(firebaseMessage);
            log.info("Notification sent to token: {}, response: {}", token, response);

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send notification to token: " + token, e);
            
            // If token is invalid, deactivate it
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED ||
                e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                deviceTokenRepository.deactivateByToken(token);
                log.info("Deactivated invalid token: {}", token);
            }
        }
    }

    public void sendSilentNotification(UUID userId, Map<String, String> data) {
        try {
            List<DeviceToken> tokens = deviceTokenRepository.findByUserId(userId);
            
            if (tokens.isEmpty()) {
                log.warn("No device tokens found for user: {}", userId);
                return;
            }

            List<String> activeTokens = tokens.stream()
                    .map(DeviceToken::getToken)
                    .collect(Collectors.toList());

            // Build silent message (no notification payload)
            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(activeTokens)
                    .putAllData(data)
                    .build();

            BatchResponse response = FirebaseMessaging.getInstance().sendMulticast(message);

            log.info("Silent notification sent to user: {}, success: {}, failure: {}", 
                    userId, response.getSuccessCount(), response.getFailureCount());

            if (response.getFailureCount() > 0) {
                handleFailedTokens(response, tokens);
            }

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send silent notification to user: " + userId, e);
        }
    }

    private void handleFailedTokens(BatchResponse response, List<DeviceToken> tokens) {
        List<SendResponse> responses = response.getResponses();
        
        for (int i = 0; i < responses.size(); i++) {
            Exception exception = responses.get(i).getException();
            
            if (exception != null && exception instanceof FirebaseMessagingException) {
                FirebaseMessagingException fcmException = (FirebaseMessagingException) exception;
                MessagingErrorCode errorCode = fcmException.getMessagingErrorCode();
                
                if (errorCode == MessagingErrorCode.UNREGISTERED ||
                    errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                    
                    DeviceToken token = tokens.get(i);
                    deviceTokenRepository.deactivateByToken(token.getToken());
                    log.info("Deactivated invalid token: {}", token.getToken());
                }
            }
        }
    }

    public boolean isInitialized() {
        return initialized;
    }
}
