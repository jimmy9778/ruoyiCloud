//package com.ruoyi.gateway.util;
//
//import net.spy.memcached.MemcachedClient;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
//import javax.annotation.Resource;
//import java.io.IOException;
//import java.net.InetSocketAddress;
//
//@Component
//public class MemCacheRunner implements CommandLineRunner {
////    protected Logger logger =  LoggerFactory.getLogger(this.getClass());
//
//    @Resource
//    private  MemCacheSource memcacheSource;
//
//    private MemcachedClient client = null;
//
//    @Override
//    public void run(String... args) throws Exception {
//        try {
//            client = new MemcachedClient(new InetSocketAddress(memcacheSource.getIp(),memcacheSource.getPort()));
//        } catch (IOException e) {
////            logger.error("inint MemcachedClient failed ",e);
//        }
//    }
//
//    public MemcachedClient getClient() {
//        return client;
//    }
//
//}