package net.xjboss.jminiblink;

import com.sun.jna.*;
import com.sun.jna.platform.EnumConverter;
import lombok.Getter;
import lombok.val;
import net.xjboss.jminiblink.events.Listener;
import net.xjboss.jminiblink.js.JsObjBuilder;
import net.xjboss.jminiblink.natives.NativeMiniBlink;
import net.xjboss.jminiblink.natives.NativeWinUtil;
import net.xjboss.jminiblink.natives.enums.wkeProxyType;
import net.xjboss.jminiblink.natives.enums.wkeWindowType;
import net.xjboss.jminiblink.natives.struct.wkeProxy;
import net.xjboss.jminiblink.natives.struct.wkeSettings;
import net.xjboss.jminiblink.objects.AObj;
import net.xjboss.jminiblink.webview.BlinkView;
import net.xjboss.jminiblink.webview.BlinkViewWindow;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class BlinkBrowser {
    @Getter
    private static BlinkBrowser instance;
    private final static AtomicInteger thread_num=new AtomicInteger();
    private NativeMiniBlink mNative;
    private NativeWinUtil mWinUtil;
    @Getter
    private static final String version="1.0";

    private Thread thread_blink=new Thread(()-> start());

    private String native_path;
    @Getter
    private boolean enabled=true;
    private LinkedBlockingQueue<BlinkTask> tasks=new LinkedBlockingQueue <>();
    @Getter
    private BlinkListenerManager listenerManager=new BlinkListenerManager();
    private final JsObjBuilder fJsb=new JsObjBuilder(mNative,this);

    public JsObjBuilder buildJS(){
        return fJsb;
    }

    BlinkBrowser(BlinkSetting setting){
        boolean notNull=setting!=null;
        native_path=notNull?setting.dll_path+File.separator:"libs"+File.separator;
        thread_blink.setName("Java MiniBlink Browser Library "+thread_num.incrementAndGet());
        thread_blink.start();
    }
    private void addTypes(){
        DefaultTypeMapper map=new DefaultTypeMapper();
        map.addTypeConverter(wkeWindowType.class,new EnumConverter<wkeWindowType>(wkeWindowType.class));
        map.addTypeConverter(wkeProxyType.class,new EnumConverter<wkeProxyType>(wkeProxyType.class));

        mNative=Native.loadLibrary(native_path+"miniblink"+(System.getProperty("os.arch").equalsIgnoreCase("AMD64")?"64":""),NativeMiniBlink.class,Collections.singletonMap(Library.OPTION_TYPE_MAPPER,map));

    }
    private void start(){
        addTypes();
        mWinUtil=Native.loadLibrary(native_path+"win"+(System.getProperty("os.arch").equalsIgnoreCase("AMD64")?"64":"32")+"utils",NativeWinUtil.class);
        mNative.wkeInit();
        while (enabled&&mWinUtil.canLoop()){
            mWinUtil.handleWindow();
            while (!tasks.isEmpty()){
                try {
                    tasks.take().start();
                }catch (InterruptedException e){
                    e.printStackTrace();
                }catch (RuntimeException e){
                    e.printStackTrace();
                }
            }
        }
    }
    public boolean checkThread(){
        return Thread.currentThread()==thread_blink;
    }
    public void autoRunTask(Runnable task){
        if(checkThread()){
            task.run();
        }else {
            runTask(task).join();
        }
    }
    public BlinkView createView(){
        val view=new AObj<BlinkView>();
        autoRunTask(()->{
            val webView=mNative.wkeCreateWebView();
            val bv=new BlinkView(mNative, webView, this);
            view.setObj(bv);
        });
        return view.getObj();
    }

    public BlinkViewWindow createWindow(wkeWindowType type, int x, int y, int width, int height){
        val window=new AObj<BlinkViewWindow>();
        autoRunTask(()->{
            val webView=mNative.wkeCreateWebWindow(type.ordinal(),Pointer.NULL,x,y,width,height);
            val bv= new BlinkViewWindow(mNative,webView,this);
            window.setObj(bv);
        });
        return window.getObj();
    }

    public String miniBlinkVer(){
        val obj=new AObj<String>();
        autoRunTask(()->obj.setObj(mNative.wkeVersionString()));
        return obj.getObj();
    }

    public void setProxy(wkeProxy proxy){
        autoRunTask(()->mNative.wkeSetProxy(proxy));
    }

    public void configure(wkeSettings settings){
        autoRunTask(()->mNative.wkeConfigure(settings));
    }

    public BlinkTask runTask(Runnable run){
        val task=new BlinkTask(thread_blink,run);
        tasks.add(task);
        return task;
    }
    public void shutdown(){
        autoRunTask(()->{
            enabled = false;
            mNative.wkeShutdown();
            instance=null;
        });
    }
    public <A extends Listener> void registerListener(Class<A> tClass){
        autoRunTask(()->{
            listenerManager.registerListener(tClass);
        });
    }

    public <A extends Listener> void registerListener(A tObject){
        autoRunTask(()->{
            listenerManager.registerListener(tObject);
        });
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if(enabled) {
            shutdown();
        }
    }
}
