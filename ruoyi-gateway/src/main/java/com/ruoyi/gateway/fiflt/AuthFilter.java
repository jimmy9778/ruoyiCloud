package com.ruoyi.gateway.fiflt;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.gateway.config.UrlProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

/**
 * 网关鉴权
 */
@Slf4j
@Component
@EnableConfigurationProperties(UrlProperties.class)
public class AuthFilter implements GlobalFilter, Ordered {
	@Autowired
	UrlProperties urlProperties;


	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		String urlTokenList = urlProperties.getUrl();

		String url = exchange.getRequest().getURI().getPath();
		log.info("url:{}", url);
		JSONObject jsonObject = JSONObject.parseObject(urlTokenList);
		//演示用，后边换到从heads或者cookie中获取
		String token = exchange.getRequest().getHeaders().getFirst(Constants.URLTOKEN);
		String urlVer = url.substring(1);
		urlVer = urlVer.substring(0, urlVer.indexOf("/"));
		// 跳过不需要验证的路径

		if (jsonObject.containsKey(urlVer)) {
			String apiKey = jsonObject.get(urlVer).toString();
			List<String> stringList  = Arrays.asList(apiKey.split(","));
			boolean match = stringList.stream().anyMatch((api) -> api.equals(token));
			return match?chain.filter(exchange) : setUnauthorizedResponse(exchange, "token can't null or empty string");
		} else {
			return chain.filter(exchange);
		}
	}

	private Mono<Void> setUnauthorizedResponse(ServerWebExchange exchange, String msg) {
		ServerHttpResponse originalResponse = exchange.getResponse();
		originalResponse.setStatusCode(HttpStatus.UNAUTHORIZED);
		originalResponse.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
		byte[] response = null;
		try {
			response = JSON.toJSONString(R.error(401, msg)).getBytes(Constants.UTF8);
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