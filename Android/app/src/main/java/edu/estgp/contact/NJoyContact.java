package edu.estgp.contact;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by vrealinho on 31/03/18.
 */

public class NJoyContact {
    private static ArrayList<Contact> listContact = null;

    public static ArrayList<Contact> getContactList(Activity activity) {
        listContact = new ArrayList<Contact>();

        Cursor cursor = activity.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,null, null, null);

        listContact.clear();
        while (cursor.moveToNext()) {
            String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            Contact contact = new Contact();
            contact.setName(name);
            contact.setPhone(phoneNumber);
            listContact.add(contact);
        }

        if (cursor!=null) {
            cursor.close();
        }

        return listContact;
    }

    public static String findContactByPhone(Activity activity, String phone) {
        if (listContact == null) {
            getContactList(activity);
        }

        for (Contact contact: listContact) {
            if (phone.contains(contact.getPhone())) {
                return contact.getName();
            }
        }

        return phone;
    }
}
