package net.xjboss.jminiblink.natives.callbacks;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;

public interface wkeNavigationCallback extends Callback {
    boolean invoke(Pointer webView, Pointer param, int navigationType, Pointer url);
}
