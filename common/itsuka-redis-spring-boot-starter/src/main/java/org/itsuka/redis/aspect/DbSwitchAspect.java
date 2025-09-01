package org.itsuka.redis.aspect;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.itsuka.redis.annotation.DbSwitch;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Aspect
@RequiredArgsConstructor
public class DbSwitchAspect {
    private final RedisConnectionFactory redisConnectionFactory;

    @Pointcut("@annotation(org.itsuka.redis.annotation.DbSwitch)")
    public void dbSwitchAspect() {}

    @Around("dbSwitchAspect()")
    public Object doDbSwitch(ProceedingJoinPoint pjp) {
        Signature signature = pjp.getSignature();
        if (!(signature instanceof MethodSignature methodSignature)) {
            throw new AssertionError();
        }
        DbSwitch dbSwitch = methodSignature.getMethod().getAnnotation(DbSwitch.class);
        int originalDatabase = 0;
        try {
            originalDatabase = setDatabase(dbSwitch.database());
            return pjp.proceed();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            setDatabase(originalDatabase);
        }
    }

    private int setDatabase(int database) {
        int originalDatabase;
        if (redisConnectionFactory instanceof LettuceConnectionFactory lettuceConnectionFactory) {
            originalDatabase = lettuceConnectionFactory.getDatabase();
            lettuceConnectionFactory.setDatabase(database);
            lettuceConnectionFactory.afterPropertiesSet();
        } else if (redisConnectionFactory instanceof JedisConnectionFactory jedisConnectionFactory) {
            originalDatabase = jedisConnectionFactory.getDatabase();
            if (jedisConnectionFactory.getSentinelConfiguration() != null) {
                jedisConnectionFactory.getSentinelConfiguration().setDatabase(database);
            }
            if (jedisConnectionFactory.getStandaloneConfiguration() != null) {
                jedisConnectionFactory.getStandaloneConfiguration().setDatabase(database);
            }
            jedisConnectionFactory.afterPropertiesSet();
        } else {
            throw new AssertionError();
        }
        return originalDatabase;
    }
}
