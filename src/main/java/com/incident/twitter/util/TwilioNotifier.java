package com.incident.twitter.util;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

public class TwilioNotifier {
    public static final String accountSID = "AC53085720d44cf04e1bc39e7f3de0846b";
    public static final String accountToken = "909c194cda55f4c12fca8d47aed1898e";

    public static void notify(String message){
        Twilio.init(accountSID, accountToken);
        Message.creator(new PhoneNumber("+96171702791"),
                new PhoneNumber("+13522680255"), message).create();
        Message.creator(new PhoneNumber("+9613295847"),
                new PhoneNumber("+13522680255"), message).create();
    }
}
