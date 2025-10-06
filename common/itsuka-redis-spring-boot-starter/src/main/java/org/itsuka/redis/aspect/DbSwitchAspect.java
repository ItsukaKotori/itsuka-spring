package org.itsuka.redis.aspect;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.itsuka.core.utils.JacksonDeepCopyer;
import org.itsuka.redis.ConnectionFactoryHolder;
import org.itsuka.redis.annotation.DbSwitch;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Aspect
@RequiredArgsConstructor
public class DbSwitchAspect {
    private final RedisConnectionFactory redisConnectionFactory;

    private final RedisProperties redisProperties;

    private final JacksonDeepCopyer deepCopyer;

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
        RedisProperties.ClientType clientType = redisProperties.getClientType();
        switch (clientType) {
            case LETTUCE:
                if (redisConnectionFactory instanceof LettuceConnectionFactory lettuceConnectionFactory) {
                    return createLettuceConnectionFactory(lettuceConnectionFactory, database);
                }
                break;
            case JEDIS:
                if (redisConnectionFactory instanceof JedisConnectionFactory jedisConnectionFactory) {
                    return createJedisConnectionFactory(jedisConnectionFactory, database);
                }
        }
        return redisConnectionFactory;
    }

    private LettuceConnectionFactory createLettuceConnectionFactory(LettuceConnectionFactory original, int database) {
        LettuceConnectionFactory newFactory;
        
        // 检查是否为集群配置
        if (original.getClusterConfiguration() != null) {
            // Redis集群配置 - 集群模式不支持数据库切换
            throw new UnsupportedOperationException("Database switching is not supported in Redis Cluster mode");
        }
        // 检查是否为哨兵配置
        else if (original.getSentinelConfiguration() != null) {
            // Redis哨兵配置
            RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration();
            sentinelConfig.master(original.getSentinelConfiguration().getMaster());
            original.getSentinelConfiguration().getSentinels().forEach(sentinelConfig::sentinel);
            sentinelConfig.setDatabase(database);
            if (redisProperties.getPassword() != null) {
                sentinelConfig.setPassword(redisProperties.getPassword());
            }
            if (redisProperties.getUsername() != null) {
                sentinelConfig.setUsername(redisProperties.getUsername());
            }
            newFactory = new LettuceConnectionFactory(sentinelConfig, original.getClientConfiguration());
        }
        // 单节点配置
        else {
            RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration();
            standaloneConfig.setHostName(redisProperties.getHost());
            standaloneConfig.setPort(redisProperties.getPort());
            standaloneConfig.setDatabase(database);
            if (redisProperties.getPassword() != null) {
                standaloneConfig.setPassword(redisProperties.getPassword());
            }
            if (redisProperties.getUsername() != null) {
                standaloneConfig.setUsername(redisProperties.getUsername());
            }
            newFactory = new LettuceConnectionFactory(standaloneConfig, original.getClientConfiguration());
        }
        
        newFactory.afterPropertiesSet();
        return newFactory;
    }

    private JedisConnectionFactory createJedisConnectionFactory(JedisConnectionFactory original, int database) {
        // 对于Jedis，创建新的连接工厂副本
        JedisConnectionFactory newFactory = deepCopyer.copy(original);
        
        // 检查是否为集群配置
        if (newFactory.getClusterConfiguration() != null) {
            // Redis集群配置 - 集群模式不支持数据库切换
            throw new UnsupportedOperationException("Database switching is not supported in Redis Cluster mode");
        }
        // 检查是否为哨兵配置
        else if (newFactory.getSentinelConfiguration() != null) {
            newFactory.getSentinelConfiguration().setDatabase(database);
        }
        // 单节点配置
        else if (newFactory.getStandaloneConfiguration() != null) {
            newFactory.getStandaloneConfiguration().setDatabase(database);
        }
        
        newFactory.afterPropertiesSet();
        return newFactory;
    }
}
