package com.ruoyi.gateway.fiflt;

import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.gateway.util.MyCachedBodyOutputMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.BodyInserterContext;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;

/**
 * 网关服务缓存
 */
@Slf4j
@Component
public class EsRequestFilter implements GlobalFilter, Ordered {
//    @Resource(name = "stringRedisTemplate")
//    private ValueOperations<String, String> ops;
//    @Autowired
//    private UrlProperties urlProperties;
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String url = exchange.getRequest().getURI().getPath();
        log.info("url:{}", url);
        ServerHttpRequest originalRequest = exchange.getRequest();

        String urlVer = url.substring(1);
        urlVer = urlVer.substring(0, urlVer.indexOf("/"));
        if(urlVer.equals(Constants.ES)){
            Object ruleObject = originalRequest.getHeaders().getFirst(Constants.ES_RULE);
            //走指定的规则
            int ruleNum = 0;
            if(ruleObject != null){
                ruleNum = Integer.parseInt(ruleObject.toString());
            }
            return defaultMethod(exchange,chain, ruleNum);
        }
        return chain.filter(exchange);
    }



    private Mono<Void> defaultMethod(ServerWebExchange exchange,GatewayFilterChain chain, int ruleNum){
        ServerRequest serverRequest = ServerRequest.create(exchange, HandlerStrategies.withDefaults().messageReaders());
        MediaType mediaType = exchange.getRequest().getHeaders().getContentType();
        //重点 修改body中的json数据，可以进行参数的验证，修改，格式话等
        Mono<String> modifiedBody = serverRequest.bodyToMono(String.class).flatMap(body -> {
            //因为约定了终端传参的格式，所以只考虑json的情况，如果是表单传参，请自行发挥
            if (MediaType.APPLICATION_JSON.isCompatibleWith(mediaType) || MediaType.APPLICATION_JSON_UTF8.isCompatibleWith(mediaType)) {
//                JSONObject jsonObject = JSONObject.parseObject(body);
//                String paramStr = jsonObject.getString("param");
                String newBody;
                try{
                    newBody = modifyBody(body,ruleNum);
                }catch (Exception e){
                    return processError(e.getMessage());
                }
                return Mono.just(newBody);
            }
            return Mono.empty();
        });
        BodyInserter bodyInserter = BodyInserters.fromPublisher(modifiedBody, String.class);
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(exchange.getRequest().getHeaders());
        //重新写content length
        headers.remove("Content-Length");
        //MyCachedBodyOutputMessage 这个类完全就是CachedBodyOutputMessage，只不过CachedBodyOutputMessage不是公共的
        MyCachedBodyOutputMessage outputMessage = new MyCachedBodyOutputMessage(exchange, headers);
        return bodyInserter.insert(outputMessage, new BodyInserterContext()).then(Mono.defer(() -> {
            ServerHttpRequest decorator = this.decorate(exchange, headers, outputMessage);
            return chain.filter(exchange.mutate().request(decorator).build());
        }));
    }

    /**
     * 修改body总的方法
     * @param paramStr
     * @param ruleNum
     * @return
     * @throws Exception
     */
    private String modifyBody(String paramStr, int ruleNum) throws Exception{
        paramStr = verifySignature(paramStr);
        switch (ruleNum){
            case 1 : return case1(paramStr);
            default: return defaultCase(paramStr);
        }
    }

    /**
     * 默认修改的规则，新增规则 新增一个方法不要动默认的
     * @param paramStr
     * @return
     */
    private String defaultCase(String paramStr){
        return paramStr;
    }

    /**
     * 默认修改的规则，新增规则 新增一个方法不要动默认的
     * @param paramStr
     * @return
     */
    private String case1(String paramStr){
        JSONObject jsonObject = JSONObject.parseObject(paramStr);
        jsonObject.put("addKey","key");
        return jsonObject.toJSONString();
    }

    /**
     * 可以进行加解密 和对签名的判断
     * @param paramStr
     * @return
     * @throws Exception
     */
    private String verifySignature(String paramStr) throws Exception{
        log.info("密文{}", paramStr);
        String dParamStr;
        try{
//            dParamStr = AESUtil.decrypt(paramStr, AES_SECURTY);
        }catch (Exception e){
            throw new Exception("解密失败！");
        }
//        log.info("解密得到字符串{}", dParamStr);
        return paramStr;
    }


    private Mono processError(String message) {
        log.error(message);
        return Mono.error(new Exception(message));
    }

    private Mono<Void> setResponse(ServerWebExchange exchange, String msg) {
        ServerHttpResponse originalResponse = exchange.getResponse();
        originalResponse.setStatusCode(HttpStatus.FORBIDDEN);
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

    ServerHttpRequestDecorator decorate(ServerWebExchange exchange, HttpHeaders headers, MyCachedBodyOutputMessage outputMessage) {
        return new ServerHttpRequestDecorator(exchange.getRequest()) {
            public HttpHeaders getHeaders() {
                long contentLength = headers.getContentLength();
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.putAll(super.getHeaders());
                if (contentLength > 0L) {
                    httpHeaders.setContentLength(contentLength);
                } else {
                    httpHeaders.set("Transfer-Encoding", "chunked");
                }
                return httpHeaders;
            }
            public Flux<DataBuffer> getBody() {
                return outputMessage.getBody();
            }
        };
    }


    @Override
    public int getOrder() {
        return -200;
    }
}