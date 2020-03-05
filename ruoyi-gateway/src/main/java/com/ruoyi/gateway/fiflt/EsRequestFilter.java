package com.ruoyi.gateway.fiflt;

import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.gateway.util.ModifyBody;
import lombok.extern.slf4j.Slf4j;
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

/**
 * 网关服务缓存
 */
@Slf4j
@Component
public class EsRequestFilter extends ModifyBody implements GlobalFilter, Ordered {

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

    /**
     * 修改body总的方法
     * @param paramStr
     * @param ruleNum
     * @return
     * @throws Exception
     */
    @Override
    public String modifyBody(String paramStr, int ruleNum) throws Exception{
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
    @Override
    public String verifySignature(String paramStr) throws Exception{
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




    @Override
    public int getOrder() {
        return -200;
    }
}