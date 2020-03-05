package com.ruoyi.gateway.util;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.support.BodyInserterContext;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public abstract class ModifyBody {

	private Mono<Void> defaultMethod(ServerWebExchange exchange, GatewayFilterChain chain, int ruleNum){
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
	public String modifyBody(String paramStr,int ruleNum){
		verifySignature();
		return paramStr;
	}

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
	private Mono processError(String message) {
		return Mono.error(new Exception(message));
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
}