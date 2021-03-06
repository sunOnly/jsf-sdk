/**
 * Copyright 2004-2048 .
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ipd.jsf.gd.transport;

import com.ipd.jsf.gd.util.Constants;
import com.ipd.jsf.gd.msg.ConnectListener;
import com.ipd.jsf.gd.util.NamedThreadFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Title: <br>
 * <p/>
 * Description: <br>
 * <p/>
 */
public class ServerTransportConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerTransportConfig.class);

    private int port =20220;
    private String host = "localhost";
    private String contextPath;

    private int BACKLOG = 35536;
    private Constants.ProtocolType protocolType = Constants.DEFAULT_PROTOCOL_TYPE;
    private boolean REUSEADDR = true;
    private boolean KEEPALIVE = true;
    private boolean TCPNODELAY = true;
    private int CONNECTTIMEOUT = 5000;
    private int TIMEOUT = 2000;//server side timeout config default ..
    private int serverBusinessPoolSize = 200; //default business pool set to 200

    private String serverBusinessPoolType = Constants.THREADPOOL_TYPE_CACHED;

    private boolean useEpoll = Boolean.FALSE;

    private String poolQueueType = Constants.QUEUE_TYPE_NORMAL;  // 队列类型
    private int poolQueueSize = 0; // 队列大小

    private int parentNioEventThreads = 0; // boss线程

    private int childNioEventThreads = 0; // worker线程==IO线程

    private List<ConnectListener> connectListeners;

    private boolean printMessage = false; // 是否debug模式打印消息体
    private int maxConnection = 100; // 最大连接数 default set to 100
    private int payload = 8 * 1024 * 1024; // 最大数据包 default set to 8M
    private int buffer; // 缓冲器大小
    private boolean telnet = true; // 是否允许telnet
    private String dispatcher; // 线程方法模型
    private boolean daemon = true; // 是否守护线程，true随主线程退出而退出，false需要主动退出
    private Map<String,String> parameters;//其他一些参数配置


    public String getServerTransportKey() {
        String key = host + ":" + port;
        return key;
    }


    private static Map<Integer,EventLoopGroup> parentGroupMap = new ConcurrentHashMap<Integer,EventLoopGroup>();
    private static Map<Integer,EventLoopGroup> childGroupMap = new ConcurrentHashMap<Integer,EventLoopGroup>();


    private synchronized EventLoopGroup initParentEventLoopGroup(){
        int threads;
        if(parentNioEventThreads == 0){
            threads = Math.max(4, Constants.CPU_CORES / 2);
        }else{
            threads = parentNioEventThreads;
        }
        NamedThreadFactory threadName = new NamedThreadFactory("JSF-SEV-BOSS", isDaemon());
        EventLoopGroup eventLoopGroup;
        if(this.isUseEpoll()){
            eventLoopGroup = new EpollEventLoopGroup(threads,threadName);
        }else{
            eventLoopGroup = new NioEventLoopGroup(threads,threadName);
        }

        return eventLoopGroup;
    }

    private synchronized EventLoopGroup initChildEventLoopGroup(){
        int threads = childNioEventThreads > 0 ?
                childNioEventThreads : // 外部配置了，传入为准
                Math.max(8, Constants.DEFAULT_IO_THREADS); // 默认cpu+1,至少8个
        NamedThreadFactory threadName = new NamedThreadFactory("JSF-SEV-WORKER", isDaemon());
        EventLoopGroup eventLoopGroup = null;
        if(this.isUseEpoll()){
            eventLoopGroup = new EpollEventLoopGroup(threads,threadName);
        }else{
            eventLoopGroup = new NioEventLoopGroup(threads,threadName);
        }
        return eventLoopGroup;
    }

    /*
     *same eventloop for different port with same protocol?
     */
    public EventLoopGroup getParentEventLoopGroup(){
        EventLoopGroup parent = parentGroupMap.get(protocolType.value()) ;
        if(parent == null){
            parent = initParentEventLoopGroup();
            parentGroupMap.put(protocolType.value(),parent);
        }
        return parent;
    }

    public EventLoopGroup getChildEventLoopGroup(){
        EventLoopGroup child = childGroupMap.get(protocolType.value()) ;
        if(child == null){
            child = initChildEventLoopGroup();
            childGroupMap.put(protocolType.value(),child);
        }
        return child;
    }


    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Constants.ProtocolType getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(Constants.ProtocolType protocolType) {
        this.protocolType = protocolType;
    }

    public int getBACKLOG() {
        return BACKLOG;
    }

    public void setBACKLOG(int BACKLOG) {
        this.BACKLOG = BACKLOG;
    }

    public boolean isREUSEADDR() {
        return REUSEADDR;
    }

    public void setREUSEADDR(boolean REUSEADDR) {
        this.REUSEADDR = REUSEADDR;
    }

    public boolean isKEEPALIVE() {
        return KEEPALIVE;
    }

    public void setKEEPALIVE(boolean KEEPALIVE) {
        this.KEEPALIVE = KEEPALIVE;
    }

    public boolean isTCPNODELAY() {
        return TCPNODELAY;
    }

    public void setTCPNODELAY(boolean TCPNODELAY) {
        this.TCPNODELAY = TCPNODELAY;
    }

    public int getCONNECTTIMEOUT() {
        return CONNECTTIMEOUT;
    }

    public void setCONNECTTIMEOUT(int CONNECTTIMEOUT) {
        this.CONNECTTIMEOUT = CONNECTTIMEOUT;
    }

    public int getTIMEOUT() {
        return TIMEOUT;
    }

    public void setTIMEOUT(int TIMEOUT) {
        this.TIMEOUT = TIMEOUT;
    }

    public int getParentNioEventThreads() {
        return parentNioEventThreads;
    }

    public void setParentNioEventThreads(int parentNioEventThreads) {
        this.parentNioEventThreads = parentNioEventThreads;
    }

    public int getChildNioEventThreads() {
        return childNioEventThreads;
    }

    public void setChildNioEventThreads(int childNioEventThreads) {
        this.childNioEventThreads = childNioEventThreads;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public int getServerBusinessPoolSize() {
        return serverBusinessPoolSize;
    }

    public void setServerBusinessPoolSize(int serverBusinessPoolSize) {
        this.serverBusinessPoolSize = serverBusinessPoolSize;
    }

    public String getServerBusinessPoolType() {
        return serverBusinessPoolType;
    }

    public void setServerBusinessPoolType(String serverBusinessPoolType) {
        this.serverBusinessPoolType = serverBusinessPoolType;
    }

    public boolean isUseEpoll() {

        return (useEpoll); //default enable Epoll on linux platform
    }

    public void setUseEpoll(boolean useEpoll) {
        this.useEpoll = useEpoll;
    }

    public String getPoolQueueType() {
        return poolQueueType;
    }

    public void setPoolQueueType(String poolQueueType) {
        this.poolQueueType = poolQueueType;
    }

    public void setPrintMessage(boolean printMessage) {
        this.printMessage = printMessage;
    }

    public boolean isPrintMessage() {
        return printMessage;
    }

    public List<ConnectListener> getConnectListeners() {
        return connectListeners;
    }

    public void setConnectListeners(List<ConnectListener> connectListeners) {
        this.connectListeners = connectListeners;
    }

    public int getPoolQueueSize() {
        return poolQueueSize;
    }

    public void setPoolQueueSize(int poolQueueSize) {
        this.poolQueueSize = poolQueueSize;
    }

    public int getMaxConnection() {
        return maxConnection;
    }

    public void setMaxConnection(int maxConnection) {
        this.maxConnection = maxConnection;
    }

    public int getPayload() {
        return payload;
    }

    public void setPayload(int payload) {
        this.payload = payload;
    }

    public int getBuffer() {
        return buffer;
    }

    public void setBuffer(int buffer) {
        this.buffer = buffer;
    }

    public boolean isTelnet() {
        return telnet;
    }

    public void setTelnet(boolean telnet) {
        this.telnet = telnet;
    }

    public String getDispatcher() {
        return dispatcher;
    }

    public void setDispatcher(String dispatcher) {
        this.dispatcher = dispatcher;
    }

    public void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }

    public boolean isDaemon() {
        return daemon;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }
}