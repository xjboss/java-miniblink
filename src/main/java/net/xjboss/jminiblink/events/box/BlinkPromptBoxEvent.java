package net.xjboss.jminiblink.events.box;

import lombok.Getter;
import lombok.ToString;
import net.xjboss.jminiblink.webview.BlinkView;

@Getter
@ToString(callSuper = true)
public class BlinkPromptBoxEvent extends BlinkBoxEvent {
    private String defaultResult;
    private String result;

    public BlinkPromptBoxEvent(BlinkView view, String msg, String defaultResult, String result) {
        super(view, msg);
        this.defaultResult = defaultResult;
        this.result = result;
    }
}
