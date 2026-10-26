package com.monef;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
@Component
class RateLimiter {
 private final StringRedisTemplate redis;private final boolean enabled;private final Clock clock;
 private record Window(Instant expires,int count) {}
 private final Map<String,Window> windows=new ConcurrentHashMap<>();
 private final DefaultRedisScript<Long> script=new DefaultRedisScript<>("local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],ARGV[1]); end; return n",Long.class);
 RateLimiter(StringRedisTemplate redis,@Value("${monef.redis-enabled}") boolean enabled,Clock clock){this.redis=redis;this.enabled=enabled;this.clock=clock;}
 void check(String key,int max,int seconds) {
  long count;
  if(enabled){try{Long n=redis.execute(script,List.of("monef:limit:"+key),String.valueOf(seconds));if(n==null)throw new IllegalStateException();count=n;}catch(Exception e){throw new ApiException(503,"限流服务暂时不可用，请稍后重试");}}
  else {var now=clock.instant();if(windows.size()>1000)windows.entrySet().removeIf(e->!e.getValue().expires().isAfter(now));var w=windows.compute(key,(k,v)->v==null||!v.expires().isAfter(now)?new Window(now.plusSeconds(seconds),1):new Window(v.expires(),v.count()+1));count=w.count();}
  if(count>max)throw new ApiException(429,"请求过于频繁，请稍后重试");
 }
}
