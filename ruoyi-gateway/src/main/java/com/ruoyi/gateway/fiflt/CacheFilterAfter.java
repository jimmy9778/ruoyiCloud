package com.ruoyi.gateway.fiflt;

import com.alibaba.fastjson.JSONObject;
import com.ruoyi.gateway.config.UrlProperties;
import com.ruoyi.gateway.util.MemCacheRunner;
import lombok.extern.slf4j.Slf4j;
import net.spy.memcached.MemcachedClient;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * 网关服务缓存
 */
@Slf4j
@Component
public class CacheFilterAfter implements GlobalFilter, Ordered {
//    @Resource(name = "stringRedisTemplate")
//    private ValueOperations<String, String> ops;

    @Autowired
    MemCacheRunner memCacheRunner;
//    @Autowired
//    private CacheUtil ops;
    @Autowired
    private UrlProperties urlProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        MemcachedClient ops = memCacheRunner.getClient();
        ServerHttpRequest originalRequest = exchange.getRequest();
        String urlPath = originalRequest.getURI().getPath();
        String cacheUrl = urlProperties.getCacheUrl();
        String[] cacheUrls = cacheUrl.split(",");
        //只要请求地址在 缓存目录中，就要存放到缓存中
        if(Arrays.asList(cacheUrls).contains(urlPath)){
            ServerHttpResponse originalResponse = exchange.getResponse();
            DataBufferFactory bufferFactory = originalResponse.bufferFactory();
            ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
                @Override
                public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                    if (body instanceof Flux) {
                        Flux<? extends DataBuffer> fluxBody = (Flux<? extends DataBuffer>) body;
                        return super.writeWith(fluxBody.map(dataBuffer -> {
                            // probably should reuse buffers
                            byte[] content = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(content);
                            //释放掉内存
                            DataBufferUtils.release(dataBuffer);
                            String json = new String(content, Charset.forName("UTF-8"));
                            JSONObject jsonObject = JSONObject.parseObject(json);
                            if(jsonObject.get("code").toString().equals("200")){
                                ops.set(urlPath,30000,json);
                            }
                            //TODO，s就是response的值，想修改、查看就随意而为了
                            byte[] uppedContent = new String(content, Charset.forName("UTF-8")).getBytes();
                            return bufferFactory.wrap(uppedContent);
                        }));
                    }
                    // if body is not a flux. never got there.
                    return super.writeWith(body);
                }
            };
            // replace response with decorator
            log.info("the response text has cache");
            return chain.filter(exchange.mutate().response(decoratedResponse).build());
        }else{
            return chain.filter(exchange);
        }
    }


    @Override
    public int getOrder() {
        return -2;
    }
}