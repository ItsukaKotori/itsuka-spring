package org.itsuka.redis.aspect;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.itsuka.redis.ConnectionFactoryHolder;
import org.itsuka.redis.annotation.DbSwitch;
import org.itsuka.web.utils.JacksonDeepCopyer;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Aspect
@RequiredArgsConstructor
public class DbSwitchAspect {
    private final RedisConnectionFactory redisConnectionFactory;

    private final RedisProperties redisProperties;

    @Pointcut("@annotation(org.itsuka.redis.annotation.DbSwitch)")
    public void dbSwitchAspect() {}

    @Around("dbSwitchAspect()")
    public Object doDbSwitch(ProceedingJoinPoint pjp) {
        Signature signature = pjp.getSignature();
        if (!(signature instanceof MethodSignature methodSignature)) {
            throw new AssertionError();
        }
        DbSwitch dbSwitch = methodSignature.getMethod().getAnnotation(DbSwitch.class);
        ConnectionFactoryHolder.set(connectionFactoryWithSpecifiedDb(dbSwitch.database()));
        try {
            return pjp.proceed();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            ConnectionFactoryHolder.remove();
        }
    }

    private RedisConnectionFactory connectionFactoryWithSpecifiedDb(int database) {
        RedisConnectionFactory connectionFactory = JacksonDeepCopyer.copy(redisConnectionFactory);
        RedisProperties.ClientType clientType = redisProperties.getClientType();
        switch (clientType) {
            case LETTUCE:
                if (connectionFactory instanceof LettuceConnectionFactory lettuceConnectionFactory) {
                    lettuceConnectionFactory.setDatabase(database);
                    lettuceConnectionFactory.afterPropertiesSet();
                }
                break;
            case JEDIS:
                if (connectionFactory instanceof JedisConnectionFactory jedisConnectionFactory) {
                    if (jedisConnectionFactory.getSentinelConfiguration() != null) {
                        jedisConnectionFactory.getSentinelConfiguration().setDatabase(database);
                    }
                    if (jedisConnectionFactory.getStandaloneConfiguration() != null) {
                        jedisConnectionFactory.getStandaloneConfiguration().setDatabase(database);
                    }
                    jedisConnectionFactory.afterPropertiesSet();
                }
        }
        return connectionFactory;
    }
}
