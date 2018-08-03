package edu.estgp.sms;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.telephony.SmsManager;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

import edu.estgp.njoy.TTS;
import edu.estgp.njoy.njoy.R;

/**
 * Created by vrealinho on 30/03/18.
 */

public class NJoySMS {

    private static List<SMS> listSMS = new ArrayList<SMS>();

    /**
     * Returns a list of SMS messages based on selection
     * @param activity      Main activity
     * @param selection     The selection clause
     * @return  List<SMS>
     */
    private static List<SMS> getSMSMessages(Activity activity, String selection) {
        listSMS = new ArrayList<SMS>();

        Uri message = Uri.parse("content://sms/inbox");
        ContentResolver contentResolver = activity.getContentResolver();

        Cursor cursor = contentResolver.query(message, null, selection, null, null);
        activity.startManagingCursor(cursor);
        int totalSMS = cursor.getCount();

        if (cursor.moveToFirst()) {
            for (int i = 0; i < totalSMS; i++) {
                SMS sms = new SMS();
                sms.setId(cursor.getString(cursor.getColumnIndexOrThrow("_id")));
                sms.setAddress(cursor.getString(cursor.getColumnIndexOrThrow("address")));
                sms.setMsg(cursor.getString(cursor.getColumnIndexOrThrow("body")));
                sms.setReadState(cursor.getString(cursor.getColumnIndex("read")));
                sms.setTime(cursor.getString(cursor.getColumnIndexOrThrow("date")));
                if (cursor.getString(cursor.getColumnIndexOrThrow("type")).contains("1")) {
                    sms.setFolderName("inbox");
                }
                else {
                    sms.setFolderName("sent");
                }

                listSMS.add(sms);
                cursor.moveToNext();
            }
        }
        else {
            Log.d("NJOY", activity.getString(R.string.no_sms));
        }
        cursor.close();

        return listSMS;
    }

    /**
     * Returns the number of unread SMS messages
     * @param activity  Main activity
     * @return          The number of unread SMS messages
     */
    public static int getMessageCountUnread(Activity activity) {

        Uri message = Uri.parse("content://sms/inbox");
        ContentResolver contentResolver = activity.getContentResolver();
        Cursor c = contentResolver.query(message, null, "read = 0", null, null);
        int unreadMessagesCount = c.getCount();
        c.deactivate();
        return unreadMessagesCount;
    }

    /**
     * Return all SMS messages
     * @param activity  Main activity
     * @return          List<SMS>
     */
    public static List<SMS> getAllMessages(Activity activity) {
        return getSMSMessages(activity, null);
    }

    /**
     * Returns all unread SMS messages
     * @param activity  Main activity
     * @return          List<SMS>
     */
    public static List<SMS> getUnreadMessages(Activity activity) {
        return getSMSMessages(activity, "read=0");
    }

    /**
     * Send an SMS message
     * @param activity  Main activity
     * @param phoneNo   Destination
     * @param msg       The message
     * @return          true if message was sent; false otherwise
     */
    public static boolean sendSMS(Activity activity, String phoneNo, String msg) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNo, null, msg, null, null);
            TTS.speak(activity, activity.getString(R.string.sms_sent));
            return true;
        }
        catch (Exception ex) {
            ex.printStackTrace();
            TTS.speak(activity, activity.getString(R.string.sms_sent_error));
            return false;
        }
    }

    /**
     * Mark a message as read given the id
     * @param activity  Main activity
     * @param id        SMS id
     */
    public static int markMessageReadById(Activity activity, int id) {
        ContentResolver contentResolver = activity.getContentResolver();
        ContentValues values = new ContentValues();
        values.put("read", true);
        int rowsAffected = contentResolver.update(Uri.parse("content://sms/inbox"), values, "_id=" + id, null);
        Log.d("NJOY", "setReadById id: " + id + ", rowsAffected : " + rowsAffected);

        return rowsAffected;
    }

    /**
     * Mark a message as read given the position
     * @param activity  Main activity
     * @param position  SMS position
     */
    public static void markMessageReadByPosition(Activity activity, int position) {
        markMessageReadById(activity, Integer.parseInt(listSMS.get(position).getId()));
    }
}
