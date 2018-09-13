'use strict'

const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

exports.sendNotification = functions
        .database
        .ref('/notifications/{user_id}/{notification_id}')
        .onWrite((data, context) => {

            const user_id = context.params.user_id;
            const notification_id = context.params.notification_id;

            console.log('We have a notification to sent to: ', user_id);

            if(!data.after.val())
            {
                return console.log('A notification was deleted: ', notification_id);
            }
            else
            {
                var fromUser = admin.database().ref(`/notifications/${user_id}/${notification_id}`).once('value');

                return fromUser.then(fromUserResult => {

                    const from_user_id = fromUserResult.val().from;
                    const notification_type = fromUserResult.val().type;
                    const message_content = fromUserResult.val().content

                    console.log('You Have A New Notification From: ', from_user_id);
                    console.log('Notification type: ', notification_type);
                    console.log('Message content is: ', message_content);

                    var userQuery = admin.database().ref(`Users/${from_user_id}/name`).once('value');
                    var device_token = admin.database().ref(`/Users/${user_id}/device_token`).once('value');
    
                    return Promise.all([userQuery, device_token]).then(result =>{

                        var userName = result[0].val();
                        var token_id = result[1].val();

                        var payload;
                        
                        switch(notification_type)
                        {
                            case "message":
                            payload = {
                                notification: {
                                    title: "New Message",
                                    body:  `Aloha! ${userName} Sent you a message: ${message_content}`,
                                    icon:  "default",
                                    sound: "default",
                                    click_action: "com.example.leeronziv.alohaworld_chatvideo_MESSAGE_NOTIFICATION"
                                },
                                data: {
                                    sender_id : from_user_id
                                    }
                                };
                                break;

                            case "friend_request":
                                payload = {
                                    notification: {
                                        title: "Friend Request",
                                        body:  `Aloha! ${userName} Wants To Be Your Friend`,
                                        icon:  "default",
                                        sound: "default",
                                        click_action: "com.example.leeronziv.alohaworld_chatvideo_FRIENDREQUEST_NOTIFICATION"
                                    },
                                    data: {
                                        sender_id : from_user_id
                                        }
                                    };
                                    break;

                            case "video_call":
                                payload = {
                                    notification: {
                                        title: "Video Call",
                                        body:  `Aloha! ${userName} Wants To Video Chat With You`,
                                        icon:  "default",
                                        sound: "default",
                                        click_action: "com.example.leeronziv.alohaworld_chatvideo_VIDEO_NOTIFICATION"
                                    },
                                    data: {
                                        sender_id : from_user_id
                                        }
                                    };
                                    break;
                        }
                    
                        return admin.messaging().sendToDevice(token_id, payload).then(response =>{
                
                            return console.log('This was the notification feature');
                
                        });
                    });
                });
            }
        });