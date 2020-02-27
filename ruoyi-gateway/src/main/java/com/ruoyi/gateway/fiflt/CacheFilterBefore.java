package com.ruoyi.gateway.fiflt;

import com.ruoyi.common.constant.Constants;
import com.ruoyi.gateway.config.UrlProperties;
import com.ruoyi.gateway.util.MemCacheRunner;
import lombok.extern.slf4j.Slf4j;
import net.spy.memcached.MemcachedClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * 网关服务缓存
 */
@Slf4j
@Component
public class CacheFilterBefore implements GlobalFilter, Ordered {
//    @Autowired
//    private CacheUtil ops;
//    @Resource(name = "stringRedisTemplate")
//    private ValueOperations<String, String> ops;

    @Autowired
    MemCacheRunner memCacheRunner;
    @Autowired
    private UrlProperties urlProperties;
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        MemcachedClient ops = memCacheRunner.getClient();
        String url = exchange.getRequest().getURI().getPath();
        log.info("url:{}", url);
        ServerHttpRequest originalRequest = exchange.getRequest();
        String urlPath = originalRequest.getURI().getPath();
        String cacheUrl = urlProperties.getCacheUrl();
        String[] cacheUrls = cacheUrl.split(",");
        if(Arrays.asList(cacheUrls).contains(urlPath)){
            String json = ops.get(urlPath)!=null?ops.get(urlPath).toString():null;
            if(json != null ){
                log.info("the request has cache, response");
                return setOKResponse(exchange,json);
            }
        }
        return chain.filter(exchange);
    }

    private Mono<Void> setOKResponse(ServerWebExchange exchange, String msg) {
        ServerHttpResponse originalResponse = exchange.getResponse();
        originalResponse.setStatusCode(HttpStatus.OK);
        originalResponse.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        byte[] response = null;
        try {
            response = msg.getBytes(Constants.UTF8);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        DataBuffer buffer = originalResponse.bufferFactory().wrap(response);
        return originalResponse.writeWith(Flux.just(buffer));
    }
    @Override
    public int getOrder() {
        return -200;
    }
}