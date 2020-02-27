//package com.ruoyi.gateway.util;
//import net.sf.ehcache.Cache;
//import net.sf.ehcache.CacheManager;
//import net.sf.ehcache.Element;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Lazy;
//import org.springframework.stereotype.Component;
//
///**
// * EhCache 缓存工具类
// * 使用String格式的value
// *
// * @author zp
// */
//@Component
//public class CacheUtil {
//
//	@Autowired
//	@Lazy
//	private CacheManager cacheManager;
//
//	// 默认的缓存存在时间（秒）
//	private static final int DEFAULT_LIVE_SECOND = 20 * 60;
//	private static final String PROJECT = "project";
//
//	/**
//	 * 添加缓存
//	 * @param key
//	 * @param value
//	 * @param timeToLiveSeconds 缓存生存时间（秒）
//	 */
//	public void set(String key,String value,int timeToLiveSeconds){
//		Cache cache = cacheManager.getCache(PROJECT);
//		Element element = new Element(
//				key, value,
//				0,// timeToIdleSeconds=0
//				timeToLiveSeconds);
//		cache.put(element);
//	}
//
//	/**
//	 * 添加缓存
//	 * 使用默认生存时间 {@link }
//	 * @param key
//	 * @param value
//	 */
//	public void set(String key,String value){
//		Cache cache = cacheManager.getCache(PROJECT);
//		Element element = new Element(
//				key, value,
//				0,// timeToIdleSeconds
//				DEFAULT_LIVE_SECOND);
//		cache.put(element);
//	}
//
//	/**
//	 * 添加缓存
//	 * @param key
//	 * @param value
//	 * @param timeToIdleSeconds 对象空闲时间，指对象在多长时间没有被访问就会失效。
//	 *                          只对eternal为false的有效。传入0，表示一直可以访问。以秒为单位。
//	 * @param timeToLiveSeconds 缓存生存时间（秒）
//	 *                          只对eternal为false的有效
//	 */
//	public void set(String key,String value,int timeToIdleSeconds, int timeToLiveSeconds){
//		Cache cache = cacheManager.getCache(PROJECT);
//		if(cache == null){
//			cacheManager.addCache(PROJECT);
//			cache = cacheManager.getCache(PROJECT);
//		}
//		Element element = new Element(
//				key, value,
//				timeToIdleSeconds,
//				timeToLiveSeconds);
//		cache.put(element);
//	}
//
//	/**
//	 * 获取缓存
//	 * @param key
//	 * @return
//	 */
//	public String get(String key){
//		Cache cache = cacheManager.getCache(PROJECT);
//		if(cache == null){
//			cacheManager.addCache(PROJECT);
//			cache = cacheManager.getCache(PROJECT);
//		}
//		Element element = cache.get(key);
//		if(element == null){
//			return null;
//		}
//		return (String) element.getObjectValue();
//	}
//
//}