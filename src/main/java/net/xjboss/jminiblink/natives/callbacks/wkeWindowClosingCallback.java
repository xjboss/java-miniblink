package net.xjboss.jminiblink.natives.callbacks;

import com.sun.jna.Pointer;

import com.sun.jna.Callback;

public interface wkeWindowClosingCallback extends Callback {
    boolean invoke(Pointer webView, Pointer param);
}
