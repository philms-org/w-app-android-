
package com.vastlb.wing_me.Classes

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.media.RingtoneManager
import android.net.Uri
import com.vastlb.wing_me.Groups.GroupChatActivity
import com.vastlb.wing_me.Launch.LaunchActivity
import com.vastlb.wing_me.R
import com.vastlb.wing_me.Settings.NotificationsActivity
import com.vastlb.wing_me.User.ChatActivity

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class MessagingService: FirebaseMessagingService() {

    val CHANNEL_ID = "100"

    @SuppressLint("UnspecifiedImmutableFlag")
    override fun onMessageReceived(p0: RemoteMessage) {
        super.onMessageReceived(p0)
        val clickAction = getData(p0, "click_action")

        if (clickAction == "open_inbox") {
            val user_Id = getData(p0, "user_Id")
            val message_Id = getData(p0, "message_Id")
            val message = getData(p0, "message")
            val date = getData(p0, "date")

            if (Constants.setChatMessage != null && Constants.chatUserID == user_Id) {
                Constants.setChatMessage!!(message_Id, message, date)
                return
            } else if (Constants.setLastMessage != null) {
                Constants.setLastMessage!!(user_Id, message, date, true)
            }
        } else if (clickAction == "open_group") {
            val comment_Id = getData(p0, "comment_Id")
            val message_Id = getData(p0, "message_Id")
            val user_name = getData(p0, "user_name")
            val gender = getData(p0, "gender")
            val message = getData(p0, "message")
            val date = getData(p0, "date")

            if (Constants.setGroupMessage != null && Constants.chatGroupID == comment_Id) {
                Constants.setGroupMessage!!(user_name, gender, message_Id, message, date)
                return
            } else if (Constants.setLastGroup != null) {
                Constants.setLastGroup!!(comment_Id, message, date, true)
            }
        }
        val intent = getIntent(clickAction, p0)

        val pendingIntent: PendingIntent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        } else {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        }
        val soundURI: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(p0.notification!!.title)
            .setContentText(p0.notification!!.body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSound(soundURI)
            .setContentIntent(pendingIntent)

        createNotificationChannel()

        with(NotificationManagerCompat.from(this)) {
            notify(10, builder.build())
        }
    }

    fun getData(p0: RemoteMessage, key: String): String {
        val data = p0.data

        if (data.get(key) != null) {
            return data.get(key)!!
        }
        return ""
    }

    fun getIntent(clickAction: String?, p0: RemoteMessage): Intent {
        if (clickAction == "open_message") {
            val user_Id = getData(p0, "user_Id")
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("ID", user_Id)
            return intent
        } else if (clickAction == "open_group") {
            val comment_Id = getData(p0, "comment_Id")
            val intent = Intent(this, GroupChatActivity::class.java)
            intent.putExtra("ID", comment_Id)
            return intent
        } else if (clickAction == "open_notification") {
            val intent = Intent(this, NotificationsActivity::class.java)
            return intent
        } else {
            val intent = Intent(this, LaunchActivity::class.java)
            return intent
        }
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT

            val channel = NotificationChannel(CHANNEL_ID, "Name", importance).apply {
                description = "Description"
            }
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}