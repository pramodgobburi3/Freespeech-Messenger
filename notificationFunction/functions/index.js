'use strict'

const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp(functions.config().firebase);


exports.sendNotification = functions.database.ref('/notifications/{user_id}/{notification_id}').onWrite(event => {
	
	const user_id = event.params.user_id;
	
	const notification_id = event.params.notification_id;
	
	console.log('We have a notification to send to : ', user_id);
	
	if(!event.data.val()) {
		
		return console.log('A notification has been deleted fromt the database : ', notification_id);
		
	}
	

	
	const fromUser = admin.database().ref(`/notifications/${user_id}/${notification_id}`).once('value');
	return fromUser.then(fromUserResult => {
		
		const from_user_id = fromUserResult.val().from;
		console.log('You have a new notification from : ', from_user_id);
		
		const userQuery = admin.database().ref(`Users/${from_user_id}/name`).once('value');
		const deviceToken = admin.database().ref(`/Users/${user_id}/device_token`).once('value');
		return Promise.all([userQuery, deviceToken]).then(result => {
		
				
			const userName = result[0].val();
			const token_id = result[1].val();
			
			const payload = {
				notification: {
					title : "New Friend Request",
					body : `${userName} has sent you a friend request`,
					sound: "default",
					icon: "default",
					click_action : "com.example.pramodgobburi.freespeech_TARGET_NOTIFICATION"
				},
				data : {
					from_user_id: from_user_id
				}
			
			};
		
			return admin.messaging().sendToDevice(token_id, payload).then(response => {
				
				console.log('This was the notification feature');
			
			});
		
		
		});
	
			
	});		
		
		
	
	
	
});


exports.chatNotificatication = functions.database.ref('/messages/{sender_id}/{receiver_id}/{message_id}').onWrite(event => {

	const sender_id = event.params.sender_id;
	const receiver_id = event.params.receiver_id;
	const message_id = event.params.message_id;
	
	if(!event.data.val()) {
		
		return console.log('A message notification has been deleted from the database : ', message_id);
	}
	
	
	if(!event.data.previous.exists()) {
		const chatDetails = admin.database().ref(`/messages/${sender_id}/${receiver_id}/${message_id}`).once('value');
		return chatDetails.then(chatDetailsResult => {
			
			const chat_sender_id = chatDetailsResult.val().from;
			const chat_message = chatDetailsResult.val().message;
			
			if(`${chat_sender_id}` == `${sender_id}`) {
				
					const userQuery = admin.database().ref(`/Users/${chat_sender_id}/name`).once('value');
					return userQuery.then(userResult => {
						
						const userName = userResult.val();
						
						const deviceToken = admin.database().ref(`/Users/${receiver_id}/device_token`).once('value');
				
						return deviceToken.then(result => {
							
							const token_id = result.val();
							
							const payload = {
								notification: {
									title : `New message from ${userName}`,
									body : `${chat_message}`,
									icon: "https://ibb.co/hbS5Hm",
									sound: "default"
								},
							};
							
						
							
							return admin.messaging().sendToDevice(token_id, payload).then(response => {
							
								console.log(`Message ${chat_message} was sent to ${receiver_id}`);
						
							});
							
							
							
						});
					
					
					});
			}
			
			
			
		});
	}
	else{
		return
	}
	
	
	
});
