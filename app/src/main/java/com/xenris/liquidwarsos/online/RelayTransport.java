// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.online;

import com.xenris.liquidwarsos.net.GameTransport;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import okhttp3.*;
import okio.ByteString;
import org.json.JSONObject;

/** Bounded byte streams preserve the legacy GameWire protocol over authenticated WSS. */
public final class RelayTransport implements Closeable {
    private final PlatformApi api;
    private final String code;
    private final boolean host;
    private final Map<Integer,Pipe> streams=new ConcurrentHashMap<>();
    private final BlockingQueue<Pipe> arrivals=new LinkedBlockingQueue<>(6);
    private volatile WebSocket socket;
    private volatile boolean closed;
    private volatile boolean accepting=true;
    private final CountDownLatch connected=new CountDownLatch(1);
    private volatile IOException failure;
    private final ScheduledExecutorService ticker=Executors.newSingleThreadScheduledExecutor();
    private static final Pipe END=new Pipe(null,-1);
    public RelayTransport(PlatformApi api,String code,boolean host){this.api=api;this.code=code;this.host=host;}
    public synchronized void open() throws IOException {
        if(closed)throw new IOException("连接已关闭");
        if(socket!=null)return;
        try{
            String ticket=api.call("POST","/online/rooms/"+code+"/ticket",null).getString("ticket");
            Request request=new Request.Builder().url(PlatformApi.ORIGIN.replace("https://","wss://")+"/ws/game?ticket="+ticket).header("User-Agent","Haoke-LiquidWars/1.84 (Android)").build();
            socket=PlatformApi.HTTP.newWebSocket(request,new WebSocketListener(){
                @Override public void onMessage(WebSocket ws,String text){
                    try{
                        JSONObject event=new JSONObject(text);String type=event.optString("type");int slot=event.optInt("slot",-1);
                        if(type.equals("connected")){
                            if(!host)streams.put(0,new Pipe(RelayTransport.this,0));connected.countDown();
                        }else if(host&&type.equals("join")&&slot>=1&&slot<=5){
                            if(!accepting){ws.send("{\"type\":\"close\",\"slot\":"+slot+"}");return;}
                            Pipe pipe=new Pipe(RelayTransport.this,slot);Pipe old=streams.putIfAbsent(slot,pipe);
                            if(old!=null||!arrivals.offer(pipe))close();
                        }else if(host&&type.equals("left")){
                            Pipe pipe=streams.remove(slot);if(pipe!=null)pipe.end();
                        }
                    }catch(Exception invalid){fail(new IOException("联网数据无效"));}
                }
                @Override public void onMessage(WebSocket ws,ByteString bytes){
                    byte[] data=bytes.toByteArray();int slot=host?(data.length>0?data[0]&255:-1):0;
                    Pipe pipe=streams.get(slot);if(pipe!=null&&!pipe.offer(host?Arrays.copyOfRange(data,1,data.length):data))fail(new IOException("网络积压，请重新加入"));
                }
                @Override public void onFailure(WebSocket ws,Throwable t,Response response){fail(new IOException("连接中断，请回大厅重试"));}
                @Override public void onClosing(WebSocket ws,int code,String reason){ws.close(code,null);fail(new IOException("房主离开或房间已关闭"));}
                @Override public void onClosed(WebSocket ws,int code,String reason){fail(new IOException("房间已关闭"));}
            });
            ticker.scheduleAtFixedRate(()->{WebSocket s=socket;if(s!=null&&!closed)s.send("{\"type\":\"ping\"}");},10,10,TimeUnit.SECONDS);
        }catch(Exception e){fail(new IOException("无法连接游戏服务，请稍后重试",e));throw failure;}
    }
    private void awaitOpen() throws IOException {
        try{if(!connected.await(18,TimeUnit.SECONDS))throw new IOException("等待房主连接超时");}
        catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException("已取消连接");}
        if(failure!=null)throw failure;
        if(closed)throw new IOException("连接已关闭");
    }
    public GameTransport.ListenerFactory listener(){return ()->{open();awaitOpen();arrivals.remove(END);accepting=true;return new GameTransport.Listener(){
        private volatile boolean stopped;
        @Override public GameTransport.Connection accept() throws IOException {
            try{Pipe p=arrivals.take();if(p==END||stopped||closed)throw new IOException("停止接收玩家");return p;}
            catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException(e);}
        }
        @Override public void close(){stopped=true;accepting=false;arrivals.offer(END);}
    };};}
    public GameTransport.Connector connector(){return new GameTransport.Connector(){
        @Override public GameTransport.Connection connect() throws IOException {open();awaitOpen();return streams.get(0);}
        @Override public long timeoutMillis(){return 25000;}
        @Override public void close(){RelayTransport.this.close();}
    };}
    private void send(int slot,byte[] bytes,int offset,int count) throws IOException {
        if(closed||socket==null)throw new IOException("连接已关闭");
        if(socket.queueSize()>262144)throw new IOException("网络繁忙，请重新加入");
        byte[] packet=new byte[count+(host?1:0)];if(host)packet[0]=(byte)slot;
        System.arraycopy(bytes,offset,packet,host?1:0,count);
        if(!socket.send(ByteString.of(packet)))throw new IOException("无法发送战斗指令");
    }
    private void fail(IOException problem){failure=problem;close();}
    @Override public synchronized void close(){
        if(closed)return;closed=true;connected.countDown();ticker.shutdownNow();
        for(Pipe p:streams.values())p.end();streams.clear();arrivals.clear();arrivals.offer(END);
        if(socket!=null)socket.cancel();
    }
    private static final class Pipe implements GameTransport.Connection {
        private final RelayTransport owner;private final int slot;
        private final ArrayDeque<byte[]> chunks=new ArrayDeque<>();private int buffered,index;private byte[] current;private boolean ended;
        Pipe(RelayTransport owner,int slot){this.owner=owner;this.slot=slot;}
        synchronized boolean offer(byte[] data){if(ended)return true;if(buffered+data.length>262144)return false;chunks.add(data);buffered+=data.length;notifyAll();return true;}
        synchronized void end(){ended=true;notifyAll();}
        private synchronized int readBytes(byte[] bytes,int off,int len)throws IOException{
            if(len==0)return 0;
            while(current==null||index==current.length){
                current=chunks.poll();index=0;if(current!=null)continue;if(ended)return -1;
                try{wait();}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException(e);}
            }
            int n=Math.min(len,current.length-index);System.arraycopy(current,index,bytes,off,n);index+=n;buffered-=n;return n;
        }
        private final InputStream input=new InputStream(){
            @Override public int read()throws IOException{byte[] b=new byte[1];return readBytes(b,0,1)<0?-1:b[0]&255;}
            @Override public int read(byte[] b,int off,int len)throws IOException{return readBytes(b,off,len);}
        };
        private final OutputStream output=new OutputStream(){
            @Override public void write(int b)throws IOException{write(new byte[]{(byte)b});}
            @Override public void write(byte[] b,int off,int len)throws IOException{owner.send(slot,b,off,len);}
        };
        @Override public InputStream input(){return input;}
        @Override public OutputStream output(){return output;}
        @Override public void close(){end();if(owner!=null&&!owner.closed){if(owner.host)owner.socket.send("{\"type\":\"close\",\"slot\":"+slot+"}");else owner.close();}}
    }
}
